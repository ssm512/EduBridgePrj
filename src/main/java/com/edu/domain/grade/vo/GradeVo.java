package com.edu.domain.grade.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GradeVo {
    private Long gradeId;             // 성적 고유 ID, grades.grade_id
    private Long examId;              // 시험 ID, exams.exam_id
    private Long studentId;           // 학생 ID, students.student_id
    private BigDecimal score;         // 학생이 받은 점수
    private Integer rankNo;           // 석차
    private String comment;           // 성적 관련 비고 또는 강사 코멘트
    private LocalDateTime createdAt;  // 성적 등록일시
    private LocalDateTime updatedAt;  // 성적 수정일시
}