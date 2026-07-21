package com.edu.domain.grade.aigrading.controller;

import com.edu.domain.grade.aigrading.dto.request.ExamQuestionItemRequest;
import com.edu.domain.grade.aigrading.dto.response.ExamQuestionExtractResponse;
import com.edu.domain.grade.aigrading.dto.response.ExamQuestionResponse;
import com.edu.domain.grade.aigrading.service.ExamQuestionExtractService;
import com.edu.domain.grade.aigrading.service.ExamQuestionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 시험 문항 조회/확정/자동추출 REST API (AIG-02/03/04)
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class ExamQuestionController {

    private final ExamQuestionService examQuestionService;
    private final ExamQuestionExtractService examQuestionExtractService;

    public ExamQuestionController(ExamQuestionService examQuestionService,
                                   ExamQuestionExtractService examQuestionExtractService) {
        this.examQuestionService = examQuestionService;
        this.examQuestionExtractService = examQuestionExtractService;
    }

    /**
     * AIG-02 POST /api/exams/{examId}/questions/extract - AI 문항 자동 추출 (초안, DB 미저장)
     * 결과는 화면에서 검토/수정 후 AIG-04(PUT)로 확정 저장해야 한다.
     */
    @PostMapping("/exams/{examId}/questions/extract")
    public ExamQuestionExtractResponse extractQuestions(@PathVariable Long examId,
                                                         Authentication authentication) {
        return examQuestionExtractService.extractQuestions(examId, authentication.getName());
    }

    /** AIG-03 GET /api/exams/{examId}/questions - 시험 문항 조회 */
    @GetMapping("/exams/{examId}/questions")
    public List<ExamQuestionResponse> getQuestions(@PathVariable Long examId) {
        return examQuestionService.getQuestions(examId);
    }

    /** AIG-04 PUT /api/exams/{examId}/questions - 시험 문항 확정 저장 (전체 교체) */
    @PutMapping("/exams/{examId}/questions")
    public List<ExamQuestionResponse> saveQuestions(@PathVariable Long examId,
                                                     @Valid @RequestBody List<ExamQuestionItemRequest> items) {
        return examQuestionService.saveQuestions(examId, items);
    }
}
