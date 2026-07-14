package com.edu.domain.grade.mapper;

import com.edu.domain.grade.vo.ExamVo;
import com.edu.domain.grade.vo.GradeVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface GradeMapper {

    // 시험 등록
    // exams 테이블에 시험명, 반, 과목, 시험일, 만점 등을 저장한다.
    int insertExam(ExamVo examVo);

    // 시험 목록 조회
    // classId가 있으면 특정 반의 시험만 조회하고,
    // subject가 있으면 특정 과목의 시험만 조회한다.
    List<Map<String, Object>> selectExamList(@Param("classId") Long classId,
                                             @Param("subject") String subject);

    // 시험 정보 수정
    // examId 기준으로 exams 테이블의 시험 정보를 수정한다.
    int updateExam(ExamVo examVo);

    // 시험 삭제
    // examId 기준으로 exams 테이블의 시험 정보를 삭제한다.
    // grades 테이블에 연결된 성적이 있으면 FK 제약 때문에 삭제가 실패할 수 있다.
    int deleteExam(@Param("examId") Long examId);

    // 성적 등록
    // grades 테이블에 학생별 시험 점수, 석차, 비고를 저장한다.
    int insertGrade(GradeVo gradeVo);

    // 성적 수정
    // gradeId 기준으로 점수, 석차, 비고를 수정한다.
    int updateGrade(GradeVo gradeVo);

    // 성적 삭제
    // gradeId 기준으로 grades 테이블의 성적 정보를 삭제한다.
    int deleteGrade(@Param("gradeId") Long gradeId);

    // 특정 시험의 성적 목록 조회
    // 시험을 선택했을 때 해당 시험에 입력된 학생별 성적을 조회한다.
    List<Map<String, Object>> selectGradesByExam(@Param("examId") Long examId);

    // 학생별 성적 추이 조회
    // studentId 기준으로 시험일 순서대로 점수 변화를 조회한다.
    // subject가 있으면 해당 과목만 필터링한다.
    List<Map<String, Object>> selectGradeTrend(@Param("studentId") Long studentId,
                                               @Param("subject") String subject);

    // 반별 성적 추이 조회
    // classId 기준으로 시험별 평균 점수 변화를 조회한다.
    List<Map<String, Object>> selectClassGradeTrend(@Param("classId") Long classId,
                                                    @Param("subject") String subject);

    // 반 평균 조회
    // classId가 있으면 특정 반만 조회하고, subject가 있으면 해당 과목 시험만 평균에 포함한다.
    List<Map<String, Object>> selectClassAverageStats(@Param("classId") Long classId,
                                                      @Param("subject") String subject);

    // 과목별 성적 조회
    // subject 기준으로 시험, 반, 학생별 성적 목록을 조회한다.
    List<Map<String, Object>> selectGradesBySubject(@Param("subject") String subject,
                                                    @Param("classId") Long classId);

    // 선택한 시험의 수강 학생 목록 + 기존 성적 조회
    // 성적 입력 화면에서 학생명 기준으로 점수를 입력하기 위해 사용
    List<Map<String, Object>> selectStudentGradeRowsByExam(@Param("examId") Long examId);
}
