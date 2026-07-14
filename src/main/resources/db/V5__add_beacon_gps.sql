-- =============================================================
-- 비콘(강의실)에 GPS 기준 좌표 추가 — GPS 출석 검증용
-- 비콘이 강의실에 물리적으로 설치되므로 비콘 좌표를 강의실 기준점으로 사용한다.
-- 학생 GPS가 이 좌표 기준 GPS_RADIUS_METERS(기본 100m) 이내여야 출석 인정.
-- (좌표가 비어있으면 GPS 검증은 생략, UUID/RSSI만 검증)
-- =============================================================

ALTER TABLE beacons
    ADD COLUMN gps_latitude  NUMERIC(10,7),
    ADD COLUMN gps_longitude NUMERIC(10,7);

COMMENT ON COLUMN beacons.gps_latitude  IS '비콘(강의실) 기준 위도 - GPS 출석 검증용';
COMMENT ON COLUMN beacons.gps_longitude IS '비콘(강의실) 기준 경도 - GPS 출석 검증용';
