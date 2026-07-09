package com.edu.domain.member.mapper;

import com.edu.domain.member.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    /**
     * 로그인 ID로 사용자 조회
     * JWT 로그인 / Spring Security 인증 시 사용
     */
    UserDto selectByLoginId(@Param("loginId") String loginId);

    /**
     * 사용자 PK로 조회
     */
    UserDto selectByUserId(@Param("userId") Long userId);

    /**
     * 회원 등록
     * 비밀번호는 Service에서 BCrypt 암호화 후 전달
     */
    int insertUser(UserDto user);

    /**
     * 회원 정보 수정
     */
    int updateUser(UserDto user);

    /**
     * 마지막 로그인 시간 갱신
     */
    int updateLastLoginAt(@Param("userId") Long userId);

    /**
     * 회원 상태 변경
     * ACTIVE / INACTIVE / WITHDRAWN
     */
    int updateStatus(@Param("userId") Long userId,
                     @Param("statusCode") String statusCode);
}