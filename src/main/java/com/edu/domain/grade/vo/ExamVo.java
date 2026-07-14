package com.edu.domain.grade.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExamVo {
    private Long examId;              // 시험 고유 ID, exams.exam_id
    private Long classId;             // 시험이 속한 반 ID, classes.class_id
    private String examName;          // 시험명
    private String subject;           // 과목명
    private LocalDate examDate;       // 시험일
    private BigDecimal totalScore;    // 시험 만점
    private Long createdBy;           // 시험 등록자 ID, users.user_id
    private LocalDateTime createdAt;  // 시험 등록일시
}