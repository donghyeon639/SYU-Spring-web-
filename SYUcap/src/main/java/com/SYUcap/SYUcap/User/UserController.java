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
    @PostMapping("/signup")
    String addUser(@RequestParam String userId, @RequestParam String password,
                          @RequestParam String userName ) {
        userService.addUser(userId, password, userName);
        return "login.html";
    }

    @PostMapping("/login")
    String login(@RequestParam String userId, @RequestParam String password) {
        boolean result = userService.login(userId, password);
        if (result) {
            return "main";
        } else {
            return "redirect:/login?error=true";
        }
    }

}
