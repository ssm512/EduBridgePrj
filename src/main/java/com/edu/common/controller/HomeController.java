package com.edu.common.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * 루트("/"): 로그인 화면.
     * 단, 이미 로그인된(ACCESS_TOKEN 쿠키 보유) 상태면 역할 화면으로 보낸다.
     */
    @GetMapping("/")
    public String home() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/home";
        }
        return "loginForm";
    }

    /** 로그인 화면 직접 접근용 */
    @GetMapping("/loginForm.html")
    public String loginForm() {
        return "loginForm";
    }

    /** 회원가입 화면 */
    @GetMapping("/signInForm.html")
    public String signInForm() {
        return "signInForm";
    }
}
