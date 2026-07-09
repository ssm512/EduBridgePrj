-- =============================================================
-- EduBridge 데이터베이스 스키마 (PostgreSQL)
-- 기준: EduBridge_테이블정의서_과제제외.xlsx (2026-07-08, 1차 개발 범위)
-- 제외: 과제 등록/제출/채점/피드백 등 과제 기능 전체
-- =============================================================

-- 재실행 시 초기화 (필요 시 주석 해제)
-- DROP TABLE IF EXISTS system_settings, activity_logs, notifications,
--   dashboard_snapshots, ai_usage_logs, counseling_records,
--   notice_reads, notice_files, notice_targets, notices,
--   grades, exams, fee_payments, fees,
--   attendance_records, beacons, enrollments, classes,
--   teachers, student_parents, parents, students,
--   refresh_tokens, users CASCADE;

-- =============================================================
-- 1. 인증/인가
-- =============================================================

-- 회원 (ADMIN/TEACHER/STUDENT/PARENT 공통)
CREATE TABLE users (
    user_id        BIGSERIAL     PRIMARY KEY,
    login_id       VARCHAR(50)   NOT NULL,
    password       VARCHAR(100)  NOT NULL,                              -- BCrypt 해시
    name           VARCHAR(50)   NOT NULL,
    email          VARCHAR(255),
    phone          VARCHAR(20),
    role_code      VARCHAR(20)   NOT NULL,                              -- ADMIN/TEACHER/STUDENT/PARENT
    status_code    VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',             -- ACTIVE/INACTIVE/WITHDRAWN
    last_login_at  TIMESTAMP,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP,
    CONSTRAINT uk_users_login_id UNIQUE (login_id),
    CONSTRAINT ck_users_role   CHECK (role_code IN ('ADMIN','TEACHER','STUDENT','PARENT')),
    CONSTRAINT ck_users_status CHECK (status_code IN ('ACTIVE','INACTIVE','WITHDRAWN'))
);
CREATE INDEX idx_users_role_status ON users (role_code, status_code);

COMMENT ON TABLE  users IS '회원 - 로그인 계정 및 공통 회원 정보';
COMMENT ON COLUMN users.role_code IS 'ADMIN/TEACHER/STUDENT/PARENT';
COMMENT ON COLUMN users.status_code IS 'ACTIVE/INACTIVE/WITHDRAWN';

-- 리프레시 토큰 (JWT Refresh Token 저장 및 만료 관리)
CREATE TABLE refresh_tokens (
    token_id       BIGSERIAL     PRIMARY KEY,
    user_id        BIGINT        NOT NULL REFERENCES users(user_id),
    refresh_token  VARCHAR(500)  NOT NULL,
    expires_at     TIMESTAMP     NOT NULL,
    revoked_yn     CHAR(1)       NOT NULL DEFAULT 'N',
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_refresh_revoked CHECK (revoked_yn IN ('Y','N'))
);

COMMENT ON TABLE refresh_tokens IS '리프레시 토큰 - 로그아웃/재발급 처리';

-- =============================================================
-- 2. 회원관리
-- =============================================================

