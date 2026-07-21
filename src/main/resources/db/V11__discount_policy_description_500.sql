-- FEE-10 보완: 할인정책 설명(description) 길이를 요구사항명세서 기준(500자)에 맞춘다.
-- V9에서 VARCHAR(255)로 생성되었고, V10에서도 손대지 않아 여전히 255였던 것을 500으로 확장.
ALTER TABLE discount_policies ALTER COLUMN description TYPE VARCHAR(500);

COMMENT ON COLUMN discount_policies.description IS '설명 (최대 500자, 명세서 기준)';
