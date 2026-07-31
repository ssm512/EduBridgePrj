-- =============================================================
-- V10: Phase 1.5 - 회비 할인정책 확장(영수증 발급 포함) + AI 시험지 자동채점
-- 근거: EduBridge_테이블정의서_0716_V10_Phase1.5반영.xlsx (컬럼정의서/인덱스_FK/코드정의 시트)
--
-- 팀 결정 반영 사항 (2026-07-21):
--   1) discount_policies 를 V9(policy_id, condition_type 있음)에서 V10 문서 기준으로 정렬
--      - PK: policy_id -> discount_policy_id 리네이밍
--      - condition_type 컬럼 삭제 (분류 표기용 컬럼, 문서에서 제외됨)
--      - created_by(등록자), updated_at(수정일시) 컬럼 추가
--      => 연동 Java 코드(DiscountPolicyVo/Mapper/Service/Controller, discountPolicyList.html)도 같이 수정됨
--   2) fee_payments.receipt_no 컬럼 삭제 (신규 payment_receipts.receipt_no 로 일원화 - 검토보고서 우선순위1 항목)
--      => FeePaymentVo/FeePaymentHistoryResponse/FeeMapper.xml/feeList.html 도 같이 수정됨
--
-- 미확정 사항 (문서에 코드 그룹 없음 - 팀 확인 필요):
--   ai_grading_runs.status_code 는 코드정의 시트에 코드 그룹이 정의되어 있지 않아 CHECK 제약을 걸지 않음.
--   exam_submissions.status_code(UPLOADED/ANALYZING/REVIEW_REQUIRED/CONFIRMED/FAILED) 값과의 관계를
--   팀 회의에서 확정한 뒤 CHECK 제약을 추가하는 것을 권장.
-- =============================================================


-- =============================================================
-- 1. discount_policies 정렬 (V9 -> V10 문서 기준)
-- =============================================================

ALTER TABLE discount_policies RENAME COLUMN policy_id TO discount_policy_id;

ALTER TABLE discount_policies DROP COLUMN IF EXISTS condition_type;

ALTER TABLE discount_policies ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users(user_id);
ALTER TABLE discount_policies ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 기존 행(있다면)의 created_by 를 첫 번째 ADMIN 계정으로 백필한 뒤 NOT NULL 확정
UPDATE discount_policies
   SET created_by = (SELECT user_id FROM users WHERE role_code = 'ADMIN' ORDER BY user_id LIMIT 1)
 WHERE created_by IS NULL;

ALTER TABLE discount_policies ALTER COLUMN created_by SET NOT NULL;

COMMENT ON COLUMN discount_policies.created_by IS '등록자 (users.user_id)';
COMMENT ON COLUMN discount_policies.updated_at IS '수정일시';


-- =============================================================
-- 2. 회비할인적용 (FEE-15~16) - 회비별 할인 정책 적용 상세 및 금액 이력
-- =============================================================

CREATE TABLE fee_discounts (
    fee_discount_id    BIGSERIAL      PRIMARY KEY,
    fee_id             BIGINT         NOT NULL REFERENCES fees(fee_id),
    discount_policy_id BIGINT         REFERENCES discount_policies(discount_policy_id),
    discount_amount    NUMERIC(12,2)  NOT NULL,
    reason             VARCHAR(500),
    applied_by         BIGINT         NOT NULL REFERENCES users(user_id),
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_fee_discounts_amount CHECK (discount_amount >= 0)
);

COMMENT ON TABLE fee_discounts IS '회비할인적용 - 회비별 할인 정책 적용 상세 및 금액 이력 (fees와 N:1)';


-- =============================================================
-- 3. 납부영수증 (FEE-17~20) - 납부 완료 영수증 번호, 발급 및 취소 상태 관리
-- =============================================================

