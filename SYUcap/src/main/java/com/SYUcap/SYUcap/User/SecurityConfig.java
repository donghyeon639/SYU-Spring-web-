package com.SYUcap.SYUcap.User;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // CSRF: 마이페이지는 테스트 편의상 토큰 검사는 생략하지만,
        // 인가 규칙에서 여전히 인증(authentication)은 필수로 둔다.
        http.csrf(csrf -> csrf
                .ignoringRequestMatchers("/mypage/**")
        );

        http.authorizeHttpRequests(authorize -> authorize
                // 로그인/회원가입, 정적 리소스, 홈 등은 모두 허용
                .requestMatchers("/", "/home", "/login", "/signup", "/css/**", "/js/**", "/images/**").permitAll()
                // 게시판과 마이페이지는 반드시 로그인한 사용자만 접근 가능
                .requestMatchers("/board/**", "/mypage", "/mypage/**").authenticated()
                .anyRequest().permitAll()
        );

        http.formLogin(formLogin -> formLogin
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/home", true)
                .failureUrl("/login?error")
                .usernameParameter("userId")
                .passwordParameter("password")
                .permitAll()
        );

        http.logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/home")
        );

        http.httpBasic();

        return http.build();
    }
}