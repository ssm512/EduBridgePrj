package com.edu.domain.grade.aigrading.dto.response;

import java.math.BigDecimal;

/**
 * AIG-06 Gemini 채점 응답 파싱용 문항별 결과 1건.
 * 클라이언트에 직접 반환하지 않는 내부 DTO - 최종적으로 answer_results에 저장된다.
 * questionId는 프롬프트에 함께 전달한 exam_questions.question_id를 Gemini가 그대로 echo하도록 요청해서
 * questionNo 문자열 매칭보다 안전하게 매칭한다.
 */
public record AnswerGradingItem(
        Long questionId,
        String recognizedAnswer,
        BigDecimal score,
        String resultCode,
        String reason,
        BigDecimal confidence
) {
}
