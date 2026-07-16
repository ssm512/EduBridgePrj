package com.edu.domain.ai.dto.request;

import lombok.Data;

@Data
public class GradeAnalysisRequest {
    private Long studentId;       // 성적 분석 대상 학생 ID
    private String subject;       // 선택 과목, 비어 있으면 전체 과목 분석
    private String analysisFocus; // 분석 시 추가로 고려할 관리자 요청사항
}
