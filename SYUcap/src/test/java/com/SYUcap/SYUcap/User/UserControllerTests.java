package com.SYUcap.SYUcap.User;

import org.springframework.boot.test.context.TestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(UserController.class)
@WebMvcTest
class   UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;


    @Test
    @DisplayName("GET /signup 요청 시 signup.html 뷰를 반환한다")
    void signup_GET_요청() throws Exception {
        // Arrange (준비)
        // 이 테스트는 특별한 준비 단계가 필요 없음

        // Act (실행)
        ResultActions resultActions = mockMvc.perform(get("/signup"));

        // Assert (검증)
        resultActions
                .andExpect(status().isOk())
                .andExpect(view().name("signup.html"));
    }

    @Test
    @DisplayName("GET /login 요청 시 (에러 없을 때) login 뷰를 반환한다")
    void login_GET_요청_에러없음() throws Exception {
        // Arrange (준비)
        // 특별한 준비 단계 없음

        // Act (실행)
        ResultActions resultActions = mockMvc.perform(get("/login"));

        // Assert (검증)
        resultActions
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeDoesNotExist("error"));
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        public UserService userService() {
            return mock(UserService.class);
        }
        @Bean
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }
    }
}