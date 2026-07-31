package com.edu.domain.attendance.dto.response;

import lombok.Data;

/**
 * 학생이 출석할 반 선택용 옵션 (본인 수강 강의).
 * GET /api/attendance/my-classes 응답 항목.
 * MyBatis가 직접 매핑하므로(record 아님) @Data 클래스로 둔다.
 */
@Data
public class ClassOptionResponse {
    private Long classId;
    private String className;
}
