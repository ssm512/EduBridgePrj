package com.edu.domain.member.mapper;

import com.edu.domain.member.dto.parent.ParentDto;
import com.edu.domain.member.dto.parent.ParentSearchRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ParentMapper {

    /**
     * PAR-01 학부모 등록 (users INSERT 후 호출)
     * useGeneratedKeys로 parentId가 채워진다
     */
    int insertParent(ParentDto parent);

    /**
     * PAR-02 학부모 목록 조회 (users 조인, 키워드 + 페이징)
     */
    List<ParentDto> selectParents(@Param("cond") ParentSearchRequest cond);

    /**
     * PAR-02 학부모 목록 전체 건수 (페이징용)
     */
    long countParents(@Param("cond") ParentSearchRequest cond);

    /**
     * 학부모 PK로 단건 조회 (users 조인)
     */
    ParentDto selectByParentId(@Param("parentId") Long parentId);

    /**
     * 로그인ID로 학부모 조회 (PAR-03 연결 시 사용)
     */
    ParentDto selectByLoginId(@Param("loginId") String loginId);

    /**
     * 학부모 상세(parents) 수정 - 주소
     * users 쪽(이름 등)은 UserMapper.updateUser로 수정한다
     */
    int updateParent(ParentDto parent);

    /**
     * PAR-03 학생-학부모 연결 (student_parents INSERT)
     * useGeneratedKeys 없이 반환값은 insert 건수, PK는 selectStudentParentId로 조회
     */
    int insertStudentParent(@Param("studentId") Long studentId,
                            @Param("parentId") Long parentId,
                            @Param("relationCode") String relationCode,
                            @Param("primaryYn") String primaryYn);

    /**
     * PAR-03 중복 연결 체크 (uk_sp_student_parent)
     */
    boolean existsStudentParent(@Param("studentId") Long studentId,
                                @Param("parentId") Long parentId);

    /**
     * PAR-03 연결 PK 조회 (응답용 studentParentId)
     */
    Long selectStudentParentId(@Param("studentId") Long studentId,
                               @Param("parentId") Long parentId);

    /**
     * 연결 내역 존재 확인 (해당 학생의 연결인지 검증 포함)
     */
    boolean existsStudentParentById(@Param("studentParentId") Long studentParentId,
                                    @Param("studentId") Long studentId);

    /**
     * 연결 내역 수정 - 관계코드/주보호자 여부 (명세서 외 추가 API)
     */
    int updateStudentParent(@Param("studentParentId") Long studentParentId,
                            @Param("relationCode") String relationCode,
                            @Param("primaryYn") String primaryYn);
}
