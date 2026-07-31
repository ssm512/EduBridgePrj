package com.edu.domain.grade.aigrading.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * submission_files 테이블 (MyBatis 매핑용) - AIG-05 학생 답안지 페이지별 이미지 파일
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionFileVo {

    /** 학생답안파일 PK */
    private Long submissionFileId;

    /** exam_submissions.submission_id FK */
    private Long submissionId;

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

    /** 페이지 번호 (다중 페이지 답안지, 재업로드 시에도 이어서 증가) */
    private Integer pageNo;

    private LocalDateTime createdAt;
}
