package com.edu.domain.grade.aigrading.service;

/**
 * AIG-06 AI 채점 실행 - 비동기 처리 담당.
 * ExamSubmissionServiceImpl.startGrading()이 exam_submissions를 ANALYZING으로 전이하고 ai_grading_runs를
 * RUNNING으로 등록한 "직후" 이 서비스를 비동기 호출한다 - 요청 스레드는 여기서 기다리지 않고 바로 응답한다.
 *
 * 별도 인터페이스로 분리한 이유: Spring @Async는 프록시를 거쳐야 동작하는데, 같은 클래스 안에서
 * this.asyncMethod()로 자기 자신을 호출하면 프록시를 우회해 동기로 실행돼버린다(자기호출 문제).
 * ExamSubmissionServiceImpl이 이 빈을 주입받아 호출하는 구조로 그 문제를 피한다.
 */
public interface AiExamGradingService {

    /**
     * 실제 Gemini 멀티모달 채점 호출 + answer_results 저장 + exam_submissions/ai_grading_runs 결과 반영.
     * 예외가 나도 이 메서드 밖으로 던지지 않는다(비동기라 호출자가 받을 수 없음) - 내부에서 잡아
     * exam_submissions.status_code=FAILED, ai_grading_runs.status_code=FAILED로 직접 반영한다.
     */
    void gradeSubmissionAsync(Long submissionId, Long gradingRunId, Long triggeredByUserId);
}
