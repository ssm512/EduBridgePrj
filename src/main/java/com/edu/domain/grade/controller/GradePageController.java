package com.edu.domain.grade.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
public class GradePageController {

    @GetMapping("/admin/grades")
    @PreAuthorize("hasRole('ADMIN')")
    public String gradeManagingPage() {
        return "admin/grade/gradeManage";
    }

    // 강사 성적 입력 화면
    @GetMapping("/teacherPage/grades")
    @PreAuthorize("hasRole('TEACHER')")
    public String teacherGradeManagingPage() {
        return "teacher/grade/gradeManage";
    }

    // 학생 본인 성적 조회 화면
    @GetMapping("/studentPage/grades")
    @PreAuthorize("hasRole('STUDENT')")
    public String studentGradePage() {
        return "student/grades";
    }

    // 학부모 자녀 성적 조회 화면
    @GetMapping("/parentPage/grades")
    @PreAuthorize("hasRole('PARENT')")
    public String parentGradePage() {
        return "parent/grades";
    }

}
