package com.edu.domain.ai.mapper;

import com.edu.domain.ai.vo.AiUsageLogVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AiMapper {

    // AI 요청/응답 이력을 ai_usage_logs 테이블에 저장한다.
    int insertAiUsageLog(AiUsageLogVo aiUsageLogVo);

    // 관리자 화면에서 AI 사용 로그를 최근순으로 조회한다.
    List<Map<String, Object>> selectAiUsageLogs(@Param("featureCode") String featureCode,
                                                @Param("successYn") String successYn,
                                                @Param("fromDate") String fromDate,
                                                @Param("toDate") String toDate,
                                                @Param("limit") int limit);

    // 월간 리포트 생성에 사용할 학생 기본 정보를 조회한다.
    Map<String, Object> selectStudentReportBase(@Param("studentId") Long studentId);

    // 월간 리포트 생성에 사용할 월별 출석 요약을 조회한다.
    List<Map<String, Object>> selectMonthlyAttendanceSummary(@Param("studentId") Long studentId,
                                                             @Param("targetMonth") String targetMonth);

    // 월간 리포트 생성에 사용할 월별 성적 목록을 조회한다.
    List<Map<String, Object>> selectMonthlyGradeSummary(@Param("studentId") Long studentId,
                                                        @Param("targetMonth") String targetMonth);

    // 월간 리포트 생성에 사용할 월별 회비 목록을 조회한다.
    List<Map<String, Object>> selectMonthlyFeeSummary(@Param("studentId") Long studentId,
                                                      @Param("targetMonth") String targetMonth);

    // 월간 리포트 생성에 사용할 월별 상담 목록을 조회한다.
    List<Map<String, Object>> selectMonthlyCounselingSummary(@Param("studentId") Long studentId,
                                                             @Param("targetMonth") String targetMonth);

    // 상담 ID로 요약 대상 상담 내용을 조회한다.
    Map<String, Object> selectCounselingForSummary(@Param("counselingId") Long counselingId);

    // 상담 요약 화면에서 검색 카테고리와 키워드로 상담 기록 선택 목록을 조회한다.
    List<Map<String, Object>> selectCounselingOptionsForSummary(@Param("category") String category,
                                                                @Param("keyword") String keyword);

    // 상담 요약 검색 입력창의 자동완성 후보를 조회한다.
    List<Map<String, Object>> selectCounselingSearchSuggestions(@Param("category") String category,
                                                               @Param("keyword") String keyword);

    // AI 리포트 화면의 학생 선택 목록을 조회한다.
    List<Map<String, Object>> selectActiveStudentOptions();

    // 성적 분석 화면에서 학생명, 학생번호, 반 기준으로 학생 후보를 조회한다.
    List<Map<String, Object>> selectGradeStudentOptions(@Param("category") String category,
                                                        @Param("keyword") String keyword);

    // 성적 분석 화면에서 선택 학생이 응시한 과목 목록을 조회한다.
    List<Map<String, Object>> selectGradeSubjectOptions(@Param("studentId") Long studentId);

    // 성적 분석 화면에서 반 기준 검색에 사용할 반 후보를 조회한다.
    List<Map<String, Object>> selectGradeClassOptions(@Param("keyword") String keyword);

    // 성적 분석에 사용할 학생의 시험별 성적 이력을 조회한다.
    List<Map<String, Object>> selectStudentGradeAnalysisRows(@Param("studentId") Long studentId,
                                                             @Param("subject") String subject);
}