CREATE TABLE payment_receipts (
    receipt_id    BIGSERIAL      PRIMARY KEY,
    payment_id    BIGINT         NOT NULL REFERENCES fee_payments(payment_id),
    receipt_no    VARCHAR(50)    NOT NULL,
    status_code   VARCHAR(20)    NOT NULL DEFAULT 'ISSUED',
    issued_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    issued_by     BIGINT         NOT NULL REFERENCES users(user_id),
    cancelled_at  TIMESTAMP,
    cancel_reason VARCHAR(500),
    CONSTRAINT ck_payment_receipts_status CHECK (status_code IN ('ISSUED', 'CANCELLED')),
    CONSTRAINT uk_receipt_payment UNIQUE (payment_id),
    CONSTRAINT uk_receipt_no      UNIQUE (receipt_no)
);

COMMENT ON TABLE payment_receipts IS '납부영수증 - 납부 완료 영수증 번호, 발급 및 취소 상태 관리 (fee_payments 1:1)';


-- =============================================================
-- 4. fee_payments.receipt_no 삭제 (payment_receipts.receipt_no 로 일원화)
-- =============================================================

ALTER TABLE fee_payments DROP COLUMN IF EXISTS receipt_no;


-- =============================================================
-- 5. 시험자료 (GRADE-13~14) - 문제지·정답지 원본 파일 메타데이터
-- =============================================================

CREATE TABLE exam_documents (
    exam_document_id BIGSERIAL      PRIMARY KEY,
    exam_id          BIGINT         NOT NULL REFERENCES exams(exam_id),
    document_type    VARCHAR(20)    NOT NULL,
    original_name    VARCHAR(255)   NOT NULL,
    stored_name      VARCHAR(255)   NOT NULL,
    file_path        VARCHAR(500)   NOT NULL,
    mime_type        VARCHAR(100)   NOT NULL,
    file_size        BIGINT         NOT NULL,
    page_no          INTEGER,
    uploaded_by      BIGINT         NOT NULL REFERENCES users(user_id),
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_exam_documents_type CHECK (document_type IN ('QUESTION', 'ANSWER_KEY'))
);

COMMENT ON TABLE exam_documents IS '시험자료 - 문제지/정답지 원본 파일 메타데이터 (이미지/PDF)';


-- =============================================================
-- 6. 시험문항 (GRADE-15) - 문항별 문제, 정답, 배점 및 채점 기준
-- =============================================================

CREATE TABLE exam_questions (
    question_id        BIGSERIAL      PRIMARY KEY,
    exam_id            BIGINT         NOT NULL REFERENCES exams(exam_id),
    question_no        INTEGER        NOT NULL,
    question_type      VARCHAR(30)    NOT NULL,
    question_text      TEXT,
    correct_answer     TEXT           NOT NULL,
    max_score          NUMERIC(6,2)   NOT NULL,
    grading_criteria   TEXT,
    review_required_yn CHAR(1)        NOT NULL DEFAULT 'N',
    sort_order         INTEGER        NOT NULL,
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_exam_questions_type   CHECK (question_type IN ('MULTIPLE_CHOICE', 'SHORT_ANSWER', 'ESSAY')),
    CONSTRAINT ck_exam_questions_review CHECK (review_required_yn IN ('Y', 'N')),
    CONSTRAINT ck_exam_questions_score  CHECK (max_score >= 0),
    CONSTRAINT uk_exam_question_no UNIQUE (exam_id, question_no)
);

COMMENT ON TABLE exam_questions IS '시험문항 - 문항별 문제/정답/배점/채점기준 (시험별 문항번호 UNIQUE)';


-- =============================================================
-- 7. 학생시험제출 (GRADE-16~18) - 학생별 답안지 업로드 및 AI 채점·검수 상태
-- =============================================================

CREATE TABLE exam_submissions (
    submission_id   BIGSERIAL      PRIMARY KEY,
    exam_id         BIGINT         NOT NULL REFERENCES exams(exam_id),
    student_id      BIGINT         NOT NULL REFERENCES students(student_id),
    status_code     VARCHAR(30)    NOT NULL DEFAULT 'UPLOADED',
    ai_total_score  NUMERIC(6,2),
    confirmed_score NUMERIC(6,2),
    ai_confidence   NUMERIC(5,4),
    error_message   VARCHAR(1000),
    uploaded_by     BIGINT         NOT NULL REFERENCES users(user_id),
    reviewed_by     BIGINT         REFERENCES users(user_id),
    grade_id        BIGINT         REFERENCES grades(grade_id),
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at     TIMESTAMP,
    confirmed_at    TIMESTAMP,
    CONSTRAINT ck_exam_submissions_status CHECK (status_code IN ('UPLOADED', 'ANALYZING', 'REVIEW_REQUIRED', 'CONFIRMED', 'FAILED')),
    CONSTRAINT uk_submission_exam_student UNIQUE (exam_id, student_id)
);

