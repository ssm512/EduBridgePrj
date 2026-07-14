package com.edu.domain.notification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 알림 이력 조회 조건 (NTI-01)
 * GET /api/v1/notifications?userId=&notificationType=&readYn=&page=1&size=10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSearchRequest {

    /** 수신자 필터 (ADMIN 전용 - 일반 사용자는 서버에서 본인 것으로 강제) */
    private Long userId;

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
