-- =============================================================
-- V12: 학원 이름 설정 (웹 타이틀 · 앱 상단 표시용, 멀티테넌트 시 학원별로 값이 다름)
--   ※ V11은 타 팀원이 사용 중이라 V12로 배정 (하민욱)
-- =============================================================
INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('ACADEMY_NAME', 'EduBridge', '학원 이름 (웹 브라우저 타이틀·앱 상단 표시)')
ON CONFLICT (setting_key) DO NOTHING;
