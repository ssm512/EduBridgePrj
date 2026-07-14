package com.edu.domain.notice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * NOT-01 공지 등록 요청
 * POST /api/notices
 *
 * targetType이 ALL이 아니면 targetIds(대상 PK 목록)가 1개 이상 필요하다. (서비스에서 검증)
 * - CLASS   → class_id 목록
 * - STUDENT → student_id 목록
 * - PARENT  → parent_id 목록
 * - TEACHER → teacher_id 목록
 */
public record NoticeCreateRequest(

        @NotBlank(message = "title은 필수입니다")
        @Size(max = 200, message = "title은 200자 이하여야 합니다")
        String title,

        @NotBlank(message = "content는 필수입니다")
        String content,

        @NotBlank(message = "targetType은 필수입니다")
        @Pattern(regexp = "ALL|CLASS|STUDENT|PARENT|TEACHER",
                 message = "targetType은 ALL/CLASS/STUDENT/PARENT/TEACHER 중 하나여야 합니다")
        String targetType,

        /** targetType != ALL일 때 대상 PK 목록 */
        List<Long> targetIds
) {
}
