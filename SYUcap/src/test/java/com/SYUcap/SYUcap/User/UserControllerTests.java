package com.SYUcap.SYUcap.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
public class UserControllerTests {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void signup_get_예외없이_호출됨() {
        assertDoesNotThrow(() -> userController.signup());
    }

    @Test
    void signup_post_정상입력_예외없이_호출됨() {
        assertDoesNotThrow(() ->
                userController.addUser("tester", "Password1!", "테스터", "dummy")
        );
    }

    @Test
    void signup_post_짧은비밀번호_예외없이_호출됨() {
        assertDoesNotThrow(() ->
                userController.addUser("tester", "abc", "테스터", "dummy")
        );
    }
}
