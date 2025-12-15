package com.SYUcap.SYUcap.User;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void addUser(String userId, String password, String passwordConfirm,String username) {
        // 아이디 공백 검사
        if (userId != null && userId.contains(" ")) {
            throw new IllegalArgumentException("아이디에 공백이 포함될 수 없습니다.");
        }

        // 제어문자 검사 (아이디)
        if (containsControlChar(userId)) {
            throw new IllegalArgumentException("아이디에 제어 문자가 포함될 수 없습니다.");
        }

        // 제어문자 검사 (비밀번호)
        if (containsControlChar(password)) {
            throw new IllegalArgumentException("비밀번호에 제어 문자가 포함될 수 없습니다.");
        }

        // 아이디 중복 검사
        if (userRepository.findByUserId(userId).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        // 비밀번호 유효성 검사: 6~16자, 특수문자 1개 이상
        if (!isValidPasswordlength(password)) {
            throw new IllegalArgumentException("비밀번호는 6~16자이어야 합니다");
        }
        if(!password.equals(passwordConfirm)) {
            throw new IllegalArgumentException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }
        if (!containsSpecialCh(password)) {
            throw new IllegalArgumentException("비밀번호에 특수문자가 포함되어야 합니다.");
        }
        Users user = new Users();
        var hash = passwordEncoder.encode(password);
        user.setUserId(userId);
        user.setPassword(hash);
        user.setUserName(username);
        userRepository.save(user);
    }

    private boolean isValidPasswordlength(String password) {
        if (password == null) return false;
        int len = password.length();
        if (len < 6 || len > 16) return false;
        return true;
    }
    private boolean containsSpecialCh(String password){
        String specialChars = "!@#$%^&*()-_=+[]{}|;:'\",.<>?/`~";
        for (char c : password.toCharArray()) {
            if (specialChars.indexOf(c) != -1) {
                return true;
            }
        }
        return false;
    }

    // 제어문자(\n, \t, \r 등) 포함 여부 검사
    private boolean containsControlChar(String input) {
        if (input == null) return false;
        for (char c : input.toCharArray()) {
            if (Character.isISOControl(c)) {
                return true;
            }
        }
        return false;
    }
}