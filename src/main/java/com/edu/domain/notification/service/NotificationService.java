package com.edu.domain.notification.service;

import com.edu.common.dto.PageResponse;
import com.edu.domain.notification.dto.request.NotificationSearchRequest;
import com.edu.domain.notification.dto.response.NotificationListResponse;
import com.edu.domain.notification.dto.response.NotificationReadResponse;

public interface NotificationService {

    /**
     * 알림 이력 조회 (NTI-01)
     * ADMIN 이 아니면 userId 필터를 본인으로 강제한다 (남의 알림 조회 차단)
     *
     * @param currentUserId 로그인 사용자 PK (JWT userId 클레임)
     * @param admin         ADMIN 권한 여부
     */
    PageResponse<NotificationListResponse> getNotificationList(NotificationSearchRequest search,
                                                               Long currentUserId, boolean admin);

    /**
     * 알림 읽음 처리 (NTI-02) - 멱등
     * ADMIN 이 아니면 본인 알림만 처리 가능
     */
    NotificationReadResponse markRead(Long notificationId, Long currentUserId, boolean admin);

    /**
     * 알림 생성 - 내부 전용 (생성 API 없음, 다른 도메인 이벤트에서 호출)
     *
     * @param userId           수신자 users.user_id
     * @param notificationType ATTENDANCE / FEE / NOTICE
     */
    void createNotification(Long userId, String notificationType, String title, String message);

    /**
     * 학생의 학부모 전원에게 알림 생성 - 회비 알림 등 보호자 대상 알림용
     * 연결된 학부모가 없으면 아무 일도 하지 않는다 (예외 아님)
     */
    void notifyParentsOfStudent(Long studentId, String notificationType, String title, String message);

    /**
     * 학생의 학부모 전원에게 알림 생성하되, 같은 (타입 + 제목) 알림을 이미 받은 수신자는 건너뛴다.
     * 회비 예정/미납 배치 알림의 중복 방지용 - 청구월 기준 1회 (FEE-08/09).
     *
     * @return 실제로 생성된 알림 수 (중복으로 건너뛴 건 제외)
     */
    int notifyParentsOfStudentOnce(Long studentId, String notificationType, String title, String message);
}
