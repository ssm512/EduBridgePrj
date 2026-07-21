package com.edu.domain.grade.aigrading.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * exam_documents 테이블 (MyBatis 매핑용) - AIG-01 시험자료(문제지/정답지) 업로드
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamDocumentVo {

    /** 시험자료 PK */
    private Long examDocumentId;

    /** exams.exam_id FK */
    private Long examId;

    /** QUESTION(문제지) / ANSWER_KEY(정답지) */
    private String documentType;

    /** 업로드 당시 원본 파일명 */
    private String originalName;

    /** 서버 저장 파일명 (UUID + 확장자) */
    private String storedName;

    /** 서버 저장 절대 경로 */
    private String filePath;

    /** MIME 타입 */
    private String mimeType;

    /** 파일 크기 (byte) */
    private Long fileSize;

    /** 페이지 번호 (다중 페이지 문제지인 경우, 선택) */
    private Integer pageNo;

    /** users.user_id FK - 업로더 */
    private Long uploadedBy;

    /** 업로드 일시 */
    private LocalDateTime createdAt;
}
