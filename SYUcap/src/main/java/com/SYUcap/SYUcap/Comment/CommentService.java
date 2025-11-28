package com.SYUcap.SYUcap.Comment;

import com.SYUcap.SYUcap.Board.Board;
import com.SYUcap.SYUcap.Board.BoardRepository;
import com.SYUcap.SYUcap.User.UserRepository;
import com.SYUcap.SYUcap.User.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository; // [추가] 유저 조회를 위해 필요

    /**
     * 댓글 생성 (사용자 정보 저장 추가)
     */
    @Transactional
    public void createComment(Long boardId, String content, String userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음 id=" + boardId));

        // 로그인한 사용자 정보 조회
        Users user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음 id=" + userId));

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setBoard(board);

        // 작성자 정보 연결
        comment.setUser(user);
        comment.setAuthorName(user.getUserName()); // 화면 표시용 이름 저장

        commentRepository.save(comment);
    }


    @Transactional
    public void updateComment(Long commentId, String newContent, String currentUserId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글 없음 id=" + commentId));

        // [검증] 댓글 작성자와 현재 로그인한 사용자가 같은지 확인
        validateOwner(comment, currentUserId);

        comment.setContent(newContent);
    }


    @Transactional
    public void deleteComment(Long commentId, String currentUserId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글 없음 id=" + commentId));

        // [검증] 댓글 작성자와 현재 로그인한 사용자가 같은지 확인
        validateOwner(comment, currentUserId);

        commentRepository.delete(comment);
    }

    public List<Comment> getCommentsByBoardId(Long boardId) {
        return commentRepository.findByBoardId(boardId);
    }

    // [내부 메서드] 작성자 본인 확인 로직
    private void validateOwner(Comment comment, String currentUserId) {
        // 댓글에 작성자 정보가 없거나(옛날 데이터), 작성자 ID가 다르면 예외 발생
        if (comment.getUser() == null || !comment.getUser().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("작성자만 수정/삭제할 수 있습니다.");
        }
    }
}