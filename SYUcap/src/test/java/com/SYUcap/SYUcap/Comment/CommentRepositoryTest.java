package com.SYUcap.SYUcap.Comment;

import com.SYUcap.SYUcap.Board.Board;
import com.SYUcap.SYUcap.Board.BoardRepository;
import com.SYUcap.SYUcap.User.UserRepository;
import com.SYUcap.SYUcap.User.Users;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("[성능] 댓글 조회 시 N+1 문제 없이 한 방 쿼리로 가져오는지 확인")
    void findByBoardId_NPlusOne_Check() {
        // 1. 게시글 1개 생성
        Board board = new Board();
        board.setTitle("테스트 게시글");
        board.setCategory("잡담");
        boardRepository.save(board);

        // 2. 댓글 10개 생성 (작성자가 모두 다름)
        // 작성자가 다를 때, 작성자 정보를 가져오기 위해 쿼리가 10번 더 나가는지 확인해야 함
        for (int i = 1; i <= 10; i++) {
            Users user = new Users();
            user.setUserId("user" + i);
            user.setPassword("pass");
            user.setUserName("유저" + i);
            userRepository.save(user);

            Comment comment = new Comment();
            comment.setContent("댓글내용" + i);
            comment.setBoard(board);
            comment.setUser(user);
            comment.setAuthorName(user.getUserName());
            commentRepository.save(comment);
        }

        // 3. 영속성 컨텍스트 비우기
        em.flush();
        em.clear();

        System.out.println("========== [쿼리 카운트 시작] ==========");

        // 4. 실행
        List<Comment> comments = commentRepository.findByBoardId(board.getId());

        // 5. [검증] 작성자 이름(User 엔티티)
        // N+1 문제가 있다면 여기서 select 쿼리가 계속 나옴ㅁ
        for (Comment comment : comments) {
            String userName = comment.getUser().getUserName();
        }

        System.out.println("========== [쿼리 카운트 종료] ==========");

        // 6. 결과 확인
        assertThat(comments).hasSize(10);
    }
}