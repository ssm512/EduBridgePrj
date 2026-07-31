package com.edu.domain.classroom.mapper;

import com.edu.domain.classroom.dto.ClassDto;
import com.edu.domain.classroom.dto.ClassSearchRequest;
import com.edu.domain.classroom.dto.ClassStudentDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ClassMapper {

    /**
     * CLS-01 반 등록
     * useGeneratedKeys로 classId가 채워진다
     */
    int insertClass(ClassDto clazz);

    /**
     * CLS-02 반 목록 조회 (담당강사 조인 + 수강인원 집계, 상태/키워드 + 페이징)
     */
    List<ClassDto> selectClasses(@Param("cond") ClassSearchRequest cond);

    /**
     * CLS-02 반 목록 전체 건수 (페이징용)
     */
    long countClasses(@Param("cond") ClassSearchRequest cond);

    /**
     * 반 PK로 단건 조회
     */
    ClassDto selectByClassId(@Param("classId") Long classId);

    /**
     * CLS-03 반 상세의 수강 학생 목록 (enrollments + students + users 조인)
     */
    List<ClassStudentDto> selectClassStudents(@Param("classId") Long classId);

    /**
     * CLS-04 반 수정
     */
    int updateClass(ClassDto clazz);

    /**
     * [추가 2026-07-20] 로그인 강사의 담당반 목록 (teacher/classes 화면)
     * 상태/키워드 + 페이징, classes.teacher_id를 로그인 loginId로 한정
     */
    List<ClassDto> selectMyClasses(@Param("loginId") String loginId, @Param("cond") ClassSearchRequest cond);

    /**
     * [추가 2026-07-20] 로그인 강사의 담당반 전체 건수 (페이징용)
     */
    long countMyClasses(@Param("loginId") String loginId, @Param("cond") ClassSearchRequest cond);

    /**
     * [추가 2026-07-20] 해당 반이 로그인 강사의 담당반인지 (0이면 담당 아님 → 403, 상세조회 소유권 검증용)
     */
    int existsTeacherClass(@Param("loginId") String loginId, @Param("classId") Long classId);
}
