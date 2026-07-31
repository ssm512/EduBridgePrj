package com.edu.domain.member.controller;

import com.edu.common.dto.PageResponse;
import com.edu.domain.member.dto.parent.StudentParentLinkRequest;
import com.edu.domain.member.dto.parent.StudentParentUpdateRequest;
import com.edu.domain.member.dto.student.StudentCreateRequest;
import com.edu.domain.member.dto.student.StudentDetailResponse;
import com.edu.domain.member.dto.student.StudentResponse;
import com.edu.domain.member.dto.student.StudentSearchRequest;
import com.edu.domain.member.dto.student.StudentUpdateRequest;
import com.edu.domain.member.service.ParentService;
import com.edu.domain.member.service.StudentService;
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

import java.util.Map;

/**
 * 학생관리 API (명세서 STU-01 ~ STU-04)
 * /api/students 매핑 (2026-07-14 API URL /api 프리픽스 통일)
 * 권한: 등록/수정 ADMIN, 목록 ADMIN·TEACHER, 상세 4개 롤(본인/자녀 검증은 서비스에서)
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final ParentService parentService;

    public StudentController(StudentService studentService, ParentService parentService) {
        this.studentService = studentService;
        this.parentService = parentService;
    }

    /**
     * STU-01 POST /api/students - 학생 등록
     * 계정(users) + 학생 상세(students) 동시 등록
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public StudentResponse createStudent(@Valid @RequestBody StudentCreateRequest request) {
        return studentService.createStudent(request);
    }

    /**
     * STU-02 GET /api/students - 학생 목록 조회
     * 반(classId)/키워드(이름/로그인ID/학번) + 페이징
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public PageResponse<StudentResponse> getStudents(@ModelAttribute StudentSearchRequest cond) {
        return studentService.getStudents(cond);
    }

    /**
     * STU-03 GET /api/students/{studentId} - 학생 상세 조회 (보호자/수강반 포함)
     * STUDENT는 본인, PARENT는 자녀만 조회 가능 (서비스에서 검증)
     */
    @GetMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public StudentDetailResponse getStudent(@PathVariable Long studentId,
                                            Authentication authentication) {
        return studentService.getStudent(studentId, authentication);
    }

    /**
     * STU-04 PUT /api/students/{studentId} - 학생 수정
     * 학번/학교/학년/메모
     */
    @PutMapping("/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public StudentResponse updateStudent(@PathVariable Long studentId,
                                         @Valid @RequestBody StudentUpdateRequest request) {
        return studentService.updateStudent(studentId, request);
    }

    /**
     * PAR-03 POST /api/students/{studentId}/parents - 학생-학부모 연결
     * (URL이 /students 하위라 이 컨트롤러에 위치, 로직은 ParentService)
     */
    @PostMapping("/{studentId}/parents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Long> linkParent(@PathVariable Long studentId,
                                        @Valid @RequestBody StudentParentLinkRequest request) {
        Long studentParentId = parentService.linkParent(studentId, request);
        return Map.of("studentParentId", studentParentId);
    }

    /**
     * PUT /api/students/{studentId}/parents/{studentParentId} - 연결 내역 수정 (명세서 외 추가)
     * 관계코드/주보호자 여부 변경
     */
    @PutMapping("/{studentId}/parents/{studentParentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> updateParentLink(@PathVariable Long studentId,
                                                @PathVariable Long studentParentId,
                                                @Valid @RequestBody StudentParentUpdateRequest request) {
        parentService.updateParentLink(studentId, studentParentId, request);
        return Map.of("message", "연결 내역이 수정되었습니다");
    }
}
