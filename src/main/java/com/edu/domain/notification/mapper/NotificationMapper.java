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

    /**
     * 학생의 학부모 user_id 목록 조회 - 회비 알림 수신자 결정용
     * (students <- student_parents -> parents -> users 경로)
     */
    List<Long> selectParentUserIdsByStudentId(@Param("studentId") Long studentId);
}
