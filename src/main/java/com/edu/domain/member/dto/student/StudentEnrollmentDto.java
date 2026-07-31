package com.edu.domain.member.dto.student;

import lombok.Data;

import java.time.LocalDate;

/**
 * STU-03 상세의 수강반 정보 (enrollments + classes 조인, MyBatis 매핑용)
 * 반/수강 파트의 Enrollment DTO와 구분하기 위해 Student 접두어 사용
 */
@Data
public class StudentEnrollmentDto {

    /** 수강 PK */
    private Long enrollmentId;

    /** 반 PK */
    private Long classId;

    /** 반 이름 */
    private String className;

    /** 과목 */
    private String subject;

    /** 수강 시작일 */
    private LocalDate enrollDate;

    /** 수강 종료일 */
    private LocalDate endDate;

    /** 수강 상태 (ACTIVE/ENDED) */
    private String statusCode;
}
