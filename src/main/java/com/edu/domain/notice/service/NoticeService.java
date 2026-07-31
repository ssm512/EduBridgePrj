package com.edu.domain.notice.service;

import com.edu.common.dto.PageResponse;
import com.edu.domain.notice.dto.request.NoticeCreateRequest;
import com.edu.domain.notice.dto.request.NoticeSearchRequest;
import com.edu.domain.notice.dto.request.NoticeUpdateRequest;
import com.edu.domain.notice.dto.response.NoticeDetailResponse;
import com.edu.domain.notice.dto.response.NoticeFileResponse;
import com.edu.domain.notice.dto.response.NoticeResponse;
import com.edu.domain.notice.vo.NoticeFileVo;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 공지사항 서비스 (명세서 NOT-01 ~ NOT-08)
 */
public interface NoticeService {

    /** NOT-01 공지 등록 (작성자 = 현재 로그인 사용자, 대상 지정 포함) */
    NoticeResponse createNotice(NoticeCreateRequest request, Authentication authentication);

    /** NOT-02 대상자별 공지 목록 조회 (롤별 가시성 적용) */
    PageResponse<NoticeResponse> getNotices(NoticeSearchRequest cond, Authentication authentication);

    /** NOT-03 공지 상세 조회 (본문/대상/첨부/읽음 여부, 대상자 아니면 403) */
    NoticeDetailResponse getNotice(Long noticeId, Authentication authentication);

    /** NOT-04 공지 수정 (ADMIN 또는 작성자 본인만) */
    NoticeResponse updateNotice(Long noticeId, NoticeUpdateRequest request, Authentication authentication);

    /** NOT-05 공지 삭제 - 소프트 삭제 (ADMIN 또는 작성자 본인만) */
    void deleteNotice(Long noticeId, Authentication authentication);

    /** NOT-06 첨부파일 업로드 (ADMIN 또는 작성자 본인만) */
    List<NoticeFileResponse> uploadFiles(Long noticeId, List<MultipartFile> files, Authentication authentication);

    /** NOT-07 첨부파일 다운로드 - 접근 검증 후 파일 메타데이터 반환 (스트리밍은 컨트롤러) */
    NoticeFileVo getDownloadFile(Long fileId, Authentication authentication);

    /** NOT-08 공지 읽음 처리 (현재 사용자 기준 UPSERT) */
    void markRead(Long noticeId, Authentication authentication);

    /** 첨부파일 삭제 (명세서 외 - 첨부 관리용, ADMIN 또는 작성자 본인만) */
    void deleteFile(Long fileId, Authentication authentication);

    /**
     * 공지 대상(targetType)에 해당하는 실제 알림 수신자 user_id 목록 조회 (명세서 외 - 알림 연동용)
     * ALL이면 활성 회원 전체, CLASS면 수강생+학부모+담당강사까지 포함해서 반환한다.
     */
    List<Long> resolveTargetUserIds(Long noticeId, String targetType);
}
