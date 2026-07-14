package com.edu.domain.counseling.service;

import com.edu.domain.counseling.vo.CounselingVo;

import java.util.List;
import java.util.Map;

public interface CounselingService {

    CounselingVo createCounseling(CounselingVo counselingVo, String loginId);

    List<Map<String, Object>> getCounselingList(Long studentId,
                                                Long parentId,
                                                Long teacherId,
                                                String visibilityCode);

    List<Map<String, Object>> getActiveStudentOptions();

    List<Map<String, Object>> getParentOptionsByStudent(Long studentId);

    List<Map<String, Object>> getTeacherOptions();

    Map<String, Object> getCounselingDetail(Long counselingId);

    void updateCounseling(Long counselingId, CounselingVo counselingVo);

    void deleteCounseling(Long counselingId);
}
