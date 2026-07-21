package com.edu.domain.grade.aigrading.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * AIG-04 문항 확정 저장 요청 - 시험 하나의 문항 목록을 통째로 교체한다(PUT, 전체 replace).
 * PUT /api/exams/{examId}/questions - body: List&lt;ExamQuestionItemRequest&gt; (GradeController.saveGradesBatch와 동일한 리스트 바디 패턴)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionItemRequest {

    /** 문항 번호 (요청 목록 내에서 중복 불가) */
    @NotNull(message = "문항 번호를 입력해주세요")
    private Integer questionNo;

    /** MULTIPLE_CHOICE / SHORT_ANSWER / ESSAY */
    @NotBlank(message = "문항 유형을 선택해주세요")
    @Pattern(regexp = "^(MULTIPLE_CHOICE|SHORT_ANSWER|ESSAY)$", message = "문항 유형은 MULTIPLE_CHOICE, SHORT_ANSWER, ESSAY 중 하나여야 합니다")
    private String questionType;

    /** 문제 내용 (선택 - 이미지 문제지만으로 채점하는 경우 비워둘 수 있음) */
    private String questionText;

    /** 정답 */
    @NotBlank(message = "정답을 입력해주세요")
    private String correctAnswer;

    /** 배점 */
    @NotNull(message = "배점을 입력해주세요")
    @PositiveOrZero(message = "배점은 0 이상이어야 합니다")
    private BigDecimal maxScore;

    /** 채점 기준 (선택) */
    private String gradingCriteria;

    /**
     * 검수 필요 여부 요청값 (Y/N, 선택 - 미입력 시 N).
     * ESSAY는 서버가 무조건 Y로 강제한다(ExamGradingRuleService.resolveQuestionReviewRequired).
     */
    @Pattern(regexp = "^[YN]$", message = "검수 필요 여부는 Y 또는 N 이어야 합니다")
    private String reviewRequiredYn;

    /** 화면 표시 정렬 순서 */
    @NotNull(message = "정렬 순서를 입력해주세요")
    private Integer sortOrder;
}
