package com.edu.domain.notice.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 공지 조회 시 "현재 로그인 사용자" 정보를 SQL에 전달하는 컨텍스트.
 *
 * NOT-02(목록)·NOT-03(상세)의 "대상자별 조회"를 구현하기 위해
 * 서비스에서 로그인 사용자의 롤과 롤별 PK를 채워 Mapper에 넘긴다.
 * - ADMIN   : 제한 없음
 * - TEACHER : teacherId (본인 대상/담당 반 공지 + 본인 작성 공지)
 * - STUDENT : studentId (본인 대상/수강 반 공지)
 * - PARENT  : parentId  (본인 대상/자녀 대상/자녀 수강 반 공지)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeQueryContext {

    /** users PK */
    private Long userId;

    /** ADMIN/TEACHER/STUDENT/PARENT */
    private String roleCode;

    /** roleCode=STUDENT일 때 students PK */
    private Long studentId;

    /** roleCode=PARENT일 때 parents PK */
    private Long parentId;

    /** roleCode=TEACHER일 때 teachers PK */
    private Long teacherId;
}
