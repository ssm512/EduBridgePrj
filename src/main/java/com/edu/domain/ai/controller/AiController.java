package com.edu.domain.ai.controller;

import com.edu.domain.ai.dto.request.CounselingSummaryRequest;
import com.edu.domain.ai.dto.request.GradeAnalysisRequest;
import com.edu.domain.ai.dto.request.MonthlyReportRequest;
import com.edu.domain.ai.dto.request.NoticeDraftRequest;
import com.edu.domain.ai.dto.response.AiGenerateResponse;
import com.edu.domain.ai.service.AiService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    // 출석/성적/회비/상담 데이터를 기반으로 월간 학생 리포트 초안을 생성한다.
    @PostMapping("/reports/monthly")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public AiGenerateResponse monthlyReport(@RequestBody MonthlyReportRequest request,
                                             Authentication authentication) {
        return aiService.generateMonthlyReport(request, authentication.getName());
    }

    // 상담 기록 또는 직접 입력한 상담 내용을 요약한다.
    @PostMapping("/counseling/summary")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public AiGenerateResponse counselingSummary(@RequestBody CounselingSummaryRequest request,
                                                Authentication authentication) {
        return aiService.summarizeCounseling(request, authentication.getName());
    }

    // 키워드 기반 공지사항 초안을 생성한다.
    @PostMapping("/notices/draft")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public AiGenerateResponse noticeDraft(@RequestBody NoticeDraftRequest request,
                                          Authentication authentication) {
        return aiService.draftNotice(request, authentication.getName());
    }

    // 학생의 성적 이력 데이터를 기반으로 성적 변화와 학습 코멘트를 생성한다.
    @PostMapping("/grades/analysis")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public AiGenerateResponse gradeAnalysis(@RequestBody GradeAnalysisRequest request,
                                            Authentication authentication) {
        return aiService.analyzeGrades(request, authentication.getName());
    }

    // AI 사용 로그를 조회한다.
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> logs(@RequestParam(required = false) String featureCode,
                                          @RequestParam(required = false) String fromDate,
                                          @RequestParam(required = false) String toDate,
                                          @RequestParam(required = false) Integer limit) {
        return aiService.getAiUsageLogs(featureCode, fromDate, toDate, limit);
    }

    // 리포트 생성 화면에서 사용할 학생 선택 목록을 조회한다.
    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> students() {
        return aiService.getStudentOptions();
    }

    // 성적 분석 화면에서 학생명, 학생번호, 반 기준으로 학생 후보를 조회한다.
    @GetMapping("/grades/students")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> gradeStudents(@RequestParam(required = false) String category,
                                                   @RequestParam(required = false) String keyword) {
        return aiService.getGradeStudentOptions(category, keyword);
    }

    // 성적 분석 화면에서 선택 학생이 응시한 과목 후보를 조회한다.
    @GetMapping("/grades/subjects")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> gradeSubjects(@RequestParam Long studentId) {
        return aiService.getGradeSubjectOptions(studentId);
    }

    // 성적 분석 화면에서 반 기준 검색에 사용할 반 후보를 조회한다.
    @GetMapping("/grades/classes")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> gradeClasses(@RequestParam(required = false) String keyword) {
        return aiService.getGradeClassOptions(keyword);
    }

    // 상담 요약 화면에서 상담 기록을 검색하고 선택할 때 사용하는 목록을 조회한다.
    @GetMapping("/counseling/options")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> counselingOptions(@RequestParam(required = false) String category,
                                                       @RequestParam(required = false) String keyword) {
        return aiService.getCounselingOptions(category, keyword);
    }

    // 선택한 상담 검색 카테고리의 자동완성 후보를 조회한다.
    @GetMapping("/counseling/suggestions")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> counselingSuggestions(@RequestParam(required = false) String category,
                                                           @RequestParam(required = false) String keyword) {
        return aiService.getCounselingSearchSuggestions(category, keyword);
    }
}
