package com.edu.domain.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiGenerateResponse {
    private String result;           // AI가 생성한 결과 텍스트
    private Long aiLogId;            // 저장된 AI 사용 로그 ID
}
