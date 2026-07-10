-- =============================================================
-- 개발/테스트용 시드 계정
-- 비밀번호는 모두 1234 (BCrypt, strength 10)
-- 운영 배포 시 이 마이그레이션은 제외하거나 계정을 교체할 것.
-- =============================================================

INSERT INTO users (login_id, password, name, email, role_code, status_code) VALUES
    ('admin',    '$2a$10$IbPokdkqonftgkxBwwYvm.pBtUK2BsmVM1VBp8nHgL6uxIbTJfEo6', '관리자',   'admin@edu.kr',   'ADMIN',   'ACTIVE'),
    ('mads', '$2a$10$IbPokdkqonftgkxBwwYvm.pBtUK2BsmVM1VBp8nHgL6uxIbTJfEo6', '마동석쌤',   'mads@edu.kr','TEACHER', 'ACTIVE'),
    ('leesj', '$2a$10$IbPokdkqonftgkxBwwYvm.pBtUK2BsmVM1VBp8nHgL6uxIbTJfEo6', '이서준',   'leesj@edu.kr','STUDENT', 'ACTIVE'),
    ('leesj_p',  '$2a$10$IbPokdkqonftgkxBwwYvm.pBtUK2BsmVM1VBp8nHgL6uxIbTJfEo6', '이서준맘', 'leesj_p@edu.kr', 'PARENT',  'ACTIVE')
ON CONFLICT (login_id) DO NOTHING;

INSERT INTO students (user_id) VALUES
    ((select user_id from users where login_id = 'leesj'))
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO teachers (user_id) VALUES
    ((select user_id from users where login_id = 'mads'))
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO parents (user_id) VALUES
    ((select user_id from users where login_id = 'leesj_p'))
ON CONFLICT (user_id) DO NOTHING;


