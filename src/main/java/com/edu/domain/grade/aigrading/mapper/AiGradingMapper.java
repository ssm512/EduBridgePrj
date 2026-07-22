package com.edu.domain.grade.aigrading.mapper;

import com.edu.domain.grade.aigrading.vo.AiGradingRunVo;
import com.edu.domain.grade.aigrading.vo.AnswerResultVo;
import com.edu.domain.grade.aigrading.vo.ExamDocumentVo;
import com.edu.domain.grade.aigrading.vo.ExamQuestionVo;
import com.edu.domain.grade.aigrading.vo.ExamSubmissionVo;
import com.edu.domain.grade.aigrading.vo.SubmissionFileVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 시험지 자동채점(AIG-01~10) 매퍼 - exam_documents/exam_questions/exam_submissions/submission_files/
 * ai_grading_runs/answer_results 6개 테이블을 하나로 통합했다.
 * XML: resources/mapper/grade/aigrading/AiGradingMapper.xml
 *
 * 원래 테이블 1개당 매퍼 1개(ExamDocumentMapper/ExamQuestionMapper/ExamSubmissionMapper/
 * SubmissionFileMapper/AiGradingRunMapper/AnswerResultMapper)로 나눠져 있던 걸 통합했다(2026-07-22).
 * 통합하면서 여러 매퍼에 같은 이름(selectByExamId, selectBySubmissionId)으로 존재하던 메서드는
 * 한 인터페이스 안에서 이름이 겹칠 수 없어 대상 테이블을 붙여 구분했다:
 *   selectByExamId(documents)      -&gt; selectDocumentsByExamId
 *   selectByExamId(questions)      -&gt; selectQuestionsByExamId
 *   selectBySubmissionId(files)    -&gt; selectFilesBySubmissionId
 *   selectBySubmissionId(results)  -&gt; selectAnswerResultsBySubmissionId
 *   deleteBySubmissionId(results)  -&gt; deleteAnswerResultsBySubmissionId (위 리네임과 짝 맞춤)
 * 그 외 메서드명은 원래 이름을 그대로 유지했다.
 */
@Mapper
public interface AiGradingMapper {

    // =====================================================================
    // 공통 존재 확인
    // =====================================================================

    /** examId가 실제 존재하는 시험인지 확인 (문서/문항/제출 업로드 전 검증에 공용으로 쓰인다) */
    boolean existsExam(@Param("examId") Long examId);

    /** studentId가 실제 존재하는 학생인지 확인 (AIG-05 답안 업로드 전 검증) */
    boolean existsStudent(@Param("studentId") Long studentId);

    // =====================================================================
    // exam_documents (AIG-01/02)
    // =====================================================================

    /** AIG-01 시험자료 메타데이터 등록. useGeneratedKeys로 examDocumentId가 채워진다 */
    int insertDocument(ExamDocumentVo document);

    /** 시험자료 단건 조회 (업로드 응답 재조회용) */
    ExamDocumentVo selectByDocumentId(@Param("examDocumentId") Long examDocumentId);

    /** AIG-02 문항 자동 추출용 - 이 시험에 업로드된 문제지/정답지 전체 조회 (page_no 순) */
    List<ExamDocumentVo> selectDocumentsByExamId(@Param("examId") Long examId);

    // =====================================================================
    // exam_questions (AIG-03/04)
    // =====================================================================

    /**
     * 이 시험에 학생 답안 제출(exam_submissions)이 이미 있는지 확인.
     * AIG-04는 전체 교체(delete+insert) 방식이라, 채점이 시작된 뒤 문항을 바꾸면
     * answer_results가 참조하는 question_id가 끊어질 수 있어 저장 전에 막는다.
     */
    boolean existsSubmissionForExam(@Param("examId") Long examId);

    /** AIG-03 시험 문항 목록 조회 (정렬 순서 기준). AIG-06/AIG-09에서 채점용 문항 목록 조회에도 재사용 */
    List<ExamQuestionVo> selectQuestionsByExamId(@Param("examId") Long examId);

    /** AIG-04 저장 전 기존 문항 전체 삭제 (전체 교체 방식) */
    int deleteByExamId(@Param("examId") Long examId);

    /** AIG-04 문항 등록 (useGeneratedKeys로 questionId가 채워진다) */
    int insertQuestion(ExamQuestionVo question);

    // =====================================================================
    // exam_submissions (AIG-05~10)
    // =====================================================================

    /** AIG-05 재업로드 판단용 - 시험+학생 단위 기존 제출 조회 (uk_submission_exam_student) */
    ExamSubmissionVo selectByExamAndStudent(@Param("examId") Long examId, @Param("studentId") Long studentId);

