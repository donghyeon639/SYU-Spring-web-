package com.SYUcap.SYUcap.User;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc // 보안 필터 활성화 (로그인 플로우 검증)
@ActiveProfiles("test")
@Transactional
public class UserIntergrationTest {

    private static final Logger log = LoggerFactory.getLogger(UserIntergrationTest.class);

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("스프링 컨텍스트 로드")
    void contextLoads() { }

    @Test
    @DisplayName("회원가입 성공 시 로그인 페이지 이동")
    void signup_success_moves_to_login_page() throws Exception {
        // given
        String userId = "joinUser01";
        String rawPw = "Passw0rd!";
        String confirm = "Passw0rd!";
        String name = "Tester";
        // when
        var result = mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("userId", userId)
                        .param("password", rawPw)
                        .param("passwordConfirm", confirm)
                        .param("userName", name))

                .andExpect(status().isOk())
                .andReturn();
        // then (추가 검증: 로그인 페이지 콘텐츠 일부 포함 여부)
        assertThat(result.getResponse().getContentAsString()).contains("로그인");
    }

    @Test
    @DisplayName("회원가입 시 비밀번호 암호화 성공")
    void signup_password_encrypted() throws Exception {
        // given
        String userId = "encryptUser01";
        String rawPw = "Abcdef1!";
        String confirm = rawPw;
        String name = "동현";
        // when
        mockMvc.perform(post("/signup")
                .with(csrf())
                .param("userId", userId)
                .param("password", rawPw)
                .param("passwordConfirm", confirm)
                .param("userName", name))
                .andExpect(status().isOk());
        // then
        Users saved = userRepository.findByUserId(userId).orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo(rawPw);
        assertThat(passwordEncoder.matches(rawPw, saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("로그인 성공 시 /home 리다이렉트")
    void login_success_redirects_home() throws Exception {
        // given (사전 가입)
        String userId = "loginUser01";
        String rawPw = "Strong1!";
        String confirm = rawPw;
        String name = "로그인";
        mockMvc.perform(post("/signup")
                .with(csrf())
                .param("userId", userId)
                .param("password", rawPw)
                .param("passwordConfirm", confirm)
                .param("userName", name))
                .andExpect(status().isOk());
        // when
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("userId", userId)
                .param("password", rawPw))
        //then
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }
    @Test
    @DisplayName("회원가입 버튼 연속 클릭시 중복 저장 방지")
    void signup_double_submit_prevents_duplicate() throws Exception {
        // given: 첫 요청으로 가입
        String userId = "doubleClickUser01";
        String rawPw = "Click1!";
        String confirm = rawPw;
        String name = "더블";
        mockMvc.perform(post("/signup")
                .with(csrf())
                .param("userId", userId)
                .param("password", rawPw)
                .param("passwordConfirm", confirm)
                .param("userName", name))
                .andExpect(status().isOk());

        // when: 동일 데이터로 즉시 두 번째 제출 (연속 클릭 시나리오)
        mockMvc.perform(post("/signup")
                .with(csrf())
                .param("userId", userId)
                .param("password", rawPw)
                .param("passwordConfirm", confirm)
                .param("userName", name))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("이미 존재하는 아이디입니다."));

        // then: 해당 userId로 DB에 하나만 존재
        long count = userRepository.findAll().stream()
                .filter(u -> userId.equals(u.getUserId()))
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("동시 가입(동일 아이디): 하나만 성공, 나머지는 400, DB에는 1건")
    void concurrent_signup_same_userId_only_one_saved() throws Exception {
        // given
        String userId = "concurrentUser01";
        String rawPw = "Passw0rd!";
        String name = "동시";
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger bad = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();

        // when
        IntStream.range(0, threads).forEach(i -> pool.submit(() -> {
            try {
                start.await();
                var res = mockMvc.perform(post("/signup")
                                .with(csrf())
                                .param("userId", userId)
                                .param("password", rawPw)
                                .param("passwordConfirm", rawPw)
                                .param("userName", name))
                        .andReturn();
                int status = res.getResponse().getStatus();
                if (status == 200) ok.incrementAndGet();
                else if (status == 400) bad.incrementAndGet();
                else errors.incrementAndGet();
            } catch (Exception e) {
                errors.incrementAndGet();
            } finally {
                done.countDown();
            }
        }));
        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        // then
        assertThat(ok.get()).isEqualTo(1);
        assertThat(bad.get()).isEqualTo(threads - 1);
        assertThat(errors.get()).isEqualTo(0);
        long count = userRepository.findAll().stream().filter(u -> userId.equals(u.getUserId())).count();
        assertThat(count).isEqualTo(1);
    }

}
