package com.edu.domain.fee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 회비 알림 배치 수동 실행 결과 (명세서 외 - 테스트/운영용).
 * 각 배치가 새로 생성한 알림 건수 (중복으로 건너뛴 건 제외).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeNotificationRunResponse {
    private int upcomingCreated;   // FEE-08 납부 예정 알림 생성 수
    private int overdueCreated;    // FEE-09 미납 알림 생성 수
}
