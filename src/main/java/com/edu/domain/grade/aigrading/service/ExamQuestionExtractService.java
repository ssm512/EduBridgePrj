package com.edu.domain.grade.aigrading.service;

import com.edu.domain.grade.aigrading.dto.response.ExamQuestionExtractResponse;

/**
 * AI(Gemini) 이미지 분석 기반 문항 자동 추출 (AIG-02).
 *
 * 주의: 기존 AI 도메인(com.edu.domain.ai)의 Gemini 연동은 전부 텍스트 프롬프트만 다뤄왔고,
 * 이미지(멀티모달) 입력은 이번이 처음 시도하는 신규 통합이다 - 검토보고서에서 지적한 미검증 리스크.
 * 실제 Gemini에 이미지가 정상 전달되는지는 로컬에서 진짜 시험지 이미지로 1회 실행해 확인이 필요하다.
 */
public interface ExamQuestionExtractService {

    /**
     * examId에 업로드된 시험자료(문제지/정답지) 이미지를 Gemini에 보내 문항을 구조화한다.
     * DB에 저장하지 않는다 - 결과는 초안이며, 화면에서 검토/수정 후 AIG-04로 확정 저장해야 한다.
     */
    ExamQuestionExtractResponse extractQuestions(Long examId, String loginId);
}
