package com.edu.domain.counseling.service;

import com.edu.domain.counseling.vo.CounselingVo;

import java.util.List;
import java.util.Map;

public interface CounselingService {

    CounselingVo createCounseling(CounselingVo counselingVo, String loginId);

    List<Map<String, Object>> getCounselingList(Long studentId,
                                                Long parentId,
                                                Long teacherId,
                                                String visibilityCode,
                                                String fromDate,
                                                String toDate,
                                                String loginId);

    List<Map<String, Object>> getParentChildren(String loginId);

    List<Map<String, Object>> getSharedCounselingListForParent(String loginId, Long studentId);

    List<Map<String, Object>> getActiveStudentOptions();

    List<Map<String, Object>> getParentOptionsByStudent(Long studentId);

    List<Map<String, Object>> getActiveParentOptions();

    List<Map<String, Object>> getTeacherOptions();

    Map<String, Object> getLoginTeacherInfo(String loginId);

    Map<String, Object> getCounselingDetail(Long counselingId, String loginId);

    void updateCounseling(Long counselingId, CounselingVo counselingVo, String loginId);

    void deleteCounseling(Long counselingId, String loginId);
}
