-- =============================================================
-- 성적관리: 시험일레 시간까지 저장하기 위해 exams.exam_date 타입을 TIMESTAMP 로 변경
ALTER TABLE exams
    ALTER COLUMN exam_date TYPE TIMESTAMP
    USING exam_date::timestamp;
