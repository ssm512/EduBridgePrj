package com.edu.domain.notice.service.impl;

import com.edu.common.dto.PageResponse;
import com.edu.common.exception.ApiException;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.mapper.UserMapper;
import com.edu.domain.notice.dto.request.NoticeCreateRequest;
import com.edu.domain.notice.dto.request.NoticeSearchRequest;
import com.edu.domain.notice.dto.request.NoticeUpdateRequest;
import com.edu.domain.notice.dto.response.NoticeDetailResponse;
import com.edu.domain.notice.dto.response.NoticeFileResponse;
import com.edu.domain.notice.dto.response.NoticeResponse;
import com.edu.domain.notice.dto.response.NoticeTargetResponse;
import com.edu.domain.notice.mapper.NoticeFileMapper;
import com.edu.domain.notice.mapper.NoticeMapper;
import com.edu.domain.notice.service.NoticeService;
import com.edu.domain.notice.vo.NoticeFileVo;
import com.edu.domain.notice.vo.NoticeQueryContext;
import com.edu.domain.notice.vo.NoticeVo;
import com.edu.domain.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 공지사항 서비스 구현 (NOT-01 ~ NOT-08)
 *
 * 권한 정책
 * - 등록: ADMIN/TEACHER (SecurityConfig + @PreAuthorize에서 차단)
 * - 수정/삭제/첨부 관리: ADMIN 또는 작성자 본인(TEACHER)만 → 아니면 403
 * - 목록/상세/다운로드/읽음: 롤별 가시성(NoticeMapper roleVisibility)으로 검증
 * - 삭제는 소프트 삭제(notice_status=DELETED) — 읽음/첨부 이력 보존
 */
