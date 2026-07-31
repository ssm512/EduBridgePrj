package com.edu.domain.member.controller;

import com.edu.common.dto.PageResponse;
import com.edu.domain.member.dto.PasswordResetResponse;
import com.edu.domain.member.dto.UserSearchRequest;
import com.edu.domain.member.dto.UserUpdateRequest;
import com.edu.domain.member.dto.UserDetailResponse;
import com.edu.domain.member.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원관리 API (명세서 USER-01 ~ USER-03)
 * /api/users 매핑 (2026-07-14 API URL /api 프리픽스 통일, SecurityConfig에 /api/users/** ADMIN 규칙)
 * 전체 ADMIN 전용
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * USER-01 GET /api/users - 회원 목록 조회
     * 권한/상태/키워드 필터 + 페이징
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserDetailResponse> getUsers(@ModelAttribute UserSearchRequest cond) {
        return userService.getUsers(cond);
    }

    /**
     * USER-02 GET /api/users/{userId} - 회원 상세 조회
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDetailResponse getUser(@PathVariable Long userId) {
        return userService.getUser(userId);
    }

    /**
     * USER-03 PUT /api/users/{userId} - 회원 수정
     * 이름/이메일/연락처/상태 수정
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDetailResponse updateUser(@PathVariable Long userId,
                                         @Valid @RequestBody UserUpdateRequest request) {
        return userService.updateUser(userId, request);
    }

    /**
     * POST /api/users/{userId}/password-reset - 관리자 비밀번호 초기화 (A안)
     * 임시 비밀번호를 발급하고 해당 회원은 다음 로그인 시 비밀번호 변경이 강제된다.
     * 임시 비밀번호는 이 응답에서 한 번만 확인 가능.
     */
    @PostMapping("/{userId}/password-reset")
    @PreAuthorize("hasRole('ADMIN')")
    public PasswordResetResponse resetPassword(@PathVariable Long userId) {
        return userService.resetPassword(userId);
    }
}
