package com.edu.domain.notice.controller;

import com.edu.common.dto.PageResponse;
import com.edu.domain.notice.dto.request.NoticeCreateRequest;
import com.edu.domain.notice.dto.request.NoticeSearchRequest;
import com.edu.domain.notice.dto.request.NoticeUpdateRequest;
import com.edu.domain.notice.dto.response.NoticeDetailResponse;
import com.edu.domain.notice.dto.response.NoticeFileResponse;
import com.edu.domain.notice.dto.response.NoticeResponse;
import com.edu.domain.notice.service.NoticeService;
import com.edu.domain.notice.vo.NoticeFileVo;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 공지사항 API (명세서 NOT-01 ~ NOT-08)
 * /api/notices, /api/notice-files 매핑 (2026-07-14 API URL /api 프리픽스 통일)
 * 권한: 등록/수정/삭제/첨부업로드 ADMIN·TEACHER / 목록/상세/다운로드/읽음 4개 롤
 *      (TEACHER는 본인 작성 공지만 수정/삭제 가능 — 서비스 단 검증, 위반 시 403)
 */
@RestController
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    /**
     * NOT-01 POST /api/notices - 공지 등록 (대상 지정 포함)
     */
    @PostMapping("/api/notices")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public NoticeResponse createNotice(@Valid @RequestBody NoticeCreateRequest request,
                                       Authentication authentication) {
        return noticeService.createNotice(request, authentication);
    }

    /**
     * NOT-02 GET /api/notices - 대상자별 공지 목록 조회
     * 대상유형/키워드 + 페이징 (STUDENT/PARENT는 자기 대상 공지만 보임)
     */
    @GetMapping("/api/notices")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public PageResponse<NoticeResponse> getNotices(@ModelAttribute NoticeSearchRequest cond,
                                                   Authentication authentication) {
        return noticeService.getNotices(cond, authentication);
    }

    /**
     * NOT-03 GET /api/notices/{noticeId} - 공지 상세 조회 (본문/대상/첨부/읽음 여부)
     */
    @GetMapping("/api/notices/{noticeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public NoticeDetailResponse getNotice(@PathVariable Long noticeId,
                                          Authentication authentication) {
        return noticeService.getNotice(noticeId, authentication);
    }

    /**
     * NOT-04 PUT /api/notices/{noticeId} - 공지 수정 (제목/내용/대상)
     */
    @PutMapping("/api/notices/{noticeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public NoticeResponse updateNotice(@PathVariable Long noticeId,
                                       @Valid @RequestBody NoticeUpdateRequest request,
                                       Authentication authentication) {
        return noticeService.updateNotice(noticeId, request, authentication);
    }

    /**
     * NOT-05 DELETE /api/notices/{noticeId} - 공지 삭제 (소프트 삭제: 상태 변경)
     */
    @DeleteMapping("/api/notices/{noticeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public void deleteNotice(@PathVariable Long noticeId, Authentication authentication) {
        noticeService.deleteNotice(noticeId, authentication);
    }

    /**
     * NOT-06 POST /api/notices/{noticeId}/files - 공지 첨부파일 업로드 (multipart, 복수 가능)
     */
    @PostMapping("/api/notices/{noticeId}/files")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public List<NoticeFileResponse> uploadFiles(@PathVariable Long noticeId,
                                                @RequestParam("files") List<MultipartFile> files,
                                                Authentication authentication) {
        return noticeService.uploadFiles(noticeId, files, authentication);
    }

    /**
     * NOT-07 GET /api/notice-files/{fileId}/download - 공지 첨부파일 다운로드
     * 원본 파일명으로 attachment 다운로드 (한글 파일명 지원)
     */
    @GetMapping("/api/notice-files/{fileId}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId,
                                                 Authentication authentication) {
        NoticeFileVo file = noticeService.getDownloadFile(fileId, authentication);
        FileSystemResource resource = new FileSystemResource(file.getFilePath());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.getOriginalName(), java.nio.charset.StandardCharsets.UTF_8)
                                .build().toString())
                .body(resource);
    }

    /**
     * NOT-08 POST /api/notices/{noticeId}/read - 공지 읽음 처리 (UPSERT, 중복 호출 안전)
     */
    @PostMapping("/api/notices/{noticeId}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<Void> markRead(@PathVariable Long noticeId, Authentication authentication) {
        noticeService.markRead(noticeId, authentication);
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /api/notice-files/{fileId} - 첨부파일 삭제 (명세서 외 - 첨부 관리용)
     */
    @DeleteMapping("/api/notice-files/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public void deleteFile(@PathVariable Long fileId, Authentication authentication) {
        noticeService.deleteFile(fileId, authentication);
    }
}
