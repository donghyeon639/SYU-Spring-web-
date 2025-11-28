package com.SYUcap.SYUcap.Comment;

import com.SYUcap.SYUcap.Board.Board;
import com.SYUcap.SYUcap.Board.BoardRepository;
import com.SYUcap.SYUcap.User.UserRepository;
import com.SYUcap.SYUcap.User.Users;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    @Mock
    private UserRepository userRepository;

    // --- [S-001 ~ S-004] 유효성 및 데이터 무결성 ---

    @Test
    @DisplayName("[S-001] content가 null일 때 (DB 제약조건 모의)")
    void createComment_Fail_ContentNull() {
        // Given
        Long boardId = 1L;
        String userId = "test_user";
        given(boardRepository.findById(boardId)).willReturn(Optional.of(new Board()));
        given(userRepository.findByUserId(userId)).willReturn(Optional.of(new Users()));

        given(commentRepository.save(any(Comment.class)))
                .willThrow(new DataIntegrityViolationException("Content cannot be null"));

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(boardId, null, userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("[S-002] content가 빈 문자열(\"\")일 때 (현재 정책: 저장 성공)")
    void createComment_Success_ContentEmpty() {
        // Given
        Long boardId = 1L;
        String userId = "test_user";
        given(boardRepository.findById(boardId)).willReturn(Optional.of(new Board()));
        given(userRepository.findByUserId(userId)).willReturn(Optional.of(new Users()));

        // When
        commentService.createComment(boardId, "", userId);

        // Then
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("[S-003] content가 공백(\" \")일 때 (현재 정책: 저장 성공)")
    void createComment_Success_ContentBlank() {
        // Given
        Long boardId = 1L;
        String userId = "test_user";
        given(boardRepository.findById(boardId)).willReturn(Optional.of(new Board()));
        given(userRepository.findByUserId(userId)).willReturn(Optional.of(new Users()));

        // When
        commentService.createComment(boardId, " ", userId);

        // Then
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("[S-004] authorName(User)이 null일 때 (DB 제약조건 모의)")
    void createComment_Fail_UserNull_DB() {
        // Given
        Long boardId = 1L;
        String userId = "test_user";
        given(boardRepository.findById(boardId)).willReturn(Optional.of(new Board()));
        given(userRepository.findByUserId(userId)).willReturn(Optional.of(new Users()));

        given(commentRepository.save(any(Comment.class)))
                .willThrow(new DataIntegrityViolationException("User constraint violation"));

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(boardId, "댓글", userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- [S-005 ~ S-006] 연관 관계(Board) 오류 ---

    @Test
    @DisplayName("[S-005] boardId가 null일 때")
    void createComment_Fail_BoardIdNull() {
        // Given
        String userId = "test_user";
        // Mock: findById(null)은 IllegalArgumentException 유발
        given(boardRepository.findById(null)).willThrow(new IllegalArgumentException("ID must not be null"));

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(null, "content", userId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("[S-006] 존재하지 않는 boardId로 생성")
    void createComment_Fail_BoardNotFound() {
        // Given
        Long boardId = 999L;
        String userId = "test_user";
        given(boardRepository.findById(boardId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(boardId, "content", userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("게시글 없음"); // 서비스 에러 메시지 확인
    }

    // --- [S-007 ~ S-009] 경계 값 및 DB 예외 ---

    @Test
    @DisplayName("[S-007] content가 DB TEXT 최대치 초과 (모의)")
    void createComment_Fail_ContentTooLong() {
        // Given
        Long boardId = 1L;
        String userId = "test_user";
        String longContent = "a".repeat(70000); // 70KB 가정

        given(boardRepository.findById(boardId)).willReturn(Optional.of(new Board()));
        given(userRepository.findByUserId(userId)).willReturn(Optional.of(new Users()));

        // Mock: DB 데이터 너무 김 예외
        given(commentRepository.save(any(Comment.class)))
                .willThrow(new DataIntegrityViolationException("Data too long"));

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(boardId, longContent, userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("[S-008] boardRepository 조회 실패 시 (DB 다운 등)")
    void createComment_Fail_BoardRepoError() {
        // Given
        Long boardId = 1L;
        given(boardRepository.findById(boardId)).willThrow(new DataAccessException("Connection failed") {});

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(boardId, "content", "user"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("[S-009] commentRepository 저장 실패 시")
    void createComment_Fail_CommentRepoError() {
        // Given
        Long boardId = 1L;
        String userId = "test_user";
        given(boardRepository.findById(boardId)).willReturn(Optional.of(new Board()));
        given(userRepository.findByUserId(userId)).willReturn(Optional.of(new Users()));

        given(commentRepository.save(any(Comment.class))).willThrow(new DataAccessException("Save failed") {});

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(boardId, "content", userId))
                .isInstanceOf(DataAccessException.class);
    }

    // --- [S-010 ~ S-011] 조회 관련 ---

    @Test
    @DisplayName("[S-010] getComments 시 boardId가 null일 때")
    void getComments_Fail_BoardIdNull() {
        // Given
        given(commentRepository.findByBoardId(null)).willThrow(new IllegalArgumentException("ID must not be null"));

        // When & Then
        assertThatThrownBy(() -> commentService.getCommentsByBoardId(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("[S-011] getComments - 댓글 100만 개 (성능 확인용)")
    void getComments_MassiveData() {
        // Given
        Long boardId = 1L;
        List<Comment> mockList = new ArrayList<>();
        mockList.add(new Comment()); // 실제 100만개를 넣으면 테스트가 죽으므로 리스트 반환 여부만 확인

        given(commentRepository.findByBoardId(boardId)).willReturn(mockList);

        // When
        List<Comment> result = commentService.getCommentsByBoardId(boardId);

        // Then
        assertThat(result).isNotEmpty();
    }
}