package com.edu.domain.ai.dto.request;

import lombok.Data;

@Data
public class CounselingSummaryRequest {
    private Long counselingId;       // 기존 상담 기록을 요약할 때 사용하는 상담 ID
    private String content;          // 직접 입력한 상담 내용을 요약할 때 사용하는 본문
}
