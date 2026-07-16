package com.edu.domain.ai.dto.request;

import lombok.Data;

@Data
public class NoticeDraftRequest {
    private String keywords;         // 공지 초안에 반영할 핵심 키워드
    private String tone;             // 공지 문체, 예: 정중하게/친근하게/간결하게
    private String targetType;       // 공지 대상, ALL/CLASS/STUDENT/PARENT/TEACHER
}
