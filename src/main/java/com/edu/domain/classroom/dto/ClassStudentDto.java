package com.edu.domain.classroom.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * CLS-03 반 상세의 수강 학생 정보 (enrollments + students + users 조인, MyBatis 매핑용)
 */
@Data
public class ClassStudentDto {

    /** 수강 PK */
    private Long enrollmentId;

    /** 학생 PK */
    private Long studentId;

    /** 학번 */
    private String studentNo;

    /** 학생 이름 */
    private String name;

    /** 학년 */
    private String gradeLevel;

    /** 수강 시작일 */
    private LocalDate enrollDate;

    /** 수강 종료일 */
    private LocalDate endDate;

    /** 수강 상태 (ACTIVE/ENDED) */
    private String statusCode;
}
