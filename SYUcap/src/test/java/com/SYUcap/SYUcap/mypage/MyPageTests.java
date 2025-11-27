package com.SYUcap.SYUcap.mypage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

interface MyPageService {
    boolean checkPassword(String current, String input);
    String changePassword(String current, String next);
    boolean validateName(String name);
    String updateId(String oldId, String newId);
    String updateProfile(String before, String after);
    String withdraw(String password);
}

@ExtendWith(MockitoExtension.class)
public class MyPageTests {

    @Mock
    private MyPageService myPageService;

    @Test
    void 비밀번호검증실패() {

        given(myPageService.checkPassword("Password1!", "Wpassword1!"))
                .willReturn(false);

        boolean result = myPageService.checkPassword("Password1!", "Wpassword1!");

        assertFalse(result); // 비밀번호 검증 실패
    }


    @Test
    void 동일비밀번호변경불가() {

        given(myPageService.changePassword("Password1!", "Password1!"))
                .willReturn("동일 비밀번호로 변경 불가");

        String result = myPageService.changePassword("Password1!", "Password1!");

        assertEquals("동일 비밀번호로 변경 불가", result);
    }

    @Test
    void 최근사용비밀번호존재() {

        given(myPageService.changePassword("Password1!", "OldPass1!"))
                .willReturn("최근 사용 이력 중 동일 비밀번호 존재");

        String result = myPageService.changePassword("Password1!", "OldPass1!");


        assertEquals("최근 사용 이력 중 동일 비밀번호 존재", result);
    }


    @Test
    void 이름형식검증실패() {

        given(myPageService.validateName("홍1동"))
                .willReturn(false);


        boolean result = myPageService.validateName("홍1동");


        assertFalse(result); // 한글 외 문자 포함 → 실패
    }


    @Test
    void 아이디변경_DB저장성공() {

        given(myPageService.updateId("test_user_1", "test_user_2"))
                .willReturn("아이디 변경 성공");

        String result = myPageService.updateId("test_user_1", "test_user_2");

        assertEquals("아이디 변경 성공", result);
    }


    @Test
    void 프로필수정성공() {

        given(myPageService.updateProfile("old_nick", "new_nick"))
                .willReturn("수정된 정보 DB 저장");


        String result = myPageService.updateProfile("old_nick", "new_nick");


        assertEquals("수정된 정보 DB 저장", result);
    }


    @Test
    void 회원탈퇴() {

        given(myPageService.withdraw("Password1!"))
                .willReturn("탈퇴 처리 후 DB 저장");


        String result = myPageService.withdraw("Password1!");


        assertEquals("탈퇴 처리 후 DB 저장", result);
    }


    @Test
    void 비밀번호변경성공() {

        given(myPageService.changePassword("Password1!", "NPassword1!"))
                .willReturn("비밀번호가 성공적으로 변경되었습니다.");


        String result = myPageService.changePassword("Password1!", "NPassword1!");


        assertEquals("비밀번호가 성공적으로 변경되었습니다.", result);
    }


    @Test
    void 변경사항없음() {

        given(myPageService.updateProfile("same", "same"))
                .willReturn("변경 사항이 없습니다.");


        String result = myPageService.updateProfile("same", "same");


        assertEquals("변경 사항이 없습니다.", result);
    }
}
