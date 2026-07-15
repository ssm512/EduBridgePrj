package com.edu.domain.member.service.impl;

import com.edu.common.dto.PageResponse;
import com.edu.common.exception.ApiException;
import com.edu.common.util.TempPasswordUtil;
import com.edu.domain.auth.service.RefreshTokenService;
import com.edu.domain.member.dto.PasswordResetResponse;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.dto.UserSearchRequest;
import com.edu.domain.member.dto.UserUpdateRequest;
import com.edu.domain.member.dto.UserDetailResponse;
import com.edu.domain.member.mapper.UserMapper;
import com.edu.domain.member.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public UserServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           RefreshTokenService refreshTokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public PageResponse<UserDetailResponse> getUsers(UserSearchRequest cond) {
        long totalCount = userMapper.countUsers(cond);
        List<UserDetailResponse> items = userMapper.selectUsers(cond).stream()
                .map(UserDetailResponse::from)
                .toList();
        return PageResponse.of(items, cond.getPage(), cond.getSize(), totalCount);
    }

    @Override
    public UserDetailResponse getUser(Long userId) {
        return UserDetailResponse.from(findUserOrThrow(userId));
    }

    @Override
    @Transactional
    public UserDetailResponse updateUser(Long userId, UserUpdateRequest request) {
        UserDto user = findUserOrThrow(userId);

        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setStatusCode(request.statusCode());
        userMapper.updateUser(user);

        return UserDetailResponse.from(userMapper.selectByUserId(userId));
    }

    /**
     * 관리자 비밀번호 초기화 (A안).
     * 임시 비밀번호 생성 → BCrypt 저장 + must_change_password = TRUE
     * → 기존 Refresh Token 전부 폐기.
     * 임시 비밀번호 원문은 응답으로 한 번만 반환된다.
     */
    @Override
    @Transactional
    public PasswordResetResponse resetPassword(Long userId) {
        UserDto user = findUserOrThrow(userId);

        String tempPassword = TempPasswordUtil.create();
        userMapper.resetPassword(userId, passwordEncoder.encode(tempPassword));
        refreshTokenService.revokeAllForUser(userId);

        return new PasswordResetResponse(userId, user.getLoginId(), tempPassword);
    }

    /** 회원 존재 확인 후 반환, 없으면 404 */
    private UserDto findUserOrThrow(Long userId) {
        UserDto user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다. userId=" + userId);
        }
        return user;
    }
}
