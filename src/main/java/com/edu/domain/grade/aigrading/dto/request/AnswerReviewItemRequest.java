package com.edu.domain.grade.aigrading.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AIG-08 교사 검수 결과 저장 요청 1건.
 */
@Data
public class AnswerReviewItemRequest {

    /** answer_results.answer_result_id - 검수 대상 문항 결과 */
    @NotNull
    private Long answerResultId;

    /** 교사가 확정한 점수 (0 ~ 해당 문항 배점) */
    @NotNull
    private BigDecimal confirmedScore;

    /** 교사 검수 코멘트 (선택) */
    private String teacherComment;
}
