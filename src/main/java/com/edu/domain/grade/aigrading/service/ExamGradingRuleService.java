package com.edu.domain.grade.aigrading.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 시험지 자동채점의 핵심 검증/재계산 규칙 (AIG-08 검수결과 저장, AIG-09 최종 확정, AIG-10 재채점).
 *
 * 다른 도메인(파일 업로드, Gemini 연동, DB)에 의존하지 않는 순수 로직이라 단위 테스트로 검증 가능하다.
 * {@link com.edu.domain.attendance.service.AttendanceVerificationService}와 동일한 패턴.
 *
 * 아직 팀 확인이 필요한 값들(신뢰도 임계값, 점수 반올림 규칙)은 상수로 박아두지 않고 파라미터로 받는다 -
 * 추후 system_settings 시드 값으로 옮기기 쉽게 하기 위함 (attendance의 GPS_RADIUS_METERS와 동일한 방향).
 */
public interface ExamGradingRuleService {

    /** AIG-09 서버 재검증: 문항별 확정 점수(answer_results.confirmed_score)를 합산한다. null 항목은 0으로 취급 */
    BigDecimal sumConfirmedScores(List<BigDecimal> questionScores);

    /** AIG-09 서버 재검증: 합산 점수가 시험 만점(exams.total_score)을 초과하지 않는지 확인 */
    boolean isWithinExamTotal(BigDecimal summedScore, BigDecimal examTotalScore);

    /**
     * AIG-09 확정 시 grades.score(NUMERIC(5,1)) 저장을 위한 스케일 변환.
     * exam_questions.max_score/answer_results.confirmed_score는 NUMERIC(6,2)라 소수 둘째자리까지 있을 수 있는데,
     * grades.score는 소수 첫째자리까지만 저장 가능 - 검토보고서에서 지적된 반올림 규칙 미정 문제를 HALF_UP으로 우선 확정.
     * (팀 확인 후 규칙이 바뀌면 이 메서드만 수정하면 되도록 별도로 분리)
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
     * 허용 흐름: UPLOADED -> ANALYZING -> (REVIEW_REQUIRED | FAILED)
     *           REVIEW_REQUIRED -> CONFIRMED (AIG-09 확정)
     *           REVIEW_REQUIRED -> ANALYZING (AIG-10 재채점)
     *           FAILED -> ANALYZING (AIG-10 재채점)
     * CONFIRMED는 종료 상태로 더 이상 전이하지 않는다.
     */
    boolean isValidStatusTransition(String fromStatus, String toStatus);
}
