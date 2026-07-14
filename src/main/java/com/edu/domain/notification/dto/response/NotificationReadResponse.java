package com.edu.domain.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 알림 읽음 처리 응답 (NTI-02)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationReadResponse {

    /** 알림 PK */
    private Long notificationId;

    /** 처리 후 읽음 여부 (항상 Y) */
    private String readYn;
}
