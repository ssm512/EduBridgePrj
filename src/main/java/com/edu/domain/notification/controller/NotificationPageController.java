package com.edu.domain.notification.controller;

import com.edu.common.dto.PageResponse;
import com.edu.domain.notification.dto.request.NotificationSearchRequest;
import com.edu.domain.notification.dto.response.NotificationListResponse;
import com.edu.domain.notification.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 알림 이력 화면 라우팅 (SCR-W-18)
 * 목록/필터/페이징은 서버 렌더링 (회비 목록과 같은 패턴)
 */
@Controller
@RequestMapping("/admin/notifications")
public class NotificationPageController {

    private final NotificationService notificationService;

    public NotificationPageController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** GET /admin/notifications - 알림 발송 이력 화면 */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String notificationList(@ModelAttribute("search") NotificationSearchRequest search, Model model) {
        // @PreAuthorize 로 ADMIN 만 진입 가능하므로 admin=true 고정, currentUserId 는 사용되지 않음
        // (위 어노테이션을 바꾸면 이 호출도 같이 바꿔야 한다)
        PageResponse<NotificationListResponse> notificationPage =
                notificationService.getNotificationList(search, null, true);

        model.addAttribute("title", "알림이력");
        model.addAttribute("notificationPage", notificationPage);
        return "admin/notification/notificationList";
    }
}
