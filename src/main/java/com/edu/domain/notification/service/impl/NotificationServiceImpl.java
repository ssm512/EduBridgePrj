package com.edu.domain.notification.service.impl;

import com.edu.common.dto.PageResponse;
import com.edu.common.exception.ApiException;
import com.edu.domain.notification.dto.request.NotificationSearchRequest;
import com.edu.domain.notification.dto.response.NotificationListResponse;
import com.edu.domain.notification.dto.response.NotificationReadResponse;
import com.edu.domain.notification.mapper.NotificationMapper;
import com.edu.domain.notification.service.FcmService;
import com.edu.domain.notification.service.NotificationService;
import com.edu.domain.notification.vo.NotificationVo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final FcmService fcmService;

    public NotificationServiceImpl(NotificationMapper notificationMapper, FcmService fcmService) {
        this.notificationMapper = notificationMapper;
        this.fcmService = fcmService;
    }

    @Override
    public PageResponse<NotificationListResponse> getNotificationList(NotificationSearchRequest search,
                                                                      Long currentUserId, boolean admin) {
        // 데이터 범위 제한: ADMIN 외에는 userId 파라미터를 무시하고 본인으로 강제
        // (파라미터 검증으로 403 을 던지는 대신 조용히 덮어쓴다 - 본인 알림함 조회가 기능의 기본 동작이므로)
        // TODO(팀 확인): TEACHER 가 담당 반 학생의 알림을 볼 수 있어야 하는지 - 회의 안건
        if (!admin) {
            search.setUserId(currentUserId);
        }

        long totalElements = notificationMapper.countNotificationList(search);
        if (totalElements == 0) {
            return PageResponse.of(List.of(), search.getPage(), search.getSize(), 0);
        }

        List<NotificationListResponse> content = notificationMapper.selectNotificationList(search);
        return PageResponse.of(content, search.getPage(), search.getSize(), totalElements);
    }

    @Override
    @Transactional
    public NotificationReadResponse markRead(Long notificationId, Long currentUserId, boolean admin) {
        NotificationVo notification = notificationMapper.selectByNotificationId(notificationId);
        if (notification == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다");
        }

        // 소유권 검증: 본인 알림이 아니면 403 (ADMIN 은 예외)
        if (!admin && !notification.getUserId().equals(currentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인의 알림만 읽음 처리할 수 있습니다");
        }

        // 이미 읽은 알림이면 UPDATE 생략 - 멱등이므로 에러가 아니라 같은 응답을 반환
        if (!notification.isRead()) {
            notificationMapper.updateReadYn(notificationId);
        }

        return new NotificationReadResponse(notificationId, "Y");
    }

    @Override
    @Transactional
    public int markAllRead(Long currentUserId) {
        return notificationMapper.updateAllReadByUser(currentUserId);
    }

    @Override
    @Transactional
    public void createNotification(Long userId, String notificationType, String title, String message) {
        // 실제 외부 발송(카카오/이메일 등)은 없으므로 APP 채널 즉시 발송 완료로 기록
        // TODO(팀 확인): 발송 채널/실패 시뮬레이션 여부 - 회의 안건
        NotificationVo notification = NotificationVo.builder()
                .userId(userId)
                .notificationType(notificationType)
                .title(title)
                .message(message)
                .sendChannel("APP")
                .sendStatus("SENT")
                .sentAt(LocalDateTime.now())
                .build();

        notificationMapper.insertNotification(notification);

        // 인앱 알림 저장 후 FCM 푸시 발송(@Async, 미설정 시 no-op)
        fcmService.sendToUser(userId, notificationType, title, message);
    }

    @Override
    @Transactional
    public void notifyParentsOfStudent(Long studentId, String notificationType, String title, String message) {
        List<Long> parentUserIds = notificationMapper.selectParentUserIdsByStudentId(studentId);

        // 연결된 학부모가 없어도 본 작업(회비 등록 등)이 실패하면 안 되므로 조용히 종료
        for (Long parentUserId : parentUserIds) {
            createNotification(parentUserId, notificationType, title, message);
        }
    }

    @Override
    @Transactional
    public int notifyParentsOfStudentOnce(Long studentId, String notificationType, String title, String message) {
        List<Long> parentUserIds = notificationMapper.selectParentUserIdsByStudentId(studentId);

        int created = 0;
        for (Long parentUserId : parentUserIds) {
            // 같은 제목의 알림을 이미 받은 학부모는 건너뛴다 (청구월 기준 1회)
            if (notificationMapper.existsByUserAndTypeAndTitle(parentUserId, notificationType, title)) {
                continue;
            }
            createNotification(parentUserId, notificationType, title, message);
            created++;
        }
        return created;
    }
}
