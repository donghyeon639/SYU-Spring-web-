package com.SYUcap.SYUcap.User;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(UserControllerTest.MockConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("GET /signup 요청 시 signup.html 뷰를 반환한다")
    void signup_get_returns_signup_view() throws Exception {
        // Given
        // 별도 준비 없음

        // When
        ResultActions resultActions = mockMvc.perform(get("/signup"));

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(view().name("signup.html"));
    }

    @Test
    @DisplayName("GET /login 요청 시 (에러 없을 때) login 뷰를 반환한다")
    void login_get_returns_login_view_without_error() throws Exception {
        // Given
        // 별도 준비 없음

        // When
        ResultActions resultActions = mockMvc.perform(get("/login"));

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeDoesNotExist("error"));
    }

    @Test
    @DisplayName("POST /signup 비밀번호 검증 실패 시 signup.html로 에러 메시지와 함께 반환한다")
    void signup_post_invalid_password_returns_signup_with_error() throws Exception {
        // Given
        String userId = "tester";
        String badPassword = "abcde"; // 6자 미만 또는 특수문자 없음
        String userName = "테스터";
        doThrow(new IllegalArgumentException("비밀번호는 6~16자이며 특수문자를 최소 1개 포함해야 합니다."))
                .when(userService).addUser(userId, badPassword, userName);

        // When
        ResultActions resultActions = mockMvc.perform(post("/signup")
                .param("userId", userId)
                .param("password", badPassword)
                .param("userName", userName));

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(view().name("signup.html"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("GET /users/check-id 중복 아이디면 available=false 를 반환한다")
    void checkId_duplicate_returns_available_false() throws Exception {
        // Given
        String userId = "dupUser";
        Users existing = new Users();
        existing.setUserId(userId);
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        // When
        ResultActions resultActions = mockMvc.perform(get("/users/check-id")
                .param("userId", userId));

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.available").value(false));
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        public UserService userService() { return mock(UserService.class); }
        @Bean
        public UserRepository userRepository() { return mock(UserRepository.class); }
    }
}