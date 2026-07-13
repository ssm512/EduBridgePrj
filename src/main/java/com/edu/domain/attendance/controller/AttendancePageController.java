package com.edu.domain.attendance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AttendancePageController {

    @GetMapping("/admin/attendance")
    public String attendence() {
        return "admin/student/attendance";
    }

}
