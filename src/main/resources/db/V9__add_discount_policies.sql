-- =============================================================
-- V9: 할인정책 (FEE-10)
--   discount_policies       — 관리자가 등록하는 할인정책 마스터 (형제할인/장기수강할인 등)
--   fees.discount_policy_id — 회비 1건에 적용된 정책 참조 (1:1, 선택)
-- fees.discount_amount 는 그대로 유지 (정책 적용 시 자동계산된 최종 금액을 스냅샷으로 저장,
-- 정책 미적용 시 기존처럼 관리자가 직접 입력 가능 - 하위호환).
-- =============================================================

CREATE TABLE IF NOT EXISTS discount_policies (
    policy_id       BIGSERIAL      PRIMARY KEY,
    policy_name     VARCHAR(100)   NOT NULL,
    discount_type   VARCHAR(10)    NOT NULL,                       -- RATE(정률) / FIXED(정액)
    discount_value  NUMERIC(12,2)  NOT NULL,                       -- RATE: 0~100(%), FIXED: 원 단위 금액
    condition_type  VARCHAR(20)    NOT NULL,                       -- SIBLING/LONG_TERM/MULTI_CLASS/MANUAL 등 (분류 표기용, 자동판정 없음)
    start_date      DATE,                                          -- NULL = 시작 제한 없음
    end_date        DATE,                                          -- NULL = 종료 제한 없음
    active_yn       CHAR(1)        NOT NULL DEFAULT 'Y',
    description     VARCHAR(255),
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_discount_policies_type       CHECK (discount_type IN ('RATE', 'FIXED')),
    CONSTRAINT ck_discount_policies_value      CHECK (discount_value >= 0),
    CONSTRAINT ck_discount_policies_rate_range CHECK (discount_type <> 'RATE' OR discount_value <= 100),
    CONSTRAINT ck_discount_policies_active     CHECK (active_yn IN ('Y', 'N')),
    CONSTRAINT ck_discount_policies_period     CHECK (start_date IS NULL OR end_date IS NULL OR start_date <= end_date)
);

COMMENT ON TABLE discount_policies IS '할인정책 - 관리자가 등록하는 할인 유형(정률/정액) 마스터';

-- 회비 1건은 정책 최대 1개만 적용 (FEE-10 Phase 1 범위: 다중 적용/우선순위 규칙 없음)
ALTER TABLE fees
    ADD COLUMN IF NOT EXISTS discount_policy_id BIGINT REFERENCES discount_policies(policy_id);

COMMENT ON COLUMN fees.discount_policy_id IS '적용된 할인정책 (선택). NULL이면 discount_amount 를 관리자가 직접 입력한 것.';
