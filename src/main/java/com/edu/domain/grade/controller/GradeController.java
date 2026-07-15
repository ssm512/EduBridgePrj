package com.edu.domain.grade.controller;

import com.edu.domain.grade.mapper.GradeMapper;
import com.edu.domain.grade.vo.ExamVo;
import com.edu.domain.grade.vo.GradeVo;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.mapper.UserMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
        examVo.setTotalScore(limitToHundred(examVo.getTotalScore())); // 만점은 최대 100점까지만 허용
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
        examVo.setTotalScore(limitToHundred(examVo.getTotalScore())); // 수정 시에도 만점 상한을 동일하게 적용
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
    @Transactional
    public GradeVo insertGrade(@RequestBody GradeVo gradeVo) {
        prepareGradeScore(gradeVo);
        gradeMapper.insertGrade(gradeVo);
        gradeMapper.updateGradeRanksByExam(gradeVo.getExamId());
        return gradeVo;
    }

    // 성적 일괄 저장
    // 여러 학생의 점수를 한 번에 저장한 뒤 해당 시험 석차를 한 번만 다시 계산한다.
    @PostMapping("/grades/batch")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Transactional
    public String saveGradesBatch(@RequestBody List<GradeVo> gradeRows) {
        if (gradeRows == null || gradeRows.isEmpty()) {
            return "저장할 성적 없음";
        }

        Long examId = gradeRows.get(0).getExamId();
        for (GradeVo gradeVo : gradeRows) {
            if (gradeVo.getExamId() == null) {
                gradeVo.setExamId(examId);
            }
            prepareGradeScore(gradeVo);

            if (gradeVo.getGradeId() == null) {
                gradeMapper.insertGrade(gradeVo);
            } else {
                gradeMapper.updateGrade(gradeVo);
            }
        }

        gradeMapper.updateGradeRanksByExam(examId);
        return "성적 일괄 저장 완료";
    }

    // 성적 수정
    @PutMapping("/grades/{gradeId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Transactional
    public String updateGrade(@PathVariable Long gradeId, @RequestBody GradeVo gradeVo) {
        gradeVo.setGradeId(gradeId);
        if (gradeVo.getExamId() == null) {
            gradeVo.setExamId(gradeMapper.selectExamIdByGradeId(gradeId));
        }
        prepareGradeScore(gradeVo);
        gradeMapper.updateGrade(gradeVo);
        gradeMapper.updateGradeRanksByExam(gradeVo.getExamId());
        return "성적 수정 완료";
    }

    // 성적 삭제
    @DeleteMapping("/grades/{gradeId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Transactional
    public String deleteGrade(@PathVariable Long gradeId) {
        Long examId = gradeMapper.selectExamIdByGradeId(gradeId);
        gradeMapper.deleteGrade(gradeId);
        if (examId != null) {
            gradeMapper.updateGradeRanksByExam(examId);
        }
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
                                                Authentication authentication,
                                                @RequestParam(required = false) String subject) {
        validateStudentGradeAccess(studentId, authentication);
        return gradeMapper.selectGradeTrend(studentId, subject);
    }

    // 학생 본인 성적 목록 조회
    @GetMapping("/grades/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<Map<String, Object>> myGrades(Authentication authentication,
                                              @RequestParam(required = false) String subject) {
        Long studentId = findLoginStudentId(authentication);
        return gradeMapper.selectStudentGradeList(studentId, subject);
    }

    // 학생 본인 성적 추이 과목 목록 조회
    @GetMapping("/grades/my/subjects")
    @PreAuthorize("hasRole('STUDENT')")
    public List<Map<String, Object>> myGradeSubjects(Authentication authentication) {
        Long studentId = findLoginStudentId(authentication);
        return gradeMapper.selectTrendSubjectOptions(studentId, null);
    }

    // 학생 본인 성적 추이 조회
    @GetMapping("/grades/my/trends")
    @PreAuthorize("hasRole('STUDENT')")
    public List<Map<String, Object>> myGradeTrends(Authentication authentication,
                                                   @RequestParam(required = false) String subject) {
        Long studentId = findLoginStudentId(authentication);
        return gradeMapper.selectGradeTrend(studentId, subject);
    }

    // 학부모에게 연결된 자녀 목록 조회
    @GetMapping("/grades/children")
    @PreAuthorize("hasRole('PARENT')")
    public List<Map<String, Object>> myChildren(Authentication authentication) {
        return gradeMapper.selectChildrenByParentLoginId(authentication.getName());
    }

    // 학부모 자녀 성적 목록 조회
    @GetMapping("/grades/children/{studentId}")
    @PreAuthorize("hasRole('PARENT')")
    public List<Map<String, Object>> childGrades(@PathVariable Long studentId,
                                                 Authentication authentication,
                                                 @RequestParam(required = false) String subject) {
        validateParentChildAccess(studentId, authentication);
        return gradeMapper.selectStudentGradeList(studentId, subject);
    }

    // 학부모 자녀 성적 과목 목록 조회
    @GetMapping("/grades/children/{studentId}/subjects")
    @PreAuthorize("hasRole('PARENT')")
    public List<Map<String, Object>> childGradeSubjects(@PathVariable Long studentId,
                                                        Authentication authentication) {
        validateParentChildAccess(studentId, authentication);
        return gradeMapper.selectTrendSubjectOptions(studentId, null);
    }

    // 학부모 자녀 성적 추이 조회
    @GetMapping("/grades/children/{studentId}/trends")
    @PreAuthorize("hasRole('PARENT')")
    public List<Map<String, Object>> childGradeTrends(@PathVariable Long studentId,
                                                      Authentication authentication,
                                                      @RequestParam(required = false) String subject) {
        validateParentChildAccess(studentId, authentication);
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

    // 성적 추이 과목 선택 목록 조회
    // 학생 또는 반을 선택한 뒤 실제 성적이 있는 과목만 토글 목록에 보여준다.
    @GetMapping("/grades/trend-subjects")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> trendSubjectOptions(@RequestParam(required = false) Long studentId,
                                                         @RequestParam(required = false) Long classId) {
        return gradeMapper.selectTrendSubjectOptions(studentId, classId);
    }

    // 등록된 시험 과목 선택 목록 조회
    // 관리자/강사가 과목명을 직접 입력하지 않고 현재 등록된 과목 중에서 선택할 때 사용한다.
    @GetMapping("/grades/subject-options")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> subjectOptions() {
        return gradeMapper.selectExamSubjectOptions();
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
                                                     @RequestParam(required = false) Long classId,
                                                     @RequestParam(required = false) Long studentId) {
        return gradeMapper.selectGradesBySubject(subject, classId, studentId);
    }

    // 선택한 시험의 수강 학생 목록과 성적 입력 상태 조회
    @GetMapping("/exams/{examId}/students")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> studentGradeRowsByExam(@PathVariable Long examId) {
        return gradeMapper.selectStudentGradeRowsByExam(examId);
    }

    // 등록된 반 선택 목록 조회
    @GetMapping("/grades/classes")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> classOptions() {
        return gradeMapper.selectClassOptions();
    }

    // 수강중인 학생 선택 목록 조회
    @GetMapping("/grades/students")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> activeStudentOptions() {
        return gradeMapper.selectActiveStudentOptions();
    }

    // 점수 입력값은 화면과 서버 양쪽에서 최대 100점까지만 허용한다.
    private BigDecimal limitToHundred(BigDecimal score) {
        if (score == null) {
            return null;
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (score.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BigDecimal.valueOf(100);
        }
        return score;
    }

    // 석차는 저장된 점수 기준으로 자동 계산하므로 요청값을 받지 않는다.
    private void prepareGradeScore(GradeVo gradeVo) {
        gradeVo.setScore(limitToHundred(gradeVo.getScore()));
        gradeVo.setRankNo(0);
    }

    // 로그인한 학생의 student_id를 찾는다.
    private Long findLoginStudentId(Authentication authentication) {
        Long studentId = gradeMapper.selectStudentIdByLoginId(authentication.getName());
        if (studentId == null) {
            throw new AccessDeniedException("학생 정보가 연결되어 있지 않습니다.");
        }
        return studentId;
    }

    // 학생/학부모 성적 조회는 본인 또는 연결된 자녀만 허용한다.
    private void validateStudentGradeAccess(Long studentId, Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN") || hasRole(authentication, "ROLE_TEACHER")) {
            return;
        }

        if (hasRole(authentication, "ROLE_STUDENT")) {
            Long loginStudentId = findLoginStudentId(authentication);
            if (studentId.equals(loginStudentId)) {
                return;
            }
        }

        if (hasRole(authentication, "ROLE_PARENT")) {
            validateParentChildAccess(studentId, authentication);
            return;
        }

        throw new AccessDeniedException("성적 조회 권한이 없습니다.");
    }

    // 학부모가 해당 학생의 보호자인지 확인한다.
    private void validateParentChildAccess(Long studentId, Authentication authentication) {
        if (!gradeMapper.existsParentChildByLoginId(authentication.getName(), studentId)) {
            throw new AccessDeniedException("자녀 성적 조회 권한이 없습니다.");
        }
    }

    // Authentication 권한 보유 여부 확인
    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }








}
