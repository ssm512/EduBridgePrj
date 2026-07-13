-- =============================================================
-- 출석기록에 퇴실 시각 컬럼 추가 (조퇴 자동 판정용)
-- 학생이 나갈 때 '퇴실'을 찍으면 이 컬럼에 기록되고,
-- 수업 종료시간(classes.end_time)보다 이르면 LEAVE(조퇴)로 판정한다.
-- =============================================================

ALTER TABLE attendance_records
    ADD COLUMN check_out_at TIMESTAMP;

COMMENT ON COLUMN attendance_records.check_out_at IS '퇴실 시각 (조퇴 판정용). 등원=checked_at, 퇴실=check_out_at';
