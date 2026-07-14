package com.edu.domain.counseling.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CounselingVo {
    private Long counselingId;             // 상담 고유 ID, counseling_records.counseling_id
    private Long studentId;                // 상담 대상 학생 ID, students.student_id
    private Long parentId;                 // 학부모 상담 시 학부모 ID, parents.parent_id
    private Long teacherId;                // 상담 담당 강사 ID, teachers.teacher_id
    private LocalDate counselingDate;      // 상담일
    private String counselingType;         // 상담 유형, STUDENT/PARENT
    private String content;                // 상담 내용
    private String visibilityCode;         // 공개 범위, PRIVATE/SHARED
    private Long createdBy;                // 등록자 ID, users.user_id
    private LocalDateTime createdAt;       // 등록일시
}
