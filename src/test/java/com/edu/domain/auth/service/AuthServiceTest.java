package com.edu.domain.auth.service;

import com.edu.common.exception.ApiException;
import com.edu.common.mail.MailService;
import com.edu.domain.auth.dto.AuthResponse;
import com.edu.domain.auth.dto.LoginRequest;
import com.edu.domain.auth.dto.SignupRequest;
import com.edu.domain.auth.dto.UserResponse;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.dto.student.StudentDto;
import com.edu.domain.member.mapper.ParentMapper;
import com.edu.domain.member.mapper.StudentMapper;
import com.edu.domain.member.mapper.TeacherMapper;
import com.edu.domain.member.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 회원가입/로그인 단위 테스트 (AuthService).
 * DB·스프링 컨텍스트 없이 Mockito로 의존성을 대체해 순수 로직만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock UserMapper userMapper;
    @Mock StudentMapper studentMapper;
    @Mock ParentMapper parentMapper;
    @Mock TeacherMapper teacherMapper;
    @Mock JwtService jwtService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock MailService mailService;

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, userMapper, studentMapper,
                parentMapper, teacherMapper, jwtService, refreshTokenService, passwordEncoder, mailService);
    }

    @Test
    @DisplayName("회원가입 - 비밀번호는 BCrypt로 암호화해 저장하고 학생 상세도 함께 등록한다")
    void signup_student_success() {
        SignupRequest req = new SignupRequest(
                "hong", "pw1234", "홍길동", "hong@test.com", "010-0000-0000",
                "STUDENT", "서울고", "1", null, null);

        given(userMapper.selectByLoginId("hong")).willReturn(null);          // 아이디 미사용
        given(passwordEncoder.encode("pw1234")).willReturn("ENC(pw1234)");
        given(userMapper.selectByUserId(any())).willReturn(UserDto.builder()
                .userId(1L).loginId("hong").name("홍길동").email("hong@test.com")
                .roleCode("STUDENT").statusCode("ACTIVE").build());

        UserResponse res = authService.signup(req);

        assertThat(res.loginId()).isEqualTo("hong");
        assertThat(res.roleCode()).isEqualTo("STUDENT");

        // 평문이 아니라 암호화된 값으로 저장되는지 검증
        ArgumentCaptor<UserDto> captor = ArgumentCaptor.forClass(UserDto.class);
        verify(userMapper).insertUser(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("ENC(pw1234)");
        assertThat(captor.getValue().getPassword()).isNotEqualTo("pw1234");

        verify(studentMapper).insertStudent(any(StudentDto.class));          // 학생 상세도 INSERT
    }

    @Test
    @DisplayName("회원가입 - 이미 존재하는 아이디면 409 예외, INSERT는 하지 않는다")
    void signup_duplicateId_throws() {
        SignupRequest req = new SignupRequest(
                "hong", "pw1234", "홍길동", "hong@test.com", null,
                "STUDENT", null, null, null, null);
        given(userMapper.selectByLoginId("hong"))
                .willReturn(UserDto.builder().userId(1L).loginId("hong").build());

        assertThatThrownBy(() -> authService.signup(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("이미 사용 중인 아이디");

        verify(userMapper, never()).insertUser(any());
    }

    @Test
    @DisplayName("로그인 - 인증 성공 시 Access/Refresh 토큰을 발급하고 마지막 로그인 시각을 갱신한다")
    void login_success() {
        LoginRequest req = new LoginRequest("hong", "pw1234");
        UserDto user = UserDto.builder()
                .userId(1L).loginId("hong").name("홍길동").roleCode("STUDENT").statusCode("ACTIVE").build();

        given(userMapper.selectByLoginId("hong")).willReturn(user);
        given(jwtService.createAccessToken(user)).willReturn("access-token");
        given(refreshTokenService.createRefreshToken(user)).willReturn("refresh-token");
        given(jwtService.getAccessTokenExpiresInSeconds()).willReturn(1800L);

        AuthResponse res = authService.login(req);

        assertThat(res.tokenType()).isEqualTo("Bearer");
        assertThat(res.accessToken()).isEqualTo("access-token");
        assertThat(res.refreshToken()).isEqualTo("refresh-token");
        assertThat(res.user().loginId()).isEqualTo("hong");
        verify(authenticationManager).authenticate(any());
        verify(userMapper).updateLastLoginAt(1L);
    }

    @Test
    @DisplayName("로그인 - 아이디/비밀번호가 틀리면 인증 예외가 전파되고 로그인 시각은 갱신하지 않는다")
    void login_badCredentials_throws() {
        LoginRequest req = new LoginRequest("hong", "wrong");
        willThrow(new BadCredentialsException("bad")).given(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);

        verify(userMapper, never()).updateLastLoginAt(any());
    }
}
