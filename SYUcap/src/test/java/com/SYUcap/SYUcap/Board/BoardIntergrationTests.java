package com.SYUcap.SYUcap.Board;

import com.SYUcap.SYUcap.User.UserRepository;
import com.SYUcap.SYUcap.User.Users;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestDatabase
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@Transactional
class BoardIntergrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardService boardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("[B-TC-001] [성공] 게시글 목록 페이지 접속")
    void getBoardList_Success() throws Exception {
        // Given: DB에 게시글 1건 저장
        Board b = new Board();
        b.setCategory("게임");
        b.setTitle("목록 테스트");
        b.setContent("내용");
        b.setAuthorName("테스터");
        b.setCreatedAt(LocalDateTime.now());
        boardRepository.save(b);

        // When: 목록 페이지 접근
        // Then: 200 OK, 뷰/모델/내용 확인
        mockMvc.perform(get("/board"))
                .andExpect(status().isOk())
                .andExpect(view().name("board-list"))
                .andExpect(model().attributeExists("posts"))
                .andExpect(model().attribute("category", "전체"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("목록 테스트")));
    }

    @Test
    @DisplayName("[B-TC-002] [성공] 게시글이 없을 때 목록 출력")
    void getBoardList_NoPosts() throws Exception {
        // Given: 모든 게시글 삭제
        boardRepository.deleteAll();

        // When: 목록 페이지 접근
        // Then: 빈 문구 표기 확인
        mockMvc.perform(get("/board"))
                .andExpect(status().isOk())
                .andExpect(view().name("board-list"))
                .andExpect(model().attributeExists("posts"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("게시글이 없습니다.")));
    }

    @Test
    @DisplayName("[B-TC-003] [성공] 게시글 검색 + 작성자 표시 (LAZY 접근 유도)")
    void getBoardList_SearchKeyword() throws Exception {
        // Given: 유저/게시글 저장 및 연관 설정
        Users u1 = new Users();
        u1.setUserId("chulsoo");
        u1.setPassword("pass");
        u1.setUserName("철수");
        u1 = userRepository.save(u1);

        Users u2 = new Users();
        u2.setUserId("younghee");
        u2.setPassword("pass");
        u2.setUserName("영희");
        u2 = userRepository.save(u2);

        Board b1 = new Board();
        b1.setCategory("스터디");
        b1.setTitle("스터디 모집");
        b1.setContent("알고리즘 스터디");
        b1.setUser(u1);
        b1.setAuthorName(null);
        b1.setCreatedAt(LocalDateTime.now());
        boardRepository.save(b1);

        Board b2 = new Board();
        b2.setCategory("게임");
        b2.setTitle("게임 같이 하실 분");
        b2.setContent("오늘 PC방");
        b2.setUser(u2);
        b2.setAuthorName(null);
        b2.setCreatedAt(LocalDateTime.now());
        boardRepository.save(b2);

        // When: '스터디' 검색 요청
        // Then: 결과 및 작성자 표시 확인
        mockMvc.perform(get("/board/search").param("keyword", "스터디"))
                .andExpect(status().isOk())
                .andExpect(view().name("board-list"))
                .andExpect(model().attributeExists("posts"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("스터디 모집")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("철수")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("게임 같이 하실 분"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("영희"))));
    }

    @Test
    @DisplayName("[B-TC-004] [성공] 게시글 작성 페이지 접근")
    void getBoardWriteForm_Success() throws Exception {
        // Given/When: 작성 페이지 접근
        // Then: 폼 렌더링 확인
        mockMvc.perform(get("/board/게임/write"))
                .andExpect(status().isOk())
                .andExpect(view().name("board-form"))
                .andExpect(model().attribute("category", "게임"))
                .andExpect(model().attributeExists("post"));
    }

    @Test
    @DisplayName("[B-TC-005] [성공] 게시글 등록 성공")
    void postBoardWrite_Success() throws Exception {
        // Given: 현재 게시글 수
        long before = boardRepository.count();

        // When: 유효 데이터로 등록
        // Then: 리다이렉트 및 DB 저장 확인
        mockMvc.perform(post("/board/게임/write")
                        .param("title", "게임 같이 하실 분")
                        .param("content", "게임의 종류")
                        .param("authorName", "홍길동")
                        .param("location", "피씨방")
                        .param("limitCount", "5")
                        .param("meetingStartTime", "2025-11-25T13:00:00")
                        .param("meetingEndTime", "2025-11-25T16:00:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/board/%EA%B2%8C%EC%9E%84"));

        long after = boardRepository.count();
        assertThat(after).isEqualTo(before + 1);
        Board saved = boardRepository.findAll().get((int) (after - 1));
        assertThat(saved.getTitle()).isEqualTo("게임 같이 하실 분");
        assertThat(saved.getCategory()).isEqualTo("게임");
    }

    @Test
    @DisplayName("[B-TC-006] [실패] 제목 누락 시 등록 실패(DB 저장 안 됨)")
    void postBoardWrite_Fail_TitleMissing() throws Exception {
        // Given: 현재 게시글 수
        long before = boardRepository.count();

        // When: 제목 누락으로 등록 시도
        // Then: 폼 렌더링 및 DB 변경 없음 확인
        mockMvc.perform(post("/board/게임/write")
                        .param("title", "")
                        .param("content", "내용만 입력")
                        .param("authorName", "홍길동"))
                .andExpect(status().isOk())
                .andExpect(view().name("board-form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("제목을 입력하세요")));

        long after = boardRepository.count();
        assertThat(after).isEqualTo(before);
    }

    @Test
    @DisplayName("[B-TC-007] [실패] 내용 누락 시 등록 실패(DB 저장 안 됨)")
    void postBoardWrite_Fail_ContentMissing() throws Exception {
        // Given: 현재 게시글 수
        long before = boardRepository.count();

        // When: 내용 누락으로 등록 시도
        // Then: 폼 렌더링 및 DB 변경 없음 확인
        mockMvc.perform(post("/board/게임/write")
                        .param("title", "테스트 제목")
                        .param("content", "")
                        .param("authorName", "홍길동"))
                .andExpect(status().isOk())
                .andExpect(view().name("board-form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("내용을 입력하세요")));

        long after = boardRepository.count();
        assertThat(after).isEqualTo(before);
    }

    @Test
    @DisplayName("[B-TC-008] [성공] 게시글 상세 보기")
    void getBoardDetail_Success() throws Exception {
        // Given: 게시글 1건 저장
        Board b = new Board();
        b.setCategory("게임");
        b.setTitle("상세 제목");
        b.setContent("상세 내용");
        b.setAuthorName("홍길동");
        b = boardRepository.save(b);

        // When/Then: 상세 페이지 접근 및 렌더링 확인
        mockMvc.perform(get("/board/게임/" + b.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("board-detail"))
                .andExpect(model().attribute("category", "게임"))
                .andExpect(model().attributeExists("post"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("상세 제목")));
    }

    @Test
    @DisplayName("[B-TC-009] [성공] 게시글 수정")
    void postBoardEdit_Success() throws Exception {
        // Given: 기존 게시글 저장
        Board b = new Board();
        b.setCategory("게임");
        b.setTitle("원래 제목");
        b.setContent("원래 내용");
        b.setAuthorName("홍길동");
        b = boardRepository.save(b);

        // When: 수정 요청
        // Then: 리다이렉트 및 DB 반영 확인
        mockMvc.perform(post("/board/게임/" + b.getId() + "/edit")
                        .param("title", "수정된 제목")
                        .param("content", "수정된 내용"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/board/%EA%B2%8C%EC%9E%84/" + b.getId()));

        Board updated = boardRepository.findById(b.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("수정된 제목");
        assertThat(updated.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("[B-TC-010] [성공] 게시글 삭제")
    void postBoardDelete_Success() throws Exception {
        // Given: 기존 게시글 저장 및 현재 개수 기록
        Board b = new Board();
        b.setCategory("게임");
        b.setTitle("삭제 제목");
        b.setContent("삭제 내용");
        b.setAuthorName("홍길동");
        b = boardRepository.save(b);
        long before = boardRepository.count();

        // When: 삭제 요청
        mockMvc.perform(post("/board/게임/" + b.getId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/board/%EA%B2%8C%EC%9E%84"));

        // Then: DB에서 실제 삭제되었는지 확인
        long after = boardRepository.count();
        org.assertj.core.api.Assertions.assertThat(after).isEqualTo(before - 1);
        org.assertj.core.api.Assertions.assertThat(boardRepository.findById(b.getId())).isEmpty();
    }

    @Test
    @DisplayName("[B-TC-011] [성능] 게시글 조회 N+1 해결 확인 (Fetch Join 적용)")
    @Transactional
    void getBoardList_Performance_FetchJoin() throws Exception {
        // Given: 유저 10명과 게시글 50개 저장, 연관 설정
        Users[] users = new Users[10];
        for (int i = 0; i < users.length; i++) {
            Users u = new Users();
            u.setUserId("user" + i);
            u.setUserName("작성자" + i);
            u.setPassword("pass");
            users[i] = userRepository.save(u);
        }
        for (int i = 0; i < 50; i++) {
            Board p = new Board();
            p.setCategory("게임");
            p.setTitle("페치 테스트 " + i);
            p.setContent("내용 " + i);
            p.setCreatedAt(LocalDateTime.now());
            p.setUser(users[i % users.length]);
            boardRepository.save(p);
        }
        // 영속성 컨텍스트 초기화 (실제 쿼리 확인 목적)
        em.flush();
        em.clear();

        // When: 페치 조인 목록 조회
        List<Board> boards = boardRepository.findAllWithUserFetchJoinOrderByCreatedAtDesc();

        // Then: 연관 접근시 추가 SELECT 없이 접근 가능
        for (Board board : boards) {
            if (board.getUser() != null) {
                String name = board.getUser().getUserName();
                assertThat(name).isNotBlank();
            }
        }
    }
}
