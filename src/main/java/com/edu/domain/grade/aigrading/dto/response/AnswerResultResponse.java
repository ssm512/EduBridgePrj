package com.edu.domain.grade.aigrading.dto.response;

import com.edu.domain.grade.aigrading.vo.AnswerResultVo;
import com.edu.domain.grade.aigrading.vo.ExamQuestionVo;

import java.math.BigDecimal;

/**
 * AIG-07 문항별 채점 결과 조회 응답. answer_results(AI 채점/교사 검수 결과) + exam_questions(문항 정보)를
 * 화면에서 같이 봐야 검수가 가능하므로 조인해서 한 응답으로 합친다.
 */
public record AnswerResultResponse(
        Long answerResultId,
        Long questionId,
        Integer questionNo,
        String questionType,
        String questionText,
        String correctAnswer,
        BigDecimal maxScore,
        String recognizedAnswer,
        BigDecimal aiScore,
        BigDecimal confirmedScore,
        String resultCode,
        String aiReason,
        BigDecimal confidence,
        String reviewRequiredYn,
        String teacherComment
) {
    public static AnswerResultResponse from(AnswerResultVo result, ExamQuestionVo question) {
        return new AnswerResultResponse(
                result.getAnswerResultId(),
                question.getQuestionId(),
                question.getQuestionNo(),
                question.getQuestionType(),
                question.getQuestionText(),
                question.getCorrectAnswer(),
                question.getMaxScore(),
                result.getRecognizedAnswer(),
                result.getAiScore(),
                result.getConfirmedScore(),
                result.getResultCode(),
                result.getAiReason(),
                result.getConfidence(),
                result.getReviewRequiredYn(),
                result.getTeacherComment()
        );
    }
}
