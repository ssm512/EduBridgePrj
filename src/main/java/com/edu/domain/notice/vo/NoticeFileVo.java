package com.edu.domain.notice.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * notice_files 테이블 (MyBatis 매핑용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeFileVo {

    /** 첨부파일 PK */
    private Long fileId;

    /** 공지 PK */
    private Long noticeId;

    /** 업로드 당시 원본 파일명 (다운로드 시 이 이름으로 내려줌) */
    private String originalName;

    /** 서버 저장 파일명 (UUID + 확장자, 충돌 방지) */
    private String storedName;

    /** 서버 저장 절대 경로 */
    private String filePath;

    /** 파일 크기 (byte) */
    private Long fileSize;

    /** 업로드 일시 */
    private LocalDateTime createdAt;
}
