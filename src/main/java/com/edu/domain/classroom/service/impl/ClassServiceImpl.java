package com.edu.domain.classroom.service.impl;

import com.edu.common.dto.PageResponse;
import com.edu.common.exception.ApiException;
import com.edu.domain.classroom.dto.ClassCreateRequest;
import com.edu.domain.classroom.dto.ClassDetailResponse;
import com.edu.domain.classroom.dto.ClassDto;
import com.edu.domain.classroom.dto.ClassResponse;
import com.edu.domain.classroom.dto.ClassSearchRequest;
import com.edu.domain.classroom.dto.ClassUpdateRequest;
import com.edu.domain.classroom.mapper.ClassMapper;
import com.edu.domain.classroom.mapper.EnrollmentMapper;
import com.edu.domain.classroom.service.ClassService;
import com.edu.domain.member.mapper.TeacherMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ClassServiceImpl implements ClassService {

    private final ClassMapper classMapper;
    private final TeacherMapper teacherMapper;
    private final EnrollmentMapper enrollmentMapper;

    public ClassServiceImpl(ClassMapper classMapper, TeacherMapper teacherMapper,
                            EnrollmentMapper enrollmentMapper) {
        this.classMapper = classMapper;
        this.teacherMapper = teacherMapper;
        this.enrollmentMapper = enrollmentMapper;
    }

    /**
     * CLS-01 반 등록.
     * teacherId가 지정된 경우 존재 검증 후 INSERT.
     */
    @Override
    @Transactional
    public ClassResponse createClass(ClassCreateRequest request) {
        validateTeacher(request.teacherId());

        ClassDto clazz = ClassDto.builder()
                .className(request.className())
                .teacherId(request.teacherId())
                .subject(request.subject())
                .classroom(request.classroom())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .build();
        classMapper.insertClass(clazz);   // useGeneratedKeys로 classId 채워짐

        return ClassResponse.from(classMapper.selectByClassId(clazz.getClassId()));
    }

    @Override
    public PageResponse<ClassResponse> getClasses(ClassSearchRequest cond) {
        long totalCount = classMapper.countClasses(cond);
        List<ClassResponse> items = classMapper.selectClasses(cond).stream()
                .map(ClassResponse::from)
                .toList();
        return PageResponse.of(items, cond.getPage(), cond.getSize(), totalCount);
    }

    /**
     * CLS-03 반 상세 조회 (반 정보 + 수강 학생 목록)
     */
    @Override
    public ClassDetailResponse getClass(Long classId) {
        ClassDto clazz = findClassOrThrow(classId);
        return new ClassDetailResponse(
                ClassResponse.from(clazz),
                classMapper.selectClassStudents(classId)
        );
    }

    /**
     * CLS-04 반 수정 (반명/담당강사/과목/강의실/시간/상태)
     *
     * [추가 2026-07-14] 반 종료 시 수강 일괄 종료 (팀 결정 정책)
     * - ACTIVE→CLOSED 전이 시에만 해당 반의 수강중(ACTIVE) 수강을 전부 ENDED 처리 (종료일 = 처리 당일)
     * - 반 수정과 같은 트랜잭션이므로 중간 실패 시 전체 롤백
     * - CLOSED→ACTIVE 재개 시 종료된 수강은 복구하지 않음 (이력 보존, 필요 시 수강관리에서 재등록)
     */
    @Override
    @Transactional
    public ClassResponse updateClass(Long classId, ClassUpdateRequest request) {
        ClassDto clazz = findClassOrThrow(classId);
        validateTeacher(request.teacherId());

        // 상태 전이 감지: 기존 ACTIVE → 요청 CLOSED일 때만 수강 일괄 종료 대상
        boolean closing = "ACTIVE".equals(clazz.getStatusCode())
                && "CLOSED".equals(request.statusCode());

        clazz.setClassName(request.className());
        clazz.setTeacherId(request.teacherId());
        clazz.setSubject(request.subject());
        clazz.setClassroom(request.classroom());
        clazz.setStartTime(request.startTime());
        clazz.setEndTime(request.endTime());
        clazz.setStatusCode(request.statusCode());
        classMapper.updateClass(clazz);

        if (closing) {
            enrollmentMapper.endAllActiveByClassId(classId, LocalDate.now());
        }

        return ClassResponse.from(classMapper.selectByClassId(classId));
    }

    /** 반 존재 확인, 없으면 404 */
    private ClassDto findClassOrThrow(Long classId) {
        ClassDto clazz = classMapper.selectByClassId(classId);
        if (clazz == null) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "반을 찾을 수 없습니다. classId=" + classId);
        }
        return clazz;
    }

    /** 담당 강사 존재 확인 (null이면 미지정으로 통과), 없으면 404 */
    private void validateTeacher(Long teacherId) {
        if (teacherId != null && teacherMapper.selectByTeacherId(teacherId) == null) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "강사를 찾을 수 없습니다. teacherId=" + teacherId);
        }
    }
}
