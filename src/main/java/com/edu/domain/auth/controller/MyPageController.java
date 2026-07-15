package com.edu.domain.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 마이페이지 화면 라우팅.
 * 데이터는 화면 로드 후 JS(fetch)가 GET/PUT /api/auth/me 를 호출해 채우고 저장한다.
 * (인증 필요 - SecurityConfig anyRequest().authenticated())
 */
@Controller
public class MyPageController {

    /** GET /mypage.html - 마이페이지 화면 */
    @GetMapping("/mypage.html")
    public String myPage() {
        return "mypage";
    }
}
