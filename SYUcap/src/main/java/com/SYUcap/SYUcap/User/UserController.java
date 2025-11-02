package com.SYUcap.SYUcap.User;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    /** 회원가입 폼 */
    @GetMapping("/signup")
    String signup() {
        return "signup.html";
    }

    @GetMapping("/login")
    String login(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", true);
        }
        return "login";
    }
    /** 회원가입 */
    @PostMapping("/signup")
    String addUser(@RequestParam String userId,
                   @RequestParam String password,
                   @RequestParam String passwordConfirm,
                   @RequestParam String userName ) {
        userService.addUser(userId, password, passwordConfirm, userName);
        return "login.html";
    }
}
