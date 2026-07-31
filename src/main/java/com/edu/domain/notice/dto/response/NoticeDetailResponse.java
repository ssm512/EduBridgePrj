package com.edu.domain.notice.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * NOT-03 공지 상세 응답 (본문 + 대상 + 첨부 + 읽음 여부)
 */
public record NoticeDetailResponse(
        Long noticeId,
        String title,
        String content,
        Long writerId,
        String writerName,
        String writerLoginId,
        String targetType,
        String noticeStatus,
        String readYn,
        long readCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<NoticeTargetResponse> targets,
        List<NoticeFileResponse> files
) {
}
