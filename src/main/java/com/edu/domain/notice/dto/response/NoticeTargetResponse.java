package com.edu.domain.notice.dto.response;

import com.edu.domain.notice.vo.NoticeTargetVo;

/**
 * 공지 대상 응답 (NOTICE-05 대상별 공지)
 */
public record NoticeTargetResponse(
        Long targetId,
        String targetType,    // CLASS/STUDENT/PARENT/TEACHER
        Long targetRefId,
        String targetName     // 반 이름 또는 회원 이름
) {
    public static NoticeTargetResponse from(NoticeTargetVo vo) {
        return new NoticeTargetResponse(
                vo.getTargetId(),
                vo.getTargetType(),
                vo.getTargetRefId(),
                vo.getTargetName()
        );
    }
}
