package com.edu.domain.classroom.controller;

import com.edu.common.dto.PageResponse;
import com.edu.domain.classroom.dto.ClassCreateRequest;
import com.edu.domain.classroom.dto.ClassDetailResponse;
import com.edu.domain.classroom.dto.ClassResponse;
import com.edu.domain.classroom.dto.ClassSearchRequest;
import com.edu.domain.classroom.dto.ClassUpdateRequest;
import com.edu.domain.classroom.service.ClassService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 반관리 API (명세서 CLS-01 ~ CLS-04)
 * /api/classes 매핑 (2026-07-14 API URL /api 프리픽스 통일)
 * 권한: 등록/수정 ADMIN, 목록/상세 ADMIN·TEACHER
 */
@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    /**
     * CLS-01 POST /api/classes - 반 등록
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ClassResponse createClass(@Valid @RequestBody ClassCreateRequest request) {
        return classService.createClass(request);
    }

    /**
     * CLS-02 GET /api/classes - 반 목록 조회
     * 상태/키워드(반이름·과목·강사명) + 페이징
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public PageResponse<ClassResponse> getClasses(@ModelAttribute ClassSearchRequest cond) {
        return classService.getClasses(cond);
    }

    /**
     * CLS-03 GET /api/classes/{classId} - 반 상세 조회 (수강 학생 목록 포함)
     * [수정 2026-07-16] TEACHER는 본인 담당반만 조회 가능하도록 소유권 검증 추가 (ADMIN은 전체 허용)
     */
    @GetMapping("/{classId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ClassDetailResponse getClass(@PathVariable Long classId, Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return classService.getClass(classId);
        }
        return classService.getClassForTeacher(classId, authentication.getName());
    }

    /**
     * CLS-04 PUT /api/classes/{classId} - 반 수정
     */
    @PutMapping("/{classId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClassResponse updateClass(@PathVariable Long classId,
                                     @Valid @RequestBody ClassUpdateRequest request) {
        return classService.updateClass(classId, request);
    }

    /**
     * [추가 2026-07-16] GET /api/classes/my - 로그인 강사의 담당반 목록 (teacher/classes 화면)
     * 상태/키워드 + 페이징, classes.teacher_id를 로그인 강사로 서버에서 한정
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResponse<ClassResponse> getMyClasses(@ModelAttribute ClassSearchRequest cond,
                                                     Authentication authentication) {
        return classService.getMyClasses(authentication.getName(), cond);
    }

    /** Authentication 권한 보유 여부 확인 */
    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
