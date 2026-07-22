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

    /**
     * 자동 결석 전용 등록 (ATT-08). 이미 같은 날 기록이 있으면(동시 체크인/중복 수강행) 조용히 무시한다.
     * ON CONFLICT DO NOTHING 이라 유니크 위반 예외를 던지지 않으며, 반환값 = 실제 삽입 행수(1=삽입, 0=이미 있음).
     */
    int insertAbsentIfAbsent(AttendanceRecord record);

    /** PK 단건 조회 */
    AttendanceRecord findById(@Param("attendanceId") Long attendanceId);

    /** 학생/반/기간/이름 조건 전체 목록 조회 (통계 집계 등, 페이징 없음). keyword = 학생 이름 부분 일치 */
    List<AttendanceRecord> findList(@Param("studentId") Long studentId,
                                    @Param("classId") Long classId,
                                    @Param("fromDate") LocalDate fromDate,
                                    @Param("toDate") LocalDate toDate,
                                    @Param("keyword") String keyword);

    /** 이력 페이지 조회 (ATT-03, LIMIT/OFFSET) */
    List<AttendanceRecord> findPage(@Param("studentId") Long studentId,
                                    @Param("classId") Long classId,
                                    @Param("fromDate") LocalDate fromDate,
                                    @Param("toDate") LocalDate toDate,
                                    @Param("keyword") String keyword,
                                    @Param("size") int size,
                                    @Param("offset") int offset);

    /** 위 목록의 전체 건수 (페이징용) */
    long countList(@Param("studentId") Long studentId,
                   @Param("classId") Long classId,
                   @Param("fromDate") LocalDate fromDate,
                   @Param("toDate") LocalDate toDate,
                   @Param("keyword") String keyword);

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

    /** student_id로 학생 이름 조회 (등원/퇴실 알림 메시지용) */
    String getStudentName(@Param("studentId") Long studentId);

    /** 로그인한 학부모의 자녀 목록 (student_parents 연결) */
    java.util.List<com.edu.domain.attendance.dto.response.ChildOptionResponse>
            findMyChildren(@Param("loginId") String loginId);

    /** 해당 student가 로그인 학부모의 자녀인지 확인 (0이면 자녀 아님 → 403 처리) */
    int countChildOfParent(@Param("loginId") String loginId, @Param("studentId") Long studentId);

    // ===== 강사 담당반 스코프 (classes.teacher_id 기준, 읽기전용) =====

    /** 로그인 강사가 담당하는 반 목록 */
    java.util.List<com.edu.domain.attendance.dto.response.ClassOptionResponse>
            findMyTeacherClasses(@Param("loginId") String loginId);

    /** 해당 반이 로그인 강사의 담당반인지 확인 (0이면 담당 아님 → 403 처리, 쓰기 경로용) */
    int countTeacherClass(@Param("loginId") String loginId, @Param("classId") Long classId);

    /**
     * 결석 자동화용: 오늘 요일에 수업이 있고 종료시각이 cutoff 이전인 ACTIVE 반 목록.
     * days_of_week가 NULL이면 매일 수업으로 간주.
     */
    List<Long> findEndedClasses(@Param("dayCode") String dayCode,
                                @Param("cutoff") java.time.LocalTime cutoff);

    /** 가장 최근 출석기록 일자 (없으면 null) — 재가동 백필 시작점 판단용 */
    java.time.LocalDate findLatestAttendanceDate();

    /** 강사 담당반으로 한정한 출석 이력 전체 (통계 집계용) */
    List<AttendanceRecord> findTeacherList(@Param("loginId") String loginId,
                                           @Param("classId") Long classId,
                                           @Param("fromDate") LocalDate fromDate,
                                           @Param("toDate") LocalDate toDate,
                                           @Param("keyword") String keyword);

    /** 강사 담당반으로 한정한 출석 이력 페이지 */
    List<AttendanceRecord> findTeacherPage(@Param("loginId") String loginId,
                                           @Param("classId") Long classId,
                                           @Param("fromDate") LocalDate fromDate,
                                           @Param("toDate") LocalDate toDate,
                                           @Param("keyword") String keyword,
                                           @Param("size") int size,
                                           @Param("offset") int offset);

    /** 강사 담당반으로 한정한 출석 이력 건수 */
    long countTeacherList(@Param("loginId") String loginId,
                          @Param("classId") Long classId,
                          @Param("fromDate") LocalDate fromDate,
                          @Param("toDate") LocalDate toDate,
                          @Param("keyword") String keyword);

    /** 로그인한 학생이 수강 중인 반 목록 (출석 대상 선택용) */
    java.util.List<com.edu.domain.attendance.dto.response.ClassOptionResponse>
            findMyClasses(@Param("loginId") String loginId);

    /** 앱 오늘 수업: 오늘 요일에 수업 있는 본인 반 + 그날 출석기록(LEFT JOIN) */
    java.util.List<com.edu.domain.attendance.dto.response.TodayClassResponse>
            findMyTodayClasses(@Param("loginId") String loginId,
                               @Param("dayCode") String dayCode,
                               @Param("date") java.time.LocalDate date);

    /** 결석 대상: 해당 반의 ACTIVE 수강생 중 그 날짜에 출석 기록이 없는 student_id 목록 */
    java.util.List<Long> findAbsentCandidates(@Param("classId") Long classId,
                                              @Param("date") java.time.LocalDate date);

    /**
     * 강사 앱 "오늘 우리 반 현황" 로스터: 해당 반 ACTIVE 수강생 전원 + 그 날짜 출석기록(LEFT JOIN).
     * 출석기록이 없으면 statusCode=null(미출석)로 내려간다.
     */
    java.util.List<com.edu.domain.attendance.dto.response.ClassRosterEntryResponse>
            findClassRosterForDate(@Param("classId") Long classId,
                                   @Param("date") java.time.LocalDate date);

    String getClassName(@Param("classId") Long classId);

    Long getMyUserId(@Param("loginId") String loginId);

    /**
     * 활동로그 저장 (activity_logs). 출석 검증 실패 사유 기록 등에 사용.
     * actionType 예: ATTENDANCE_FAIL (나중에 활동로그 화면에서 분류/탭 조회)
     */
    int insertActivityLog(@Param("userId") Long userId,
                          @Param("actionType") String actionType,
                          @Param("targetTable") String targetTable,
                          @Param("targetId") Long targetId,
                          @Param("description") String description);
}
