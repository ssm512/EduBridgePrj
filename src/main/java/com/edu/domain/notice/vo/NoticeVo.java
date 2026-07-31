package com.edu.domain.notice.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * notices 테이블 + 작성자/읽음/집계 조인 결과 (MyBatis 매핑용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeVo {

    /** 공지 PK */
    private Long noticeId;

    /** 제목 */
    private String title;

    /** 내용 (목록 조회 시에는 채우지 않음) */
    private String content;

    /** 작성자 users PK */
    private Long writerId;

    /** 공지 대상 유형 (ALL/CLASS/STUDENT/PARENT/TEACHER) */
    private String targetType;

    /** 공지 상태 (PUBLISHED/DELETED) - 소프트 삭제 */
    private String noticeStatus;

    /** 등록일시 */
    private LocalDateTime createdAt;

    /** 수정일시 */
    private LocalDateTime updatedAt;

    // ----- 조인/집계 컬럼 -----

    /** 작성자 이름 (users) */
    private String writerName;

    /** 작성자 로그인ID (화면에서 "내 공지" 판별용) */
    private String writerLoginId;

    /** 현재 로그인 사용자의 읽음 여부 (Y/N) */
    private String readYn;

    /** 읽음 처리된 수신자 수 */
    private long readCount;

    /** 첨부파일 개수 */
    private int fileCount;
}
