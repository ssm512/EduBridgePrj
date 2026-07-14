package com.edu.domain.counseling.service.impl;

import com.edu.domain.counseling.mapper.CounselingMapper;
import com.edu.domain.counseling.service.CounselingService;
import com.edu.domain.counseling.vo.CounselingVo;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.mapper.UserMapper;
import org.springframework.stereotype.Service;

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
                                                       String visibilityCode) {
        return counselingMapper.selectCounselingList(studentId, parentId, teacherId, visibilityCode);
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
    public List<Map<String, Object>> getTeacherOptions() {
        return counselingMapper.selectTeacherOptions();
    }

    @Override
    public Map<String, Object> getCounselingDetail(Long counselingId) {
        return counselingMapper.selectCounselingDetail(counselingId);
    }

    @Override
    public void updateCounseling(Long counselingId, CounselingVo counselingVo) {
        counselingVo.setCounselingId(counselingId);
        counselingMapper.updateCounseling(counselingVo);
    }

    @Override
    public void deleteCounseling(Long counselingId) {
        counselingMapper.deleteCounseling(counselingId);
    }
}
