package com.SYUcap.SYUcap.Comment;

import com.SYUcap.SYUcap.Board.Board;
import com.SYUcap.SYUcap.Board.BoardRepository;
import com.SYUcap.SYUcap.User.Users;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class CommentUnitTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private BoardRepository boardRepository;

    @Test
    @DisplayName("[S-001] [유효성] content가 null이면 DataIntegrityViolationException 발생")
    void createComment_ContentNull_Throws() {
        // Given
        Long boardId = 1L;
        // When & Then
        assertThatThrownBy(() -> commentService.createComment(boardId, null, "author", null))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(commentRepository, never()).save(any(Comment.class));
        verify(boardRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("[S-002] [정책] content가 빈 문자열이면 현재 저장 성공(정책 변경 시 예외 기대)")
    void createComment_ContentEmpty_SavesCurrentPolicy() {
        // Given
        Long boardId = 1L;
        Board fakeBoard = new Board();
        fakeBoard.setId(boardId);
        given(boardRepository.findById(boardId)).willReturn(java.util.Optional.of(fakeBoard));
        org.mockito.ArgumentCaptor<Comment> captor = org.mockito.ArgumentCaptor.forClass(Comment.class);

        // When: 로그인 유저 없음
        commentService.createComment(boardId, "", "author", null);

        // Then
        verify(commentRepository, times(1)).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getContent()).isEqualTo("");
    }

    @Test
    @DisplayName("[S-003] [정책] content가 공백이면 현재 저장 성공(정책 변경 시 예외 기대)")
    void createComment_ContentBlank_SavesCurrentPolicy() {
        // Given
        Long boardId = 1L;
        Board fakeBoard = new Board();
        fakeBoard.setId(boardId);
        given(boardRepository.findById(boardId)).willReturn(java.util.Optional.of(fakeBoard));
        org.mockito.ArgumentCaptor<Comment> captor = org.mockito.ArgumentCaptor.forClass(Comment.class);

        // When: 로그인 유저 없음
        commentService.createComment(boardId, " ", "author", null);

        // Then
        verify(commentRepository, times(1)).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getContent()).isEqualTo(" ");
    }

    @Test
    @DisplayName("[S-004] [유효성] authorName이 null이면 DataIntegrityViolationException 발생")
    void createComment_AuthorNull_Throws() {
        // Given
        Long boardId = 1L;
        // When & Then (로그인 유저도 없음)
        assertThatThrownBy(() -> commentService.createComment(boardId, "댓글", null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(commentRepository, never()).save(any(Comment.class));
        verify(boardRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("[S-005] [유효성] boardId가 null이면 IllegalArgumentException 발생")
    void createComment_BoardIdNull_Throws() {
        // Given
        Long boardId = null;
        // When & Then
        assertThatThrownBy(() -> commentService.createComment(boardId, "댓글", "author", null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(commentRepository, never()).save(any(Comment.class));
        verify(boardRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("[S-006] [실패] 존재하지 않는 boardId로 생성")
    void createComment_Fail_BoardNotFound() {
        // Given
        Long boardId = 999L;
        given(boardRepository.findById(boardId)).willReturn(java.util.Optional.empty());
        // When & Then
        assertThatThrownBy(() -> commentService.createComment(boardId, "content", "author", null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("[S-007] [경계] content가 DB TEXT 최대치 초과 (시뮬레이션)")
    void createComment_Fail_ContentTooLong() {
        // Given
        Long boardId = 1L;
        String longContent = "a".repeat(70000);
        Board fakeBoard = new Board();
        fakeBoard.setId(boardId);
        given(boardRepository.findById(boardId)).willReturn(java.util.Optional.of(fakeBoard));
        given(commentRepository.save(any(Comment.class))).willThrow(org.springframework.dao.DataIntegrityViolationException.class);

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(boardId, longContent, "author", null))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("[S-008] [예외] Board 조회 시 DB 오류 발생")
    void createComment_Exception_BoardRepoFails() {
        // Given
        Long boardId = 1L;
        given(boardRepository.findById(boardId)).willThrow(new org.springframework.dao.DataAccessException("DB error") {});
        // When & Then
        assertThatThrownBy(() -> commentService.createComment(boardId, "content", "author", null))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    @DisplayName("[S-009] [예외] Comment 저장 시 DB 오류 발생")
    void createComment_Exception_CommentRepoFails() {
        // Given
        Long boardId = 1L;
        Board fakeBoard = new Board();
        fakeBoard.setId(boardId);
        given(boardRepository.findById(boardId)).willReturn(java.util.Optional.of(fakeBoard));
        given(commentRepository.save(any(Comment.class))).willThrow(new org.springframework.dao.DataAccessException("DB error") {});

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(boardId, "content", "author", null))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    @DisplayName("[S-010] [실패] 댓글 목록 조회 시 boardId가 null일 때")
    void getComments_Fail_BoardIdNull() {
        // Given
        given(commentRepository.findByBoardId(null)).willThrow(new IllegalArgumentException("ID must not be null"));
        // When & Then
        assertThatThrownBy(() -> commentService.getCommentsByBoardId(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("[S-011] [성공] 로그인 유저로 댓글 작성 시 Users 연동 및 authorName 설정")
    void createComment_WithLoginUser_SetsUserAndAuthor() {
        // Given
        Long boardId = 1L;
        Board fakeBoard = new Board();
        fakeBoard.setId(boardId);
        given(boardRepository.findById(boardId)).willReturn(java.util.Optional.of(fakeBoard));
        org.mockito.ArgumentCaptor<Comment> captor = org.mockito.ArgumentCaptor.forClass(Comment.class);
        Users loginUser = new Users();
        loginUser.setId(10L);
        loginUser.setUserName("로그인유저");

        // When
        commentService.createComment(boardId, "댓글", null, loginUser);

        // Then
        verify(commentRepository, times(1)).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getUser()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getUser().getUserName()).isEqualTo("로그인유저");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getAuthorName()).isEqualTo("로그인유저");
    }
}