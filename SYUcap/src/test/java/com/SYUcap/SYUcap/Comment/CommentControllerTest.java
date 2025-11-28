package com.SYUcap.SYUcap.Comment;

import com.SYUcap.SYUcap.User.CustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    private final CustomUser mockUser = new CustomUser(
            "test_user",
            "password",
            java.util.Collections.emptyList(),
            1L,
            "홍길동"
    );

    // --- [C-001 ~ C-005] 요청 값 검증 ---

    @Test
    @DisplayName("[C-001] content 파라미터 누락 -> 400 Bad Request")
    void addComment_Fail_ContentMissing() throws Exception {
        mockMvc.perform(post("/comments/write")
                        .param("boardId", "1")
                        .param("cat", "게임")
                        // content 없음
                        .with(user(mockUser))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[C-002] content 빈 값 -> 302 Redirect (성공 처리)")
    void addComment_ContentEmpty() throws Exception {
        mockMvc.perform(post("/comments/write")
                        .param("boardId", "1")
                        .param("cat", "게임")
                        .param("content", "")
                        .with(user(mockUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("[C-003] GET 방식으로 댓글 등록 시도 -> 405 Method Not Allowed")
    void addComment_Fail_MethodGet() throws Exception {
        mockMvc.perform(get("/comments/write")
                        .param("boardId", "1")
                        .param("content", "test")
                        .with(user(mockUser)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("[C-004] boardId 타입 오류 (문자) -> 400 Bad Request")
    void addComment_Fail_IdTypeMismatch() throws Exception {
        mockMvc.perform(post("/comments/write")
                        .param("boardId", "abc")
                        .param("content", "test")
                        .with(user(mockUser))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[C-005] boardId 음수 값 -> 500 (서비스 예외 발생 시)")
    void addComment_IdNegative() throws Exception {
        // 서비스가 음수 ID 거부한다고 가정
        doThrow(new IllegalArgumentException("Invalid ID"))
                .when(commentService).createComment(eq(-1L), anyString(), anyString());

        mockMvc.perform(post("/comments/write")
                        .param("boardId", "-1")
                        .param("cat", "게임")
                        .param("content", "test")
                        .with(user(mockUser))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // --- [C-008] 잘못된 경로 ---

    @Test
    @DisplayName("[C-008] 존재하지 않는 엔드포인트 -> 404 Not Found")
    void invalidEndpoint() throws Exception {
        mockMvc.perform(post("/comments/write/delete") // 없는 경로
                        .with(user(mockUser))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // --- [C-009 ~ C-010] 보안 ---

    @Test
    @DisplayName("[C-009] XSS 스크립트 입력 -> 302 Redirect (저장은 됨)")
    void addComment_XSS() throws Exception {
        String xss = "<script>alert(1)</script>";
        mockMvc.perform(post("/comments/write")
                        .param("boardId", "1")
                        .param("cat", "게임")
                        .param("content", xss)
                        .with(user(mockUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // 서비스까지 데이터가 넘어갔는지 확인
        verify(commentService).createComment(anyLong(), eq(xss), anyString());
    }

    @Test
    @DisplayName("[C-010] SQL Injection 입력 -> 302 Redirect (저장은 됨)")
    void addComment_SQLInjection() throws Exception {
        String sqli = "' OR '1'='1'";
        mockMvc.perform(post("/comments/write")
                        .param("boardId", "1")
                        .param("cat", "게임")
                        .param("content", sqli)
                        .with(user(mockUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // --- [C-012 ~ C-013] 경계 값 ---

    @Test
    @DisplayName("[C-012] 대용량 Content 전송 (2MB) -> 302 Redirect")
    void addComment_TooLarge() throws Exception {
        String hugeContent = "a".repeat(2000000);
        mockMvc.perform(post("/comments/write")
                        .param("boardId", "1")
                        .param("cat", "게임")
                        .param("content", hugeContent)
                        .with(user(mockUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("[C-013] 이모지 입력 -> 302 Redirect")
    void addComment_Emoji() throws Exception {
        String emoji = "안녕하세요 👍";
        mockMvc.perform(post("/comments/write")
                        .param("boardId", "1")
                        .param("cat", "게임")
                        .param("content", emoji)
                        .with(user(mockUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(commentService).createComment(anyLong(), eq(emoji), anyString());
    }
}