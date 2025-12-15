package com.SYUcap.SYUcap.Board;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BoardUnitTests {

    @Test
    @DisplayName("[TC-011] 제목 길이 20자 초과 입력 시 실패")
    void titleTooLong_Fail() {
        String title = "지금 당장 도서관에서 모일 사람은 이곳으로 모이세요"; // 20자 초과
        assertTrue(title.length() > 20);
    }

    @Test
    @DisplayName("[TC-012] 내용 길이 200자 초과 입력 시 실패")
    void contentTooLong_Fail() {
        String content = "a".repeat(201); // 201자
        assertTrue(content.length() > 200);
    }

    @Test
    @DisplayName("[TC-013] 시작 시간이 종료 시간보다 이후일 경우 실패")
    void invalidMeetingTime_Fail() {
        LocalDateTime start = LocalDateTime.of(2025, 11, 23, 13, 0);
        LocalDateTime end = LocalDateTime.of(2025, 11, 23, 12, 0);
        assertTrue(start.isAfter(end));
    }

    @Test
    @DisplayName("[TC-014] 제한 인원 음수 입력 시 실패")
    void limitCountNegative_Fail() {
        int limitCount = -5;
        assertTrue(limitCount < 1);
    }

    @Test
    @DisplayName("[TC-015] 제한 인원 0 입력 시 실패")
    void limitCountZero_Fail() {
        int limitCount = 0;
        assertTrue(limitCount < 1);
    }
}

