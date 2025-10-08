package com.SYUcap.SYUcap;

import com.SYUcap.SYUcap.Board.Board;
import com.SYUcap.SYUcap.Board.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final BoardService boardService;

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        model.addAttribute("active", "home");
        model.addAttribute("categories", new String[]{"게임", "스터디", "영화", "운동", "밥약"});

        // 최근 글 10개 (createdAt 내림차순)
        List<Board> recentPosts = boardService.findAll().stream()
                .sorted(Comparator.comparing(Board::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
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
        return "mypage";
    }
}
