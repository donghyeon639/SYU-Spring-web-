package com.SYUcap.SYUcap.Board;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.SYUcap.SYUcap.User.UserRepository;
import com.SYUcap.SYUcap.User.CustomUser;
import com.SYUcap.SYUcap.Group.GroupService;
import com.SYUcap.SYUcap.Group.Groups;
import com.SYUcap.SYUcap.JoinRequest.JoinRequestService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {

    private final BoardService boardService;
    private final UserRepository userRepository;
    private final GroupService groupService;
    private final JoinRequestService joinRequestService;

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
        List<Board> boards = boardService.getByCategorySorted(cat);
        model.addAttribute("active", "home");
        model.addAttribute("category", cat);
        model.addAttribute("posts", boards);
        return "board-list";
    }

    /** 글쓰기 폼 */
    @GetMapping("/{cat}/write")
    public String writeForm(@PathVariable("cat") String cat, Model model, Authentication auth) {
        model.addAttribute("active", "home");
        model.addAttribute("category", cat);
        String authorName = null;
        if (auth != null && auth.getPrincipal() instanceof CustomUser cu) {
            authorName = cu.getUserName();
        }
        model.addAttribute("authorName", authorName);
        model.addAttribute("post", new Board());
        model.addAttribute("isEdit", false);  // 새 글 작성
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
            Authentication auth
    ) {
        // 로그인 사용자 정보(Authentication) 전달하여 작성자 연결
        boardService.createPost(cat, title, content, location, meetingStartTime, meetingEndTime, limitCount, auth);
        String encodedCat = URLEncoder.encode(cat, StandardCharsets.UTF_8);
        return "redirect:/board/" + encodedCat; // 저장 후 목록으로
    }

    /** 글 상세 보기 */
    @GetMapping("/{cat}/{id}")
    public String detail(@PathVariable String cat, @PathVariable Long id, Model model,Authentication auth) {
        Board board = boardService.getById(id); // 존재하지 않으면 예외
        if (!cat.equals(board.getCategory())) {
            // URL의 카테고리와 실제 글의 카테고리가 다르면 해당 카테고리로 리다이렉트
            return "redirect:/board/" + board.getCategory() + "/" + id;
        }
        model.addAttribute("active", "home");
        model.addAttribute("category", cat);
        model.addAttribute("post", board);

        Optional<Groups> groupOpt = groupService.getGroupByBoardId(id);
        if (groupOpt.isPresent()) {
            Groups group = groupOpt.get();
            model.addAttribute("group", group);

            // 로그인한 경우 사용자 상태 확인
            if (auth != null && auth.getPrincipal() instanceof CustomUser) {
                CustomUser currentUser = (CustomUser) auth.getPrincipal();
                boolean isMember = groupService.isMember(currentUser.getId(), group.getId());
                boolean isLeader = groupService.isLeader(currentUser.getId(), group.getId());
                boolean hasPendingRequest = joinRequestService.hasPendingRequest(currentUser.getId(), group.getId());

                model.addAttribute("isMember", isMember);
                model.addAttribute("isLeader", isLeader);
                model.addAttribute("hasPendingRequest", hasPendingRequest);
            }
        }
        return "board-detail";
    }

    /** 글 수정 폼 */
    @GetMapping("/{cat}/{id}/edit")
    public String editForm(@PathVariable String cat, @PathVariable Long id, Model model, Authentication auth) {
        Board board = boardService.getById(id);
        if (!cat.equals(board.getCategory())) {
            return "redirect:/board/" + board.getCategory() + "/" + id + "/edit";
        }
        model.addAttribute("active", "home");
        model.addAttribute("category", cat);
        model.addAttribute("post", board);
        model.addAttribute("authorName", board.getAuthorName());
        model.addAttribute("isEdit", true);  // 수정 모드
        return "board-form";
    }

    /** 글 수정 처리 */
    @PostMapping("/{cat}/{id}/edit")
    public String editSubmit(
            @PathVariable String cat,
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime meetingStartTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime meetingEndTime,
            @RequestParam(required = false) Integer limitCount
    ) {
        boardService.updatePost(id, title, content, location, meetingStartTime, meetingEndTime, limitCount);
        String encodedCat = URLEncoder.encode(cat, StandardCharsets.UTF_8);
        return "redirect:/board/" + encodedCat + "/" + id; // 수정 후 상세페이지로
    }

    /** 글 삭제 */
    @PostMapping("/{cat}/{id}/delete")
    public String delete(@PathVariable String cat, @PathVariable Long id) {
        boardService.delete(id);
        String encodedCat = URLEncoder.encode(cat, StandardCharsets.UTF_8);
        return "redirect:/board/" + encodedCat;
    }
}