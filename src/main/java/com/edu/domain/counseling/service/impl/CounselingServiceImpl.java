package com.edu.domain.counseling.service.impl;

import com.edu.domain.counseling.mapper.CounselingMapper;
import com.edu.domain.counseling.service.CounselingService;
import com.edu.domain.counseling.vo.CounselingVo;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class CounselingServiceImpl implements CounselingService {

    private final CounselingMapper counselingMapper;
    private final UserMapper userMapper;

    public CounselingServiceImpl(CounselingMapper counselingMapper, UserMapper userMapper) {
        this.counselingMapper = counselingMapper;
        this.userMapper = userMapper;
    }

    @Override
    public CounselingVo createCounseling(CounselingVo counselingVo, String loginId) {
        UserDto loginUser = userMapper.selectByLoginId(loginId);
        counselingVo.setCreatedBy(loginUser.getUserId());  // 현재 로그인한 사용자 ID를 등록자 ID로 저장
        counselingMapper.insertCounseling(counselingVo);
        return counselingVo;
    }

    @Override
    public List<Map<String, Object>> getCounselingList(Long studentId,
                                                       Long parentId,
                                                       Long teacherId,
                                                       String visibilityCode,
                                                       String loginId) {
        UserDto loginUser = getLoginUser(loginId);
        return counselingMapper.selectCounselingList(studentId, parentId, teacherId, visibilityCode,
                loginUser.getUserId(), loginUser.getRoleCode());
    }

    @Override
    public List<Map<String, Object>> getActiveStudentOptions() {
        return counselingMapper.selectActiveStudentOptions();
    }

    @Override
    public List<Map<String, Object>> getParentOptionsByStudent(Long studentId) {
        return counselingMapper.selectParentOptionsByStudent(studentId);
    }

    @Override
    public List<Map<String, Object>> getActiveParentOptions() {
        return counselingMapper.selectActiveParentOptions();
    }

    @Override
    public List<Map<String, Object>> getTeacherOptions() {
        return counselingMapper.selectTeacherOptions();
    }

    @Override
    public Map<String, Object> getCounselingDetail(Long counselingId, String loginId) {
        UserDto loginUser = getLoginUser(loginId);
        Map<String, Object> counseling = counselingMapper.selectCounselingDetail(
                counselingId, loginUser.getUserId(), loginUser.getRoleCode());
        if (counseling == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "조회할 수 없는 상담 기록입니다.");
        }
        return counseling;
    }

    @Override
    public void updateCounseling(Long counselingId, CounselingVo counselingVo, String loginId) {
        UserDto loginUser = getLoginUser(loginId);
        counselingVo.setCounselingId(counselingId);
        int updated = counselingMapper.updateCounseling(counselingVo, loginUser.getUserId(), loginUser.getRoleCode());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정할 수 없는 상담 기록입니다.");
        }
    }

    @Override
    public void deleteCounseling(Long counselingId, String loginId) {
        UserDto loginUser = getLoginUser(loginId);
        int deleted = counselingMapper.deleteCounseling(counselingId, loginUser.getUserId(), loginUser.getRoleCode());
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제할 수 없는 상담 기록입니다.");
        }
    }

    // 상담 공개 범위 판단에 필요한 현재 로그인 사용자 정보를 조회한다.
    private UserDto getLoginUser(String loginId) {
        UserDto loginUser = userMapper.selectByLoginId(loginId);
        if (loginUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보가 없습니다.");
        }
        return loginUser;
    }
}
