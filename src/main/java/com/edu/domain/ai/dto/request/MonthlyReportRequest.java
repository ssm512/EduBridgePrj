package com.edu.domain.ai.dto.request;

import lombok.Data;

@Data
public class MonthlyReportRequest {
    private Long studentId;          // 리포트를 생성할 학생 ID
    private String targetMonth;      // 조회 월, yyyy-MM
    private String teacherComment;   // 강사/관리자가 추가로 입력하는 참고 코멘트
}
