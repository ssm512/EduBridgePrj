package com.edu.domain.notification.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** FCM 디바이스 토큰 매퍼 (device_tokens) */
@Mapper
public interface DeviceTokenMapper {

    /** 토큰 등록/갱신 (fcm_token UNIQUE 기준 upsert - 같은 기기 토큰이 다른 사용자로 오면 소유자 갱신) */
    int upsert(@Param("userId") Long userId,
               @Param("token") String token,
               @Param("platform") String platform);

    /** 토큰 삭제 (로그아웃/무효 토큰 정리) */
    int deleteByToken(@Param("token") String token);

    /** 사용자의 FCM 토큰 목록 (푸시 발송 대상) */
    List<String> findTokensByUserId(@Param("userId") Long userId);
}
