package com.edu.domain.grade.aigrading.dto.response;

import com.edu.domain.grade.aigrading.vo.ExamQuestionVo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AIG-03 시험 문항 조회 응답
 */
public record ExamQuestionResponse(
        Long questionId,
        Long examId,
        Integer questionNo,
        String questionType,
        String questionText,
        String correctAnswer,
        BigDecimal maxScore,
        String gradingCriteria,
        String reviewRequiredYn,
        Integer sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ExamQuestionResponse from(ExamQuestionVo vo) {
        return new ExamQuestionResponse(
                vo.getQuestionId(),
                vo.getExamId(),
                vo.getQuestionNo(),
                vo.getQuestionType(),
                vo.getQuestionText(),
                vo.getCorrectAnswer(),
                vo.getMaxScore(),
                vo.getGradingCriteria(),
                vo.getReviewRequiredYn(),
                vo.getSortOrder(),
                vo.getCreatedAt(),
                vo.getUpdatedAt()
        );
    }
}
