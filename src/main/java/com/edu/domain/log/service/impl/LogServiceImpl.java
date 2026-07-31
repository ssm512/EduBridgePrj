package com.edu.domain.log.service.impl;

import com.edu.common.dto.PageResponse;
import com.edu.domain.log.dto.request.LogSearchRequest;
import com.edu.domain.log.mapper.LogMapper;
import com.edu.domain.log.service.LogService;
import com.edu.domain.log.vo.ActivityLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LogServiceImpl implements LogService {

    private final LogMapper logMapper;

    public LogServiceImpl(LogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @Override
    public PageResponse<ActivityLog> getLogs(LogSearchRequest cond) {
        long total = logMapper.countList(cond);
        List<ActivityLog> items = logMapper.findList(cond);
        return PageResponse.of(items, cond.getPage(), cond.getSize(), total);
    }

    @Override
    public List<String> getActionTypes() {
        return logMapper.findActionTypes();
    }
}
