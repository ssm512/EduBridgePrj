package com.edu.domain.classroom.service.impl;

import com.edu.common.dto.PageResponse;
import com.edu.common.exception.ApiException;
import com.edu.domain.classroom.dto.ClassDto;
import com.edu.domain.classroom.dto.EnrollmentCreateRequest;
import com.edu.domain.classroom.dto.EnrollmentDto;
import com.edu.domain.classroom.dto.EnrollmentEndRequest;
import com.edu.domain.classroom.dto.EnrollmentResponse;
import com.edu.domain.classroom.dto.EnrollmentSearchRequest;
import com.edu.domain.classroom.mapper.ClassMapper;
import com.edu.domain.classroom.mapper.EnrollmentMapper;
import com.edu.domain.classroom.service.EnrollmentService;
import com.edu.domain.member.dto.student.StudentDto;
import com.edu.domain.member.mapper.StudentMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentMapper enrollmentMapper;
    private final ClassMapper classMapper;
    private final StudentMapper studentMapper;

    public EnrollmentServiceImpl(EnrollmentMapper enrollmentMapper,
                                 ClassMapper classMapper,
                                 StudentMapper studentMapper) {
        this.enrollmentMapper = enrollmentMapper;
        this.classMapper = classMapper;
        this.studentMapper = studentMapper;
    }

    /**
     * ENR-01 수강 등록.
     * 학생 404 → 반 404 → 종료된 반 400 → 중복 수강 409 순으로 검증 후 INSERT.
     */
    @Override
    @Transactional
    public EnrollmentResponse enroll(EnrollmentCreateRequest request) {
        StudentDto student = studentMapper.selectByStudentId(request.studentId());
        if (student == null) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "학생을 찾을 수 없습니다. studentId=" + request.studentId());
        }
        if (!"ACTIVE".equals(student.getStatusCode())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "재원중(ACTIVE) 학생만 수강 등록할 수 있습니다");
        }
        ClassDto clazz = classMapper.selectByClassId(request.classId());
        if (clazz == null) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "반을 찾을 수 없습니다. classId=" + request.classId());
        }
        if (!"ACTIVE".equals(clazz.getStatusCode())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "종료된 반에는 수강 등록할 수 없습니다");
        }
        if (enrollmentMapper.existsActiveEnrollment(request.studentId(), request.classId())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 수강 중인 학생입니다");
        }

        EnrollmentDto enrollment = EnrollmentDto.builder()
                .studentId(request.studentId())
                .classId(request.classId())
                .enrollDate(request.enrollDate() != null ? request.enrollDate() : LocalDate.now())
                .build();
        enrollmentMapper.insertEnrollment(enrollment);   // enrollmentId 채워짐

        return EnrollmentResponse.from(
                enrollmentMapper.selectByEnrollmentId(enrollment.getEnrollmentId()));
    }

    @Override
    public PageResponse<EnrollmentResponse> getEnrollments(EnrollmentSearchRequest cond) {
        long totalCount = enrollmentMapper.countEnrollments(cond);
        List<EnrollmentResponse> items = enrollmentMapper.selectEnrollments(cond).stream()
                .map(EnrollmentResponse::from)
                .toList();
        return PageResponse.of(items, cond.getPage(), cond.getSize(), totalCount);
    }

    /**
     * ENR-02 수강 해제.
     * DELETE가 아니라 상태(ENDED) + 종료일 기록. 이미 종료된 수강은 409.
     */
    @Override
    @Transactional
    public EnrollmentResponse endEnrollment(Long enrollmentId, EnrollmentEndRequest request) {
        EnrollmentDto enrollment = enrollmentMapper.selectByEnrollmentId(enrollmentId);
        if (enrollment == null) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "수강 내역을 찾을 수 없습니다. enrollmentId=" + enrollmentId);
        }
        if ("ENDED".equals(enrollment.getStatusCode())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 종료된 수강입니다");
        }

        LocalDate endDate = request.endDate() != null ? request.endDate() : LocalDate.now();
        enrollmentMapper.endEnrollment(enrollmentId, endDate);

        return EnrollmentResponse.from(enrollmentMapper.selectByEnrollmentId(enrollmentId));
    }
}
