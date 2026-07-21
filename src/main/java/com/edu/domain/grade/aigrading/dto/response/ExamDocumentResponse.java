package com.edu.domain.grade.aigrading.dto.response;

import com.edu.domain.grade.aigrading.vo.ExamDocumentVo;

import java.time.LocalDateTime;

/**
 * AIG-01 시험자료 업로드 응답 (저장 경로 등 서버 내부 정보는 노출하지 않는다 - NoticeFileResponse와 동일한 원칙)
 */
public record ExamDocumentResponse(
        Long examDocumentId,
        Long examId,
        String documentType,
        String originalName,
        Long fileSize,
        String mimeType,
        Integer pageNo,
        LocalDateTime createdAt
) {
    public static ExamDocumentResponse from(ExamDocumentVo vo) {
        return new ExamDocumentResponse(
                vo.getExamDocumentId(),
                vo.getExamId(),
                vo.getDocumentType(),
                vo.getOriginalName(),
                vo.getFileSize(),
                vo.getMimeType(),
                vo.getPageNo(),
                vo.getCreatedAt()
        );
    }
}
