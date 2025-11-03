package com.SYUcap.SYUcap.Board;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private BoardRepository boardRepository;

    @Test
    @DisplayName("[S-TC-002] [실패] 존재하지 않는 게시글에 댓글 생성")
    void createComment_Fail_BoardNotFound() {
        // Given
        Long boardId = 999L;
        given(boardRepository.findById(boardId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(boardId, "content", "author"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("[S-TC-003] [실패] boardId가 null일 때")
    void createComment_Fail_BoardIdNull() {
        // Given
        // findById(null)은 IllegalArgumentException을 발생시킨다고 가정
        given(boardRepository.findById(null)).willThrow(new IllegalArgumentException("ID must not be null"));

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(null, "content", "author"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("[S-TC-004] [실패] content가 null일 때 (DB 제약)")
    void createComment_Fail_ContentNull() {
        // Given
        Long boardId = 1L;
        Board fakeBoard = new Board();
        fakeBoard.setId(boardId);
        given(boardRepository.findById(boardId)).willReturn(Optional.of(fakeBoard));

        // DB save()가 DataIntegrityViolationException을 던진다고 가정
        given(commentRepository.save(any(Comment.class))).willThrow(DataIntegrityViolationException.class);

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(boardId, null, "author"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("[S-TC-005] [정책] content가 빈 문자열(\"\")일 때")
    void createComment_Policy_ContentEmpty() {
        // Given
        Long boardId = 1L;
        Board fakeBoard = new Board();
        fakeBoard.setId(boardId);
        given(boardRepository.findById(boardId)).willReturn(Optional.of(fakeBoard));

        // When
        commentService.createComment(boardId, "", "author");

        // Then (현재 로직은 빈 문자열도 저장함)
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("[S-TC-006] [정책] content가 공백(\" \")일 때")
    void createComment_Policy_ContentBlank() {
        // Given
        Long boardId = 1L;
        Board fakeBoard = new Board();
        fakeBoard.setId(boardId);
        given(boardRepository.findById(boardId)).willReturn(Optional.of(fakeBoard));

        // When
        commentService.createComment(boardId, " ", "author");

        // Then (현재 로직은 공백도 저장함)
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("[S-TC-008] [실패] authorName이 null일 때 (DB 제약)")
    void createComment_Fail_AuthorNameNull() {
        // Given
        Long boardId = 1L;
        Board fakeBoard = new Board();
        fakeBoard.setId(boardId);
        given(boardRepository.findById(boardId)).willReturn(Optional.of(fakeBoard));

        // DB save()가 DataIntegrityViolationException을 던진다고 가정
        given(commentRepository.save(any(Comment.class))).willThrow(DataIntegrityViolationException.class);

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(boardId, "content", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("[S-TC-009] [예외] Board 조회 시 DB 오류 발생")
    void createComment_Exception_BoardRepoFails() {
        // Given
        Long boardId = 1L;
        given(boardRepository.findById(boardId)).willThrow(new DataAccessException("DB error") {});

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(boardId, "content", "author"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("[S-TC-010] [예외] Comment 저장 시 DB 오류 발생")
    void createComment_Exception_CommentRepoFails() {
        // Given
        Long boardId = 1L;
        Board fakeBoard = new Board();
        fakeBoard.setId(boardId);
        given(boardRepository.findById(boardId)).willReturn(Optional.of(fakeBoard));
        given(commentRepository.save(any(Comment.class))).willThrow(new DataAccessException("DB error") {});

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(boardId, "content", "author"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("[S-TC-013] [실패] 댓글 목록 조회 시 boardId가 null일 때")
    void getComments_Fail_BoardIdNull() {
        // Given
        // findByBoardId(null)은 IllegalArgumentException을 발생시킨다고 가정
        given(commentRepository.findByBoardId(null)).willThrow(new IllegalArgumentException("ID must not be null"));

        // When & Then
        assertThatThrownBy(() -> commentService.getCommentsByBoardId(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}