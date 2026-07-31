package com.edu.domain.grade.aigrading.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ai_grading_runs 테이블 (MyBatis 매핑용) - AIG-06 AI 채점 실행별 상태/모델/오류 이력.
 * status_code는 코드정의 시트에 그룹이 없어(V10 SQL 주석 참고) CHECK 제약 없이 VARCHAR로만 둠 - 팀 확인 전까지
 * 이 클래스에서 'RUNNING'/'SUCCESS'/'FAILED' 세 값만 사용한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGradingRunVo {

    /** AI채점실행 PK */
    private Long gradingRunId;

    /** exam_submissions.submission_id FK */
    private Long submissionId;

    /** ai_usage_logs.ai_log_id FK - Gemini 호출 로그 연결 (완료 후 채워짐) */
    private Long aiLogId;

    /** 사용 모델명 (예: gemini-2.5-flash) */
    private String modelName;

    /** 실행 차수 - 재채점(AIG-10) 시 +1 */
    private Integer runNo;

    /** RUNNING / SUCCESS / FAILED (팀 미확정, 임시 값) */
    private String statusCode;

    /** 요청 프롬프트 요약 */
    private String requestSummary;

    /** Gemini 원본 응답 */
    private String rawResponse;

    /** 실패 시 오류 메시지 */
    private String errorMessage;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
