package com.SYUcap.SYUcap.Comment;

import com.SYUcap.SYUcap.User.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;

    // 댓글 등록
    @PostMapping("/write")
    public String addComment(
            @RequestParam Long boardId,
            @RequestParam String cat,
            @RequestParam String content,
            Authentication auth
    ) {
        if (auth == null) {
            return "redirect:/login"; // 로그인이 안 되어있으면 로그인 페이지로
        }

        // 로그인한 사용자의 ID (userId) 가져오기
        CustomUser user = (CustomUser) auth.getPrincipal();
        commentService.createComment(boardId, content, user.getUsername()); // CustomUser의 getUsername()이 userId를 반환한다고 가정

        String encodedCat = URLEncoder.encode(cat, StandardCharsets.UTF_8);
        return "redirect:/board/" + encodedCat + "/" + boardId;
    }

    // 댓글 수정
    @PostMapping("/update")
    public String updateComment(
            @RequestParam Long boardId,
            @RequestParam String cat,
            @RequestParam Long commentId,
            @RequestParam String content,
            Authentication auth
    ) {
        if (auth == null) {
            return "redirect:/login";
        }

        CustomUser user = (CustomUser) auth.getPrincipal();
        // 서비스에 '현재 로그인한 사람의 ID'를 같이 넘겨서 검증 요청
        commentService.updateComment(commentId, content, user.getUsername());

        String encodedCat = URLEncoder.encode(cat, StandardCharsets.UTF_8);
        return "redirect:/board/" + encodedCat + "/" + boardId;
    }

    // 댓글 삭제
    @PostMapping("/delete")
    public String deleteComment(
            @RequestParam Long boardId,
            @RequestParam String cat,
            @RequestParam Long commentId,
            Authentication auth
    ) {
        if (auth == null) {
            return "redirect:/login";
        }

        CustomUser user = (CustomUser) auth.getPrincipal();
        // 서비스에 '현재 로그인한 사람의 ID'를 같이 넘겨서 검증 요청
        commentService.deleteComment(commentId, user.getUsername());

        String encodedCat = URLEncoder.encode(cat, StandardCharsets.UTF_8);
        return "redirect:/board/" + encodedCat + "/" + boardId;
    }
}