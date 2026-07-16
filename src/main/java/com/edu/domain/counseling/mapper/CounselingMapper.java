package com.edu.domain.counseling.mapper;

import com.edu.domain.counseling.vo.CounselingVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CounselingMapper {

    // 상담 등록
    // 학생/학부모 상담 내용을 counseling_records 테이블에 저장한다.
    int insertCounseling(CounselingVo counselingVo);

    // 상담 목록 조회
    // 선택 조건이 있으면 학생, 학부모, 강사, 공개 범위 기준으로 필터링한다.
    List<Map<String, Object>> selectCounselingList(@Param("studentId") Long studentId,
                                                   @Param("parentId") Long parentId,
                                                   @Param("teacherId") Long teacherId,
                                                   @Param("visibilityCode") String visibilityCode,
                                                   @Param("viewerUserId") Long viewerUserId,
                                                   @Param("viewerRoleCode") String viewerRoleCode);

    // 수강중인 학생 목록 조회
    // 상담 등록 화면에서 학생명 자동완성 목록으로 사용한다.
    List<Map<String, Object>> selectActiveStudentOptions();

    // 학생과 연결된 학부모 목록 조회
    // 학생 선택 시 학부모명을 자동으로 채우기 위해 사용한다.
    List<Map<String, Object>> selectParentOptionsByStudent(@Param("studentId") Long studentId);

    // 수강중인 학생과 연결된 학부모 목록 조회
    // 상담 목록을 학부모명 기준으로 필터링할 때 사용한다.
    List<Map<String, Object>> selectActiveParentOptions();

    // 등록된 강사 목록 조회
    // 상담 담당 강사를 선택할 때 사용한다.
    List<Map<String, Object>> selectTeacherOptions();

    // 로그인 ID로 현재 강사 정보 조회
    // 강사 상담 화면에서 본인 강사명을 자동 입력할 때 사용한다.
    Map<String, Object> selectTeacherByLoginId(@Param("loginId") String loginId);

    // 상담 상세 조회
    // 수정 폼에 기존 상담 내용을 채우기 위해 counselingId 기준으로 조회한다.
    Map<String, Object> selectCounselingDetail(@Param("counselingId") Long counselingId,
                                               @Param("viewerUserId") Long viewerUserId,
                                               @Param("viewerRoleCode") String viewerRoleCode);

    // 상담 수정
    // 상담 대상, 상담일, 상담 유형, 공개 범위, 내용을 수정한다.
    int updateCounseling(@Param("counselingVo") CounselingVo counselingVo,
                         @Param("viewerUserId") Long viewerUserId,
                         @Param("viewerRoleCode") String viewerRoleCode);

    // 상담 삭제
    // 잘못 등록된 상담 기록을 counselingId 기준으로 삭제한다.
    int deleteCounseling(@Param("counselingId") Long counselingId,
                         @Param("viewerUserId") Long viewerUserId,
                         @Param("viewerRoleCode") String viewerRoleCode);
}
