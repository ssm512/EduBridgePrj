package com.edu.domain.grade.aigrading;

import com.edu.domain.grade.aigrading.service.impl.ExamGradingRuleServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 채점 검증/재계산 규칙 단위 테스트 (DB/Gemini 불필요, 혼자 실행 가능).
 * 실행: ./gradlew test  또는 IDE에서 이 클래스 Run
 */
class ExamGradingRuleServiceTest {

    private final ExamGradingRuleServiceImpl service = new ExamGradingRuleServiceImpl();

    // ── AIG-09 점수 합산 ──────────────────────────────────────
    @Test
    @DisplayName("점수 합산: 정상 목록은 단순 합계")
    void sum_normal() {
        List<BigDecimal> scores = Arrays.asList(new BigDecimal("10"), new BigDecimal("8.5"), new BigDecimal("5"));
        assertEquals(new BigDecimal("23.5"), service.sumConfirmedScores(scores));
    }

    @Test
    @DisplayName("점수 합산: null 항목은 0으로 취급")
    void sum_withNulls() {
        List<BigDecimal> scores = Arrays.asList(new BigDecimal("10"), null, new BigDecimal("5"));
        assertEquals(new BigDecimal("15"), service.sumConfirmedScores(scores));
    }

    @Test
    @DisplayName("점수 합산: 빈 목록/null 목록은 0")
    void sum_emptyOrNull() {
        assertEquals(BigDecimal.ZERO, service.sumConfirmedScores(Collections.emptyList()));
        assertEquals(BigDecimal.ZERO, service.sumConfirmedScores(null));
    }

    // ── AIG-09 만점 초과 검증 ─────────────────────────────────
    @Test
    @DisplayName("만점 검증: 합산 점수가 만점과 같으면 통과(경계값 포함)")
    void withinTotal_equalIsOk() {
        assertTrue(service.isWithinExamTotal(new BigDecimal("100"), new BigDecimal("100")));
    }

    @Test
    @DisplayName("만점 검증: 합산 점수가 만점보다 작으면 통과, 크면 실패")
    void withinTotal_underAndOver() {
        assertTrue(service.isWithinExamTotal(new BigDecimal("95"), new BigDecimal("100")));
        assertFalse(service.isWithinExamTotal(new BigDecimal("100.1"), new BigDecimal("100")));
    }

    @Test
    @DisplayName("만점 검증: null 값은 실패 처리")
    void withinTotal_null() {
        assertFalse(service.isWithinExamTotal(null, new BigDecimal("100")));
        assertFalse(service.isWithinExamTotal(new BigDecimal("100"), null));
    }

    // ── AIG-09 grades.score 스케일 반올림 ─────────────────────
    @Test
    @DisplayName("반올림: NUMERIC(6,2) -> grades.score NUMERIC(5,1) HALF_UP")
    void roundToGradeScale() {
        assertEquals(new BigDecimal("85.2"), service.roundToGradeScale(new BigDecimal("85.24")));
        assertEquals(new BigDecimal("85.3"), service.roundToGradeScale(new BigDecimal("85.25")));
        assertEquals(new BigDecimal("85.3"), service.roundToGradeScale(new BigDecimal("85.26")));
    }

    @Test
    @DisplayName("반올림: null 입력은 null 반환")
    void roundToGradeScale_null() {
        assertNull(service.roundToGradeScale(null));
    }

    // ── 검수 필요 여부 판정 ────────────────────────────────────
    @Test
    @DisplayName("검수 필요: 신뢰도가 임계값 미만이면 true, 이상이면 false(경계값 포함)")
    void reviewRequired_threshold() {
        BigDecimal threshold = new BigDecimal("0.7");
        assertTrue(service.isReviewRequired(new BigDecimal("0.65"), threshold));
        assertFalse(service.isReviewRequired(new BigDecimal("0.7"), threshold));
        assertFalse(service.isReviewRequired(new BigDecimal("0.9"), threshold));
    }

    @Test
    @DisplayName("검수 필요: 신뢰도 또는 임계값이 null이면 보수적으로 true")
    void reviewRequired_nullIsConservative() {
        assertTrue(service.isReviewRequired(null, new BigDecimal("0.7")));
        assertTrue(service.isReviewRequired(new BigDecimal("0.9"), null));
    }

    // ── AIG-04 문항유형별 검수필요 강제 ─────────────────────────
    @Test
    @DisplayName("ESSAY(서술형)는 요청값과 무관하게 항상 검수 필요(Y)로 강제")
    void reviewRequired_essayAlwaysForced() {
        assertEquals("Y", service.resolveQuestionReviewRequired("ESSAY", "N"));
        assertEquals("Y", service.resolveQuestionReviewRequired("ESSAY", null));
    }

    @Test
    @DisplayName("ESSAY가 아니면 요청값을 그대로 쓰고, 미입력이면 N")
    void reviewRequired_nonEssayUsesRequestedValue() {
        assertEquals("Y", service.resolveQuestionReviewRequired("SHORT_ANSWER", "Y"));
        assertEquals("N", service.resolveQuestionReviewRequired("MULTIPLE_CHOICE", "N"));
        assertEquals("N", service.resolveQuestionReviewRequired("MULTIPLE_CHOICE", null));
    }

    // ── AIG-10 재채점 run_no 증가 ─────────────────────────────
    @Test
    @DisplayName("run_no: 이전 이력 없으면(null/0 이하) 1부터 시작")
    void nextRunNo_firstRun() {
        assertEquals(1, service.nextRunNo(null));
        assertEquals(1, service.nextRunNo(0));
        assertEquals(1, service.nextRunNo(-1));
    }

    @Test
    @DisplayName("run_no: 이전 최대값 + 1")
    void nextRunNo_increment() {
        assertEquals(4, service.nextRunNo(3));
    }

    // ── exam_submissions.status_code 상태 전이 ─────────────────
    @Test
    @DisplayName("상태 전이: 정상 흐름은 모두 허용")
    void statusTransition_validFlow() {
        assertTrue(service.isValidStatusTransition("UPLOADED", "ANALYZING"));
        assertTrue(service.isValidStatusTransition("ANALYZING", "REVIEW_REQUIRED"));
        assertTrue(service.isValidStatusTransition("ANALYZING", "FAILED"));
        assertTrue(service.isValidStatusTransition("REVIEW_REQUIRED", "CONFIRMED"));
    }

    @Test
    @DisplayName("상태 전이: AIG-10 재채점(REVIEW_REQUIRED/FAILED -> ANALYZING)은 허용")
    void statusTransition_retryAllowed() {
        assertTrue(service.isValidStatusTransition("REVIEW_REQUIRED", "ANALYZING"));
        assertTrue(service.isValidStatusTransition("FAILED", "ANALYZING"));
    }

    @Test
    @DisplayName("상태 전이: 단계를 건너뛰거나 CONFIRMED 이후 전이는 불허")
    void statusTransition_invalid() {
        assertFalse(service.isValidStatusTransition("UPLOADED", "CONFIRMED"));
        assertFalse(service.isValidStatusTransition("CONFIRMED", "ANALYZING"));
        assertFalse(service.isValidStatusTransition("UPLOADED", "FAILED"));
    }

    @Test
    @DisplayName("상태 전이: 정의되지 않은 상태값이나 null은 불허")
    void statusTransition_unknownOrNull() {
        assertFalse(service.isValidStatusTransition("UNKNOWN", "ANALYZING"));
        assertFalse(service.isValidStatusTransition(null, "ANALYZING"));
        assertFalse(service.isValidStatusTransition("UPLOADED", null));
    }
}
