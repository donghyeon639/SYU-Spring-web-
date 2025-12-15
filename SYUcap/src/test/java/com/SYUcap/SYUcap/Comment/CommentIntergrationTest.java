package com.SYUcap.SYUcap.Comment;

import com.SYUcap.SYUcap.Board.Board;
import com.SYUcap.SYUcap.Board.BoardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class CommentIntergrationTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BoardRepository boardRepository;

    private Board board1;
    private Board board2;

    @BeforeEach
    void setUp() {
        // 각 테스트 실행 전에 DB 초기화
        commentRepository.deleteAll();
        boardRepository.deleteAll();

        // Given
        board1 = new Board();
        board1.setCategory("게임");
        board1.setTitle("게시글 1");
        boardRepository.save(board1);

        board2 = new Board();
        board2.setCategory("스터디");
        board2.setTitle("게시글 2");
        boardRepository.save(board2);

        Comment c1 = new Comment(); c1.setBoard(board1); c1.setContent("1-1"); c1.setAuthorName("A");
        Comment c2 = new Comment(); c2.setBoard(board1); c2.setContent("1-2"); c2.setAuthorName("B");
        Comment c3 = new Comment(); c3.setBoard(board2); c3.setContent("2-1"); c3.setAuthorName("C");

        commentRepository.saveAll(List.of(c1, c2, c3));
    }

    // [R-001] ~ [R-004] 테스트 4개 모두 삭제됨

}