package com.example.mypage;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller

public class MypageController {

@GetMapping("/mypage")
public String profile(){
    return "mypage/profile";
}
@GetMapping("/mypage/settings")
public String settings(){
    return "mypage/settings";
}

}
