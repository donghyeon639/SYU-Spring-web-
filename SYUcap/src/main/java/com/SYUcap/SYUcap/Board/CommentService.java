package com.SYUcap.SYUcap.Board;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository; // BoardService가 아닌 Repository가 필요합니다.

    /**
     * 댓글 생성
     * @param boardId 댓글을 달 게시글 ID
     * @param content 댓글 내용
     * @param authorName 댓글 작성자 이름
     */
    public void createComment(Long boardId, String content, String authorName) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + boardId));

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setAuthorName(authorName);
        comment.setBoard(board); // 조회한 Board 엔티티를 Comment에 연결

        commentRepository.save(comment);
    }

    /**
     * 특정 게시글의 모든 댓글 조회
     * @param boardId 조회할 게시글 ID
     * @return 댓글 리스트
     */
    public List<Comment> getCommentsByBoardId(Long boardId) {
        return commentRepository.findByBoardId(boardId);
    }
}