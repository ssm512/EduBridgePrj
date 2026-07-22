package com.edu.domain.grade.aigrading.service;

import com.edu.domain.grade.aigrading.dto.request.AnswerReviewItemRequest;
import com.edu.domain.grade.aigrading.dto.request.ExamQuestionItemRequest;
import com.edu.domain.grade.aigrading.dto.response.AnswerResultResponse;
import com.edu.domain.grade.aigrading.dto.response.ExamDocumentResponse;
import com.edu.domain.grade.aigrading.dto.response.ExamQuestionExtractResponse;
import com.edu.domain.grade.aigrading.dto.response.ExamQuestionResponse;
import com.edu.domain.grade.aigrading.dto.response.ExamSubmissionResponse;
import com.edu.domain.grade.aigrading.dto.response.GradeConfirmResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 시험지 자동채점(AIG-01~10) 통합 서비스.
 *
 * 원래 기능별로 나뉘어 있던 5개 서비스(ExamDocumentService/ExamQuestionService/
 * ExamQuestionExtractService/ExamGradingRuleService/ExamSubmissionService)를 하나로 합쳤다(2026-07-22).
 * 단, {@link AiExamGradingService}(AIG-06 실제 Gemini 채점 호출부)는 합치지 않고 별도로 남겨뒀다 -
 * {@code @Async}는 스프링 프록시를 거쳐야 동작하는데 같은 클래스 안에서 this.비동기메서드()로 자기 자신을
 * 부르면 프록시를 우회해 동기로 실행돼버리는 문제(자기호출 문제) 때문에 물리적으로 다른 빈이어야 한다.
 *
 * sumConfirmedScores~isValidStatusTransition 6개는 DB/파일/Gemini에 의존하지 않는 순수 로직이라
 * {@link AiExamGradingServiceImpl}에서도 그대로 호출해서 쓴다(다른 클래스이므로 자기호출 문제 없음).
 */
public interface ExamGradingService {

    // =====================================================================
    // AIG-01 시험자료 업로드
    // =====================================================================

    /**
     * 시험자료 업로드. documentType은 QUESTION(문제지) 또는 ANSWER_KEY(정답지).
     * 여러 파일을 한 번에 올리면 업로드 순서대로 pageNo(1부터)를 채운다.
     */
    List<ExamDocumentResponse> uploadDocuments(Long examId, String documentType,
                                                List<MultipartFile> files, Authentication authentication);

    // =====================================================================
    // AIG-02 문항 자동 추출 (Gemini 이미지 분석, DB 미저장 - 초안만 반환)
    // =====================================================================

    /**
     * examId에 업로드된 시험자료(문제지/정답지) 이미지를 Gemini에 보내 문항을 구조화한다.
     * DB에 저장하지 않는다 - 결과는 초안이며, 화면에서 검토/수정 후 saveQuestions(AIG-04)로 확정 저장해야 한다.
     */
    ExamQuestionExtractResponse extractQuestions(Long examId, String loginId);

    // =====================================================================
    // AIG-03/04 문항 조회/확정
    // =====================================================================

    /** AIG-03 시험 문항 목록 조회 */
    List<ExamQuestionResponse> getQuestions(Long examId);

    /**
     * AIG-04 시험 문항 확정 저장. 요청 목록으로 해당 시험의 문항을 전체 교체한다(PUT).
     * 이미 학생 답안 제출이 있는 시험은 저장을 막는다(409).
     */
    List<ExamQuestionResponse> saveQuestions(Long examId, List<ExamQuestionItemRequest> items);

    // =====================================================================
    // AIG-05 학생 답안지 업로드 / 상태 조회
    // =====================================================================

    /**
     * 학생 답안지 업로드. 시험+학생 단위로 exam_submissions가 없으면 새로 만들고(status=UPLOADED),
     * 이미 있으면 아직 채점 전(UPLOADED)일 때만 파일을 이어서(page_no 계속 증가) 추가한다.
     * 채점이 시작된 뒤(ANALYZING 이상)에는 재업로드를 막는다 - AIG-10 재채점 경로를 따로 쓰도록 유도.
     */
    ExamSubmissionResponse uploadSubmission(Long examId, Long studentId, List<MultipartFile> files,
                                             Authentication authentication);

    /** 제출 건 단건 조회 - statusCode 폴링용 (화면에서 "처리중입니다" 표시 후 상태 변화를 확인할 때 사용) */
    ExamSubmissionResponse getSubmission(Long submissionId);

    /**
     * 시험+학생 단위 기존 제출 조회. 없으면 null.
     * 화면에서 학생을 선택했을 때 이미 업로드된 답안지(submissionId)가 있으면 자동으로 채워주기 위한 용도 -
     * 사용자는 DB를 직접 볼 수 없으므로 submissionId를 수동으로 알아낼 방법이 없다.
     */
    ExamSubmissionResponse findSubmission(Long examId, Long studentId);

