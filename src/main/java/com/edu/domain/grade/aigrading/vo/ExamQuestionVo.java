package com.edu.domain.grade.aigrading.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * exam_questions 테이블 (MyBatis 매핑용) - AIG-03/04 시험 문항 조회/확정
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionVo {

    /** 문항 PK */
    private Long questionId;

    /** exams.exam_id FK */
    private Long examId;

    /** 문항 번호 (시험 내 UNIQUE) */
    private Integer questionNo;

    /** MULTIPLE_CHOICE / SHORT_ANSWER / ESSAY */
    private String questionType;

    /** 문제 내용 */
    private String questionText;

    /** 정답 */
    private String correctAnswer;

    /** 배점 */
    private BigDecimal maxScore;

    /** 채점 기준 (서술형 등 부분점수 판단용) */
    private String gradingCriteria;

    /** 검수 필수 여부 (Y/N) - ESSAY는 항상 Y로 강제 */
    private String reviewRequiredYn;

    /** 화면 표시 정렬 순서 */
    private Integer sortOrder;

    /** 등록 일시 */
    private LocalDateTime createdAt;

    /** 수정 일시 */
    private LocalDateTime updatedAt;
}
