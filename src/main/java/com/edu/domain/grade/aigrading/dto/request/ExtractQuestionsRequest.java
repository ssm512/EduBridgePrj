package com.edu.domain.grade.aigrading.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AIG-02 AI 문항 자동 추출 요청 - 교사가 Gemini에게 추가로 전달하고 싶은 요구사항(선택).
 * POST /api/exams/{examId}/questions/extract - body 없이 호출해도 되고(기존 호환), 이 필드만 있어도 된다.
 * 저장은 하지 않는다 - 매 요청마다 화면에서 새로 입력하는 일회성 값이다(팀 결정, 2026-07-24).
 */
@Data
public class ExtractQuestionsRequest {

    /** ExamGradingServiceImpl.EXTRACTION_PROMPT 뒤에 덧붙여 Gemini에 함께 전달할 추가 요청사항 */
    @Size(max = 1000, message = "추가 요청사항은 1000자 이하로 입력해주세요")
    private String additionalInstruction;
}
