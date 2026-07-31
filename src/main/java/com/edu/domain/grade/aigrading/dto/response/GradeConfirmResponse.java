package com.edu.domain.grade.aigrading.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AIG-09 최종 확정 응답. 기존 grades 테이블에 반영된 결과(gradeId)까지 함께 알려준다.
 */
public record GradeConfirmResponse(
        Long submissionId,
        Long gradeId,
        BigDecimal confirmedScore,
        String statusCode,
        LocalDateTime confirmedAt
) {
}
