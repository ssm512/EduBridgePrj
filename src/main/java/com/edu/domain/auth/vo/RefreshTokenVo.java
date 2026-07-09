package com.edu.domain.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * refresh_tokens 테이블 VO
 * refreshToken 컬럼에는 원문이 아니라 SHA-256 해시를 저장한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenVo {

    /** PK */
    private Long tokenId;

    /** users.user_id FK */
    private Long userId;

    /** SHA-256 해시된 refresh token */
    private String refreshToken;

    /** 만료 시각 */
    private LocalDateTime expiresAt;

    /** 폐기 여부 (Y/N) */
    private String revokedYn;

    /** 생성 시각 */
    private LocalDateTime createdAt;

    public boolean isRevoked() {
        return "Y".equals(revokedYn);
    }
}
