package com.edu.domain.classroom.mapper;

import com.edu.domain.classroom.dto.EnrollmentDto;
import com.edu.domain.classroom.dto.EnrollmentSearchRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface EnrollmentMapper {

    /**
     * ENR-01 수강 등록
     * useGeneratedKeys로 enrollmentId가 채워진다
     */
    int insertEnrollment(EnrollmentDto enrollment);

    /**
     * ENR-01 중복 수강 체크 (같은 학생-반의 ACTIVE 수강 존재 여부)
     */
    boolean existsActiveEnrollment(@Param("studentId") Long studentId,
                                   @Param("classId") Long classId);

    /**
     * 수강 PK로 단건 조회 (학생/반 조인)
     */
    EnrollmentDto selectByEnrollmentId(@Param("enrollmentId") Long enrollmentId);

    /**
     * 수강 목록 조회 (반/상태/키워드 + 페이징, 명세서 외 - 수강관리 화면용)
     */
    List<EnrollmentDto> selectEnrollments(@Param("cond") EnrollmentSearchRequest cond);

    /**
     * 수강 목록 전체 건수 (페이징용)
     */
    long countEnrollments(@Param("cond") EnrollmentSearchRequest cond);

    /**
     * ENR-02 수강 해제 (ENDED + 종료일 기록)
     */
    int endEnrollment(@Param("enrollmentId") Long enrollmentId,
                      @Param("endDate") LocalDate endDate);

    /**
     * 반 종료(ACTIVE→CLOSED) 시 해당 반의 수강중(ACTIVE) 수강 전체를 일괄 종료.
     * ClassServiceImpl.updateClass()에서 같은 트랜잭션으로 호출된다.
     *
     * @return 종료 처리된 수강 건수
     */
    int endAllActiveByClassId(@Param("classId") Long classId,
                              @Param("endDate") LocalDate endDate);
}
