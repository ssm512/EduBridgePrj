package com.edu.domain.log.controller;

import com.edu.common.dto.PageResponse;
import com.edu.domain.log.dto.request.LogSearchRequest;
import com.edu.domain.log.service.LogService;
import com.edu.domain.log.vo.ActivityLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 활동로그(감사로그) 조회 API — 관리자 전용.
 */
@RestController
@RequestMapping("/api/logs")
@PreAuthorize("hasRole('ADMIN')")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    /** 분류/기간 + 페이징 목록 */
    @GetMapping
    public PageResponse<ActivityLog> list(@ModelAttribute LogSearchRequest cond) {
        return logService.getLogs(cond);
    }

    /** 존재하는 분류(action_type) 목록 — 탭 구성용 */
    @GetMapping("/types")
    public List<String> types() {
        return logService.getActionTypes();
    }
}
