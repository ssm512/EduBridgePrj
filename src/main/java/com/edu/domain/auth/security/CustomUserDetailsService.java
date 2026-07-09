package com.edu.domain.auth.security;

import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.mapper.UserMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 시 AuthenticationManager(DaoAuthenticationProvider)가 사용하는 서비스.
 * loginId로 DB에서 회원을 찾아 UserDetails로 변환한다.
 * 비밀번호 비교(BCrypt matches)는 Spring Security가 수행한다.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    public CustomUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        UserDto user = userMapper.selectByLoginId(loginId);
        if (user == null) {
            throw new UsernameNotFoundException("존재하지 않는 사용자입니다: " + loginId);
        }

        return User.withUsername(user.getLoginId())
                .password(user.getPassword())
                .disabled(!"ACTIVE".equals(user.getStatusCode()))   // ACTIVE만 로그인 가능
                .roles(user.getRoleCode())                          // ROLE_ 접두어 자동 부여
                .build();
    }
}
