package com.edu.domain.member.controller;

import com.edu.common.dto.PageResponse;
import com.edu.domain.member.dto.parent.ParentCreateRequest;
import com.edu.domain.member.dto.parent.ParentResponse;
import com.edu.domain.member.dto.parent.ParentSearchRequest;
import com.edu.domain.member.dto.parent.ParentUpdateRequest;
import com.edu.domain.member.service.ParentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 학부모관리 API (명세서 PAR-01 ~ PAR-02)
 * PAR-03(학생-학부모 연결)은 URL이 /students/{id}/parents 이므로 StudentController에 있다.
 * 명세서 URL 그대로 /parents 매핑, 전체 ADMIN 전용
 */
@RestController
@RequestMapping("/parents")
public class ParentController {

    private final ParentService parentService;

    public ParentController(ParentService parentService) {

        this.parentService = parentService;
    }

    /**
     * PAR-01 POST /parents - 학부모 등록
     * 계정(users) + 학부모 상세(parents) 동시 등록
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ParentResponse createParent(@Valid @RequestBody ParentCreateRequest request) {
        return parentService.createParent(request);
    }

    /**
     * PAR-02 GET /parents - 학부모 목록 조회
     * 키워드(이름/로그인ID/연락처) + 페이징
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<ParentResponse> getParents(@ModelAttribute ParentSearchRequest cond) {
        return parentService.getParents(cond);
    }

    /**
     * PUT /parents/{parentId} - 학부모 수정 (명세서 외 추가 API)
     * 기본정보(users) + 주소(parents)
     */
    @PutMapping("/{parentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ParentResponse updateParent(@PathVariable Long parentId,
                                       @Valid @RequestBody ParentUpdateRequest request) {
        return parentService.updateParent(parentId, request);
    }
}