-- 학생 (users와 1:1)
CREATE TABLE students (
    student_id   BIGSERIAL     PRIMARY KEY,
    user_id      BIGINT        NOT NULL UNIQUE REFERENCES users(user_id),
    student_no   VARCHAR(30),                                           -- 내부 관리번호
    birth_date   DATE,
    school_name  VARCHAR(100),
    grade_level  VARCHAR(50),
    memo         TEXT,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE students IS '학생 상세 정보 (users와 1:1)';

-- 학부모 (users와 1:1)
CREATE TABLE parents (
    parent_id   BIGSERIAL     PRIMARY KEY,
    user_id     BIGINT        NOT NULL UNIQUE REFERENCES users(user_id),
    address     VARCHAR(255),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE parents IS '학부모 상세 정보 (users와 1:1)';

-- 학생-학부모 연결 (N:M)
CREATE TABLE student_parents (
    student_parent_id  BIGSERIAL    PRIMARY KEY,
    student_id         BIGINT       NOT NULL REFERENCES students(student_id),
    parent_id          BIGINT       NOT NULL REFERENCES parents(parent_id),
    relation_code      VARCHAR(20)  NOT NULL,                           -- FATHER/MOTHER/GUARDIAN
    primary_yn         CHAR(1)      NOT NULL DEFAULT 'N',
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sp_relation CHECK (relation_code IN ('FATHER','MOTHER','GUARDIAN')),
    CONSTRAINT ck_sp_primary  CHECK (primary_yn IN ('Y','N')),
    CONSTRAINT uk_sp_student_parent UNIQUE (student_id, parent_id)
);

COMMENT ON TABLE student_parents IS '학생-학부모 관계 매핑 (N:M)';

-- 강사 (users와 1:1)
CREATE TABLE teachers (
    teacher_id  BIGSERIAL     PRIMARY KEY,
    user_id     BIGINT        NOT NULL UNIQUE REFERENCES users(user_id),
    subject     VARCHAR(100),
    hire_date   DATE,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE teachers IS '강사 상세 정보 (users와 1:1)';

-- =============================================================
-- 3. 반/수강
-- =============================================================

-- 강의반
CREATE TABLE classes (
    class_id     BIGSERIAL     PRIMARY KEY,
    class_name   VARCHAR(100)  NOT NULL,
    teacher_id   BIGINT        REFERENCES teachers(teacher_id),
    subject      VARCHAR(100),
    classroom    VARCHAR(100),
    start_time   TIME,
    end_time     TIME,
    status_code  VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE classes IS '강의반 - 반명, 수업시간, 담당강사 관리';

-- 수강등록
CREATE TABLE enrollments (
    enrollment_id  BIGSERIAL    PRIMARY KEY,
    student_id     BIGINT       NOT NULL REFERENCES students(student_id),
    class_id       BIGINT       NOT NULL REFERENCES classes(class_id),
    enroll_date    DATE         NOT NULL,
    end_date       DATE,
    status_code    VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',              -- ACTIVE/ENDED
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_enroll_status CHECK (status_code IN ('ACTIVE','ENDED'))
);

COMMENT ON TABLE enrollments IS '수강등록 - 학생의 반 수강 이력';

-- =============================================================
-- 4. 출석관리
-- =============================================================

-- 비콘 (강의실별 BLE 비콘 정보)
CREATE TABLE beacons (
    beacon_id       BIGSERIAL     PRIMARY KEY,
    class_id        BIGINT        REFERENCES classes(class_id),
    beacon_uuid     VARCHAR(100)  NOT NULL,
    major_value     INTEGER,
    minor_value     INTEGER,
    rssi_threshold  INTEGER       NOT NULL DEFAULT -75,                 -- 출석 인정 기준
    location_name   VARCHAR(100),
    active_yn       CHAR(1)       NOT NULL DEFAULT 'Y',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_beacons_active CHECK (active_yn IN ('Y','N'))
);

COMMENT ON TABLE beacons IS '강의실별 BLE 비콘 정보 (UUID/RSSI 기준)';

-- 출석기록 (자동/수동 출석 및 검증 결과)
CREATE TABLE attendance_records (
    attendance_id    BIGSERIAL      PRIMARY KEY,
    student_id       BIGINT         NOT NULL REFERENCES students(student_id),
    class_id         BIGINT         NOT NULL REFERENCES classes(class_id),
    attendance_date  DATE           NOT NULL,
    status_code      VARCHAR(20)    NOT NULL,                           -- PRESENT/LATE/ABSENT/LEAVE
    check_type       VARCHAR(20)    NOT NULL,                           -- AUTO/MANUAL
    checked_at       TIMESTAMP,
    gps_latitude     NUMERIC(10,7),
    gps_longitude    NUMERIC(10,7),
    beacon_uuid      VARCHAR(100),
    rssi_value       INTEGER,
    failure_reason   VARCHAR(255),
    created_by       BIGINT         REFERENCES users(user_id),          -- 수동 등록자
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_att_status CHECK (status_code IN ('PRESENT','LATE','ABSENT','LEAVE')),
    CONSTRAINT ck_att_type   CHECK (check_type IN ('AUTO','MANUAL')),
    CONSTRAINT uk_att_one_day UNIQUE (student_id, class_id, attendance_date)  -- 중복 출석 방지
);
CREATE INDEX idx_att_student_date ON attendance_records (student_id, attendance_date);

COMMENT ON TABLE attendance_records IS '출석기록 - GPS/BLE 기반 자동/수동 출석 및 검증 결과';

-- =============================================================
-- 5. 회비관리
-- =============================================================

-- 회비청구
CREATE TABLE fees (
    fee_id           BIGSERIAL      PRIMARY KEY,
    student_id       BIGINT         NOT NULL REFERENCES students(student_id),
    class_id         BIGINT         REFERENCES classes(class_id),
    billing_month    CHAR(7)        NOT NULL,                           -- YYYY-MM
    fee_amount       NUMERIC(12,0)  NOT NULL DEFAULT 0,
    discount_amount  NUMERIC(12,0)  NOT NULL DEFAULT 0,
    due_date         DATE           NOT NULL,
    status_code      VARCHAR(20)    NOT NULL DEFAULT 'UNPAID',          -- PAID/UNPAID/SCHEDULED
    description      VARCHAR(255),
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_fees_status CHECK (status_code IN ('PAID','UNPAID','SCHEDULED')),
    CONSTRAINT ck_fees_month  CHECK (billing_month ~ '^\d{4}-\d{2}$')
);
CREATE INDEX idx_fees_student_month ON fees (student_id, billing_month);

COMMENT ON TABLE fees IS '회비청구 - 학생/반별 월 회비 청구 정보 (할인/납부기한 포함)';

-- 회비납부
CREATE TABLE fee_payments (
    payment_id      BIGSERIAL      PRIMARY KEY,
    fee_id          BIGINT         NOT NULL REFERENCES fees(fee_id),
    paid_amount     NUMERIC(12,0)  NOT NULL DEFAULT 0,
    paid_at         TIMESTAMP,
    payment_method  VARCHAR(30),                                        -- CASH/TRANSFER/CARD
    receipt_no      VARCHAR(50),
    cancel_yn       CHAR(1)        NOT NULL DEFAULT 'N',
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pay_cancel CHECK (cancel_yn IN ('Y','N'))
);

COMMENT ON TABLE fee_payments IS '회비납부 - 납부 처리 및 영수증 정보 (납부/취소 이력)';

-- =============================================================
-- 6. 성적관리
-- =============================================================

-- 시험
CREATE TABLE exams (
    exam_id      BIGSERIAL     PRIMARY KEY,
    class_id     BIGINT        NOT NULL REFERENCES classes(class_id),
    exam_name    VARCHAR(100)  NOT NULL,
    subject      VARCHAR(100),
    exam_date    DATE          NOT NULL,
    total_score  NUMERIC(5,1)  NOT NULL DEFAULT 100,
    created_by   BIGINT        REFERENCES users(user_id),
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE exams IS '시험 - 시험명, 과목, 반, 시험일 관리';

-- 성적
CREATE TABLE grades (
    grade_id    BIGSERIAL     PRIMARY KEY,
    exam_id     BIGINT        NOT NULL REFERENCES exams(exam_id),
    student_id  BIGINT        NOT NULL REFERENCES students(student_id),
    score       NUMERIC(5,1)  NOT NULL,
    rank_no     INTEGER,                                                -- 선택 (미사용 시 제거 예정)
    comment     VARCHAR(255),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    CONSTRAINT uk_grades_exam_student UNIQUE (exam_id, student_id)      -- 시험별 성적 중복 방지
);

COMMENT ON TABLE grades IS '성적 - 학생별 시험 점수 (성적 추이/차트)';

-- =============================================================
-- 7. 공지사항
-- =============================================================

-- 공지사항
CREATE TABLE notices (
    notice_id      BIGSERIAL     PRIMARY KEY,
    title          VARCHAR(200)  NOT NULL,
    content        TEXT          NOT NULL,
    writer_id      BIGINT        NOT NULL REFERENCES users(user_id),
    target_type    VARCHAR(20)   NOT NULL DEFAULT 'ALL',                -- ALL/CLASS/STUDENT/PARENT/TEACHER
    notice_status  VARCHAR(20)   NOT NULL DEFAULT 'PUBLISHED',
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP,
    CONSTRAINT ck_notices_target CHECK (target_type IN ('ALL','CLASS','STUDENT','PARENT','TEACHER'))
);
CREATE INDEX idx_notices_created_target ON notices (created_at, target_type);

COMMENT ON TABLE notices IS '공지사항 - 제목, 내용, 작성자 관리 (전체/반/학생 대상)';

-- 공지대상
CREATE TABLE notice_targets (
    target_id      BIGSERIAL    PRIMARY KEY,
    notice_id      BIGINT       NOT NULL REFERENCES notices(notice_id),
    target_type    VARCHAR(20)  NOT NULL,                               -- CLASS/STUDENT/PARENT/TEACHER
    target_ref_id  BIGINT       NOT NULL,                               -- class_id/student_id/parent_id/teacher_id
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_nt_target CHECK (target_type IN ('CLASS','STUDENT','PARENT','TEACHER'))
);

COMMENT ON TABLE notice_targets IS '공지대상 - 공지 대상 범위 매핑';

-- 공지첨부파일
CREATE TABLE notice_files (
    file_id        BIGSERIAL     PRIMARY KEY,
    notice_id      BIGINT        NOT NULL REFERENCES notices(notice_id),
    original_name  VARCHAR(255)  NOT NULL,
    stored_name    VARCHAR(255)  NOT NULL,
    file_path      VARCHAR(500)  NOT NULL,
    file_size      BIGINT,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE notice_files IS '공지첨부파일 메타데이터';

-- 공지읽음
CREATE TABLE notice_reads (
    read_id    BIGSERIAL  PRIMARY KEY,
    notice_id  BIGINT     NOT NULL REFERENCES notices(notice_id),
    user_id    BIGINT     NOT NULL REFERENCES users(user_id),
    read_yn    CHAR(1)    NOT NULL DEFAULT 'N',
    read_at    TIMESTAMP,
    CONSTRAINT ck_nr_read CHECK (read_yn IN ('Y','N')),
    CONSTRAINT uk_nr_notice_user UNIQUE (notice_id, user_id)
);

COMMENT ON TABLE notice_reads IS '공지 수신자별 읽음 여부';

-- =============================================================
-- 8. 상담관리
-- =============================================================

CREATE TABLE counseling_records (
    counseling_id    BIGSERIAL    PRIMARY KEY,
    student_id       BIGINT       NOT NULL REFERENCES students(student_id),
    parent_id        BIGINT       REFERENCES parents(parent_id),        -- 학부모 상담 시
    teacher_id       BIGINT       REFERENCES teachers(teacher_id),
    counseling_date  DATE         NOT NULL,
    counseling_type  VARCHAR(20),                                       -- STUDENT/PARENT
    content          TEXT         NOT NULL,
    visibility_code  VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE',           -- PRIVATE/SHARED
    created_by       BIGINT       NOT NULL REFERENCES users(user_id),
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_cr_visibility CHECK (visibility_code IN ('PRIVATE','SHARED'))
);

COMMENT ON TABLE counseling_records IS '상담기록 - 학생/학부모 상담 이력 (공개범위 포함)';

-- =============================================================
-- 9. AI기능
-- =============================================================

CREATE TABLE ai_usage_logs (
    ai_log_id       BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(user_id),
    feature_code    VARCHAR(30)  NOT NULL,                              -- REPORT/NOTICE/COUNSELING
    request_prompt  TEXT         NOT NULL,
    response_text   TEXT,
    model_name      VARCHAR(50)  DEFAULT 'Gemini',
    success_yn      CHAR(1)      NOT NULL DEFAULT 'Y',
    error_message   TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_ai_success CHECK (success_yn IN ('Y','N'))
);

COMMENT ON TABLE ai_usage_logs IS 'AI사용로그 - Gemini API 요청/응답 이력 (API Key 미저장)';

-- =============================================================
-- 10. 대시보드 (선택 구현)
-- =============================================================

CREATE TABLE dashboard_snapshots (
    snapshot_id         BIGSERIAL  PRIMARY KEY,
    snapshot_date       DATE       NOT NULL,
    attendance_summary  JSONB,
    fee_summary         JSONB,
    grade_summary       JSONB,
    created_at          TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE dashboard_snapshots IS '대시보드스냅샷 - 일/월 단위 통계 (선택 구현)';

-- =============================================================
-- 11. 알림
-- =============================================================

CREATE TABLE notifications (
    notification_id    BIGSERIAL     PRIMARY KEY,
    user_id            BIGINT        NOT NULL REFERENCES users(user_id),
    notification_type  VARCHAR(30)   NOT NULL,                          -- ATTENDANCE/FEE/NOTICE
    title              VARCHAR(200)  NOT NULL,
    message            TEXT          NOT NULL,
    send_channel       VARCHAR(20)   NOT NULL DEFAULT 'APP',            -- APP/KAKAO/EMAIL/SMS
    send_status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING',        -- PENDING/SENT/FAILED
    sent_at            TIMESTAMP,
    read_yn            CHAR(1)       NOT NULL DEFAULT 'N',
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_noti_type    CHECK (notification_type IN ('ATTENDANCE','FEE','NOTICE')),
    CONSTRAINT ck_noti_channel CHECK (send_channel IN ('APP','KAKAO','EMAIL','SMS')),
    CONSTRAINT ck_noti_status  CHECK (send_status IN ('PENDING','SENT','FAILED')),
    CONSTRAINT ck_noti_read    CHECK (read_yn IN ('Y','N'))
);

COMMENT ON TABLE notifications IS '알림이력 - 출석/회비/공지 알림 발송 이력';

-- =============================================================
-- 12. 로그
-- =============================================================

CREATE TABLE activity_logs (
    activity_log_id  BIGSERIAL     PRIMARY KEY,
    user_id          BIGINT        REFERENCES users(user_id),           -- 비회원 오류 로그 가능 (NULL 허용)
    action_type      VARCHAR(50)   NOT NULL,                            -- LOGIN/CREATE/UPDATE/DELETE
    target_table     VARCHAR(50),
    target_id        BIGINT,
    ip_address       VARCHAR(45),
    user_agent       VARCHAR(500),
    description      TEXT,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_logs_created_user ON activity_logs (created_at, user_id);

COMMENT ON TABLE activity_logs IS '활동로그 - 사용자 주요 행위 기록 (관리자 조회)';

-- =============================================================
-- 13. 배포/설정
-- =============================================================

CREATE TABLE system_settings (
    setting_id     BIGSERIAL     PRIMARY KEY,
    setting_key    VARCHAR(100)  NOT NULL UNIQUE,
    setting_value  VARCHAR(500)  NOT NULL,
    description    VARCHAR(255),
    updated_by     BIGINT        REFERENCES users(user_id),
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE system_settings IS '시스템설정 - 출석 허용시간, GPS 반경 등 운영 설정';

-- 기본 설정값 예시
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
    ('ATTENDANCE_ALLOW_MINUTES', '10',  '출석 인정 허용시간(분)'),
    ('GPS_RADIUS_METERS',        '100', 'GPS 출석 인정 반경(m)'),
    ('RSSI_DEFAULT_THRESHOLD',   '-75', 'BLE RSSI 기본 기준값');
