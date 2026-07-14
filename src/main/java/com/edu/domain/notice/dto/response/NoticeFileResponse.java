package com.edu.domain.notice.dto.response;

import com.edu.domain.notice.vo.NoticeFileVo;

import java.time.LocalDateTime;

/**
 * 공지 첨부파일 응답 (저장 경로 등 서버 내부 정보는 노출하지 않는다)
 */
public record NoticeFileResponse(
        Long fileId,
        String originalName,
        Long fileSize,
        LocalDateTime createdAt
) {
    public static NoticeFileResponse from(NoticeFileVo vo) {
        return new NoticeFileResponse(
                vo.getFileId(),
                vo.getOriginalName(),
                vo.getFileSize(),
                vo.getCreatedAt()
        );
    }
}
