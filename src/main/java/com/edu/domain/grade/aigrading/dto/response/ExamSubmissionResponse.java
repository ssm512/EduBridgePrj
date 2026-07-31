package com.edu.domain.grade.aigrading.dto.response;

import com.edu.domain.grade.aigrading.vo.ExamSubmissionVo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AIG-05 학생 답안지 업로드 응답. AIG-06 이후 채점 진행 상태(statusCode)도 같은 형태로 재사용한다.
 */
public record ExamSubmissionResponse(
        Long submissionId,
        Long examId,
        Long studentId,
        String statusCode,
        LocalDateTime createdAt,
        List<SubmissionFileResponse> files
) {
    public static ExamSubmissionResponse from(ExamSubmissionVo vo, List<SubmissionFileResponse> files) {
        return new ExamSubmissionResponse(
                vo.getSubmissionId(),
                vo.getExamId(),
                vo.getStudentId(),
                vo.getStatusCode(),
                vo.getCreatedAt(),
                files
        );
    }
}
