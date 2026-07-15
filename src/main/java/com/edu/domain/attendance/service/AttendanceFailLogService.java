package com.edu.domain.attendance.service;

import com.edu.domain.attendance.mapper.AttendanceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 출석 검증 실패를 활동로그(activity_logs)에 기록하는 서비스.
 *
 * 핵심: {@code REQUIRES_NEW} 로 별도 트랜잭션에서 커밋한다.
 * 출석 요청은 검증 실패 시 예외를 던져 롤백되는데, 같은 트랜잭션에서 로그를 남기면
 * 로그까지 롤백돼 실패 사유가 유실된다. 그래서 별도 빈 + 새 트랜잭션으로 분리해
 * 호출부가 롤백돼도 실패 로그는 남도록 한다.
 */
@Service
public class AttendanceFailLogService {

    /** 활동로그 분류(action_type) — 나중에 활동로그 화면에서 탭/필터로 사용 */
    public static final String ACTION_ATTENDANCE_FAIL = "ATTENDANCE_FAIL";

    private final AttendanceMapper attendanceMapper;

    public AttendanceFailLogService(AttendanceMapper attendanceMapper) {
        this.attendanceMapper = attendanceMapper;
    }

    /**
     * 출석 실패 기록.
     *
     * @param userId      실패한 학생의 users.user_id (없으면 null 가능)
     * @param classId     시도한 반 class_id (target_id로 저장)
     * @param description 실패코드 + 감지값이 담긴 상세 설명
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, Long classId, String description) {
        attendanceMapper.insertActivityLog(
                userId, ACTION_ATTENDANCE_FAIL, "attendance_records", classId, description);
    }
}
