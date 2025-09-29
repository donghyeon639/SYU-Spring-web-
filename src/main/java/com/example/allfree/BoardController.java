package com.example.allfree;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/board")
public class BoardController {

    private final PostService postService;

    public BoardController(PostService postService) {
        this.postService = postService;
    }

    // 허용 카테고리 (검증 & 라벨 표기)
    private static final List<String> ALLOWED = List.of("게임", "스터디", "영화", "운동", "밥약");

    /** 전체 목록 (옵션) */
    @GetMapping
    public String listAll(Model model) {
        List<Post> posts = postService.findAll().stream()
                .sorted(Comparator.comparing(Post::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        model.addAttribute("active", "home");
        model.addAttribute("category", "전체");
        model.addAttribute("posts", posts);
        return "board-list";
    }

    /** 카테고리별 목록 */
    @GetMapping("/{cat}")
    public String listByCategory(@PathVariable("cat") String cat, Model model) {
        validateCategory(cat);

        List<Post> posts = postService.findAll().stream()
                .filter(p -> cat.equals(p.getCategory()))
                .sorted(Comparator.comparing(Post::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        model.addAttribute("active", "home");
        model.addAttribute("category", cat);
        model.addAttribute("posts", posts);
        return "board-list";
    }

    /** 글쓰기 폼 */
    @GetMapping("/{cat}/write")
    public String writeForm(@PathVariable("cat") String cat, Model model) {
        validateCategory(cat);
        model.addAttribute("active", "home");
        model.addAttribute("category", cat);
        model.addAttribute("post", new Post());
        return "board-form";
    }

    /** 저장 */
    @PostMapping("/{cat}/write")
    public String writeSubmit(
            @PathVariable("cat") String cat,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime meetingStartTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime meetingEndTime,
            @RequestParam(required = false) Integer limitCount,
            @RequestParam(defaultValue = "익명") String authorName
    ) {
        validateCategory(cat);

        Post p = new Post();
        p.setCategory(cat);
        p.setTitle(title);
        p.setContent(content);
        p.setLocation(location);
        p.setMeetingStartTime(meetingStartTime);
        p.setMeetingEndTime(meetingEndTime);
        p.setLimitCount(limitCount);
        p.setAuthorName(authorName);
        // createdAt은 @PrePersist에서 자동 세팅

        postService.save(p);
        return "redirect:/board/" + cat; // 저장 후 목록으로
    }

    /** 글 상세 보기 */
    @GetMapping("/{cat}/{id}")
    public String detail(@PathVariable String cat, @PathVariable Long id, Model model) {
        validateCategory(cat);
        Post post = postService.findById(id); // 존재하지 않으면 예외
        if (!cat.equals(post.getCategory())) {
            // URL의 카테고리와 실제 글의 카테고리가 다르면 해당 카테고리로 리다이렉트
            return "redirect:/board/" + post.getCategory() + "/" + id;
        }
        model.addAttribute("active", "home");
        model.addAttribute("category", cat);
        model.addAttribute("post", post);
        return "board-detail"; // 필요 시 템플릿 추가
    }

    /** 글 삭제 (옵션) */
    @PostMapping("/{cat}/{id}/delete")
    public String delete(@PathVariable String cat, @PathVariable Long id) {
        validateCategory(cat);
        postService.delete(id);
        return "redirect:/board/" + cat;
    }

    private void validateCategory(String cat) {
        if (!ALLOWED.contains(cat)) {
            throw new IllegalArgumentException("Unknown category: " + cat);
        }
    }
}
