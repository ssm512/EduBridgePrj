package com.edu.domain.ai.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiUsageLogVo {
    private Long aiLogId;             // AI 사용 로그 ID, ai_usage_logs.ai_log_id
    private Long userId;              // 요청자 ID, users.user_id
    private String featureCode;       // AI 기능 코드, REPORT/NOTICE/COUNSELING
    private String requestPrompt;     // Gemini API에 전달한 요청 프롬프트
    private String responseText;      // Gemini API 응답 내용
    private String modelName;         // 사용 모델명
    private String successYn;         // 성공 여부, Y/N
    private String errorMessage;      // 실패 시 오류 메시지
    private LocalDateTime createdAt;  // 요청 일시
}
