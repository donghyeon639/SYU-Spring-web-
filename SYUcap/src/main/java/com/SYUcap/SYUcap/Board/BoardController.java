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
import com.SYUcap.SYUcap.Comment.CommentService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {

    private final BoardService boardService;
    private final GroupService groupService;
    private final JoinRequestService joinRequestService;
    private final CommentService commentService;

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
        model.addAttribute("isEdit", false);
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
        boardService.createPost(cat, title, content, location, meetingStartTime, meetingEndTime, limitCount, auth);
        String encodedCat = URLEncoder.encode(cat, StandardCharsets.UTF_8);
        return "redirect:/board/" + encodedCat;
    }

    /** 글 상세 보기 (수정됨: currentUserId 전달) */
    @GetMapping("/{cat}/{id}")
    public String detail(@PathVariable String cat, @PathVariable Long id, Model model, Authentication auth) {
        Board board = boardService.getById(id);
        if (!cat.equals(board.getCategory())) {
            return "redirect:/board/" + board.getCategory() + "/" + id;
        }
        model.addAttribute("active", "home");
        model.addAttribute("category", cat);
        model.addAttribute("post", board);

        // 댓글 목록
        model.addAttribute("comments", commentService.getCommentsByBoardId(id));

        // [추가] 현재 로그인한 사용자 아이디 확인 (버튼 표시용)
        String currentUserId = null;
        if (auth != null && auth.getPrincipal() instanceof CustomUser cu) {
            currentUserId = cu.getUserName();
        }
        model.addAttribute("currentUserId", currentUserId);

        Optional<Groups> groupOpt = groupService.getGroupByBoardId(id);
        if (groupOpt.isPresent()) {
            Groups group = groupOpt.get();
            model.addAttribute("group", group);

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
        model.addAttribute("isEdit", true);
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
        return "redirect:/board/" + encodedCat + "/" + id;
    }

    /** 글 삭제 */
    @PostMapping("/{cat}/{id}/delete")
    public String delete(@PathVariable String cat, @PathVariable Long id) {
        boardService.delete(id);
        String encodedCat = URLEncoder.encode(cat, StandardCharsets.UTF_8);
        return "redirect:/board/" + encodedCat;
    }
}