package com.SYUcap.SYUcap.Group;

import com.SYUcap.SYUcap.GroupMember.GroupMembers;
import com.SYUcap.SYUcap.JoinRequest.JoinRequestService;
import com.SYUcap.SYUcap.JoinRequest.JoinRequests;
import com.SYUcap.SYUcap.User.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**그룹 관리 컨트롤러 - 그룹 조회, 멤버 관리 기능*/
@Controller
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final JoinRequestService joinRequestService;

    //그룹 메인 페이지 (내가 속한 그룹 목록)
    @GetMapping
    public String groupsMain(@RequestParam(required = false) String category,
                             Model model,
                             Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUser)) {
            return "redirect:/login";
        }

        CustomUser currentUser = (CustomUser) auth.getPrincipal();
        Long userId = currentUser.getId();

        // 카테고리별 내가 참여한 그룹들 조회
        List<GroupMembers> myGroups = groupService.getUserGroupsByCategory(userId, category);

        // 카테고리별 내가 리더인 그룹들 조회
        List<Groups> leaderGroups = groupService.getLeaderGroupsByCategory(userId, category);

        // 카테고리 목록
        String[] categories = {"게임", "스터디", "영화", "운동", "밥약"};

        model.addAttribute("active", "groups");
        model.addAttribute("myGroups", myGroups);
        model.addAttribute("leaderGroups", leaderGroups);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("currentUser", currentUser);

        return "groups";
    }

    // 그룹 상세 페이지
    @GetMapping("/{groupId}")
    public String groupDetail(@PathVariable Long groupId,
                              Model model,
                              Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUser)) {
            return "redirect:/login";
        }

        CustomUser currentUser = (CustomUser) auth.getPrincipal();

        try {
            // 그룹 정보 조회
            Optional<Groups> groupOpt = groupService.getGroupById(groupId);
            if (groupOpt.isEmpty()) {
                return "redirect:/groups";
            }

            Groups group = groupOpt.get();

            // 그룹 멤버 목록 조회
            List<GroupMembers> members = groupService.getGroupMembers(groupId);

            // 현재 사용자의 상태 확인
            boolean isMember = groupService.isMember(currentUser.getId(), groupId);
            boolean isLeader = groupService.isLeader(currentUser.getId(), groupId);
            boolean hasPendingRequest = joinRequestService.hasPendingRequest(currentUser.getId(), groupId);

            // 그룹장인 경우 대기 중인 가입 신청 조회
            List<JoinRequests> pendingRequests = null;
            if (isLeader) {
                pendingRequests = joinRequestService.getPendingJoinRequests(groupId);
            }

            model.addAttribute("active", "groups");
            model.addAttribute("group", group);
            model.addAttribute("board", group.getBoard());
            model.addAttribute("members", members);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("isMember", isMember);
            model.addAttribute("isLeader", isLeader);
            model.addAttribute("hasPendingRequest", hasPendingRequest);
            model.addAttribute("pendingRequests", pendingRequests);

            return "group-detail";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/groups";
        }
    }

    //그룹 탈퇴 처리
    @PostMapping("/{groupId}/leave")
    public String leaveGroup(@PathVariable Long groupId,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUser)) {
            return "redirect:/login";
        }

        CustomUser currentUser = (CustomUser) auth.getPrincipal();

        try {
            groupService.leaveGroup(currentUser.getId(), groupId);
            redirectAttributes.addFlashAttribute("message", "그룹에서 탈퇴했습니다.");
            return "redirect:/groups";

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/groups/" + groupId;
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "탈퇴 처리 중 오류가 발생했습니다.");
            return "redirect:/groups/" + groupId;
        }
    }

    // 멤버 강제 퇴장 (그룹장 권한)
    @PostMapping("/{groupId}/kick/{userId}")
    public String kickMember(@PathVariable Long groupId,
                             @PathVariable Long userId,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUser)) {
            return "redirect:/login";
        }

        CustomUser currentUser = (CustomUser) auth.getPrincipal();

        try {
            groupService.kickMember(userId, groupId, currentUser.getId());
            redirectAttributes.addFlashAttribute("message", "멤버를 그룹에서 내보냈습니다.");

        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "멤버 내보내기에 실패했습니다.");
        }

        return "redirect:/groups/" + groupId;
    }

    // 그룹장 위임
    @PostMapping("/{groupId}/transfer/{userId}")
    public String transferLeadership(@PathVariable Long groupId,
                                     @PathVariable Long userId,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUser)) {
            return "redirect:/login";
        }

        CustomUser currentUser = (CustomUser) auth.getPrincipal();

        try {
            groupService.transferLeadership(currentUser.getId(), userId, groupId);
            redirectAttributes.addFlashAttribute("message", "그룹장 권한을 위임했습니다.");

        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "권한 위임에 실패했습니다.");
        }

        return "redirect:/groups/" + groupId;
    }

    //그룹 해체 (그룹장만 가능)
    @PostMapping("/{groupId}/delete")
    public String deleteGroup(@PathVariable Long groupId,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUser)) {
            return "redirect:/login";
        }

        CustomUser currentUser = (CustomUser) auth.getPrincipal();

        try {
            groupService.deleteGroup(groupId, currentUser.getId());
            redirectAttributes.addFlashAttribute("message", "그룹을 해체했습니다.");
            return "redirect:/groups";

        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "그룹 해체에 실패했습니다.");
        }

        return "redirect:/groups/" + groupId;
    }

    //그룹 마감 (그룹장만 가능)
    @PostMapping("/{groupId}/close")
    public String closeGroup(@PathVariable Long groupId,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUser)) {
            return "redirect:/login";
        }

        CustomUser currentUser = (CustomUser) auth.getPrincipal();

        try {
            groupService.closeGroup(groupId, currentUser.getId());
            redirectAttributes.addFlashAttribute("message", "그룹을 마감했습니다. 더 이상 가입 신청을 받지 않습니다.");

        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "그룹 마감에 실패했습니다.");
        }

        return "redirect:/groups/" + groupId;
    }

    // 그룹 마감 해제 (그룹장만 가능)
    @PostMapping("/{groupId}/reopen")
    public String reopenGroup(@PathVariable Long groupId,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUser)) {
            return "redirect:/login";
        }

        CustomUser currentUser = (CustomUser) auth.getPrincipal();

        try {
            groupService.reopenGroup(groupId, currentUser.getId());
            redirectAttributes.addFlashAttribute("message", "그룹 마감을 해제했습니다. 다시 가입 신청을 받습니다.");

        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "마감 해제에 실패했습니다.");
        }

        return "redirect:/groups/" + groupId;
    }
}
