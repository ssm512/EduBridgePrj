package com.edu.domain.notification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 알림 이력 조회 조건 (NTI-01)
 * GET /api/v1/notifications?userName=&notificationType=&readYn=&page=1&size=10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSearchRequest {

    /**
     * 수신자 PK 필터 - 화면에서 직접 쓰진 않지만, 비-ADMIN 사용자를 본인 알림으로
     * 강제(NotificationServiceImpl)하는 데 필요하므로 남겨둔다.
     */
    private Long userId;

    /**
     * 수신자 이름 검색 (ADMIN 화면용, 부분 일치).
     * 동명이인이 있으면 여러 사람의 알림이 함께 조회된다.
     */
    private String userName;

    /** 알림 유형 필터 (ATTENDANCE / FEE / NOTICE) */
    private String notificationType;

    /** 읽음 여부 필터 (Y/N) */
    private String readYn;

    /** 페이지 번호 (1부터) */
    private int page = 1;

    /** 페이지 크기 */
    private int size = 10;

    /** SQL OFFSET 계산 */
    public int getOffset() {
        return (Math.max(page, 1) - 1) * size;
    }
}
