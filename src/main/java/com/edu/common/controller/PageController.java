package com.edu.common.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 서버 렌더링(Thymeleaf) 페이지 라우팅.
 *
 * 로그인 성공 후 프론트는 "/home" 으로 이동만 하면 되고,
 * 실제 어느 역할 화면으로 보낼지는 여기서 서버가 권한을 보고 결정한다.
 * (권한은 JWT roles 클레임 → ROLE_ADMIN / ROLE_TEACHER / ROLE_STUDENT / ROLE_PARENT)
 */
@Controller
public class PageController {

    /** 로그인 후 진입점: 역할에 맞는 화면으로 리다이렉트 */
    @GetMapping("/home")
    public String home(Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN"))   return "redirect:/adminPage";
        if (hasRole(authentication, "ROLE_TEACHER")) return "redirect:/teacherPage";
        if (hasRole(authentication, "ROLE_STUDENT")) return "redirect:/studentPage";
        if (hasRole(authentication, "ROLE_PARENT"))  return "redirect:/parentPage";
        // 알 수 없는 권한이면 로그인으로
        return "redirect:/";
    }

    /** 관리자 메인 */
    @GetMapping("/adminPage")
    public String admin() {
        return "admin/index";
    }

    /** 강사 메인 */
    @GetMapping("/teacherPage")
    public String teacher() {
        return "teacher/index";
    }

    /** 학생 메인 */
    @GetMapping("/studentPage")
    public String student() {
        return "student/index";
    }

    /** 학부모 메인 */
    @GetMapping("/parentPage")
    public String parent() {
        return "parent/index";
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (role.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
