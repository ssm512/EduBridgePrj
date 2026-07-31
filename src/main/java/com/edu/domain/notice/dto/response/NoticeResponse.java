package com.edu.domain.notice.dto.response;

import com.edu.domain.notice.vo.NoticeVo;

import java.time.LocalDateTime;

/**
 * 공지 목록/단건 공통 응답 (content 제외 — 상세는 NoticeDetailResponse)
 */
public record NoticeResponse(
        Long noticeId,
        String title,
        Long writerId,
        String writerName,
        String writerLoginId,
        String targetType,
        String noticeStatus,
        String readYn,        // 현재 로그인 사용자의 읽음 여부 (Y/N)
        long readCount,       // 읽음 처리된 수신자 수
        int fileCount,        // 첨부파일 개수
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoticeResponse from(NoticeVo vo) {
        return new NoticeResponse(
                vo.getNoticeId(),
                vo.getTitle(),
                vo.getWriterId(),
                vo.getWriterName(),
                vo.getWriterLoginId(),
                vo.getTargetType(),
                vo.getNoticeStatus(),
                vo.getReadYn() != null ? vo.getReadYn() : "N",
                vo.getReadCount(),
                vo.getFileCount(),
                vo.getCreatedAt(),
                vo.getUpdatedAt()
        );
    }
}
