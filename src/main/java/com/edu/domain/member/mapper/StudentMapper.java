package com.edu.domain.member.mapper;

import com.edu.domain.member.dto.student.GuardianDto;
import com.edu.domain.member.dto.student.StudentDto;
import com.edu.domain.member.dto.student.StudentEnrollmentDto;
import com.edu.domain.member.dto.student.StudentSearchRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentMapper {

    /**
     * STU-01 학생 등록 (users INSERT 후 호출)
     * useGeneratedKeys로 studentId가 채워진다
     */
    int insertStudent(StudentDto student);

    /**
     * STU-02 학생 목록 조회 (users 조인, 반/키워드 필터 + 페이징)
     */
    List<StudentDto> selectStudents(@Param("cond") StudentSearchRequest cond);

    /**
     * STU-02 학생 목록 전체 건수 (페이징용)
     */
    long countStudents(@Param("cond") StudentSearchRequest cond);

    /**
     * 학생 PK로 단건 조회 (users 조인)
     */
    StudentDto selectByStudentId(@Param("studentId") Long studentId);

    /**
     * STU-03 보호자 목록 (student_parents + parents + users 조인)
     */
    List<GuardianDto> selectGuardians(@Param("studentId") Long studentId);

    /**
     * STU-03 수강반 목록 (enrollments + classes 조인)
     */
    List<StudentEnrollmentDto> selectEnrollments(@Param("studentId") Long studentId);

    /**
     * STU-03 접근 검증용: 해당 학생의 보호자(로그인ID) 여부
     */
    boolean existsGuardianLoginId(@Param("studentId") Long studentId,
                                  @Param("loginId") String loginId);

    /**
     * STU-04 학생 상세(students) 수정 - 학번/학교/학년/메모
     */
    int updateStudent(StudentDto student);
}
