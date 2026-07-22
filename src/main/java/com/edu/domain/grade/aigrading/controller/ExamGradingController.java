package com.edu.domain.grade.aigrading.controller;

import com.edu.domain.grade.aigrading.dto.request.AnswerReviewItemRequest;
import com.edu.domain.grade.aigrading.dto.request.ExamQuestionItemRequest;
import com.edu.domain.grade.aigrading.dto.response.AnswerResultResponse;
import com.edu.domain.grade.aigrading.dto.response.ExamDocumentResponse;
import com.edu.domain.grade.aigrading.dto.response.ExamQuestionExtractResponse;
import com.edu.domain.grade.aigrading.dto.response.ExamQuestionResponse;
import com.edu.domain.grade.aigrading.dto.response.ExamSubmissionResponse;
import com.edu.domain.grade.aigrading.dto.response.GradeConfirmResponse;
import com.edu.domain.grade.aigrading.service.ExamGradingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AI 시험지 자동채점(AIG-01~10) 통합 REST API.
 * base path는 팀 결정(/api 통일, [[edubridge-api-prefix-decision]])에 따라 /api로 고정.
 *
 * 원래 리소스별로 3개 컨트롤러(ExamDocumentController/ExamQuestionController/ExamSubmissionController)로
 * 나뉘어 있었는데, 전부 같은 {@link ExamGradingService} 하나만 주입받고 있어서(mapper/service를
 * 하나로 합친 2026-07-22 리팩터링의 자연스러운 연장) 컨트롤러도 하나로 합쳤다.
 * mapper/service의 AiExamGradingService(@Async 자기호출 문제)처럼 물리적으로 분리해야 할 기술적
 * 제약이 없어서 통합에 특별한 제약은 없었다.
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class ExamGradingController {

    private final ExamGradingService examGradingService;

    public ExamGradingController(ExamGradingService examGradingService) {
        this.examGradingService = examGradingService;
    }

    // =====================================================================
    // AIG-01 시험자료 업로드
    // =====================================================================

    /** AIG-01 POST /api/exams/{examId}/documents - 시험자료 업로드 (문제지/정답지, multipart 복수 가능) */
    @PostMapping("/exams/{examId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ExamDocumentResponse> uploadDocuments(@PathVariable Long examId,
                                                       @RequestParam("documentType") String documentType,
                                                       @RequestParam("files") List<MultipartFile> files,
                                                       Authentication authentication) {
        return examGradingService.uploadDocuments(examId, documentType, files, authentication);
    }

    // =====================================================================
    // AIG-02/03/04 문항 자동추출/조회/확정
    // =====================================================================

    /**
     * AIG-02 POST /api/exams/{examId}/questions/extract - AI 문항 자동 추출 (초안, DB 미저장)
     * 결과는 화면에서 검토/수정 후 AIG-04(PUT)로 확정 저장해야 한다.
     */
    @PostMapping("/exams/{examId}/questions/extract")
    public ExamQuestionExtractResponse extractQuestions(@PathVariable Long examId,
                                                         Authentication authentication) {
        return examGradingService.extractQuestions(examId, authentication.getName());
    }

    /** AIG-03 GET /api/exams/{examId}/questions - 시험 문항 조회 */
    @GetMapping("/exams/{examId}/questions")
    public List<ExamQuestionResponse> getQuestions(@PathVariable Long examId) {
        return examGradingService.getQuestions(examId);
    }

    /** AIG-04 PUT /api/exams/{examId}/questions - 시험 문항 확정 저장 (전체 교체) */
    @PutMapping("/exams/{examId}/questions")
    public List<ExamQuestionResponse> saveQuestions(@PathVariable Long examId,
                                                     @Valid @RequestBody List<ExamQuestionItemRequest> items) {
        return examGradingService.saveQuestions(examId, items);
    }

    // =====================================================================
    // AIG-05 학생 답안지 업로드 / 상태 조회
    // =====================================================================

    /** AIG-05 POST /api/exams/{examId}/submissions - 학생 답안지 업로드 (multipart 복수 페이지 가능) */
    @PostMapping("/exams/{examId}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamSubmissionResponse uploadSubmission(@PathVariable Long examId,
                                                    @RequestParam("studentId") Long studentId,
                                                    @RequestParam("files") List<MultipartFile> files,
                                                    Authentication authentication) {
        return examGradingService.uploadSubmission(examId, studentId, files, authentication);
    }

    /**
     * GET /api/exams/{examId}/submissions/by-student/{studentId} - 시험+학생 단위 기존 제출 조회.
     * 화면에서 학생을 고르면 이미 업로드된 답안지가 있는지 확인해서 submissionId를 자동으로 채워주기 위한 API -
     * 사용자는 DB를 직접 볼 수 없어 submissionId를 알아낼 방법이 없으므로 필요하다. 없으면 404(빈 응답, 정상 상태).
     */
    @GetMapping("/exams/{examId}/submissions/by-student/{studentId}")
    public ResponseEntity<ExamSubmissionResponse> findSubmission(@PathVariable Long examId,
                                                                  @PathVariable Long studentId) {
        ExamSubmissionResponse response = examGradingService.findSubmission(examId, studentId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }

    /** GET /api/exam-submissions/{submissionId} - 제출 상태 단건 조회 (AIG-06 처리중 폴링용) */
    @GetMapping("/exam-submissions/{submissionId}")
    public ExamSubmissionResponse getSubmission(@PathVariable Long submissionId) {
        return examGradingService.getSubmission(submissionId);
    }

    // =====================================================================
    // AIG-06/10 AI 채점 실행 / 재채점
    // =====================================================================

    /**
     * AIG-06 POST /api/exam-submissions/{submissionId}/grade - AI 자동채점 실행 (비동기).
     * 요청은 즉시 응답하고(statusCode=ANALYZING), 실제 Gemini 채점은 백그라운드에서 진행된다.
     * 화면은 이 응답을 받으면 "처리중입니다"를 표시하고, 이후 폴링 등으로 상태 변화를 확인해야 한다.
     */
    @PostMapping("/exam-submissions/{submissionId}/grade")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExamSubmissionResponse startGrading(@PathVariable Long submissionId,
                                                Authentication authentication) {
        return examGradingService.startGrading(submissionId, authentication);
    }

    /**
     * AIG-10 POST /api/exam-submissions/{submissionId}/retry - 재채점.
     * AIG-06과 동일한 처리 경로를 그대로 재사용한다 - exam_submissions.status_code 전이 규칙상
     * REVIEW_REQUIRED/FAILED에서만 허용되고(ExamGradingServiceImpl.ALLOWED_TRANSITIONS),
     * ai_grading_runs.run_no는 자동으로 +1 되며, 이전 answer_results는 새 채점 결과로 교체된다.
     */
    @PostMapping("/exam-submissions/{submissionId}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExamSubmissionResponse retryGrading(@PathVariable Long submissionId,
                                                Authentication authentication) {
        return examGradingService.startGrading(submissionId, authentication);
    }

    // =====================================================================
    // AIG-07/08/09 검수 / 최종 확정
    // =====================================================================

    /** AIG-07 GET /api/exam-submissions/{submissionId}/results - 문항별 채점 결과(인식답안/제안점수/근거/신뢰도) 조회 */
    @GetMapping("/exam-submissions/{submissionId}/results")
    public List<AnswerResultResponse> getResults(@PathVariable Long submissionId) {
        return examGradingService.getResults(submissionId);
    }

    /** AIG-08 PUT /api/exam-submissions/{submissionId}/results - 교사 검수 결과 저장 (확정 아님, AIG-09가 최종 확정) */
    @PutMapping("/exam-submissions/{submissionId}/results")
    public List<AnswerResultResponse> submitReview(@PathVariable Long submissionId,
                                                    @Valid @RequestBody List<AnswerReviewItemRequest> items,
                                                    Authentication authentication) {
        return examGradingService.submitReview(submissionId, items, authentication);
    }

    /**
     * AIG-09 POST /api/exam-submissions/{submissionId}/confirm - 최종 확정.
     * 서버 재검증(합산 점수 ≤ 시험 만점) 후 기존 grades 테이블에 반영하고 석차를 다시 계산한다.
     */
    @PostMapping("/exam-submissions/{submissionId}/confirm")
    public GradeConfirmResponse confirmGrade(@PathVariable Long submissionId,
                                              Authentication authentication) {
        return examGradingService.confirmGrade(submissionId, authentication);
    }
}
