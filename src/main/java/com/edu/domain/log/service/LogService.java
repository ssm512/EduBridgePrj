package com.edu.domain.log.service;

import com.edu.common.dto.PageResponse;
import com.edu.domain.log.dto.request.LogSearchRequest;
import com.edu.domain.log.vo.ActivityLog;

import java.util.List;

/** 활동로그 조회 서비스 (관리자 감사로그) */
public interface LogService {

    /** 분류/기간 + 페이징 목록 */
    PageResponse<ActivityLog> getLogs(LogSearchRequest cond);

    /** 존재하는 분류(action_type) 목록 (탭 구성용) */
    List<String> getActionTypes();
}