    /** 제출 단건 조회 (업로드 응답 재조회, AIG-06 이후 상태 조회용) */
    ExamSubmissionVo selectBySubmissionId(@Param("submissionId") Long submissionId);

    /** AIG-05 신규 제출 등록. useGeneratedKeys로 submissionId가 채워진다 */
    int insertSubmission(ExamSubmissionVo submission);

    /** AIG-06 채점 시작 시 상태만 전이 (UPLOADED/REVIEW_REQUIRED/FAILED -&gt; ANALYZING) */
    int updateStatusCode(@Param("submissionId") Long submissionId, @Param("statusCode") String statusCode);

    /**
     * AIG-06 채점 완료/실패 시 결과 반영.
     * 성공: statusCode=REVIEW_REQUIRED, aiTotalScore/aiConfidence 채움.
     * 실패: statusCode=FAILED, errorMessage 채움 (aiTotalScore/aiConfidence는 null 유지).
     */
    int completeGrading(ExamSubmissionVo submission);

    /** AIG-08 교사 검수 시각/검수자 기록 (reviewed_by는 이미 값이 있으면 덮어쓰지 않는다 - 최초 검수자 유지) */
    int markReviewed(@Param("submissionId") Long submissionId, @Param("reviewedBy") Long reviewedBy);

    /** AIG-09 최종 확정 - statusCode=CONFIRMED, confirmedScore/gradeId/confirmed_at 반영 */
    int confirmSubmission(ExamSubmissionVo submission);

    // =====================================================================
    // submission_files (AIG-05)
    // =====================================================================

    /** AIG-05 답안지 파일 메타데이터 등록. useGeneratedKeys로 submissionFileId가 채워진다 */
    int insertFile(SubmissionFileVo file);

    /** 제출 건에 속한 파일 목록 조회 (page_no 순) */
    List<SubmissionFileVo> selectFilesBySubmissionId(@Param("submissionId") Long submissionId);

    /** 재업로드 시 페이지 번호를 이어서 채번하기 위한 현재 최대 page_no (파일 없으면 null) */
    Integer selectMaxPageNo(@Param("submissionId") Long submissionId);

    // =====================================================================
    // ai_grading_runs (AIG-06/AIG-10)
    // =====================================================================

    /** 이 제출 건의 마지막 실행 차수 (없으면 null) - AIG-10 재채점 시 run_no+1 계산에 사용 */
    Integer selectMaxRunNo(@Param("submissionId") Long submissionId);

    /** AIG-06 실행 시작 등록 (status=RUNNING). useGeneratedKeys로 gradingRunId가 채워진다 */
    int insertRun(AiGradingRunVo run);

    /** 실행 완료/실패 결과 반영 (status/rawResponse/errorMessage/aiLogId/completedAt) */
    int updateRunResult(AiGradingRunVo run);

    // =====================================================================
    // answer_results (AIG-06/07/08/09)
    // =====================================================================

    /** 제출 건의 기존 채점 결과 전체 삭제 (재채점 시 이전 실행 결과를 새 결과로 교체하기 위함) */
    int deleteAnswerResultsBySubmissionId(@Param("submissionId") Long submissionId);

    /** AIG-06 문항별 채점 결과 등록. useGeneratedKeys로 answerResultId가 채워진다 */
    int insertResult(AnswerResultVo result);

    /** 제출 건의 채점 결과 목록 조회 (문항 정렬 순서 기준, AIG-07) */
    List<AnswerResultVo> selectAnswerResultsBySubmissionId(@Param("submissionId") Long submissionId);

    /**
     * AIG-08 교사 검수 결과 저장 (confirmed_score/teacher_comment).
     * submissionId까지 WHERE 조건에 포함해 다른 제출 건의 answer_result_id를 실수로 건드리지 못하게 막는다.
     * 반환값(영향 행 수)이 0이면 answerResultId가 이 제출 건 소속이 아니라는 뜻 - 서비스에서 400 처리.
     */
    int updateTeacherReview(@Param("answerResultId") Long answerResultId,
                             @Param("submissionId") Long submissionId,
                             @Param("confirmedScore") BigDecimal confirmedScore,
                             @Param("teacherComment") String teacherComment);

    /**
     * AIG-09 최종 확정 시 검수가 필요 없던(review_required_yn='N') 문항의 confirmed_score를 ai_score로 채운다.
     * 이미 confirmed_score가 있으면(교사가 그래도 수정한 경우) 덮어쓰지 않는다.
     */
    int confirmAiScoreIfAbsent(@Param("answerResultId") Long answerResultId);
}
