package com.edu.domain.member.service;

import com.edu.common.dto.PageResponse;
import com.edu.domain.member.dto.parent.ParentCreateRequest;
import com.edu.domain.member.dto.parent.ParentResponse;
import com.edu.domain.member.dto.parent.ParentSearchRequest;
import com.edu.domain.member.dto.parent.ParentUpdateRequest;
import com.edu.domain.member.dto.parent.StudentParentLinkRequest;
import com.edu.domain.member.dto.parent.StudentParentUpdateRequest;

/**
 * 학부모관리 서비스 (PAR-01 ~ PAR-03)
 */
public interface ParentService {

    /** PAR-01 학부모 등록 (계정 + 학부모 상세) */
    ParentResponse createParent(ParentCreateRequest request);

    /** PAR-02 학부모 목록 조회 (키워드 + 페이징) */
    PageResponse<ParentResponse> getParents(ParentSearchRequest cond);

    /** 학부모 수정 (기본정보 + 주소) - 명세서 외 추가 API */
    ParentResponse updateParent(Long parentId, ParentUpdateRequest request);

    /**
     * PAR-03 학생-학부모 연결 (학부모는 로그인ID로 지정)
     * @return 생성된 studentParentId
     */
    Long linkParent(Long studentId, StudentParentLinkRequest request);

    /** 학생-학부모 연결 내역 수정 (관계코드/주보호자) - 명세서 외 추가 API */
    void updateParentLink(Long studentId, Long studentParentId, StudentParentUpdateRequest request);
}
