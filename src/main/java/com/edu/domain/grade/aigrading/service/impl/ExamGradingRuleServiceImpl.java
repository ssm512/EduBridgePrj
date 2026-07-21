package com.edu.domain.grade.aigrading.service.impl;

import com.edu.domain.grade.aigrading.service.ExamGradingRuleService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 시험지 자동채점 검증/재계산 규칙 구현 (AIG-08 ~ AIG-10).
 * 순수 로직이라 DB/Gemini 없이 단위 테스트로 검증 가능.
 */
@Service
public class ExamGradingRuleServiceImpl implements ExamGradingRuleService {

    /** grades.score 컬럼 정밀도 (NUMERIC(5,1)) - 소수 첫째자리까지 */
    private static final int GRADE_SCORE_SCALE = 1;

    /** exam_submissions.status_code 허용 전이 (from -> 허용되는 to 목록) */
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "UPLOADED", Set.of("ANALYZING"),
            "ANALYZING", Set.of("REVIEW_REQUIRED", "FAILED"),
            "REVIEW_REQUIRED", Set.of("CONFIRMED", "ANALYZING"),
            "FAILED", Set.of("ANALYZING"),
            "CONFIRMED", Set.of()
    );

    @Override
    public BigDecimal sumConfirmedScores(List<BigDecimal> questionScores) {
        if (questionScores == null || questionScores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal score : questionScores) {
            if (score != null) {
                sum = sum.add(score);
            }
        }
        return sum;
    }

    @Override
    public boolean isWithinExamTotal(BigDecimal summedScore, BigDecimal examTotalScore) {
        if (summedScore == null || examTotalScore == null) {
            return false;
        }
        return summedScore.compareTo(examTotalScore) <= 0;
    }

    @Override
    public BigDecimal roundToGradeScale(BigDecimal rawScore) {
        if (rawScore == null) {
            return null;
        }
        return rawScore.setScale(GRADE_SCORE_SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public boolean isReviewRequired(BigDecimal confidence, BigDecimal threshold) {
        if (confidence == null) {
            return true;   // 판독 불가 등으로 신뢰도 자체가 없으면 보수적으로 검수 필요 처리
        }
        if (threshold == null) {
            return true;   // 임계값 미설정 상태에서는 검수 없이 자동 확정되지 않도록 보수적으로 처리
        }
        return confidence.compareTo(threshold) < 0;
    }

    @Override
    public String resolveQuestionReviewRequired(String questionType, String requestedYn) {
        if ("ESSAY".equals(questionType)) {
            return "Y";
        }
        return "Y".equalsIgnoreCase(requestedYn) ? "Y" : "N";
    }

    @Override
    public int nextRunNo(Integer currentMaxRunNo) {
        if (currentMaxRunNo == null || currentMaxRunNo <= 0) {
            return 1;
        }
        return currentMaxRunNo + 1;
    }

    @Override
    public boolean isValidStatusTransition(String fromStatus, String toStatus) {
        if (fromStatus == null || toStatus == null) {
            return false;
        }
        Set<String> allowedNext = ALLOWED_TRANSITIONS.get(fromStatus);
        return allowedNext != null && allowedNext.contains(toStatus);
    }
}
