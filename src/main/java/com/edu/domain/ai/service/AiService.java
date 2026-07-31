package com.edu.domain.ai.service;

import com.edu.domain.ai.dto.request.CounselingSummaryRequest;
import com.edu.domain.ai.dto.request.GradeAnalysisRequest;
import com.edu.domain.ai.dto.request.MonthlyReportRequest;
import com.edu.domain.ai.dto.request.NoticeDraftRequest;
import com.edu.domain.ai.dto.response.AiGenerateResponse;

import java.util.List;
import java.util.Map;

public interface AiService {

    AiGenerateResponse generateMonthlyReport(MonthlyReportRequest request, String loginId);

    AiGenerateResponse summarizeCounseling(CounselingSummaryRequest request, String loginId);

    AiGenerateResponse draftNotice(NoticeDraftRequest request, String loginId);

    AiGenerateResponse analyzeGrades(GradeAnalysisRequest request, String loginId);

    List<Map<String, Object>> getAiUsageLogs(String featureCode, String successYn, String fromDate, String toDate, Integer limit);

    List<Map<String, Object>> getStudentOptions();

    List<Map<String, Object>> getGradeStudentOptions(String category, String keyword);

    List<Map<String, Object>> getGradeSubjectOptions(Long studentId);

    List<Map<String, Object>> getGradeClassOptions(String keyword);

    List<Map<String, Object>> getCounselingOptions(String category, String keyword);

    List<Map<String, Object>> getCounselingSearchSuggestions(String category, String keyword);
}
