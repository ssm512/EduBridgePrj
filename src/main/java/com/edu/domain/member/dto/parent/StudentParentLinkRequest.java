package com.edu.domain.member.dto.parent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * PAR-03 학생-학부모 연결 요청
 * POST /students/{studentId}/parents
 * 학부모는 PK 대신 로그인ID로 지정한다 (화면에서 확인하기 쉬운 값)
 */
public record StudentParentLinkRequest(

        @NotBlank(message = "학부모 로그인 ID는 필수입니다")
        String parentLoginId,

        @NotNull(message = "관계코드는 필수입니다")
        @Pattern(regexp = "FATHER|MOTHER|GUARDIAN",
                 message = "관계코드는 FATHER/MOTHER/GUARDIAN 중 하나여야 합니다")
        String relationCode,

        /** 주 보호자 여부 (Y/N, 생략 시 N) */
        @Pattern(regexp = "Y|N", message = "primaryYn은 Y 또는 N이어야 합니다")
        String primaryYn
) {
}
