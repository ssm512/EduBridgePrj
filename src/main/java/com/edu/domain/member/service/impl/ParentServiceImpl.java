package com.edu.domain.member.service.impl;

import com.edu.common.dto.PageResponse;
import com.edu.common.exception.ApiException;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.dto.parent.ParentCreateRequest;
import com.edu.domain.member.dto.parent.ParentDto;
import com.edu.domain.member.dto.parent.ParentResponse;
import com.edu.domain.member.dto.parent.ParentSearchRequest;
import com.edu.domain.member.dto.parent.ParentUpdateRequest;
import com.edu.domain.member.dto.parent.StudentParentLinkRequest;
import com.edu.domain.member.dto.parent.StudentParentUpdateRequest;
import com.edu.domain.member.mapper.ParentMapper;
import com.edu.domain.member.mapper.StudentMapper;
import com.edu.domain.member.mapper.UserMapper;
import com.edu.domain.member.service.ParentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ParentServiceImpl implements ParentService {

    private final ParentMapper parentMapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public ParentServiceImpl(ParentMapper parentMapper,
                             StudentMapper studentMapper,
                             UserMapper userMapper,
                             PasswordEncoder passwordEncoder) {
        this.parentMapper = parentMapper;
        this.studentMapper = studentMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * PAR-01 학부모 등록.
     * users INSERT + parents INSERT를 한 트랜잭션으로 묶는다.
     */
    @Override
    @Transactional
    public ParentResponse createParent(ParentCreateRequest request) {
        ParentCreateRequest.UserInfo info = request.userInfo();

        // 로그인 ID 중복 체크 → 409
        if (userMapper.selectByLoginId(info.loginId()) != null) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "이미 사용 중인 로그인 ID입니다: " + info.loginId());
        }

        // 1) users INSERT (비밀번호 BCrypt 암호화, 권한 PARENT 고정)
        UserDto user = UserDto.builder()
                .loginId(info.loginId())
                .password(passwordEncoder.encode(info.password()))
                .name(info.name())
                .email(info.email())
                .phone(info.phone())
                .roleCode("PARENT")
                .statusCode("ACTIVE")
                .build();
        userMapper.insertUser(user);   // useGeneratedKeys로 userId 채워짐

        // 2) parents INSERT
        ParentDto parent = ParentDto.builder()
                .userId(user.getUserId())
                .address(request.address())
                .build();
        parentMapper.insertParent(parent);   // parentId 채워짐

        return ParentResponse.from(parentMapper.selectByParentId(parent.getParentId()));
    }

    @Override
    public PageResponse<ParentResponse> getParents(ParentSearchRequest cond) {
        long totalCount = parentMapper.countParents(cond);
        List<ParentResponse> items = parentMapper.selectParents(cond).stream()
                .map(ParentResponse::from)
                .toList();
        return PageResponse.of(items, cond.getPage(), cond.getSize(), totalCount);
    }

    /**
     * 학부모 수정 (명세서 외 추가 API).
     * users(이름/이메일/연락처) + parents(주소)를 한 트랜잭션으로 수정.
     */
    @Override
    @Transactional
    public ParentResponse updateParent(Long parentId, ParentUpdateRequest request) {
        ParentDto parent = parentMapper.selectByParentId(parentId);
        if (parent == null) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "학부모를 찾을 수 없습니다. parentId=" + parentId);
        }

        // 1) users 기본정보 수정 (상태는 기존 값 유지)
        UserDto user = userMapper.selectByUserId(parent.getUserId());
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        userMapper.updateUser(user);

        // 2) parents 상세 수정 (주소)
        parent.setAddress(request.address());
        parentMapper.updateParent(parent);

        return ParentResponse.from(parentMapper.selectByParentId(parentId));
    }

    /**
     * PAR-03 학생-학부모 연결.
     * 학부모는 로그인ID로 지정한다.
     * 학생/학부모 존재 검증(404) + 중복 연결 검증(409) 후 student_parents INSERT.
     */
    @Override
    @Transactional
    public Long linkParent(Long studentId, StudentParentLinkRequest request) {
        if (studentMapper.selectByStudentId(studentId) == null) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "학생을 찾을 수 없습니다. studentId=" + studentId);
        }
        ParentDto parent = parentMapper.selectByLoginId(request.parentLoginId());
        if (parent == null) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "해당 로그인ID의 학부모를 찾을 수 없습니다: " + request.parentLoginId());
        }
        if (parentMapper.existsStudentParent(studentId, parent.getParentId())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 연결된 학생-학부모입니다");
        }

        String primaryYn = request.primaryYn() != null ? request.primaryYn() : "N";
        parentMapper.insertStudentParent(studentId, parent.getParentId(),
                request.relationCode(), primaryYn);

        return parentMapper.selectStudentParentId(studentId, parent.getParentId());
    }

    /**
     * 학생-학부모 연결 내역 수정 (명세서 외 추가 API).
     * 해당 학생의 연결인지 검증 후 관계코드/주보호자 여부를 변경한다.
     */
    @Override
    @Transactional
    public void updateParentLink(Long studentId, Long studentParentId,
                                 StudentParentUpdateRequest request) {
        if (!parentMapper.existsStudentParentById(studentParentId, studentId)) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "연결 내역을 찾을 수 없습니다. studentParentId=" + studentParentId);
        }
        parentMapper.updateStudentParent(studentParentId,
                request.relationCode(), request.primaryYn());
    }
}
