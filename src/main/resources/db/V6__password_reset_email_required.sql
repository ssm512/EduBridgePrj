-- =============================================================
-- V6: 비밀번호 재설정(A안: 관리자 초기화) 지원 + 이메일 필수화
-- =============================================================

-- 1) 관리자 초기화 후 "다음 로그인 시 비밀번호 변경 강제" 플래그
ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN users.must_change_password
    IS '관리자 비밀번호 초기화 후 강제 변경 여부 (TRUE면 로그인 후 비밀번호 변경 필요)';

-- 2) 이메일 필수화
--    개발 중 이메일 없이 생성된 기존 계정은 placeholder로 백필
--    (placeholder 주소는 실제 메일 발송 불가 → 해당 계정은 이메일 갱신 필요)
UPDATE users
   SET email = login_id || '@noemail.local'
 WHERE email IS NULL;

ALTER TABLE users
    ALTER COLUMN email SET NOT NULL;

COMMENT ON COLUMN users.email
    IS '이메일 (필수) - 비밀번호 재설정 등 본인 확인 용도로 사용';
