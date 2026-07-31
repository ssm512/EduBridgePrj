package com.edu.domain.attendance.dto.response;

import lombok.Data;

/**
 * 학부모의 자녀 선택용 옵션 (본인 자녀만).
 * GET /api/attendance/my-children 응답 항목.
 * MyBatis가 직접 매핑하므로 @Data 클래스로 둔다.
 */
@Data
public class ChildOptionResponse {
    private Long studentId;
    private String studentName;
}
