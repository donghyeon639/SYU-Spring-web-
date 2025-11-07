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
    @DisplayName("[C-001] [유효성] content 파라미터 누락")
    void addComment_Fail_ContentMissing() throws Exception {
        mockMvc.perform(
                        post("/board/게임/1/comment")
                        // .param("content", ...) 없음
                )
                .andExpect(status().isBadRequest()); // 400

        verify(commentService, never()).createComment(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("[C-002] [정책] content 파라미터가 빈 값(\"\")일 때")
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
    @DisplayName("[C-003] [HTTP] GET 방식으로 댓글 등록 시도")
    void addComment_Fail_MethodGet() throws Exception {
        mockMvc.perform(
                        get("/board/게임/1/comment") // GET 요청
                                .param("content", "잘못된 요청")
                )
                .andExpect(status().isMethodNotAllowed()); // 405

        verify(commentService, never()).createComment(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("[C-004] [HTTP/경계] id 경로 변수 타입 오류")
    void addComment_Fail_IdTypeMismatch() throws Exception {
        mockMvc.perform(
                        post("/board/게임/abc/comment") // id가 숫자가 아님
                                .param("content", "댓글")
                )
                .andExpect(status().isBadRequest()); // 400
    }

    @Test
    @DisplayName("[C-005] [경계] id 경로 변수 음수 값")
    void addComment_Fail_IdNegative() throws Exception {
        mockMvc.perform(
                        post("/board/게임/-1/comment")
                                .param("content", "댓글")
                )
                .andExpect(status().isFound()); // 302 Redirect

        // 컨트롤러는 음수 ID를 서비스로 일단 넘김
        verify(commentService, times(1)).createComment(-1L, "댓글", "익명");
    }

    // [C-006] addComment_Fail_InvalidCategory() 메서드 삭제됨

    // [C-007] addComment_Exception_ServiceFails() 메서드 삭제됨

    @Test
    @DisplayName("[C-008] [HTTP] 존재하지 않는 엔드포인트")
    void addComment_Fail_NotFound() throws Exception {
        mockMvc.perform(
                        post("/board/게임/1/comment/delete") // 정의되지 않은 URL
                                .param("content", "댓글")
                )
                .andExpect(status().isNotFound()); // 404
    }

    @Test
    @DisplayName("[C-009] [보안] XSS 스크립트 입력")
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
    @DisplayName("[C-010] [보안] SQL 인젝션 시도")
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
}