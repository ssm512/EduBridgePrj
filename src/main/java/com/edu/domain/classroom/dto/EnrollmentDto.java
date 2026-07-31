package com.edu.domain.classroom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * enrollments + students/users + classes 조인 결과 DTO (MyBatis 매핑용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDto {

    /** 수강 PK */
    private Long enrollmentId;

    /** 학생 PK */
    private Long studentId;

    /** 반 PK */
    private Long classId;

    /** 수강 시작일 */
    private LocalDate enrollDate;

    /** 수강 종료일 */
    private LocalDate endDate;

    /** 수강 상태 (ACTIVE/ENDED) */
    private String statusCode;

    /** 등록일 */
    private LocalDateTime createdAt;

    // ----- 조인 컬럼 -----

    /** 학생 이름 (students → users) */
    private String studentName;

    /** 학번 */
    private String studentNo;

    /** 반 이름 (classes) */
    private String className;

    /** 반 과목 */
    private String subject;
}
