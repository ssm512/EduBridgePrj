package com.edu.domain.notification.controller;

import com.edu.common.dto.PageResponse;
import com.edu.domain.notification.dto.request.NotificationSearchRequest;
import com.edu.domain.notification.dto.response.NotificationListResponse;
import com.edu.domain.notification.dto.response.NotificationReadResponse;
import com.edu.domain.notification.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 REST API (NTI-01, 02)
 * 알림 생성 API 는 없음 - 각 도메인 이벤트가 NotificationService 를 내부 호출해서 생성
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationApiController {

    private final NotificationService notificationService;

    public NotificationApiController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * GET /api/v1/notifications - 알림 이력 조회 (NTI-01)
     * ?userName=&notificationType=&readYn=&page=1&size=10
     * userName(수신자 이름 부분검색)은 ADMIN 화면 필터용.
     * 비-ADMIN 역할은 서비스에서 본인 알림(userId)으로 강제된다.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    public PageResponse<NotificationListResponse> getNotificationList(
            @ModelAttribute NotificationSearchRequest search,
            JwtAuthenticationToken authentication) {
        return notificationService.getNotificationList(search,
                currentUserId(authentication), isAdmin(authentication));
    }

    /** PUT /api/v1/notifications/{notificationId}/read - 알림 읽음 처리 (NTI-02, 멱등) */
    @PutMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    public NotificationReadResponse markRead(@PathVariable Long notificationId,
                                             JwtAuthenticationToken authentication) {
        return notificationService.markRead(notificationId,
                currentUserId(authentication), isAdmin(authentication));
    }

    /**
     * JWT userId 클레임에서 로그인 사용자 PK 추출
     * JSON 숫자는 디코딩 시 정수 타입이 보장되지 않으므로 Number 로 받아 변환
     * TODO(팀 공유): 다른 파트에서도 필요해지면 common 유틸로 추출 검토
     */
    private Long currentUserId(JwtAuthenticationToken authentication) {
        return ((Number) authentication.getToken().getClaim("userId")).longValue();
    }

    /** ADMIN 권한 여부 - SecurityConfig 가 roles 클레임을 권한으로 변환해 둔 것을 사용 */
    private boolean isAdmin(JwtAuthenticationToken authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
    }
}
