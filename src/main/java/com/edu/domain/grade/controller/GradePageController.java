package com.edu.domain.grade.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GradePageController {

    @GetMapping("/admin/grades")
    public String gradeManagingPage() {
        return "admin/grade/gradeManage";
    }

}
