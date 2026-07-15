package com.edu.domain.counseling.controller;

import com.edu.domain.counseling.service.CounselingService;
import com.edu.domain.counseling.vo.CounselingVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/counseling")
public class CounselingController {

    private final CounselingService counselingService;

    public CounselingController(CounselingService counselingService) {
        this.counselingService = counselingService;
    }

    // 상담 등록
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public CounselingVo createCounseling(@RequestBody CounselingVo counselingVo,
                                         Authentication authentication) {
        return counselingService.createCounseling(counselingVo, authentication.getName());
    }

    // 상담 목록 조회
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> counselingList(@RequestParam(required = false) Long studentId,
                                                    @RequestParam(required = false) Long parentId,
                                                    @RequestParam(required = false) Long teacherId,
                                                    @RequestParam(required = false) String visibilityCode,
                                                    Authentication authentication) {
        return counselingService.getCounselingList(studentId, parentId, teacherId, visibilityCode, authentication.getName());
    }

    // 수강중인 학생 선택 목록 조회
    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> activeStudentOptions() {
        return counselingService.getActiveStudentOptions();
    }

    // 선택 학생의 연결 학부모 목록 조회
    @GetMapping("/students/{studentId}/parents")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> parentOptionsByStudent(@PathVariable Long studentId) {
        return counselingService.getParentOptionsByStudent(studentId);
    }

    // 수강중인 학생과 연결된 학부모 선택 목록 조회
    @GetMapping("/parents")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> activeParentOptions() {
        return counselingService.getActiveParentOptions();
    }

    // 등록된 강사 선택 목록 조회
    @GetMapping("/teachers")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<Map<String, Object>> teacherOptions() {
        return counselingService.getTeacherOptions();
    }

    // 상담 상세 조회
    @GetMapping("/{counselingId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Map<String, Object> counselingDetail(@PathVariable Long counselingId,
                                                Authentication authentication) {
        return counselingService.getCounselingDetail(counselingId, authentication.getName());
    }

    // 상담 수정
    @PutMapping("/{counselingId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public String updateCounseling(@PathVariable Long counselingId,
                                   @RequestBody CounselingVo counselingVo,
                                   Authentication authentication) {
        counselingService.updateCounseling(counselingId, counselingVo, authentication.getName());
        return "상담 수정 완료";
    }

    // 상담 삭제
    @DeleteMapping("/{counselingId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public String deleteCounseling(@PathVariable Long counselingId,
                                   Authentication authentication) {
        counselingService.deleteCounseling(counselingId, authentication.getName());
        return "상담 삭제 완료";
    }
}
