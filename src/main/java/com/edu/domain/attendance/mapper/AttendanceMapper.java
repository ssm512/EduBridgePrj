package com.edu.domain.attendance.mapper;

import com.edu.domain.attendance.vo.AttendanceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
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

    /** 같은 날 같은 반 중복 출석 방지용 카운트 (ATT-13) */
    int countByStudentClassDate(@Param("studentId") Long studentId,
                                @Param("classId") Long classId,
                                @Param("attendanceDate") LocalDate attendanceDate);
}
