package com.edu.domain.classroom.controller;

import com.edu.common.dto.PageResponse;
import com.edu.domain.classroom.dto.EnrollmentCreateRequest;
import com.edu.domain.classroom.dto.EnrollmentEndRequest;
import com.edu.domain.classroom.dto.EnrollmentResponse;
import com.edu.domain.classroom.dto.EnrollmentSearchRequest;
import com.edu.domain.classroom.service.EnrollmentService;
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
 * 수강관리 API (명세서 ENR-01 ~ ENR-02 + 목록 조회)
 * 명세서 URL 그대로 /enrollments 매핑
 */
@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /**
     * ENR-01 POST /enrollments - 수강 등록
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public EnrollmentResponse enroll(@Valid @RequestBody EnrollmentCreateRequest request) {
        return enrollmentService.enroll(request);
    }

    /**
     * GET /enrollments - 수강 목록 조회 (명세서 외 추가 - 수강관리 화면용)
     * 반/상태/키워드(학생명·학번) + 페이징
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public PageResponse<EnrollmentResponse> getEnrollments(@ModelAttribute EnrollmentSearchRequest cond) {
        return enrollmentService.getEnrollments(cond);
    }

    /**
     * ENR-02 PUT /enrollments/{enrollmentId}/end - 수강 해제
     * 상태(ENDED) + 종료일 기록 방식
     */
    @PutMapping("/{enrollmentId}/end")
    @PreAuthorize("hasRole('ADMIN')")
    public EnrollmentResponse endEnrollment(@PathVariable Long enrollmentId,
                                            @Valid @RequestBody EnrollmentEndRequest request) {
        return enrollmentService.endEnrollment(enrollmentId, request);
    }
}
