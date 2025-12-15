package com.SYUcap.SYUcap.User;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


@SpringBootTest
@AutoConfigureMockMvc
public class UserPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(UserPerformanceTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("동시 가입(서로 다른 아이디): 모두 성공, DB에 모두 저장")
    void concurrent_signup_distinct_userIds_all_saved() throws Exception {
        // given
        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();

        // when
        IntStream.range(0, threads).forEach(i -> pool.submit(() -> {
            String userId = "concurrentDistinct" + i;
            try {
                start.await();
                var res = mockMvc.perform(post("/signup")
                                .param("userId", userId)
                                .param("password", "Pw" + i + "!Ab1")
                                .param("passwordConfirm", "Pw" + i + "!Ab1")
                                .param("userName", "U" + i))
                        .andReturn();
                int status = res.getResponse().getStatus();
                if (status == 200) ok.incrementAndGet();
                else errors.incrementAndGet();
            } catch (Exception e) {
                errors.incrementAndGet();
            } finally {
                done.countDown();
            }
        }));
        long startNs = System.nanoTime();
        start.countDown();
        boolean finished = done.await(30, TimeUnit.SECONDS); // 완료 여부
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        pool.shutdownNow();
        log.info("서로다른아이디 동시가입 총 경과(ms): {} / finished: {}", elapsedMs, finished);

        // then
        assertThat(finished).as("30초 내 전체 스레드 종료").isTrue();
        assertThat(elapsedMs).isLessThanOrEqualTo(30_000);
        assertThat(ok.get()).isEqualTo(threads);
        assertThat(errors.get()).isEqualTo(0);
        long saved = userRepository.findAll().stream()
                .filter(u -> u.getUserId() != null && u.getUserId().startsWith("concurrentDistinct"))
                .count();
        assertThat(saved).isEqualTo(threads);
    }
}