@Service
@Transactional(readOnly = true)
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;
    private final NoticeFileMapper noticeFileMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    /** 첨부파일 저장 루트 (application.yaml part1.upload-path = ${UPLOAD_PATH}) */
    private final String uploadPath;

    public NoticeServiceImpl(NoticeMapper noticeMapper,
                             NoticeFileMapper noticeFileMapper,
                             UserMapper userMapper,
                             NotificationService notificationService,
                             @Value("${part1.upload-path}") String uploadPath) {
        this.noticeMapper = noticeMapper;
        this.noticeFileMapper = noticeFileMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.uploadPath = uploadPath;
    }

    /**
     * NOT-01 공지 등록.
     * targetType != ALL이면 targetIds 필수(400). notices + notice_targets를 한 트랜잭션으로 등록.
     */
    @Override
    @Transactional
    public NoticeResponse createNotice(NoticeCreateRequest request, Authentication authentication) {
        validateTargets(request.targetType(), request.targetIds());
        NoticeQueryContext ctx = resolveContext(authentication);

        NoticeVo notice = NoticeVo.builder()
                .title(request.title())
                .content(request.content())
                .writerId(ctx.getUserId())
                .targetType(request.targetType())
                .build();
        noticeMapper.insertNotice(notice);   // noticeId 채워짐

        if (!"ALL".equals(request.targetType())) {
            noticeMapper.insertTargets(notice.getNoticeId(), request.targetType(), request.targetIds());
        }

        notifyTargets(notice.getNoticeId(), request.targetType(), request.title(), request.content());

        return NoticeResponse.from(noticeMapper.selectDetail(notice.getNoticeId(), ctx));
    }

    /** NOT-02 대상자별 공지 목록 조회 */
    @Override
    public PageResponse<NoticeResponse> getNotices(NoticeSearchRequest cond, Authentication authentication) {
        NoticeQueryContext ctx = resolveContext(authentication);
        long totalCount = noticeMapper.countNotices(cond, ctx);
        List<NoticeResponse> items = noticeMapper.selectNotices(cond, ctx).stream()
                .map(NoticeResponse::from)
                .toList();
        return PageResponse.of(items, cond.getPage(), cond.getSize(), totalCount);
    }

    /** NOT-03 공지 상세 조회 (대상자 아니면 403) */
    @Override
    public NoticeDetailResponse getNotice(Long noticeId, Authentication authentication) {
        NoticeQueryContext ctx = resolveContext(authentication);
        checkReadable(noticeId, ctx);

        NoticeVo notice = noticeMapper.selectDetail(noticeId, ctx);
        return new NoticeDetailResponse(
                notice.getNoticeId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getWriterId(),
                notice.getWriterName(),
                notice.getWriterLoginId(),
                notice.getTargetType(),
                notice.getNoticeStatus(),
                notice.getReadYn() != null ? notice.getReadYn() : "N",
                notice.getReadCount(),
                notice.getCreatedAt(),
                notice.getUpdatedAt(),
                noticeMapper.selectTargets(noticeId).stream().map(NoticeTargetResponse::from).toList(),
                noticeFileMapper.selectFilesByNoticeId(noticeId).stream().map(NoticeFileResponse::from).toList()
        );
    }

    /** NOT-04 공지 수정 (대상 목록은 삭제 후 재등록으로 교체) */
    @Override
    @Transactional
    public NoticeResponse updateNotice(Long noticeId, NoticeUpdateRequest request, Authentication authentication) {
        validateTargets(request.targetType(), request.targetIds());
        NoticeQueryContext ctx = resolveContext(authentication);
        NoticeVo notice = findNoticeOrThrow(noticeId);
        checkWritable(notice, ctx);

        notice.setTitle(request.title());
        notice.setContent(request.content());
        notice.setTargetType(request.targetType());
        noticeMapper.updateNotice(notice);

        noticeMapper.deleteTargets(noticeId);
        if (!"ALL".equals(request.targetType())) {
            noticeMapper.insertTargets(noticeId, request.targetType(), request.targetIds());
        }
        return NoticeResponse.from(noticeMapper.selectDetail(noticeId, ctx));
    }

    /** NOT-05 공지 소프트 삭제 (이미 삭제된 공지는 409) */
    @Override
    @Transactional
    public void deleteNotice(Long noticeId, Authentication authentication) {
        NoticeQueryContext ctx = resolveContext(authentication);
        NoticeVo notice = findNoticeOrThrow(noticeId);
        checkWritable(notice, ctx);

        if ("DELETED".equals(notice.getNoticeStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 삭제된 공지입니다");
        }
        noticeMapper.updateStatus(noticeId, "DELETED");
    }

    /**
     * NOT-06 첨부파일 업로드.
     * {upload-path}/notice/ 아래에 UUID 파일명으로 저장하고 메타데이터를 기록한다.
     */
    @Override
    @Transactional
    public List<NoticeFileResponse> uploadFiles(Long noticeId, List<MultipartFile> files,
                                                Authentication authentication) {
        if (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "업로드할 파일이 없습니다");
        }
        NoticeQueryContext ctx = resolveContext(authentication);
        NoticeVo notice = findNoticeOrThrow(noticeId);
        checkWritable(notice, ctx);

        Path dir = Paths.get(uploadPath, "notice");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "업로드 폴더를 만들 수 없습니다");
        }

        List<NoticeFileResponse> saved = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";
            String storedName = UUID.randomUUID() + extensionOf(originalName);
            Path target = dir.resolve(storedName);
            try {
                file.transferTo(target.toFile());
            } catch (IOException e) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "파일 저장에 실패했습니다: " + originalName);
            }

            NoticeFileVo vo = NoticeFileVo.builder()
                    .noticeId(noticeId)
                    .originalName(originalName)
                    .storedName(storedName)
                    .filePath(target.toAbsolutePath().toString())
                    .fileSize(file.getSize())
                    .build();
            noticeFileMapper.insertFile(vo);   // fileId 채워짐
            saved.add(NoticeFileResponse.from(noticeFileMapper.selectByFileId(vo.getFileId())));
        }
        return saved;
    }

    /** NOT-07 첨부파일 다운로드 (공지 열람 권한이 있어야 다운로드 가능) */
    @Override
    public NoticeFileVo getDownloadFile(Long fileId, Authentication authentication) {
        NoticeFileVo file = noticeFileMapper.selectByFileId(fileId);
        if (file == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "첨부파일을 찾을 수 없습니다. fileId=" + fileId);
        }
        NoticeQueryContext ctx = resolveContext(authentication);
        checkReadable(file.getNoticeId(), ctx);
        return file;
    }

    /** NOT-08 읽음 처리 (대상자만 가능, UPSERT라 중복 호출해도 안전) */
    @Override
    @Transactional
    public void markRead(Long noticeId, Authentication authentication) {
        NoticeQueryContext ctx = resolveContext(authentication);
        checkReadable(noticeId, ctx);
        noticeMapper.upsertRead(noticeId, ctx.getUserId());
    }

    /** 첨부파일 삭제 (명세서 외) - DB 메타 삭제 후 디스크 파일도 제거 */
    @Override
    @Transactional
    public void deleteFile(Long fileId, Authentication authentication) {
        NoticeFileVo file = noticeFileMapper.selectByFileId(fileId);
        if (file == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "첨부파일을 찾을 수 없습니다. fileId=" + fileId);
        }
        NoticeQueryContext ctx = resolveContext(authentication);
        checkWritable(findNoticeOrThrow(file.getNoticeId()), ctx);

        noticeFileMapper.deleteFile(fileId);
        try {
            Files.deleteIfExists(Paths.get(file.getFilePath()));
        } catch (IOException ignored) {
            // 디스크 삭제 실패는 무시 (메타데이터는 이미 삭제됨)
        }
    }

    /**
     * 공지 대상(targetType)에 해당하는 실제 알림 수신자 user_id 목록 조회 (명세서 외 - 알림 연동용)
     * ALL이면 활성 회원 전체, CLASS면 수강생+학부모+담당강사까지 포함해서 반환한다.
     */
    @Override
    public List<Long> resolveTargetUserIds(Long noticeId, String targetType) {
        return noticeMapper.selectTargetUserIds(noticeId, targetType);
    }

    // ===== 내부 유틸 =====

    /**
     * 공지 등록 직후 대상자 전원에게 알림 생성.
     * notification 도메인은 이미 공개된 NotificationService.createNotification만 호출하고
     * notification 패키지 코드는 건드리지 않는다.
     */
    private void notifyTargets(Long noticeId, String targetType, String title, String content) {
        List<Long> targetUserIds = resolveTargetUserIds(noticeId, targetType);
        for (Long userId : targetUserIds) {
            notificationService.createNotification(userId, "NOTICE", title, content);
        }
    }

    /** 현재 로그인 사용자의 롤/PK 컨텍스트 구성 */
    private NoticeQueryContext resolveContext(Authentication authentication) {
        UserDto user = userMapper.selectByLoginId(authentication.getName());
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다");
        }
        NoticeQueryContext.NoticeQueryContextBuilder builder = NoticeQueryContext.builder()
                .userId(user.getUserId())
                .roleCode(user.getRoleCode());

        switch (user.getRoleCode()) {
            case "STUDENT" -> builder.studentId(noticeMapper.selectStudentIdByUserId(user.getUserId()));
            case "PARENT"  -> builder.parentId(noticeMapper.selectParentIdByUserId(user.getUserId()));
            case "TEACHER" -> builder.teacherId(noticeMapper.selectTeacherIdByUserId(user.getUserId()));
            default -> { /* ADMIN: 추가 정보 불필요 */ }
        }
        return builder.build();
    }

    /** 공지 존재 확인, 없으면 404 */
    private NoticeVo findNoticeOrThrow(Long noticeId) {
        NoticeVo notice = noticeMapper.selectByNoticeId(noticeId);
        if (notice == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "공지를 찾을 수 없습니다. noticeId=" + noticeId);
        }
        return notice;
    }

    /** 열람 권한 검증: ADMIN은 전체, 그 외는 롤별 가시성(SQL)으로 판단 → 아니면 403 */
    private void checkReadable(Long noticeId, NoticeQueryContext ctx) {
        findNoticeOrThrow(noticeId);
        if ("ADMIN".equals(ctx.getRoleCode())) return;
        if (!noticeMapper.existsVisibleNotice(noticeId, ctx)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "이 공지를 볼 수 있는 권한이 없습니다");
        }
    }

    /** 수정/삭제/첨부 관리 권한 검증: ADMIN 또는 작성자 본인만 → 아니면 403 */
    private void checkWritable(NoticeVo notice, NoticeQueryContext ctx) {
        if ("ADMIN".equals(ctx.getRoleCode())) return;
        if (notice.getWriterId().equals(ctx.getUserId())) return;
        throw new ApiException(HttpStatus.FORBIDDEN, "본인이 작성한 공지만 수정/삭제할 수 있습니다");
    }

    /** targetType != ALL인데 대상이 비어 있으면 400 */
    private void validateTargets(String targetType, List<Long> targetIds) {
        if (!"ALL".equals(targetType) && (targetIds == null || targetIds.isEmpty())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "targetType=" + targetType + "일 때는 targetIds가 1개 이상 필요합니다");
        }
    }

    /** 원본 파일명에서 확장자 추출 (없으면 빈 문자열) */
    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > -1 ? fileName.substring(dot) : "";
    }
}
