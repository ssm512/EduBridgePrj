-- =============================================================
-- V13: 알림 타입에 GRADE 추가 (성적 입력/수정 알림, 신상민 2026-07-22)
--
-- AI채점 확정 및 수기 성적 입력/수정 시 학생 본인 + 학부모에게 알림을 보내기 위해
-- notifications.notification_type 허용값에 GRADE를 추가한다.
--
-- COUNSELING도 함께 열어둔다 - 아직 이 타입으로 알림을 만드는 코드는 없지만(상담 알림 기능은
-- 이번 작업 범위 아님), 나중에 상담 알림을 추가할 때 이 마이그레이션을 또 만들지 않도록
-- 미리 허용값만 넓혀둔 것 (팀 요청, 2026-07-22).
-- =============================================================

ALTER TABLE notifications DROP CONSTRAINT ck_noti_type;

ALTER TABLE notifications
    ADD CONSTRAINT ck_noti_type
    CHECK (notification_type IN ('ATTENDANCE', 'FEE', 'NOTICE', 'GRADE', 'COUNSELING'));
