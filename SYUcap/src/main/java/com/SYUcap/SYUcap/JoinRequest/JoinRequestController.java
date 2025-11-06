package com.SYUcap.SYUcap.JoinRequest;

import com.SYUcap.SYUcap.Group.GroupService;
import com.SYUcap.SYUcap.User.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**가입 신청 관리 컨트롤러 - 그룹 가입 신청/승인/거절 기능*/
@Controller
@RequestMapping("/join-requests")
@RequiredArgsConstructor
public class JoinRequestController {

    private final JoinRequestService joinRequestService;
    private final GroupService groupService;

    // 그룹 가입 신청 처리
    @PostMapping("/request")
    public String requestJoin(@RequestParam Long groupId,
                              @RequestParam(required = false) String message,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUser)) {
            return "redirect:/login";
        }

        CustomUser currentUser = (CustomUser) auth.getPrincipal();

        try {
            joinRequestService.requestJoin(currentUser.getId(), groupId, message);
            redirectAttributes.addFlashAttribute("message", "가입 신청이 완료되었습니다.");

        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "가입 신청 중 오류가 발생했습니다.");
        }

        return "redirect:/groups/" + groupId;
    }

    // 가입 신청 승인
    @PostMapping("/{requestId}/approve")
    public String approveRequest(@PathVariable Long requestId,
                                 @RequestParam Long groupId,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUser)) {
            return "redirect:/login";
        }

        CustomUser currentUser = (CustomUser) auth.getPrincipal();

        try {
            joinRequestService.approveJoinRequest(requestId, currentUser.getId());
            redirectAttributes.addFlashAttribute("message", "가입 신청을 승인했습니다.");

        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "승인 처리 중 오류가 발생했습니다.");
        }

        return "redirect:/groups/" + groupId;
    }

    // 가입 신청 거절
    @PostMapping("/{requestId}/reject")
    public String rejectRequest(@PathVariable Long requestId,
                                @RequestParam Long groupId,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUser)) {
            return "redirect:/login";
        }

        CustomUser currentUser = (CustomUser) auth.getPrincipal();

        try {
            joinRequestService.rejectJoinRequest(requestId, currentUser.getId());
            redirectAttributes.addFlashAttribute("message", "가입 신청을 거절했습니다.");

        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "거절 처리 중 오류가 발생했습니다.");
        }

        return "redirect:/groups/" + groupId;
    }

    // 가입 신청 취소 (신청자가 직접)
    @PostMapping("/{requestId}/cancel")
    public String cancelRequest(@PathVariable Long requestId,
                                @RequestParam Long groupId,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUser)) {
            return "redirect:/login";
        }

        CustomUser currentUser = (CustomUser) auth.getPrincipal();

        try {
            joinRequestService.cancelJoinRequest(requestId, currentUser.getId());
            redirectAttributes.addFlashAttribute("message", "가입 신청을 취소했습니다.");

        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "취소 처리 중 오류가 발생했습니다.");
        }

        return "redirect:/groups/" + groupId;
    }

    // 내 가입 신청 내역 조회
    @GetMapping("/my-requests")
    public String myRequests(Authentication auth, Model model) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUser)) {
            return "redirect:/login";
        }

        CustomUser currentUser = (CustomUser) auth.getPrincipal();

        try {
            List<JoinRequests> myRequests = joinRequestService.getUserJoinRequests(currentUser.getId());

            model.addAttribute("active", "groups");
            model.addAttribute("myRequests", myRequests);
            model.addAttribute("currentUser", currentUser);

            return "join-requests-my";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/groups";
        }
    }

    //그룹장이 받은 가입 신청 목록
    @GetMapping("/pending")
    public String pendingRequests(Authentication auth, Model model) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUser)) {
            return "redirect:/login";
        }

        CustomUser currentUser = (CustomUser) auth.getPrincipal();

        try {
            List<JoinRequests> pendingRequests = joinRequestService.getPendingRequestsForLeader(currentUser.getId());

            model.addAttribute("active", "groups");
            model.addAttribute("pendingRequests", pendingRequests);
            model.addAttribute("currentUser", currentUser);

            return "join-requests-pending";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/groups";
        }
    }
}