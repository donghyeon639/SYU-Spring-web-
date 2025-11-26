package com.SYUcap.SYUcap.Board;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BoardServiceTests {

    BoardRepository boardRepository = Mockito.mock(BoardRepository.class);
    BoardService boardService = new BoardService(boardRepository);

    private Board createValidBoard() {
        Board board = new Board();
        board.setTitle("정상제목");
        board.setContent("정상내용");
        board.setLimitCount(5);
        board.setMeetingStartTime(LocalDateTime.of(2025,11,23,13,0));
        board.setMeetingEndTime(LocalDateTime.of(2025,11,23,14,0));
        return board;
    }

    @Test
    @DisplayName("[TC-011] 제목 20자 초과 시 저장 실패")
    void save_TitleTooLong_Fail() {
        Board board = createValidBoard();
        board.setTitle("이 제목은 무려 20자를 훨씬 넘어서 실패해야 합니다.");

        assertThrows(IllegalArgumentException.class,
                () -> boardService.save(board));
    }

    @Test
    @DisplayName("[TC-012] 내용 200자 초과 시 저장 실패")
    void save_ContentTooLong_Fail() {
        Board board = createValidBoard();
        board.setContent("a".repeat(201));

        assertThrows(IllegalArgumentException.class,
                () -> boardService.save(board));
    }

    @Test
    @DisplayName("[TC-013] 시작 > 종료면 저장 실패")
    void save_InvalidMeetingTime_Fail() {
        Board board = createValidBoard();
        board.setMeetingStartTime(LocalDateTime.of(2025,11,23,14,0));
        board.setMeetingEndTime(LocalDateTime.of(2025,11,23,13,0));

        assertThrows(IllegalArgumentException.class,
                () -> boardService.save(board));
    }

    @Test
    @DisplayName("[TC-014] 제한 인원 음수면 저장 실패")
    void save_LimitNegative_Fail() {
        Board board = createValidBoard();
        board.setLimitCount(-1);

        assertThrows(IllegalArgumentException.class,
                () -> boardService.save(board));
    }

    @Test
    @DisplayName("[TC-015] 정상 데이터는 저장 성공")
    void save_Success() {
        Board board = createValidBoard();
        when(boardRepository.save(any(Board.class))).thenReturn(board);

        Board saved = boardService.save(board);

        assertEquals("정상제목", saved.getTitle());
        verify(boardRepository, times(1)).save(board);
    }
}
