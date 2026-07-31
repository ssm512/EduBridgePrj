package com.edu.domain.notification.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * notifications 테이블 VO
 * 알림은 사용자가 직접 생성하지 않고 각 도메인 이벤트(회비 등록, 공지 등록 등)가
 * NotificationService 를 통해 내부적으로 생성한다 (생성 API 없음 - 명세 기준)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVo {

    /** PK */
    private Long notificationId;

    /** 수신자 users.user_id FK */
    private Long userId;

    /** 알림 유형 (ATTENDANCE / FEE / NOTICE) */
    private String notificationType;

    /** 제목 */
    private String title;

    /** 내용 */
    private String message;

    /** 발송 채널 (APP / KAKAO / EMAIL / SMS) */
    private String sendChannel;

    /** 발송 상태 (PENDING / SENT / FAILED) */
    private String sendStatus;

    /** 발송 시각 */
    private LocalDateTime sentAt;

    /** 읽음 여부 (Y/N) */
    private String readYn;

    /** 생성 시각 */
    private LocalDateTime createdAt;

    public boolean isRead() {
        return "Y".equals(readYn);
    }
}
