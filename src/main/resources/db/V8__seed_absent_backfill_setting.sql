-- =============================================================
-- V8: 결석 백필 일수 설정 시드
--   ABSENT_BACKFILL_DAYS: 재가동 시 결석을 소급 채울 최대 일수 (0 = 끔)
--   관리자 시스템설정 화면에서 조절 가능하게 하기 위해 키를 등록.
-- =============================================================
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
    ('ABSENT_BACKFILL_DAYS', '7', '재가동 시 결석을 소급 채울 최대 일수 (0=끔)')
ON CONFLICT (setting_key) DO NOTHING;
