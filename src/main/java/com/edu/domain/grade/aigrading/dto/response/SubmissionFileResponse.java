package com.edu.domain.grade.aigrading.dto.response;

import com.edu.domain.grade.aigrading.vo.SubmissionFileVo;

import java.time.LocalDateTime;

/**
 * AIG-05 답안지 파일 응답 (서버 저장 경로 등 내부 정보는 노출하지 않는다 - ExamDocumentResponse와 동일한 원칙)
 */
public record SubmissionFileResponse(
        Long submissionFileId,
        String originalName,
        Long fileSize,
        String mimeType,
        Integer pageNo,
        LocalDateTime createdAt
) {
    public static SubmissionFileResponse from(SubmissionFileVo vo) {
        return new SubmissionFileResponse(
                vo.getSubmissionFileId(),
                vo.getOriginalName(),
                vo.getFileSize(),
                vo.getMimeType(),
                vo.getPageNo(),
                vo.getCreatedAt()
        );
    }
}
