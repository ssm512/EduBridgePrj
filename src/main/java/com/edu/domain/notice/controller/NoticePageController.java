package com.edu.domain.notice.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 공지사항 화면(페이지) 컨트롤러.
 * AdminModuleController의 /admin/notices placeholder 매핑을 대체한다.
 * 데이터는 화면 로드 후 JS(fetch)가 공지 REST API(/notices)를 호출해 채운다.
 */
@Controller
public class NoticePageController {

    /** GET /admin/notices - 관리자 공지관리 화면 (등록/수정/삭제/첨부/대상 지정) */
    @GetMapping("/admin/notices")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminNoticesPage() {
        return "admin/notice/notices";
    }

    /** GET /teacherPage/notices - 강사 공지 화면 (조회 + 본인 공지 작성/수정) */
    @GetMapping("/teacherPage/notices")
    @PreAuthorize("hasRole('TEACHER')")
    public String teacherNoticesPage() {
        return "teacher/notices";
    }

    /** GET /studentPage/notices - 학생 공지 조회 화면 */
    @GetMapping("/studentPage/notices")
    @PreAuthorize("hasRole('STUDENT')")
    public String studentNoticesPage() {
        return "student/notices";
    }

    /** GET /parentPage/notices - 학부모 공지 조회 화면 */
    @GetMapping("/parentPage/notices")
    @PreAuthorize("hasRole('PARENT')")
    public String parentNoticesPage() {
        return "parent/notices";
    }
}
