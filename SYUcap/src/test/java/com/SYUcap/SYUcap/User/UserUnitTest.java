package com.SYUcap.SYUcap.User;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserUnitTest {

    @Mock
    UserRepository userRepository;
    @Mock
    org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    private Users makeUser(String userId) {
        Users u = new Users();
        u.setUserId(userId);
        u.setPassword("HASH");
        u.setUserName("이름");
        return u;
    }

    @Test
    @DisplayName("addUser 실패: 아이디 중복")
    void addUser_duplicateId_fail() {
        // given
        String userId = "dupUser";
        Users existing = new Users();
        existing.setUserId(userId);
        existing.setPassword("EXIST_HASH");
        existing.setUserName("기존사용자");
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        // when
        Executable act = () -> userService.addUser(userId, "Pwd!123", "새사용자", "Pwd!123");

        // then
        assertThrows(IllegalArgumentException.class, act);
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }


    @Test
    @DisplayName("addUser 실패: 아이디에 제어문자 포함")
    void addUser_invalidUserIdControl_fail() {
        // given
        String badId = "bad\nId";

        // when
        Executable act = () -> userService.addUser(badId, "GoodPwd!1", "이름", "GoodPwd!1");

        // then
        assertThrows(IllegalArgumentException.class, act);
        verify(userRepository, never()).findByUserId(anyString());
    }



    @Test
    @DisplayName("addUser 실패: 비밀번호 제어문자 포함")
    void addUser_passwordControl_fail() {
        // given
        String userId = "cleanId";
        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());
        String badPwd = "Good\tPwd!1";

        // when
        Executable act = () -> userService.addUser(userId, badPwd, "이름", badPwd);

        // then
        assertThrows(IllegalArgumentException.class, act);
        verify(passwordEncoder, never()).encode(any());
    }


    @Test
    @DisplayName("addUser 실패: 비밀번호 길이 초과")
    void addUser_passwordTooLong_fail() {
        // given
        String userId = "userLong";
        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());
        String longPwd = "A!123456789012345"; // 17자

        // when
        Executable act = () -> userService.addUser(userId, longPwd, "이름", longPwd);

        // then
        assertThrows(IllegalArgumentException.class, act);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("addUser 실패: 비밀번호 특수문자 없음")
    void addUser_passwordNoSpecial_fail() {
        // given
        String userId = "userNoSpecial";
        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());
        String pwd = "Password1"; // 특수문자 없음

        // when
        Executable act = () -> userService.addUser(userId, pwd, "이름", pwd);

        // then
        assertThrows(IllegalArgumentException.class, act);
    }

    @Test
    @DisplayName("addUser 실패: 비밀번호 확인 불일치")
    void addUser_passwordConfirmMismatch_fail() {
        // given
        String userId = "userMismatch";
        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());
        String pwd = "Pwd!123";
        String confirm = "Pwd!1234";

        // when
        Executable act = () -> userService.addUser(userId, pwd, "이름", confirm);

        // then
        assertThrows(IllegalArgumentException.class, act);
        verify(passwordEncoder, never()).encode(any());
    }
    /*
    @Test
    @DisplayName("addUser 실패: 아이디 공백 포함")
    void addUser_userIdContainsSpace_fail() {
        // given
        String userId = "bad id";

        // when
        Executable act = () -> userService.addUser(userId, "GoodPwd!1", "이름", "GoodPwd!1");

        // then
        assertThrows(IllegalArgumentException.class, act);
        verify(userRepository, never()).findByUserId(any());
    }
    */
}