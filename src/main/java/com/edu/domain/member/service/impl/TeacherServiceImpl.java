package com.edu.domain.member.service.impl;

import com.edu.common.dto.PageResponse;
import com.edu.common.exception.ApiException;
import com.edu.domain.member.dto.teacher.TeacherCreateRequest;
import com.edu.domain.member.dto.teacher.TeacherDto;
import com.edu.domain.member.dto.teacher.TeacherResponse;
import com.edu.domain.member.dto.teacher.TeacherSearchRequest;
import com.edu.domain.member.dto.teacher.TeacherUpdateRequest;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.mapper.TeacherMapper;
import com.edu.domain.member.mapper.UserMapper;
import com.edu.domain.member.service.TeacherService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TeacherServiceImpl implements TeacherService {

    private final TeacherMapper teacherMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public TeacherServiceImpl(TeacherMapper teacherMapper,
                              UserMapper userMapper,
                              PasswordEncoder passwordEncoder) {
        this.teacherMapper = teacherMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * TEA-01 강사 등록.
     * users INSERT + teachers INSERT를 한 트랜잭션으로 묶는다.
     * 중간에 실패하면 둘 다 롤백된다.
     */
    @Override
    @Transactional
    public TeacherResponse createTeacher(TeacherCreateRequest request) {
        TeacherCreateRequest.UserInfo info = request.userInfo();

        // 로그인 ID 중복 체크 → 409
        if (userMapper.selectByLoginId(info.loginId()) != null) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "이미 사용 중인 로그인 ID입니다: " + info.loginId());
        }

        // 1) users INSERT (비밀번호 BCrypt 암호화, 권한 TEACHER 고정)
        UserDto user = UserDto.builder()
                .loginId(info.loginId())
                .password(passwordEncoder.encode(info.password()))
                .name(info.name())
                .email(info.email())
                .phone(info.phone())
                .roleCode("TEACHER")
                .statusCode("ACTIVE")
                .build();
        userMapper.insertUser(user);   // useGeneratedKeys로 userId 채워짐

        // 2) teachers INSERT
        TeacherDto teacher = TeacherDto.builder()
                .userId(user.getUserId())
                .subject(request.subject())
                .hireDate(request.hireDate())
                .build();
        teacherMapper.insertTeacher(teacher);   // teacherId 채워짐

        return TeacherResponse.from(teacherMapper.selectByTeacherId(teacher.getTeacherId()));
    }

    @Override
    public PageResponse<TeacherResponse> getTeachers(TeacherSearchRequest cond) {
        long totalCount = teacherMapper.countTeachers(cond);
        List<TeacherResponse> items = teacherMapper.selectTeachers(cond).stream()
                .map(TeacherResponse::from)
                .toList();
        return PageResponse.of(items, cond.getPage(), cond.getSize(), totalCount);
    }

    /**
     * TEA-03 강사 수정.
     * users(이름/이메일/연락처) + teachers(담당과목/입사일)를 한 트랜잭션으로 수정.
     */
    @Override
    @Transactional
    public TeacherResponse updateTeacher(Long teacherId, TeacherUpdateRequest request) {
        TeacherDto teacher = teacherMapper.selectByTeacherId(teacherId);
        if (teacher == null) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "강사를 찾을 수 없습니다. teacherId=" + teacherId);
        }

        // 1) users 기본정보 수정 (상태는 기존 값 유지)
        UserDto user = userMapper.selectByUserId(teacher.getUserId());
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        userMapper.updateUser(user);

        // 2) teachers 상세 수정
        teacher.setSubject(request.subject());
        teacher.setHireDate(request.hireDate());
        teacherMapper.updateTeacher(teacher);

        return TeacherResponse.from(teacherMapper.selectByTeacherId(teacherId));
    }
}
