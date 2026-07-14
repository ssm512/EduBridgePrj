package com.edu.domain.grade.controller;

import com.edu.domain.grade.mapper.GradeMapper;
import com.edu.domain.grade.vo.ExamVo;
import com.edu.domain.grade.vo.GradeVo;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.mapper.UserMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 성적관리 API Controller
 * 시험 등록/조회/수정/삭제와 학생별 성적 관리를 담당한다.
 */
@RestController
@RequestMapping("/api")
public class GradeController {

    private final GradeMapper gradeMapper;
    private final UserMapper userMapper;

    public GradeController(GradeMapper gradeMapper, UserMapper userMapper) {
        this.gradeMapper = gradeMapper;
        this.userMapper = userMapper;
    }

    // 시험 등록
    @PostMapping("/exams")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ExamVo insertExam(@RequestBody ExamVo examVo, Authentication authentication) {
        UserDto loginUser = userMapper.selectByLoginId(authentication.getName());
        examVo.setCreatedBy(loginUser.getUserId());  // 현재 로그인한 사용자 ID를 등록자 ID로 저장
        gradeMapper.insertExam(examVo);
        return examVo;
    }

    // 시험 목록 조회
    // classId, subject는 선택 검색 조건
    @GetMapping("/exams")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> examList(@RequestParam(required = false) Long classId,
                                              @RequestParam(required = false) String subject) {
        return gradeMapper.selectExamList(classId, subject);
    }

    // 시험 정보 수정
    @PutMapping("/exams/{examId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public String updateExam(@PathVariable Long examId, @RequestBody ExamVo examVo) {
        examVo.setExamId(examId);
        gradeMapper.updateExam(examVo);
        return "시험 수정 완료";
    }

    // 시험 삭제
    @DeleteMapping("/exams/{examId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public String deleteExam(@PathVariable Long examId) {
        gradeMapper.deleteExam(examId);
        return "시험 삭제 완료";
    }

    // 성적 등록
    @PostMapping("/grades")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public GradeVo insertGrade(@RequestBody GradeVo gradeVo) {
        gradeMapper.insertGrade(gradeVo);
        return gradeVo;
    }

    // 성적 수정
    @PutMapping("/grades/{gradeId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public String updateGrade(@PathVariable Long gradeId, @RequestBody GradeVo gradeVo) {
        gradeVo.setGradeId(gradeId);
        gradeMapper.updateGrade(gradeVo);
        return "성적 수정 완료";
    }

    // 성적 삭제
    @DeleteMapping("/grades/{gradeId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public String deleteGrade(@PathVariable Long gradeId) {
        gradeMapper.deleteGrade(gradeId);
        return "성적 삭제 완료";
    }

    // 특정 시험에 입력된 성적 목록 조회
    @GetMapping("/exams/{examId}/grades")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> gradesByExam(@PathVariable Long examId) {
        return gradeMapper.selectGradesByExam(examId);
    }

    // 학생별 성적 추이 조회
    // 성적 차트 화면에서 사용
    @GetMapping("/grades/trends")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT','PARENT')")
    public List<Map<String, Object>> gradeTrend(@RequestParam Long studentId,
                                                @RequestParam(required = false) String subject) {
        return gradeMapper.selectGradeTrend(studentId, subject);
    }

    // 반별 성적 추이 조회
    // 선택한 반의 시험별 평균 점수 변화를 차트 화면에서 사용한다.
    @GetMapping("/grades/class-trends")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> classGradeTrend(@RequestParam Long classId,
                                                     @RequestParam(required = false) String subject) {
        return gradeMapper.selectClassGradeTrend(classId, subject);
    }

    // 반 평균 조회
    // classId를 비우면 전체 반 평균을 조회하고, subject를 입력하면 해당 과목만 평균에 포함한다.
    @GetMapping("/grades/class-averages")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> classAverageStats(@RequestParam(required = false) Long classId,
                                                       @RequestParam(required = false) String subject) {
        return gradeMapper.selectClassAverageStats(classId, subject);
    }

    // 과목별 성적 조회
    // subject 기준으로 시험, 반, 학생별 성적 목록을 조회한다.
    @GetMapping("/grades/subjects")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> gradesBySubject(@RequestParam String subject,
                                                     @RequestParam(required = false) Long classId) {
        return gradeMapper.selectGradesBySubject(subject, classId);
    }

    // 선택한 시험의 수강 학생 목록과 성적 입력 상태 조회
    @GetMapping("/exams/{examId}/students")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> studentGradeRowsByExam(@PathVariable Long examId) {
        return gradeMapper.selectStudentGradeRowsByExam(examId);
    }








}
