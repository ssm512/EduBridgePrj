package com.edu.domain.grade.aigrading.service;

import com.edu.domain.grade.aigrading.dto.request.ExamQuestionItemRequest;
import com.edu.domain.grade.aigrading.dto.response.ExamQuestionResponse;

import java.util.List;

/**
 * 시험 문항 조회/확정 서비스 (AIG-03/04).
 * 지금은 AI 자동추출(AIG-02) 이전 단계 - 직접 입력 경로만 지원한다.
 */
public interface ExamQuestionService {

    /** AIG-03 시험 문항 목록 조회 */
    List<ExamQuestionResponse> getQuestions(Long examId);

    /**
     * AIG-04 시험 문항 확정 저장. 요청 목록으로 해당 시험의 문항을 전체 교체한다(PUT).
     * 이미 학생 답안 제출이 있는 시험은 저장을 막는다(409).
     */
    List<ExamQuestionResponse> saveQuestions(Long examId, List<ExamQuestionItemRequest> items);
}
