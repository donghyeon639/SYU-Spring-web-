package com.SYUcap.SYUcap.Board;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {

    private final BoardService boardService;

    // 허용 카테고리 (검증 & 라벨 표기)
    private static final List<String> ALLOWED = List.of("게임", "스터디", "영화", "운동", "밥약");

    /** 전체 목록 */
    @GetMapping
    public String listAll(Model model) {
        List<Board> boards = boardService.findAll().stream()
                .sorted(Comparator.comparing(Board::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        model.addAttribute("active", "home");
        model.addAttribute("category", "전체");
        model.addAttribute("posts", boards);
        return "board-list";
    }

    /** 카테고리별 목록 */
    @GetMapping("/{cat}")
    public String listByCategory(@PathVariable("cat") String cat, Model model) {
        validateCategory(cat);

        List<Board> boards = boardService.findAll().stream()
                .filter(p -> cat.equals(p.getCategory()))
                .sorted(Comparator.comparing(Board::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        model.addAttribute("active", "home");
        model.addAttribute("category", cat);
        model.addAttribute("posts", boards);
        return "board-list";
    }

    /** 글쓰기 폼 */
    @GetMapping("/{cat}/write")
    public String writeForm(@PathVariable("cat") String cat, Model model) {
        validateCategory(cat);
        model.addAttribute("active", "home");
        model.addAttribute("category", cat);
        model.addAttribute("post", new Board());
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

        Board board = new Board();
        board.setCategory(cat);
        board.setTitle(title);
        board.setContent(content);
        board.setLocation(location);
        board.setMeetingStartTime(meetingStartTime);
        board.setMeetingEndTime(meetingEndTime);
        board.setLimitCount(limitCount);
        board.setAuthorName(authorName);
        // createdAt은 @PrePersist에서 자동 세팅

        boardService.save(board);
        String encodedCat = URLEncoder.encode(cat, StandardCharsets.UTF_8);
        return "redirect:/board/" + encodedCat; // 저장 후 목록으로
    }

    /** 글 상세 보기 */
    @GetMapping("/{cat}/{id}")
    public String detail(@PathVariable String cat, @PathVariable Long id, Model model) {
        validateCategory(cat);
        Board board = boardService.findById(id); // 존재하지 않으면 예외
        if (!cat.equals(board.getCategory())) {
            // URL의 카테고리와 실제 글의 카테고리가 다르면 해당 카테고리로 리다이렉트
            return "redirect:/board/" + board.getCategory() + "/" + id;
        }
        model.addAttribute("active", "home");
        model.addAttribute("category", cat);
        model.addAttribute("post", board);
        return "board-detail";
    }

    /** 글 삭제 */
    @PostMapping("/{cat}/{id}/delete")
    public String delete(@PathVariable String cat, @PathVariable Long id) {
        validateCategory(cat);
        boardService.delete(id);
        String encodedCat = URLEncoder.encode(cat, StandardCharsets.UTF_8);
        return "redirect:/board/" + encodedCat;
    }

    private void validateCategory(String cat) {
        if (!ALLOWED.contains(cat)) {
            throw new IllegalArgumentException("Unknown category: " + cat);
        }
    }
}