package com.SYUcap.SYUcap.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public void addUser(String userId, String password, String username) {
        if (userRepository.findByUserId(userId).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        Users user = new Users();
        user.setUserId(userId);
        user.setPassword(password);
        user.setUserName(username);
        userRepository.save(user);
    }

    public boolean login(String userId, String password) {
        Optional<Users> user = userRepository.findByUserId(userId);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return true; // 로그인 성공
        } else {
            return false; // 로그인 실패
        }
    }
}
