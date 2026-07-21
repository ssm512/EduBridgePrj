package com.edu.domain.grade.aigrading.dto.response;

import java.util.List;

/**
 * AIG-02 문항 자동 추출 응답.
 * extractionStatus: SUCCESS(구조화 성공) / FAILED(Gemini 응답을 구조화된 JSON으로 파싱하지 못함 - questions는 빈 목록).
 * 실패해도 예외를 던지지 않고 200으로 응답한다 - "AI 실패 시 수동 입력 경로로 계속 진행 가능"이라는
 * 프로젝트 운영 원칙(제안서 16절)에 맞춰 화면에서 자연스럽게 수동 입력으로 유도하기 위함.
 */
public record ExamQuestionExtractResponse(
        List<ExtractedQuestionItem> questions,
        String extractionStatus,
        Long aiLogId
) {
}
