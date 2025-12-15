package com.SYUcap.SYUcap.Comment;

import com.SYUcap.SYUcap.Board.Board;
import com.SYUcap.SYUcap.Board.BoardRepository;
import com.SYUcap.SYUcap.User.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;

    /**
     * 댓글 생성 (로그인 유저 연동)
     */
    public void createComment(Long boardId, String content, String authorName, Users user) {
        // 유효성: boardId null 금지
        if (boardId == null) {
            throw new IllegalArgumentException("boardId가 null입니다");
        }
        // 유효성: content null 금지(정책상 DB 제약 위반으로 간주)
        if (content == null) {
            throw new DataIntegrityViolationException("content가 null입니다");
        }
        // 유효성: authorName, user null 금지(둘 다 없으면 누가 작성했는지 알 수 없음)
        if (authorName == null && user == null) {
            throw new DataIntegrityViolationException("author 정보가 없습니다");
        }

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + boardId));

        Comment comment = new Comment();
        comment.setContent(content);
        // authorName은 표시용, 로그인 유저가 있으면 우선 사용
        if (user != null && user.getUserName() != null) {
            comment.setAuthorName(user.getUserName());
        } else {
            comment.setAuthorName(authorName);
        }
        comment.setBoard(board);
        comment.setUser(user); // 댓글 작성자 정보 설정

        commentRepository.save(comment);
    }


    public List<Comment> getCommentsByBoardId(Long boardId) {
        return commentRepository.findByBoardId(boardId);
    }
}