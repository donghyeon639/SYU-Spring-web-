package com.SYUcap.SYUcap.Board;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BoardController.class)
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardService boardService;

    @MockBean
    private CommentService commentService;

    @Test
    @DisplayName("[C-TC-002] [실패] 필수 파라미터(content) 누락")
    void addComment_Fail_ContentMissing() throws Exception {
        mockMvc.perform(
                        post("/board/게임/1/comment")
                        // .param("content", ...) 없음
                )
                .andExpect(status().isBadRequest()); // 400

        verify(commentService, never()).createComment(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("[C-TC-003] [정책] content 파라미터가 빈 값(\"\")일 때")
    void addComment_Policy_ContentEmpty() throws Exception {
        mockMvc.perform(
                        post("/board/게임/1/comment")
                                .param("content", "")
                )
                .andExpect(status().isFound()); // 302 Redirect (현재 로직)

        // 현재 로직은 빈 값도 서비스로 넘김
        verify(commentService, times(1)).createComment(1L, "", "익명");
    }

    @Test
    @DisplayName("[C-TC-004] [정책] content 파라미터가 공백(\" \")일 때")
    void addComment_Policy_ContentBlank() throws Exception {
        mockMvc.perform(
                        post("/board/게임/1/comment")
                                .param("content", " ")
                )
                .andExpect(status().isFound()); // 302 Redirect (현재 로직)

        // 현재 로직은 공백도 서비스로 넘김
        verify(commentService, times(1)).createComment(1L, " ", "익명");
    }

    @Test
    @DisplayName("[C-TC-005] [실패] GET 방식으로 댓글 등록 시도")
    void addComment_Fail_MethodGet() throws Exception {
        mockMvc.perform(
                        get("/board/게임/1/comment") // GET 요청
                                .param("content", "잘못된 요청")
                )
                .andExpect(status().isMethodNotAllowed()); // 405

        verify(commentService, never()).createComment(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("[C-TC-006] [실패] 경로 변수(id) 타입 오류")
    void addComment_Fail_IdTypeMismatch() throws Exception {
        mockMvc.perform(
                        post("/board/게임/abc/comment") // id가 숫자가 아님
                                .param("content", "댓글")
                )
                .andExpect(status().isBadRequest()); // 400
    }

    @Test
    @DisplayName("[C-TC-007] [실패] 경로 변수(id)가 음수일 때")
    void addComment_Fail_IdNegative() throws Exception {
        // 컨트롤러는 음수 ID를 서비스로 일단 넘김 (서비스 레벨에서 처리)
        mockMvc.perform(
                        post("/board/게임/-1/comment")
                                .param("content", "댓글")
                )
                .andExpect(status().isFound()); // 302 Redirect

        verify(commentService, times(1)).createComment(-1L, "댓글", "익명");
    }

    @Test
    @DisplayName("[C-TC-008] [실패] 존재하지 않는 cat URL 요청")
    void addComment_Fail_InvalidCategory() throws Exception {
        // addComment 메서드는 cat에 대한 유효성 검사(validateCategory)가 없음
        // 따라서 요청은 성공하고, 리다이렉트 URL이 이상하게 생성됨
        mockMvc.perform(
                        post("/board/없는카테고리/1/comment")
                                .param("content", "댓글")
                )
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/board/없는카테고리/1"));

        // 서비스는 호출됨
        verify(commentService, times(1)).createComment(1L, "댓글", "익명");
    }

    @Test
    @DisplayName("[C-TC-009] [예외] 서비스 레이어 예외 발생")
    void addComment_Exception_ServiceFails() throws Exception {
        // Given
        // commentService가 예외를 던지도록 설정
        doThrow(new IllegalArgumentException("Test exception"))
                .when(commentService)
                .createComment(1L, "댓글", "익명");

        // When & Then
        mockMvc.perform(
                        post("/board/게임/1/comment")
                                .param("content", "댓글")
                )
                .andExpect(status().isInternalServerError()); // 500
    }

    @Test
    @DisplayName("[C-TC-012] [보안] XSS 스크립트 입력")
    void addComment_Security_XSS() throws Exception {
        String xssPayload = "<script>alert(1)</script>";
        mockMvc.perform(
                        post("/board/게임/1/comment")
                                .param("content", xssPayload)
                )
                .andExpect(status().isFound());

        // 서비스에 스크립트가 그대로 전달되는지 확인 (저장은 되되, 보여줄 때 처리)
        verify(commentService, times(1)).createComment(1L, xssPayload, "익명");
    }

    @Test
    @DisplayName("[C-TC-013] [보안] SQL 인젝션 시도")
    void addComment_Security_SQLi() throws Exception {
        String sqliPayload = "' OR '1'='1'";
        mockMvc.perform(
                        post("/board/게임/1/comment")
                                .param("content", sqliPayload)
                )
                .andExpect(status().isFound());

        // JPA가 방어하므로, 서비스에 문자열 그대로 전달됨
        verify(commentService, times(1)).createComment(1L, sqliPayload, "익명");
    }

    @Test
    @DisplayName("[C-TC-014] [실패] 존재하지 않는 엔드포인트 요청")
    void addComment_Fail_NotFound() throws Exception {
        mockMvc.perform(
                        post("/board/게임/1/comment/delete") // 정의되지 않은 URL
                                .param("content", "댓글")
                )
                .andExpect(status().isNotFound()); // 404
    }
}