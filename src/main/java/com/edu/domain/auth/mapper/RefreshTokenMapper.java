package com.edu.domain.auth.mapper;

import com.edu.domain.auth.vo.RefreshTokenVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface RefreshTokenMapper {

    /** Refresh Token(해시) 저장 */
    int insertToken(RefreshTokenVo token);

    /** 해시값으로 토큰 조회 */
    RefreshTokenVo selectByTokenHash(@Param("tokenHash") String tokenHash);

    /** 해시값으로 토큰 폐기 (revoked_yn = 'Y') */
    int revokeByTokenHash(@Param("tokenHash") String tokenHash);

    /** 특정 사용자의 활성 토큰 전체 폐기 - 1인 1토큰 정책 */
    int revokeAllByUserId(@Param("userId") Long userId);

    /** 만료된 토큰 삭제 (배치용) */
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}
