package com.edu.domain.grade.aigrading.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * answer_results 테이블 (MyBatis 매핑용) - AIG-06 문항별 AI 채점 결과, AIG-07/08 교사 검수 대상.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResultVo {

    /** 문항별채점결과 PK */
    private Long answerResultId;

    /** exam_submissions.submission_id FK */
    private Long submissionId;

    /** exam_questions.question_id FK */
    private Long questionId;

    /** AI가 인식한 학생 답안 */
    private String recognizedAnswer;

    /** AI가 채점한 점수 */
    private BigDecimal aiScore;

    /** 교사 검수 후 확정 점수 (AIG-08) */
    private BigDecimal confirmedScore;

    /** CORRECT / PARTIAL / INCORRECT / UNREADABLE */
    private String resultCode;

    /** AI 채점 근거 (서술형 등 부분점수 사유) */
    private String aiReason;

    /** AI 채점 신뢰도 (0~1) */
    private BigDecimal confidence;

    /** 검수 필요 여부 (Y/N) - 신뢰도 임계값 미만이거나 ESSAY면 Y */
    private String reviewRequiredYn;

    /** 교사 검수 코멘트 */
    private String teacherComment;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
