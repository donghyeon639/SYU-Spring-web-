package com.SYUcap.SYUcap.User;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class MypageIntergrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EntityManager entityManager;

    private Users createAndSaveUser(String userId, String rawPw, String name) {
        Users u = new Users();
        u.setUserId(userId);
        u.setPassword(passwordEncoder.encode(rawPw));
        u.setUserName(name);
        return userRepository.save(u);
    }

    @Test
    @WithMockUser(username = "test_user_1")
    @DisplayName("[MP-TC-005] 아이디 변경 성공 - 실제 DB 반영")
    void changeUserId_success_persisted() throws Exception {
        // given: 실제 DB에 유저 저장
        Users user = createAndSaveUser("test_user_1", "Password1!", "홍길동");

        // when
        mockMvc.perform(post("/mypage/edit")
                        .param("userName", user.getUserName())
                        .param("password", "")
                        .param("passwordConfirm", "")
                        .param("newUserId", "test_user_2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage"));

        // then: DB에서 아이디 변경 확인
        assertThat(userRepository.findByUserId("test_user_2")).isPresent();
        assertThat(userRepository.findByUserId("test_user_1")).isEmpty();
    }

    @Test
    @WithMockUser(username = "test_user_1")
    @DisplayName("[MP-TC-006] 이름만 수정 - 실제 DB 연동")
    void changeProfile_exceptUserId_success() throws Exception {
        // given
        Users user = createAndSaveUser("test_user_1", "Password1!", "홍길동");

        // when
        mockMvc.perform(post("/mypage/edit")
                        .param("userName", "안중근")
                        .param("password", "")
                        .param("passwordConfirm", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage"));

        // then: 이름만 변경되었는지 확인
        Users updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getUserId()).isEqualTo("test_user_1");
        assertThat(updated.getUserName()).isEqualTo("안중근");
    }

    @Test
    @WithMockUser(username = "delete_user")
    @DisplayName("[MP-TC-007] 회원 탈퇴 성공 - 실제 DB 삭제 확인")
    void deleteAccount_success_dbVerify() throws Exception {
        // given
        Users user = createAndSaveUser("delete_user", "encoded", "삭제대상");
        Long userId = user.getId();

        // when
        mockMvc.perform(delete("/mypage/{id}", userId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        // then: 1차 캐시를 비워 실제 DB 상태 확인
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findById(userId)).isEmpty();
    }

    @Test
    @WithMockUser(username = "pw_user")
    @DisplayName("[MP-TC-008] 비밀번호 변경 성공 - 실제 암호화 저장 확인")
    void changePassword_success_dbVerify() throws Exception {
        // given
        Users user = createAndSaveUser("pw_user", "oldPw", "비번유저");
        String newPassword = "NPassword1!";

        // when
        mockMvc.perform(post("/mypage/edit")
                        .param("userName", user.getUserName())
                        .param("password", newPassword)
                        .param("passwordConfirm", newPassword)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage"));

        // then: 실제 DB에서 암호화된 비밀번호 확인
        entityManager.flush();
        entityManager.clear();

        Users updatedUser = userRepository.findByUserId("pw_user").orElseThrow();
        assertThat(passwordEncoder.matches(newPassword, updatedUser.getPassword())).isTrue();
    }

    @Test
    @WithMockUser(username = "nochange_user")
    @DisplayName("[MP-TC-009] 회원정보 수정 - 변경 사항 없음 (DB 검증)")
    void updateProfile_noChanges_dbVerify() throws Exception {
        // given
        Users user = createAndSaveUser("nochange_user", "encoded", "홍길동");

        // when
        mockMvc.perform(post("/mypage/edit")
                        .param("userName", user.getUserName())
                        .param("password", "")
                        .param("passwordConfirm", "")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // then: DB에서 다시 조회해 값이 그대로인지 확인
        entityManager.flush();
        entityManager.clear();

        Users dbUser = userRepository.findById(user.getId()).orElseThrow();

        assertThat(dbUser.getUserId()).isEqualTo("nochange_user");
        assertThat(dbUser.getUserName()).isEqualTo("홍길동");
    }
}