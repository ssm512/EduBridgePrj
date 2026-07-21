package com.edu.domain.grade.aigrading.service.impl;

import com.edu.common.exception.ApiException;
import com.edu.domain.grade.aigrading.dto.request.ExamQuestionItemRequest;
import com.edu.domain.grade.aigrading.dto.response.ExamQuestionResponse;
import com.edu.domain.grade.aigrading.mapper.ExamQuestionMapper;
import com.edu.domain.grade.aigrading.service.ExamGradingRuleService;
import com.edu.domain.grade.aigrading.service.ExamQuestionService;
import com.edu.domain.grade.aigrading.vo.ExamQuestionVo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 시험 문항 조회/확정 구현 (AIG-03/04).
 */
@Service
@Transactional(readOnly = true)
public class ExamQuestionServiceImpl implements ExamQuestionService {

    private final ExamQuestionMapper examQuestionMapper;
    private final ExamGradingRuleService examGradingRuleService;

    public ExamQuestionServiceImpl(ExamQuestionMapper examQuestionMapper,
                                    ExamGradingRuleService examGradingRuleService) {
        this.examQuestionMapper = examQuestionMapper;
        this.examGradingRuleService = examGradingRuleService;
    }

    @Override
    public List<ExamQuestionResponse> getQuestions(Long examId) {
        if (!examQuestionMapper.existsExam(examId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 시험입니다. examId=" + examId);
        }
        return examQuestionMapper.selectByExamId(examId).stream()
                .map(ExamQuestionResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public List<ExamQuestionResponse> saveQuestions(Long examId, List<ExamQuestionItemRequest> items) {
        if (!examQuestionMapper.existsExam(examId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 시험입니다. examId=" + examId);
        }
        if (items == null || items.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "저장할 문항이 없습니다");
        }
        items.forEach(this::validateItem);
        validateNoDuplicateQuestionNo(items);
        if (examQuestionMapper.existsSubmissionForExam(examId)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "이미 학생 답안이 제출된 시험은 문항을 수정할 수 없습니다. examId=" + examId);
        }

        examQuestionMapper.deleteByExamId(examId);
        for (ExamQuestionItemRequest item : items) {
            ExamQuestionVo vo = ExamQuestionVo.builder()
                    .examId(examId)
                    .questionNo(item.getQuestionNo())
                    .questionType(item.getQuestionType())
                    .questionText(item.getQuestionText())
                    .correctAnswer(item.getCorrectAnswer())
                    .maxScore(item.getMaxScore())
                    .gradingCriteria(item.getGradingCriteria())
                    .reviewRequiredYn(examGradingRuleService.resolveQuestionReviewRequired(
                            item.getQuestionType(), item.getReviewRequiredYn()))
                    .sortOrder(item.getSortOrder())
                    .build();
            examQuestionMapper.insertQuestion(vo);
        }

        return getQuestions(examId);
    }

    /**
     * 문항 단건 필수값 검증.
     * 컨트롤러의 @Valid가 List&lt;T&gt; 바디에도 항목별로 적용되긴 하지만, List 파라미터에 대한
     * cascading 검증은 스프링 버전에 따라 동작이 갈릴 수 있어 서비스 레이어에서 한 번 더 방어한다.
     */
    private void validateItem(ExamQuestionItemRequest item) {
        if (item.getQuestionNo() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "문항 번호를 입력해주세요");
        }
        if (item.getQuestionType() == null
                || !Set.of("MULTIPLE_CHOICE", "SHORT_ANSWER", "ESSAY").contains(item.getQuestionType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "문항 유형은 MULTIPLE_CHOICE, SHORT_ANSWER, ESSAY 중 하나여야 합니다 (questionNo=" + item.getQuestionNo() + ")");
        }
        if (item.getCorrectAnswer() == null || item.getCorrectAnswer().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "정답을 입력해주세요 (questionNo=" + item.getQuestionNo() + ")");
        }
        if (item.getMaxScore() == null || item.getMaxScore().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "배점은 0 이상이어야 합니다 (questionNo=" + item.getQuestionNo() + ")");
        }
        if (item.getSortOrder() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "정렬 순서를 입력해주세요 (questionNo=" + item.getQuestionNo() + ")");
        }
        if (item.getReviewRequiredYn() != null
                && !item.getReviewRequiredYn().equals("Y") && !item.getReviewRequiredYn().equals("N")) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "검수 필요 여부는 Y 또는 N 이어야 합니다 (questionNo=" + item.getQuestionNo() + ")");
        }
    }

    /** 요청 목록 안에서 question_no가 중복되면 DB UNIQUE 제약 위반 전에 400으로 먼저 막는다 */
    private void validateNoDuplicateQuestionNo(List<ExamQuestionItemRequest> items) {
        Set<Integer> seen = new HashSet<>();
        for (ExamQuestionItemRequest item : items) {
            if (!seen.add(item.getQuestionNo())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "문항 번호가 중복되었습니다: " + item.getQuestionNo());
            }
        }
    }
}
