package com.SYUcap.SYUcap.User;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class MypageController {

    private final UserService userService;

    /** 마이페이지 메인 (조회) - 로그인 유저 기준 */
    @GetMapping("/mypage")
    public String mypage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Users me = userService.getCurrentUser(userDetails);
        model.addAttribute("user", me);
        return "mypage";
    }

    /** 마이페이지 수정 폼 - 로그인 유저 기준 */
    @GetMapping("/mypage/edit")
    public String mypageEdit(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Users me = userService.getCurrentUser(userDetails);
        model.addAttribute("user", me);
        return "mypage-edit";
    }

    /** 마이페이지 수정 처리 -  POST 사용 */
    @PostMapping("/mypage/edit")
    public String updateMyInfo(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam("userName") String userName,
                               @RequestParam(value = "password", required = false) String password,
                               @RequestParam(value = "passwordConfirm", required = false) String passwordConfirm,
                               @RequestParam(value = "newUserId", required = false) String newUserId) {
        userService.updateMyInfo(userDetails, userName, password, passwordConfirm, newUserId);
        return "redirect:/mypage";
    }

    /** 계정 삭제 - 게시판 삭제처럼 DELETE 메서드 사용 (REST 스타일) */
    @DeleteMapping("/mypage/{id}")
    public String deleteMyAccount(@AuthenticationPrincipal UserDetails userDetails,
                                  @PathVariable Long id) {
        // id는 URL 형식 통일을 위한 것으로, 실제 삭제는 로그인 유저 기준으로 처리
        userService.deleteMyAccount(userDetails);
        return "redirect:/home";
    }

    /** 로그아웃 기능: 스프링 시큐리티 로그아웃 URL과 연동 */
    @PostMapping("/logout")
    public String logout() {
        // SecurityConfig에서 logoutSuccessUrl("/home") 설정과 맞춰 홈으로 이동
        return "redirect:/home";
    }

}
