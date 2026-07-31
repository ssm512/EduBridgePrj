package com.edu.domain.log.mapper;

import com.edu.domain.log.dto.request.LogSearchRequest;
import com.edu.domain.log.vo.ActivityLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 활동로그 매퍼. XML: resources/mapper/log/LogMapper.xml
 */
@Mapper
public interface LogMapper {

    /** 목록 조회 (분류/기간 + 페이징) */
    List<ActivityLog> findList(@Param("cond") LogSearchRequest cond);

    /** 전체 건수 (페이징용) */
    long countList(@Param("cond") LogSearchRequest cond);

    /** 존재하는 분류(action_type) 목록 — 탭 구성용 */
    List<String> findActionTypes();
}
