package com.edu.domain.member.mapper;

import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.dto.UserSearchRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    /**
     * USER-01 회원 목록 조회
     * 권한/상태/키워드 필터 + 페이징
     */
    List<UserDto> selectUsers(@Param("cond") UserSearchRequest cond);

    /**
     * USER-01 회원 목록 전체 건수 (페이징용)
     */
    long countUsers(@Param("cond") UserSearchRequest cond);

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
     * USER-03 회원 정보 수정 (이름/이메일/연락처/상태)
     */
    int updateUser(UserDto user);

    /**
     * 마지막 로그인 시간 갱신 (AuthService 로그인 시 사용)
     */
    int updateLastLoginAt(@Param("userId") Long userId);

    /**
     * 회원 상태 변경
     * ACTIVE / INACTIVE / WITHDRAWN
     */
    int updateStatus(@Param("userId") Long userId,
                     @Param("statusCode") String statusCode);
}