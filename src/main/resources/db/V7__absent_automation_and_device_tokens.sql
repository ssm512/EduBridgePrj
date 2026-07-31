-- =============================================================
-- V7: 결석 자동화 + FCM 기반 마련
--   1) classes.days_of_week   — 수업 요일 (결석 스케줄러 요일 매칭용)
--   2) device_tokens          — FCM 디바이스 토큰 (앱 푸시용)
--   3) system_settings 시드    — 공휴일/결석 지연시간 설정
-- 모두 하위호환(기존 데이터/코드 영향 없음).
-- =============================================================

-- 1) 수업 요일 (CSV: MON~SUN). NULL이면 매일 수업으로 간주(하위호환)
ALTER TABLE classes
    ADD COLUMN IF NOT EXISTS days_of_week VARCHAR(30);

COMMENT ON COLUMN classes.days_of_week IS '수업 요일 (CSV: MON,TUE,WED,THU,FRI,SAT,SUN). NULL = 매일';

-- 2) FCM 디바이스 토큰
--    로그인 시 UPSERT(다른 user 매핑이면 갈아끼움) / 로그아웃 시 삭제 → 공용 폰 대응
CREATE TABLE IF NOT EXISTS device_tokens (
    token_id    BIGSERIAL     PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users(user_id),
    fcm_token   VARCHAR(255)  NOT NULL UNIQUE,
    platform    VARCHAR(20),                                        -- ANDROID/IOS (선택)
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_device_tokens_user ON device_tokens (user_id);

COMMENT ON TABLE device_tokens IS 'FCM 디바이스 토큰 - 사용자별 앱 푸시 발송 대상';

-- 3) 운영 설정 시드 (결석 자동화)
--    HOLIDAYS_RECURRING : 양력 고정 공휴일 (MM-DD, 매년 자동)
--    HOLIDAYS           : 개별 공휴일/임시휴강 (YYYY-MM-DD)
--    HOLIDAYS_EXCLUDE   : 반복 공휴일의 올해 예외 - '이번엔 정상수업' (YYYY-MM-DD)
--    ABSENT_DELAY_MINUTES : 수업 종료 후 결석 처리까지 지연(분)
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
    ('HOLIDAYS_RECURRING', '01-01,03-01,05-05,06-06,08-15,10-03,10-09,12-25', '양력 고정 공휴일 (MM-DD, 매년 자동 적용)'),
    ('HOLIDAYS',           '',   '개별 공휴일·임시휴강 (YYYY-MM-DD, 콤마구분)'),
    ('HOLIDAYS_EXCLUDE',   '',   '반복 공휴일의 올해 예외-정상수업 (YYYY-MM-DD, 콤마구분)'),
    ('ABSENT_DELAY_MINUTES', '10', '수업 종료 후 결석 자동처리까지 지연(분)')
ON CONFLICT (setting_key) DO NOTHING;
