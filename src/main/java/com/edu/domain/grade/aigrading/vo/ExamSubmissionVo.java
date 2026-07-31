package com.edu.domain.grade.aigrading.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * exam_submissions 테이블 (MyBatis 매핑용) - AIG-05 학생 답안지 업로드, AIG-06 이후 AI 채점/검수/확정 상태 관리
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSubmissionVo {

    /** 학생시험제출 PK */
    private Long submissionId;

    /** exams.exam_id FK */
    private Long examId;

    /** students.student_id FK */
    private Long studentId;

    /** UPLOADED / ANALYZING / REVIEW_REQUIRED / CONFIRMED / FAILED */
    private String statusCode;

    /** AI 채점 합계 점수 (AIG-06 결과) */
    private BigDecimal aiTotalScore;

    /** 교사 검수 후 확정 점수 (AIG-09) */
    private BigDecimal confirmedScore;

    /** AI 채점 신뢰도 (0~1) */
    private BigDecimal aiConfidence;

    /** 채점 실패 시 오류 메시지 */
    private String errorMessage;

    /** users.user_id FK - 답안지 업로더 */
    private Long uploadedBy;

    /** users.user_id FK - 검수자 */
    private Long reviewedBy;

    /** grades.grade_id FK - 확정 시 연결되는 성적 레코드 */
    private Long gradeId;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime confirmedAt;
}