    // =====================================================================
    // AIG-06/10 AI 채점 실행 트리거 (실제 호출은 AiExamGradingService가 비동기로 처리)
    // =====================================================================

    /**
     * AIG-06 AI 채점 실행. 문항(exam_questions)과 답안 파일(submission_files)이 준비돼 있는지 확인한 뒤
     * exam_submissions를 ANALYZING으로 전이하고 ai_grading_runs를 RUNNING으로 등록, 실제 채점은
     * 비동기로 넘기고 즉시 응답한다(화면에는 "처리중입니다" 표시). AIG-10 재채점도 상태 전이 규칙
     * (REVIEW_REQUIRED/FAILED -&gt; ANALYZING)과 run_no 자동 증가로 이미 충족되므로 이 메서드를 그대로 재사용한다.
     */
    ExamSubmissionResponse startGrading(Long submissionId, Authentication authentication);

    // =====================================================================
    // AIG-07/08/09 검수 / 최종 확정
    // =====================================================================

    /** AIG-07 문항별 채점 결과 조회 (아직 채점 전이면 빈 목록) */
    List<AnswerResultResponse> getResults(Long submissionId);

    /**
     * AIG-08 교사 검수 결과 저장. REVIEW_REQUIRED 상태일 때만 가능하다(채점 전/진행중/이미 확정된 건은 막음).
     * 상태는 바꾸지 않는다 - 최종 확정은 confirmGrade(AIG-09)에서 별도로 처리.
     */
    List<AnswerResultResponse> submitReview(Long submissionId, List<AnswerReviewItemRequest> items,
                                             Authentication authentication);

    /**
     * AIG-09 최종 확정. 검수 필요 없는 문항은 AI 점수를 자동 채택하고, 검수 필요 문항은 모두 검수(confirmed_score)가
     * 끝나 있어야 한다. 합산 점수를 서버가 재검증(시험 만점 초과 확인)한 뒤 기존 grades 테이블에 반영하고
     * gradeMapper.updateGradeRanksByExam으로 석차를 다시 계산한다.
     */
    GradeConfirmResponse confirmGrade(Long submissionId, Authentication authentication);

    // =====================================================================
    // 순수 검증/재계산 로직 (원 ExamGradingRuleService) - AiExamGradingServiceImpl에서도 호출한다
    // =====================================================================

    /** AIG-09 서버 재검증: 문항별 확정 점수(answer_results.confirmed_score)를 합산한다. null 항목은 0으로 취급 */
    BigDecimal sumConfirmedScores(List<BigDecimal> questionScores);

    /** AIG-09 서버 재검증: 합산 점수가 시험 만점(exams.total_score)을 초과하지 않는지 확인 */
    boolean isWithinExamTotal(BigDecimal summedScore, BigDecimal examTotalScore);

    /**
     * AIG-09 확정 시 grades.score(NUMERIC(5,1)) 저장을 위한 스케일 변환.
     * exam_questions.max_score/answer_results.confirmed_score는 NUMERIC(6,2)라 소수 둘째자리까지 있을 수 있는데,
     * grades.score는 소수 첫째자리까지만 저장 가능 - 검토보고서에서 지적된 반올림 규칙 미정 문제를 HALF_UP으로 우선 확정.
     */
    BigDecimal roundToGradeScale(BigDecimal rawScore);

    /**
     * 문항 채점 결과의 검수 필요 여부 판정.
     * confidence(answer_results.confidence)가 threshold 미만이면 검수 필요.
     * confidence가 null(판독 불가 등)이면 안전하게 검수 필요로 처리한다.
     */
    boolean isReviewRequired(BigDecimal confidence, BigDecimal threshold);

    /**
     * AIG-10 재채점 시 다음 실행차수(ai_grading_runs.run_no)를 계산한다.
     * 이전 실행 이력이 없으면(null 또는 0 이하) 1부터 시작.
     */
    int nextRunNo(Integer currentMaxRunNo);

    /**
     * AIG-04 문항 저장 시 review_required_yn을 확정한다.
     * ESSAY(서술형)는 검토보고서에서 지적한 대로 AI 점수를 그대로 신뢰하지 않고 검수를 강제해야 하므로
     * 클라이언트 요청값과 무관하게 항상 "Y"로 고정한다. 그 외 유형은 요청값을 쓰고, 미입력 시 "N".
     */
    String resolveQuestionReviewRequired(String questionType, String requestedYn);

    /**
     * exam_submissions.status_code 상태 전이가 허용되는지 확인한다.
     * 허용 흐름: UPLOADED -&gt; ANALYZING -&gt; (REVIEW_REQUIRED | FAILED)
     *           REVIEW_REQUIRED -&gt; CONFIRMED (AIG-09 확정)
     *           REVIEW_REQUIRED -&gt; ANALYZING (AIG-10 재채점)
     *           FAILED -&gt; ANALYZING (AIG-10 재채점)
     * CONFIRMED는 종료 상태로 더 이상 전이하지 않는다.
     */
    boolean isValidStatusTransition(String fromStatus, String toStatus);
}
