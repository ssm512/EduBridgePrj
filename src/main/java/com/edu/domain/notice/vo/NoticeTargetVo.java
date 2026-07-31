package com.edu.domain.notice.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * notice_targets 테이블 + 대상 이름 조인 결과 (MyBatis 매핑용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeTargetVo {

    /** 공지대상 PK */
    private Long targetId;

    /** 공지 PK */
    private Long noticeId;

    /** 대상 유형 (CLASS/STUDENT/PARENT/TEACHER) */
    private String targetType;

    /** 대상 PK (class_id/student_id/parent_id/teacher_id) */
    private Long targetRefId;

    /** 화면 표시용 대상 이름 (반 이름 또는 회원 이름) */
    private String targetName;
}
