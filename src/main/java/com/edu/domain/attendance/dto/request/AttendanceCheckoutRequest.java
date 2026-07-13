package com.edu.domain.attendance.dto.request;

/**
 * 퇴실 요청 (POST /attendance/checkout)
 * 학생이 나갈 때 앱에서 호출한다. 서버가 오늘 등원 기록을 찾아 퇴실 시각을 기록하고,
 * 수업 종료시간보다 이르면 LEAVE(조퇴)로 판정한다.
 */
public record AttendanceCheckoutRequest(
        Long studentId,
        Long classId
) {
}
