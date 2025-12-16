package com.SYUcap.SYUcap.Comment;

import com.SYUcap.SYUcap.Board.Board;
import com.SYUcap.SYUcap.Board.BoardRepository;
import com.SYUcap.SYUcap.User.UserRepository;
import com.SYUcap.SYUcap.User.Users;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureTestDatabase
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class CommentIntergrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Board board1;
    private Board board2;


    @BeforeEach
    void setUp() {
        // 각 테스트 실행 전에 DB 초기화
        commentRepository.deleteAll();
        boardRepository.deleteAll();
        userRepository.deleteAll();

        // Given: 게시글 2건 저장
        board1 = new Board();
        board1.setCategory("게임");
        board1.setTitle("게시글 1");
        boardRepository.save(board1);

        board2 = new Board();
        board2.setCategory("스터디");
        board2.setTitle("게시글 2");
        boardRepository.save(board2);

        // baseUserA, baseUserB의 userId가 중복되지 않도록 매 세트마다 고유한 suffix 부여
        long existingUserCount = userRepository.count();

        Users userA = new Users();
        userA.setUserId("baseUserA_" + existingUserCount);
        userA.setUserName("작성자A");
        userA.setPassword("pass");
        userA = userRepository.save(userA);

        Users userB = new Users();
        userB.setUserId("baseUserB_" + existingUserCount);
        userB.setUserName("작성자B");
        userB.setPassword("pass");
        userB = userRepository.save(userB);

        // board1용 댓글: 반드시 Users와 연관관계를 맺어 N+1 테스트 시 NPE가 발생하지 않도록 함
        Comment c1 = new Comment();
        c1.setBoard(board1);
        c1.setContent("1-1");
        c1.setAuthorName(userA.getUserName());
        c1.setUser(userA);

        Comment c2 = new Comment();
        c2.setBoard(board1);
        c2.setContent("1-2");
        c2.setAuthorName(userB.getUserName());
        c2.setUser(userB);

        // board2 댓글은 N+1 성능 테스트와 직접 관련 없으므로 user 연관 여부는 선택 사항
        Comment c3 = new Comment();
        c3.setBoard(board2);
        c3.setContent("2-1");
        c3.setAuthorName("C");

        commentRepository.saveAll(List.of(c1, c2, c3));
    }

    @Test
    @DisplayName("[C-001] content 파라미터 누락 시 400, DB 저장 없음")
    void addComment_MissingContent_BadRequest() throws Exception {
        // Given: 게시글 존재, 현재 댓글 수 기록
        long before = commentRepository.count();

        // When: content 파라미터 없이 POST 요청
        mockMvc.perform(post("/board/게임/" + board1.getId() + "/comment"))
                // Then: 400 Bad Request
                .andExpect(status().isBadRequest());

        // And: DB 저장 없음
        long after = commentRepository.count();
        assertThat(after).isEqualTo(before);
    }

    @Test
    @DisplayName("[C-002] content 빈 문자열 전송 시 302 Redirect 및 DB 저장")
    void addComment_EmptyContent_RedirectAndSaved() throws Exception {
        // Given: 현재 댓글 수 기록
        long before = commentRepository.count();

        // When: content=""로 POST 요청
        mockMvc.perform(post("/board/게임/" + board1.getId() + "/comment")
                        .param("content", ""))
                // Then: 3xx Redirect (컨트롤러 리다이렉트 정책)
                .andExpect(status().is3xxRedirection());

        // And: DB에 1건 저장됨
        long after = commentRepository.count();
        assertThat(after).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("[R-005] 댓글 조회 N+1 성능 검증 (JOIN FETCH - 쿼리 개수 검증)")
    @Transactional
    void getComments_FetchJoin_NoAdditionalSelect() {
        // Given: 동일 게시글에 서로 다른 작성자(Users)로 연결된 댓글 10건 저장
        for (int i = 0; i < 10; i++) {
            Users u = new Users();
            u.setUserId("user" + i);
            u.setUserName("작성자" + i);
            u.setPassword("pass");
            u = userRepository.save(u);

            Comment c = new Comment();
            c.setBoard(board1);
            c.setContent("댓글" + i);
            c.setAuthorName(u.getUserName());
            c.setUser(u);
            commentRepository.save(c);
        }

        // [중요] 영속성 컨텍스트 초기화 (캐시가 아닌 실제 DB 조회를 강제하기 위함)
        em.flush();
        em.clear();

        // [1] Hibernate 통계 객체 준비 및 초기화
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear(); // 기존 쿼리 기록 삭제

        System.out.println("=== [검증 시작] 쿼리 카운트 측정 ===");

        // When: 페치 조인으로 댓글 목록 조회
        List<Comment> comments = commentRepository.findByBoardIdWithUserFetchJoin(board1.getId());

        // [2] 지연 로딩 여부 확인: 연관된 User 객체를 실제로 건드려봄
        for (Comment c : comments) {
            // 만약 Fetch Join이 실패했다면, 여기서 루프 돌 때마다 SELECT 쿼리가 발생함 (N+1)
            c.getUser().getUserName();
        }

        System.out.println("=== [검증 종료] ===");

        // Then: 실행된 쿼리 개수가 정확히 1개인지 검증
        long queryCount = statistics.getPrepareStatementCount();
        System.out.println("발생한 총 쿼리 수: " + queryCount);

        // 성공 기준: 목록 조회(JOIN FETCH) 1번만 실행되어야 함
        assertThat(queryCount).isEqualTo(1L);
    }
}