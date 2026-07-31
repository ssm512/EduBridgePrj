package com.edu.domain.notification.mapper;

import com.edu.domain.notification.dto.request.NotificationSearchRequest;
import com.edu.domain.notification.dto.response.NotificationListResponse;
import com.edu.domain.notification.vo.NotificationVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {

    /** 알림 이력 조회 - 검색 조건 + 페이징 (NTI-01) */
    List<NotificationListResponse> selectNotificationList(NotificationSearchRequest search);

    /** 알림 이력 전체 건수 - 페이징 계산용 */
    long countNotificationList(NotificationSearchRequest search);

    /** 알림 단건 조회 */
    NotificationVo selectByNotificationId(@Param("notificationId") Long notificationId);

    /** 알림 생성 - 내부 서비스 전용 (생성 API 없음) */
    int insertNotification(NotificationVo notification);

    /** 읽음 처리 - read_yn = 'Y' (NTI-02) */
    int updateReadYn(@Param("notificationId") Long notificationId);

    /** 본인 알림 전체 읽음 처리 (안 읽은 것만) - 앱 "모두 읽음"용. 처리 건수 반환 */
    int updateAllReadByUser(@Param("userId") Long userId);

    /**
     * 학생의 학부모 user_id 목록 조회 - 회비 알림 수신자 결정용
     * (students <- student_parents -> parents -> users 경로)
     */
    List<Long> selectParentUserIdsByStudentId(@Param("studentId") Long studentId);

    /**
     * 학생 본인의 user_id 조회 - 성적 알림처럼 학생 본인에게도 보내야 하는 경우 사용.
     * students.user_id는 UNIQUE라 1건만 나온다.
     */
    Long selectStudentUserIdByStudentId(@Param("studentId") Long studentId);

    /**
     * 같은 수신자에게 동일 (타입 + 제목) 알림이 이미 있는지 - 중복 알림 방지용.
     * 회비 예정/미납 배치 알림을 청구월 기준 1회만 보내기 위함 (FEE-08/09).
     */
    boolean existsByUserAndTypeAndTitle(@Param("userId") Long userId,
                                        @Param("notificationType") String notificationType,
                                        @Param("title") String title);
}
