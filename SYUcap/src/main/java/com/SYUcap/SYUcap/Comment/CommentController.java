package com.SYUcap.SYUcap.Comment;

import com.SYUcap.SYUcap.User.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{cat}/{id}/comment")
    public String addComment(@PathVariable("cat") String category,
                             @PathVariable("id") Long boardId,
                             @RequestParam("content") String content,
                             Authentication auth) {
        String authorName = "익명";
        if (auth != null && auth.getPrincipal() instanceof CustomUser cu) {
            authorName = cu.getUserName();
        }
        commentService.createComment(boardId, content, authorName);
        String encodedCat = URLEncoder.encode(category, StandardCharsets.UTF_8);
        return "redirect:/board/" + encodedCat + "/" + boardId;
    }
}