COMMENT ON TABLE exam_submissions IS '학생시험제출 - 학생별 답안지 업로드 및 AI 채점/검수 상태 (시험+학생 단위, 1인 1제출)';


-- =============================================================
-- 8. 학생답안파일 (GRADE-16) - 학생 시험 제출에 포함된 페이지별 이미지 파일
-- =============================================================

CREATE TABLE submission_files (
    submission_file_id BIGSERIAL      PRIMARY KEY,
    submission_id      BIGINT         NOT NULL REFERENCES exam_submissions(submission_id),
    original_name      VARCHAR(255)   NOT NULL,
    stored_name        VARCHAR(255)   NOT NULL,
    file_path          VARCHAR(500)   NOT NULL,
    mime_type          VARCHAR(100)   NOT NULL,
    file_size          BIGINT         NOT NULL,
    page_no            INTEGER        NOT NULL,
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE submission_files IS '학생답안파일 - 학생 시험 제출에 포함된 페이지별 이미지 파일 (다중 페이지)';


-- =============================================================
-- 9. 문항별채점결과 (GRADE-19~20) - AI 인식 답안, 임시 점수, 확정 점수와 신뢰도
-- =============================================================

CREATE TABLE answer_results (
    answer_result_id   BIGSERIAL      PRIMARY KEY,
    submission_id      BIGINT         NOT NULL REFERENCES exam_submissions(submission_id),
    question_id        BIGINT         NOT NULL REFERENCES exam_questions(question_id),
    recognized_answer  TEXT,
    ai_score           NUMERIC(6,2),
    confirmed_score    NUMERIC(6,2),
    result_code        VARCHAR(20)    NOT NULL,
    ai_reason          TEXT,
    confidence         NUMERIC(5,4),
    review_required_yn CHAR(1)        NOT NULL DEFAULT 'N',
    teacher_comment    VARCHAR(1000),
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_answer_results_result CHECK (result_code IN ('CORRECT', 'PARTIAL', 'INCORRECT', 'UNREADABLE')),
    CONSTRAINT ck_answer_results_review CHECK (review_required_yn IN ('Y', 'N')),
    CONSTRAINT uk_answer_submission_question UNIQUE (submission_id, question_id)
);

COMMENT ON TABLE answer_results IS '문항별채점결과 - AI 인식 답안/임시 점수/확정 점수/신뢰도 (교사 검수 대상)';


-- =============================================================
-- 10. AI채점실행 (GRADE-21~22) - Gemini 채점 실행별 상태, 모델, 오류 및 실행 이력
-- =============================================================

CREATE TABLE ai_grading_runs (
    grading_run_id  BIGSERIAL      PRIMARY KEY,
    submission_id   BIGINT         NOT NULL REFERENCES exam_submissions(submission_id),
    ai_log_id       BIGINT         REFERENCES ai_usage_logs(ai_log_id),
    model_name      VARCHAR(100)   NOT NULL,
    run_no          INTEGER        NOT NULL DEFAULT 1,
    status_code     VARCHAR(20)    NOT NULL,  -- 코드정의 시트에 그룹 미정의 - 팀 확인 필요 (미확정 사항 참고)
    request_summary TEXT,
    raw_response    TEXT,
    error_message   VARCHAR(1000),
    started_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    TIMESTAMP,
    CONSTRAINT uk_grading_run_no UNIQUE (submission_id, run_no)
);

COMMENT ON TABLE ai_grading_runs IS 'AI채점실행 - Gemini 채점 실행별 상태/모델/오류 이력 (재채점 시 run_no 증가)';
