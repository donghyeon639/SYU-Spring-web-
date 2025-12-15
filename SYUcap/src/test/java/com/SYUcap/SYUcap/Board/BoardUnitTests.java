package com.SYUcap.SYUcap.Board;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BoardUnitTests {

    @Test
    @DisplayName("[TC-011] 제목 길이 20자 초과 입력 시 실패")
    void titleTooLong_Fail() {
        // Given: 20자를 초과하는 제목
        String title = "지금 당장 도서관에서 모일 사람은 이곳으로 모이세요";

        // When: 제목 유효성 검사 수행
        boolean valid = BoardService.isTitleValid(title);

        // Then: false 반환
        assertTrue(title.length() > 20);
        assertFalse(valid);
    }

    @Test
    @DisplayName("[TC-012] 내용 길이 200자 초과 입력 시 실패")
    void contentTooLong_Fail() {
        // Given: 200자를 초과하는 내용
        String content = "a".repeat(201);

        // When: 내용 유효성 검사 수행
        boolean valid = BoardService.isContentValid(content);

        // Then: false 반환
        assertTrue(content.length() > 200);
        assertFalse(valid);
    }

    @Test
    @DisplayName("[TC-013] 시작 시간이 종료 시간보다 이후일 경우 실패 (경고 메시지)")
    void invalidMeetingTime_Fail() {
        // Given: 시작 13:00, 종료 12:00
        LocalDateTime start = LocalDateTime.of(2025, 11, 23, 13, 0);
        LocalDateTime end = LocalDateTime.of(2025, 11, 23, 12, 0);

        // When: 시간 검증 수행
        String message = BoardService.validateMeetingTime(start, end);

        // Then: 경고 메시지 반환
        assertTrue(start.isAfter(end));
        assertEquals("시작 시간이 종료 시간보다 빠를 수 없습니다.", message);
    }

    @Test
    @DisplayName("[TC-014] 제한 인원 음수 입력 시 실패 (경고 메시지)")
    void limitCountNegative_Fail() {
        // Given: 제한 인원 -5
        int limitCount = -5;

        // When: 인원 검증 수행
        String message = BoardService.validateLimitCount(limitCount);

        // Then: 경고 메시지 반환
        assertTrue(limitCount < 1);
        assertEquals("최소 1명 이상이어야 합니다", message);
    }

    @Test
    @DisplayName("[TC-015] 제한 인원 0 입력 시 실패 (경고 메시지)")
    void limitCountZero_Fail() {
        // Given: 제한 인원 0
        int limitCount = 0;

        // When: 인원 검증 수행
        String message = BoardService.validateLimitCount(limitCount);

        // Then: 경고 메시지 반환
        assertTrue(limitCount < 1);
        assertEquals("최소 1명 이상이어야 합니다", message);
    }
}
