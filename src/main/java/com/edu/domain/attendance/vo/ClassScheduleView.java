package com.edu.domain.attendance.vo;

import lombok.Data;

import java.time.LocalTime;

/**
 * 출석 판정에 필요한 반 시간표만 읽는 조회 전용 뷰.
 *
 * classes 테이블은 신상민님(반/수강) 도메인 소유이지만,
 * 지각/조퇴 자동 판정에 수업 시작·종료 시간이 필요해 attendance에서 읽기만 한다.
 * TODO: 신상민님 반 조회 서비스/API가 나오면 그쪽 호출로 교체.
 */
@Data
public class ClassScheduleView {
    private LocalTime startTime;
    private LocalTime endTime;
}
