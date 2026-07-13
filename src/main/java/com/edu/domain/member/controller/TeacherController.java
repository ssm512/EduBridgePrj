package com.edu.domain.member.controller;

import com.edu.common.dto.PageResponse;
import com.edu.domain.member.dto.teacher.TeacherCreateRequest;
import com.edu.domain.member.dto.teacher.TeacherResponse;
import com.edu.domain.member.dto.teacher.TeacherSearchRequest;
import com.edu.domain.member.dto.teacher.TeacherUpdateRequest;
import com.edu.domain.member.service.TeacherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * 강사관리 API (명세서 TEA-01 ~ TEA-03)
 * 명세서 URL 그대로 /teachers 매핑, 전체 ADMIN 전용
 */
@RestController
@RequestMapping("/teachers")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    /**
     * TEA-01 POST /teachers - 강사 등록
     * 계정(users) + 강사 상세(teachers) 동시 등록
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherResponse createTeacher(@Valid @RequestBody TeacherCreateRequest request) {
        return teacherService.createTeacher(request);
    }

    /**
     * TEA-02 GET /teachers - 강사 목록 조회
     * 키워드(이름/로그인ID/과목) + 페이징
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<TeacherResponse> getTeachers(@ModelAttribute TeacherSearchRequest cond) {
        return teacherService.getTeachers(cond);
    }

    /**
     * TEA-03 PUT /teachers/{teacherId} - 강사 수정
     * 기본정보(users) + 담당과목/입사일(teachers)
     */
    @PutMapping("/{teacherId}")
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherResponse updateTeacher(@PathVariable Long teacherId,
                                         @Valid @RequestBody TeacherUpdateRequest request) {
        return teacherService.updateTeacher(teacherId, request);
    }
}
