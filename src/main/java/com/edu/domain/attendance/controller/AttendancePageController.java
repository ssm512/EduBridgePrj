package com.edu.domain.attendance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AttendancePageController {

    /** 관리자 출석관리 (출석부/통계/비콘) */
    @GetMapping("/admin/attendance")
    public String attendence() {
        return "admin/student/attendance";
    }

    /** 강사 출석관리 (출석부/통계/이력 — 비콘관리 제외) */
    @GetMapping("/teacher/attendance")
    public String teacherAttendance() {
        return "teacher/attendance";
    }

    /** 학생 본인 출석 조회 (읽기전용) */
    @GetMapping("/student/attendance")
    public String studentAttendance() {
        return "student/attendance";
    }

    /** 학부모 자녀 출석 조회 (읽기전용) */
    @GetMapping("/parent/attendance")
    public String parentAttendance() {
        return "parent/attendance";
    }

}
