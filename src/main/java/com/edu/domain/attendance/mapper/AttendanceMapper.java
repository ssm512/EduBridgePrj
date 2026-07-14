package com.edu.domain.attendance.mapper;

import com.edu.domain.attendance.vo.AttendanceRecord;
import com.edu.domain.attendance.vo.BeaconView;
import com.edu.domain.attendance.vo.ClassScheduleView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 출석기록 매퍼. XML: resources/mapper/attendance/AttendanceMapper.xml
 * (틀만 잡아둔 상태 — 쿼리 세부는 담당자가 채운다)
 */
@Mapper
public interface AttendanceMapper {

    /** 출석기록 등록 (useGeneratedKeys로 attendanceId 채움) */
    int insert(AttendanceRecord record);

    /** PK 단건 조회 */
    AttendanceRecord findById(@Param("attendanceId") Long attendanceId);

    /** 학생/반/기간 조건 목록 조회 (ATT-03) */
    List<AttendanceRecord> findList(@Param("studentId") Long studentId,
                                    @Param("classId") Long classId,
                                    @Param("fromDate") LocalDate fromDate,
                                    @Param("toDate") LocalDate toDate);

    /** 상태/사유 수정 (ATT-04) */
    int updateStatus(@Param("attendanceId") Long attendanceId,
                     @Param("statusCode") String statusCode,
                     @Param("failureReason") String failureReason);

    /** 출석기록 삭제 (ATT-04 삭제) */
    int deleteById(@Param("attendanceId") Long attendanceId);

    /** 상태/사유/입실/퇴실 시각 수정 (null인 항목은 변경 안 함) */
    int updateDetail(@Param("attendanceId") Long attendanceId,
                     @Param("statusCode") String statusCode,
                     @Param("failureReason") String failureReason,
                     @Param("checkedAt") LocalDateTime checkedAt,
                     @Param("checkOutAt") LocalDateTime checkOutAt);

    /** 같은 날 같은 반 중복 출석 방지용 카운트 (ATT-13) */
    int countByStudentClassDate(@Param("studentId") Long studentId,
                                @Param("classId") Long classId,
                                @Param("attendanceDate") LocalDate attendanceDate);

    /** 퇴실 처리를 위해 오늘 등원 기록 단건 조회 */
    AttendanceRecord findByStudentClassDate(@Param("studentId") Long studentId,
                                            @Param("classId") Long classId,
                                            @Param("attendanceDate") LocalDate attendanceDate);

    /** 퇴실 시각 + 상태 갱신 (조퇴 처리) */
    int updateCheckOut(@Param("attendanceId") Long attendanceId,
                       @Param("checkOutAt") LocalDateTime checkOutAt,
                       @Param("statusCode") String statusCode);

    /** 반 시간표(시작/종료) 조회 — 지각/조퇴 자동 판정용 (classes 읽기 전용) */
    ClassScheduleView findClassSchedule(@Param("classId") Long classId);

    /** 반의 활성 등록 비콘 조회 — 비콘 검증용 (없으면 null → 검증 생략) */
    BeaconView findActiveBeaconByClass(@Param("classId") Long classId);

    /** 로그인 ID로 student_id 조회 (본인 확인용, 없으면 null) */
    Long findStudentIdByLoginId(@Param("loginId") String loginId);

    /** 로그인한 학생이 수강 중인 반 목록 (출석 대상 선택용) */
    java.util.List<com.edu.domain.attendance.dto.response.ClassOptionResponse>
            findMyClasses(@Param("loginId") String loginId);

    /** 결석 대상: 해당 반의 ACTIVE 수강생 중 그 날짜에 출석 기록이 없는 student_id 목록 */
    java.util.List<Long> findAbsentCandidates(@Param("classId") Long classId,
                                              @Param("date") java.time.LocalDate date);
}
