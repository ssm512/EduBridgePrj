package com.edu.domain.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 이력 조회 응답 (NTI-01)
 * notifications + users JOIN - 관리자 이력 화면(SCR-W-18)의 '대상' 컬럼용 수신자 이름 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponse {

    /** 알림 PK */
    private Long notificationId;

    /** 수신자 PK */
    private Long userId;

    /** 수신자 이름 (users.name) */
    private String userName;

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
}
