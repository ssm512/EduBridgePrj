package com.edu.domain.classroom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 반 응답 DTO (등록/목록/수정 결과용)
 */
public record ClassResponse(
        Long classId,
        String className,
        Long teacherId,
        String teacherName,
        String subject,
        String classroom,
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,
        String statusCode,
        int activeStudentCount,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,
        /** 수업 요일 CSV (MON~SUN). null이면 매일 */
        String daysOfWeek
) {
    public static ClassResponse from(ClassDto c) {
        return new ClassResponse(
                c.getClassId(),
                c.getClassName(),
                c.getTeacherId(),
                c.getTeacherName(),
                c.getSubject(),
                c.getClassroom(),
                c.getStartTime(),
                c.getEndTime(),
                c.getStatusCode(),
                c.getActiveStudentCount(),
                c.getCreatedAt(),
                c.getDaysOfWeek()
        );
    }
}
