package com.edu.common.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 관리자 "OO관리" 모듈들의 임시(placeholder) 라우트.
 *
 * ★ 팀원 착수 지점 ★
 * 각 링크는 지금 "준비 중" 안내 화면(admin/placeholder)으로 연결된다.
 * 담당자는 아래 순서로 자기 모듈을 실제 구현하면 된다.
 *   1) 여기(AdminModuleController)에서 자기 모듈의 @GetMapping 한 줄을 지운다.
 *   2) 자기 도메인 패키지(예: com.edu.domain.member.controller)에
 *      진짜 컨트롤러를 만들고 같은 경로(/admin/members)를 매핑한다.
 *   3) templates/admin/{모듈}/ 아래에 실제 화면을 만든다.
 * 이렇게 하면 서로 파일이 겹치지 않아 동시에 작업할 수 있다.
 */
@Controller
@RequestMapping("/admin")
public class AdminModuleController {

    // [제거] /members → com.edu.domain.member.controller.MemberPageController로 이동 (실제 화면 구현됨)

    // [제거] /classes → com.edu.domain.classroom.controller.ClassPageController로 이동 (실제 화면 구현됨)

    // [제거] /enrollments → com.edu.domain.classroom.controller.EnrollmentPageController로 이동 (실제 화면 구현됨)

    @GetMapping("/fees")          // 담당 도메인: fee
    public String fees(Model model)           { return placeholder(model, "회비관리", "com.edu.domain.fee"); }

    @GetMapping("/grades")        // 담당 도메인: grade
    public String grades(Model model)         { return placeholder(model, "성적관리", "com.edu.domain.grade"); }

    // [제거] /notices → com.edu.domain.notice.controller.NoticePageController로 이동 (실제 화면 구현됨)

    @GetMapping("/counseling")    // 담당 도메인: counseling
    public String counseling(Model model)     { return placeholder(model, "상담관리", "com.edu.domain.counseling"); }

    @GetMapping("/dashboard")     // 담당 도메인: dashboard
    public String dashboard(Model model)      { return placeholder(model, "대시보드", "com.edu.domain.dashboard"); }

    @GetMapping("/notifications") // 담당 도메인: notification
    public String notifications(Model model)  { return placeholder(model, "알림관리", "com.edu.domain.notification"); }

    @GetMapping("/logs")          // 담당 도메인: log
    public String logs(Model model)           { return placeholder(model, "활동로그", "com.edu.domain.log"); }

    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('ADMIN')") // 담당 도메인: setting
    public String settings(Model model)       { return placeholder(model, "시스템설정", "com.edu.domain.setting"); }

    @GetMapping("/ai")            // 담당 도메인: ai
    public String ai(Model model)             { return placeholder(model, "Ai 리포트", "com.edu.domain.ai"); }

    private String placeholder(Model model, String title, String domainPackage) {
        model.addAttribute("title", title);
        model.addAttribute("domainPackage", domainPackage);
        return "admin/placeholder";
    }
}
