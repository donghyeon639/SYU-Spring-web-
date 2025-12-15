package com.SYUcap.SYUcap.Board;

import com.SYUcap.SYUcap.User.UserRepository;
import com.SYUcap.SYUcap.User.Users;
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
    private final UserRepository userRepository;

    // 허용 카테고리 (검증 & 라벨 표기)
    private static final List<String> ALLOWED = List.of("게임", "스터디", "영화", "운동", "밥약");

    /** 전체 목록 */
    @GetMapping
    public String listAll(Model model) {
        List<Board> boards = boardService.getAllSorted();
        model.addAttribute("active", "home");
        model.addAttribute("category", "전체");
        model.addAttribute("posts", boards);
        return "board-list";
    }

    /** 카테고리별 목록 */
    @GetMapping("/{cat}")
    public String listByCategory(@PathVariable("cat") String cat, Model model) {
        validateCategory(cat);
        List<Board> boards = boardService.getByCategorySorted(cat);
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
            @RequestParam(defaultValue = "익명") String authorName,
            Model model
    ) {
        validateCategory(cat);

        if (title == null || title.trim().isEmpty()) {
            model.addAttribute("category", cat);
            model.addAttribute("post", new Board());
            model.addAttribute("errorMessage", "제목을 입력하세요");
            return "board-form";
        }

        if (content == null || content.trim().isEmpty()) {
            model.addAttribute("category", cat);
            model.addAttribute("post", new Board());
            model.addAttribute("errorMessage", "내용을 입력하세요");
            return "board-form";
        }

        Board board = new Board();
        board.setCategory(cat);
        board.setTitle(title);
        board.setContent(content);
        board.setLocation(location);
        board.setMeetingStartTime(meetingStartTime);
        board.setMeetingEndTime(meetingEndTime);
        board.setLimitCount(limitCount);
        // 작성자 Users 연관 설정: authorName으로 조회, 없으면 생성
        if (authorName != null && !authorName.isBlank() && !"익명".equals(authorName)) {
            Users user = userRepository.findAll().stream()
                    .filter(u -> authorName.equals(u.getUserName()))
                    .findFirst()
                    .orElseGet(() -> {
                        Users u = new Users();
                        u.setUserName(authorName);
                        u.setUserId("auto_" + java.util.UUID.randomUUID());
                        u.setPassword("nopass");
                        return userRepository.save(u);
                    });
            board.setUser(user);
        }
        board.setAuthorName(authorName);

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

    /** 글 수정 폼 */
    @GetMapping("/{cat}/{id}/edit")
    public String editForm(@PathVariable String cat, @PathVariable Long id, Model model) {
        validateCategory(cat);
        Board board = boardService.findById(id);
        model.addAttribute("active", "home");
        model.addAttribute("category", cat);
        model.addAttribute("post", board);
        return "board-form";
    }

    /** 글 수정 저장 */
    @PostMapping("/{cat}/{id}/edit")
    public String editSubmit(
            @PathVariable String cat,
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime meetingStartTime,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime meetingEndTime,
            @RequestParam(required = false) Integer limitCount,
            @RequestParam(defaultValue = "익명") String authorName,
            Model model
    ) {
        validateCategory(cat);
        if (title == null || title.trim().isEmpty()) {
            model.addAttribute("category", cat);
            model.addAttribute("post", boardService.findById(id));
            model.addAttribute("errorMessage", "제목을 입력하세요");
            return "board-form";
        }
        if (content == null || content.trim().isEmpty()) {
            model.addAttribute("category", cat);
            model.addAttribute("post", boardService.findById(id));
            model.addAttribute("errorMessage", "내용을 입력하세요");
            return "board-form";
        }
        Board board = boardService.findById(id);
        board.setTitle(title);
        board.setContent(content);
        board.setLocation(location);
        board.setMeetingStartTime(meetingStartTime);
        board.setMeetingEndTime(meetingEndTime);
        // limitCount가 null이면 기존 값 유지, 기존 값도 없으면 기본 1 설정
        Integer finalLimit = (limitCount != null) ? limitCount : (board.getLimitCount() != null ? board.getLimitCount() : 1);
        board.setLimitCount(finalLimit);
        board.setAuthorName(authorName);
        boardService.save(board);
        String encodedCat = java.net.URLEncoder.encode(cat, java.nio.charset.StandardCharsets.UTF_8);
        return "redirect:/board/" + encodedCat + "/" + id;
    }

    private void validateCategory(String cat) {
        if (!ALLOWED.contains(cat)) {
            throw new IllegalArgumentException("Unknown category: " + cat);
        }
    }

    /** 게시글 검색 */
    @GetMapping("/search")
    public String search(@RequestParam String keyword, Model model) {
        List<Board> boards = boardService.searchBoards(keyword);

        model.addAttribute("active", "home");
        model.addAttribute("category", "검색결과");
        model.addAttribute("posts", boards);

        return "board-list";
    }
}

