package com.edu.domain.member.dto.parent;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 학생-학부모 연결 내역 수정 요청 (명세서 외 추가 API)
 * PUT /students/{studentId}/parents/{studentParentId}
 * 관계코드와 주 보호자 여부만 변경할 수 있다
 */
public record StudentParentUpdateRequest(

        @NotNull(message = "관계코드는 필수입니다")
        @Pattern(regexp = "FATHER|MOTHER|GUARDIAN",
                 message = "관계코드는 FATHER/MOTHER/GUARDIAN 중 하나여야 합니다")
        String relationCode,

        @NotNull(message = "primaryYn은 필수입니다")
        @Pattern(regexp = "Y|N", message = "primaryYn은 Y 또는 N이어야 합니다")
        String primaryYn
) {
}
