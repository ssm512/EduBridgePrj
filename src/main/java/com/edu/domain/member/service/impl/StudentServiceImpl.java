package com.edu.domain.member.service.impl;

import com.edu.common.dto.PageResponse;
import com.edu.common.exception.ApiException;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.dto.student.StudentCreateRequest;
import com.edu.domain.member.dto.student.StudentDetailResponse;
import com.edu.domain.member.dto.student.StudentDto;
import com.edu.domain.member.dto.student.StudentResponse;
import com.edu.domain.member.dto.student.StudentSearchRequest;
import com.edu.domain.member.dto.student.StudentUpdateRequest;
import com.edu.domain.member.mapper.StudentMapper;
import com.edu.domain.member.mapper.UserMapper;
import com.edu.domain.member.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public StudentServiceImpl(StudentMapper studentMapper,
                              UserMapper userMapper,
                              PasswordEncoder passwordEncoder) {
        this.studentMapper = studentMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * STU-01 학생 등록.
     * users INSERT + students INSERT를 한 트랜잭션으로 묶는다.
     */
    @Override
    @Transactional
    public StudentResponse createStudent(StudentCreateRequest request) {
        StudentCreateRequest.UserInfo info = request.userInfo();

        // 로그인 ID 중복 체크 → 409
        if (userMapper.selectByLoginId(info.loginId()) != null) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "이미 사용 중인 로그인 ID입니다: " + info.loginId());
        }

        // 1) users INSERT (비밀번호 BCrypt 암호화, 권한 STUDENT 고정)
        UserDto user = UserDto.builder()
                .loginId(info.loginId())
                .password(passwordEncoder.encode(info.password()))
                .name(info.name())
                .email(info.email())
                .phone(info.phone())
                .roleCode("STUDENT")
                .statusCode("ACTIVE")
                .build();
        userMapper.insertUser(user);   // useGeneratedKeys로 userId 채워짐

        // 2) students INSERT
        StudentDto student = StudentDto.builder()
                .userId(user.getUserId())
                .studentNo(request.studentNo())
                .birthDate(request.birthDate())
                .schoolName(request.schoolName())
                .gradeLevel(request.gradeLevel())
                .build();
        studentMapper.insertStudent(student);   // studentId 채워짐

        return StudentResponse.from(studentMapper.selectByStudentId(student.getStudentId()));
    }

    @Override
    public PageResponse<StudentResponse> getStudents(StudentSearchRequest cond) {
        long totalCount = studentMapper.countStudents(cond);
        List<StudentResponse> items = studentMapper.selectStudents(cond).stream()
                .map(StudentResponse::from)
                .toList();
        return PageResponse.of(items, cond.getPage(), cond.getSize(), totalCount);
    }

    /**
     * STU-03 학생 상세 조회.
     * ADMIN/TEACHER: 전체 조회 가능
     * STUDENT: 본인 것만 / PARENT: 자기 자녀 것만 → 아니면 403
     */
    @Override
    public StudentDetailResponse getStudent(Long studentId, Authentication authentication) {
        StudentDto student = findStudentOrThrow(studentId);
        checkAccess(student, authentication);

        return new StudentDetailResponse(
                StudentResponse.from(student),
                studentMapper.selectGuardians(studentId),
                studentMapper.selectEnrollments(studentId)
        );
    }

    /**
     * STU-04 학생 수정 (students 테이블만: 학번/학교/학년/메모)
     */
    @Override
    @Transactional
    public StudentResponse updateStudent(Long studentId, StudentUpdateRequest request) {
        StudentDto student = findStudentOrThrow(studentId);

        student.setStudentNo(request.studentNo());
        student.setSchoolName(request.schoolName());
        student.setGradeLevel(request.gradeLevel());
        student.setMemo(request.memo());
        studentMapper.updateStudent(student);

        return StudentResponse.from(studentMapper.selectByStudentId(studentId));
    }

    /** 학생 존재 확인, 없으면 404 */
    private StudentDto findStudentOrThrow(Long studentId) {
        StudentDto student = studentMapper.selectByStudentId(studentId);
        if (student == null) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "학생을 찾을 수 없습니다. studentId=" + studentId);
        }
        return student;
    }

    /**
     * STU-03 접근 검증.
     * JWT roles 클레임(ROLE_XXX) 기준으로 판단한다.
     */
    private void checkAccess(StudentDto student, Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // ADMIN / TEACHER는 전체 열람 가능
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_TEACHER")) {
            return;
        }

        String loginId = authentication.getName();

        // STUDENT: 본인 정보만
        if (roles.contains("ROLE_STUDENT")) {
            if (loginId.equals(student.getLoginId())) return;
            throw new ApiException(HttpStatus.FORBIDDEN, "본인의 정보만 조회할 수 있습니다");
        }

        // PARENT: 자기 자녀만
        if (roles.contains("ROLE_PARENT")) {
            if (studentMapper.existsGuardianLoginId(student.getStudentId(), loginId)) return;
            throw new ApiException(HttpStatus.FORBIDDEN, "자녀의 정보만 조회할 수 있습니다");
        }

        throw new ApiException(HttpStatus.FORBIDDEN, "조회 권한이 없습니다");
    }
}
