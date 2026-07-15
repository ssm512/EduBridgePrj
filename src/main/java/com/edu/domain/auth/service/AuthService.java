package com.edu.domain.auth.service;

import com.edu.common.exception.ApiException;
import com.edu.common.mail.MailService;
import com.edu.common.util.TempPasswordUtil;
import com.edu.domain.auth.dto.AuthResponse;
import com.edu.domain.auth.dto.LoginRequest;
import com.edu.domain.auth.dto.MyPageUpdateRequest;
import com.edu.domain.auth.dto.PasswordChangeRequest;
import com.edu.domain.auth.dto.PasswordResetRequest;
import com.edu.domain.auth.dto.SignupRequest;
import com.edu.domain.auth.dto.TokenRefreshRequest;
import com.edu.domain.auth.dto.UserResponse;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.dto.parent.ParentDto;
import com.edu.domain.member.dto.student.StudentDto;
import com.edu.domain.member.dto.teacher.TeacherDto;
import com.edu.domain.member.mapper.ParentMapper;
import com.edu.domain.member.mapper.StudentMapper;
import com.edu.domain.member.mapper.TeacherMapper;
import com.edu.domain.member.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 흐름의 중심 서비스.
 * - 아이디/비밀번호 검증 → AuthenticationManager
 * - Access Token 생성   → JwtService
 * - Refresh Token 관리  → RefreshTokenService
 * - 회원가입            → UserMapper + PasswordEncoder
 */
@Service
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final ParentMapper parentMapper;
    private final TeacherMapper teacherMapper;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserMapper userMapper,
                       StudentMapper studentMapper,
                       ParentMapper parentMapper,
                       TeacherMapper teacherMapper,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       PasswordEncoder passwordEncoder,
                       MailService mailService) {
        this.authenticationManager = authenticationManager;
        this.userMapper = userMapper;
        this.studentMapper = studentMapper;
        this.parentMapper = parentMapper;
        this.teacherMapper = teacherMapper;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    /**
     * 회원가입.
     * users INSERT + 역할별 상세 테이블(students/parents/teachers) INSERT를
     * 한 트랜잭션으로 처리한다.
     * - 강사(TEACHER)는 INACTIVE로 가입되어 관리자가 승인(ACTIVE 전환)해야 로그인 가능.
     */
    public UserResponse signup(SignupRequest request) {
        if (userMapper.selectByLoginId(request.loginId()) != null) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다");
        }

        String roleCode = request.roleCode() != null ? request.roleCode() : "STUDENT";

        UserDto user = UserDto.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))   // BCrypt 암호화
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .roleCode(roleCode)
                .statusCode("TEACHER".equals(roleCode) ? "INACTIVE" : "ACTIVE")
                .build();

        userMapper.insertUser(user);   // useGeneratedKeys로 userId 채워짐

        // 역할별 상세 테이블 INSERT (users와 1:1)
        switch (roleCode) {
            case "STUDENT" -> studentMapper.insertStudent(StudentDto.builder()
                    .userId(user.getUserId())
                    .schoolName(request.schoolName())
                    .gradeLevel(request.gradeLevel())
                    .build());
            case "PARENT" -> parentMapper.insertParent(ParentDto.builder()
                    .userId(user.getUserId())
                    .address(request.address())
                    .build());
            case "TEACHER" -> teacherMapper.insertTeacher(TeacherDto.builder()
                    .userId(user.getUserId())
                    .subject(request.subject())
                    .build());
            default -> { /* ADMIN 등은 셀프 가입 대상 아님 */ }
        }

        return UserResponse.from(userMapper.selectByUserId(user.getUserId()));
    }

    /** 로그인: 인증 성공 시 Access + Refresh Token 발급 */
    public AuthResponse login(LoginRequest request) {
        // CustomUserDetailsService → UserMapper → PasswordEncoder.matches() 흐름으로 검증
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.loginId(), request.password())
        );

        UserDto user = userMapper.selectByLoginId(request.loginId());
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "로그인 정보가 올바르지 않습니다");
        }

        userMapper.updateLastLoginAt(user.getUserId());
        return issueTokens(user);
    }

    /** Refresh Token으로 재발급 (rotation: 기존 토큰 폐기 후 새로 발급) */
    public AuthResponse refresh(TokenRefreshRequest request) {
        UserDto user = refreshTokenService.verifyAndGetUser(request.refreshToken());
        refreshTokenService.revoke(request.refreshToken());
        return issueTokens(user);
    }

    /** 로그아웃: 서버 측 Refresh Token 폐기 (Access Token은 클라이언트가 삭제) */
    public void logout(TokenRefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    /**
     * 본인 비밀번호 변경.
     * 현재 비밀번호 확인 → 새 비밀번호로 교체(강제 변경 플래그 해제)
     * → 기존 Refresh Token 전부 폐기 (다른 기기 세션 무효화)
     */
    public void changePassword(String loginId, PasswordChangeRequest request) {
        UserDto user = userMapper.selectByLoginId(loginId);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "새 비밀번호가 현재 비밀번호와 같습니다");
        }

        userMapper.updatePassword(user.getUserId(), passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAllForUser(user.getUserId());
    }

    /**
     * 마이페이지 - 본인 정보(이름/이메일/연락처) 수정.
     * 아이디(login_id)/권한/상태는 여기서 바꿀 수 없다 (statusCode는 조회한 기존 값 그대로 유지되어
     * updateUser 매퍼를 그대로 재사용해도 안전하다).
     */
    public UserResponse updateMyInfo(String loginId, MyPageUpdateRequest request) {
        UserDto user = userMapper.selectByLoginId(loginId);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다");
        }

        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        userMapper.updateUser(user);   // statusCode는 위에서 조회한 기존 값 그대로 유지됨

        return UserResponse.from(userMapper.selectByLoginId(loginId));
    }

    /**
     * 셀프 비밀번호 재설정 (로그인 화면의 "비밀번호 재설정" 버튼).
     * 아이디 + 이메일이 모두 일치하면:
     * 임시 비밀번호 생성(영문+숫자) → BCrypt 저장 + 강제 변경 플래그
     * → 기존 Refresh Token 전부 폐기 → 이메일 발송 (개발 단계: 콘솔 출력).
     *
     * 계정 존재 여부를 노출하지 않기 위해 일치하지 않아도 예외 없이 조용히 종료한다.
     * (컨트롤러는 항상 동일한 응답을 반환)
     */
    public void requestPasswordReset(PasswordResetRequest request) {
        UserDto user = userMapper.selectByLoginId(request.loginId());
        if (user == null
                || user.getEmail() == null
                || !user.getEmail().equalsIgnoreCase(request.email().trim())
                || "WITHDRAWN".equals(user.getStatusCode())) {
            return;   // 불일치/탈퇴 계정: 아무 일도 하지 않음
        }

        String tempPassword = TempPasswordUtil.create();
        userMapper.resetPassword(user.getUserId(), passwordEncoder.encode(tempPassword));
        refreshTokenService.revokeAllForUser(user.getUserId());

        mailService.sendTempPassword(user.getEmail(), user.getName(), tempPassword);
    }

    private AuthResponse issueTokens(UserDto user) {
        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.bearer(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpiresInSeconds(),
                UserResponse.from(user)
        );
    }
}
