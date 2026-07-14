package com.edu.domain.notice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * NOT-04 공지 수정 요청
 * PUT /api/notices/{noticeId}
 *
 * 제목/내용/대상을 전체 교체한다. (targets는 삭제 후 재등록)
 */
public record NoticeUpdateRequest(

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
