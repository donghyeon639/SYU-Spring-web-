package com.example.allfree;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;

@Controller
public class HomeController {

    private final PostService postService;

    // 생성자 주입
    public HomeController(PostService postService) {
        this.postService = postService;
    }

    // 홈: "/"와 "/home" 모두 대응
    @GetMapping({"/", "/home"})
    public String home(Model model) {
        model.addAttribute("active", "home");
        model.addAttribute("categories", new String[]{"게임", "스터디", "영화", "운동", "밥약"});

        // 최근 글 10개 (createdAt 내림차순)
        List<Post> recentPosts = postService.findAll().stream()
                .sorted(Comparator.comparing(Post::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(10)
                .toList();

        model.addAttribute("recentPosts", recentPosts);
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
