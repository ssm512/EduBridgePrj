package com.edu.domain.grade.aigrading.dto.response;

import java.math.BigDecimal;

/**
 * AIG-02 AI 문항 자동 추출 결과 1건.
 * ExamQuestionItemRequest와 필드가 비슷하지만 이건 "AI가 제안한 초안"이라 확정 저장(AIG-04) 전 단계이므로
 * 별도 응답 타입으로 분리한다 - reviewRequiredYn/sortOrder는 AIG-04 저장 시점에 서버가 최종 결정.
 */
public record ExtractedQuestionItem(
        Integer questionNo,
        String questionType,
        String questionText,
        String correctAnswer,
        BigDecimal maxScore,
        String gradingCriteria
) {
}
