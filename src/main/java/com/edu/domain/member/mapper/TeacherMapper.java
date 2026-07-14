package com.edu.domain.member.mapper;

import com.edu.domain.member.dto.teacher.TeacherDto;
import com.edu.domain.member.dto.teacher.TeacherSearchRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeacherMapper {

    /**
     * TEA-01 강사 등록 (users INSERT 후 호출)
     * useGeneratedKeys로 teacherId가 채워진다
     */
    int insertTeacher(TeacherDto teacher);

    /**
     * TEA-02 강사 목록 조회 (users 조인, 키워드 + 페이징)
     */
    List<TeacherDto> selectTeachers(@Param("cond") TeacherSearchRequest cond);

    /**
     * TEA-02 강사 목록 전체 건수 (페이징용)
     */
    long countTeachers(@Param("cond") TeacherSearchRequest cond);

    /**
     * 강사 PK로 단건 조회 (users 조인)
     */
    TeacherDto selectByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * TEA-03 강사 상세(teachers) 수정 - 담당과목/입사일
     * users 쪽(이름 등)은 UserMapper.updateUser로 수정한다
     */
    int updateTeacher(TeacherDto teacher);
}
