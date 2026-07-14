package com.edu.domain.member.service;

import com.edu.common.dto.PageResponse;
import com.edu.domain.member.dto.UserSearchRequest;
import com.edu.domain.member.dto.UserUpdateRequest;
import com.edu.domain.member.dto.UserDetailResponse;

/**
 * 회원관리 서비스 (USER-01 ~ USER-03)
 */
public interface UserService {

    /** USER-01 회원 목록 조회 (권한/상태/키워드 필터 + 페이징) */
    PageResponse<UserDetailResponse> getUsers(UserSearchRequest cond);

    /** USER-02 회원 상세 조회 */
    UserDetailResponse getUser(Long userId);

    /** USER-03 회원 수정 (이름/이메일/연락처/상태) */
    UserDetailResponse updateUser(Long userId, UserUpdateRequest request);
}
