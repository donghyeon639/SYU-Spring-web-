package com.example.allfree;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("active", "home");
        model.addAttribute("categories", new String[]{"게임", "스터디", "영화", "운동", "밥약"});
        return "home";
    }

    @GetMapping("/groups")
    public String groups(Model model) {
        model.addAttribute("active", "groups");
        return "groups";
    }

    @GetMapping("/notifications")
    public String notifications(Model model) {
        model.addAttribute("active", "notifications");
        return "notifications";
    }

    @GetMapping("/mypage")
    public String mypage(Model model) {
        model.addAttribute("active", "mypage");
        return "mypage";
    }
}
