package com.SYUcap.SYUcap;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice // 모든 @Controller에 대해 전역적으로 예외를 처리
public class GlobalExceptionHandler {

    // IllegalArgumentException이 발생하면 이 메서드가 대신 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        // 400 (Bad Request) 상태 코드와 예외 메시지를 본문에 담아 반환
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    // DataIntegrityViolationException이 발생하면 이 메서드가 대신 처리
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrity(DataIntegrityViolationException ex) {
        // 400 (Bad Request) 상태 코드와 사용자 친화적인 메시지를 본문에 담아 반환
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 존재하는 아이디입니다.");
    }
}