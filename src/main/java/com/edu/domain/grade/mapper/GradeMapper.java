package com.edu.domain.grade.mapper;

import com.edu.domain.grade.vo.ExamVo;
import com.edu.domain.grade.vo.GradeVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
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

    // 성적 ID로 연결된 시험 ID 조회
    // 성적 삭제 후 해당 시험의 석차를 다시 계산하기 위해 사용한다.
    Long selectExamIdByGradeId(@Param("gradeId") Long gradeId);

    // 시험 ID로 시험 만점 조회
    // 학생 점수가 시험 만점을 넘지 않도록 서버에서 한 번 더 검증할 때 사용한다.
    BigDecimal selectExamTotalScore(@Param("examId") Long examId);

    // 특정 시험의 석차 자동 재계산
    // 점수 높은 순으로 RANK를 다시 매겨 동점자는 같은 석차로 저장한다.
    int updateGradeRanksByExam(@Param("examId") Long examId);

    // 특정 시험의 성적 목록 조회
    // 시험을 선택했을 때 해당 시험에 입력된 학생별 성적을 조회한다.
    List<Map<String, Object>> selectGradesByExam(@Param("examId") Long examId);

    // 학생별 성적 추이 조회
    // studentId 기준으로 시험일 순서대로 점수 변화를 조회한다.
    // subject가 있으면 해당 과목만 필터링한다.
    List<Map<String, Object>> selectGradeTrend(@Param("studentId") Long studentId,
                                               @Param("subject") String subject);

    // 로그인 ID로 학생 ID 조회
    // 학생 본인의 성적 조회 API에서 요청 사용자의 student_id를 찾기 위해 사용한다.
    Long selectStudentIdByLoginId(@Param("loginId") String loginId);

    // 학부모가 연결된 자녀인지 확인
    // 학부모 성적 조회에서 다른 학생 성적을 볼 수 없게 검증한다.
    boolean existsParentChildByLoginId(@Param("loginId") String loginId,
                                       @Param("studentId") Long studentId);

    // 학부모에게 연결된 자녀 목록 조회
    // parentPage 자녀 성적 화면의 학생 선택 목록으로 사용한다.
    List<Map<String, Object>> selectChildrenByParentLoginId(@Param("loginId") String loginId);

    // 학생별 성적 조회 목록
    // 학생/학부모 조회 화면에서 시험명, 반, 과목, 점수, 석차를 함께 보여준다.
    List<Map<String, Object>> selectStudentGradeList(@Param("studentId") Long studentId,
                                                     @Param("subject") String subject);

    // 반별 성적 추이 조회
    // classId 기준으로 시험별 평균 점수 변화를 조회한다.
    List<Map<String, Object>> selectClassGradeTrend(@Param("classId") Long classId,
                                                    @Param("subject") String subject);

    // 성적 추이 과목 선택 목록 조회
    // 선택한 학생 또는 반에 실제 성적이 있는 시험 과목만 조회한다.
    List<Map<String, Object>> selectTrendSubjectOptions(@Param("studentId") Long studentId,
                                                        @Param("classId") Long classId);

    // 등록된 시험 과목 선택 목록 조회
    // 과목별 성적 조회와 반 평균 조회에서 과목을 직접 입력하지 않고 선택하게 한다.
    List<Map<String, Object>> selectExamSubjectOptions();

    // 반 평균 조회
    // classId가 있으면 특정 반만 조회하고, subject가 있으면 해당 과목 시험만 평균에 포함한다.
    List<Map<String, Object>> selectClassAverageStats(@Param("classId") Long classId,
                                                      @Param("subject") String subject);

    // 과목별 성적 조회
    // subject 기준으로 시험, 반, 학생별 성적 목록을 조회한다.
    List<Map<String, Object>> selectGradesBySubject(@Param("subject") String subject,
                                                    @Param("classId") Long classId,
                                                    @Param("studentId") Long studentId);

    // 선택한 시험의 수강 학생 목록 + 기존 성적 조회
    // 성적 입력 화면에서 학생명 기준으로 점수를 입력하기 위해 사용
    List<Map<String, Object>> selectStudentGradeRowsByExam(@Param("examId") Long examId);

    // 등록된 반 선택 목록 조회
    // 화면에서는 반 이름으로 선택하고, 내부 저장 시 class_id만 숨겨서 사용한다.
    List<Map<String, Object>> selectClassOptions();

    // 수강중인 학생 목록 조회
    // 학생별 성적 추이 조회에서 학생명 자동완성 목록으로 사용한다.
    List<Map<String, Object>> selectActiveStudentOptions();
}
