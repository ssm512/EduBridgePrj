package com.edu.domain.grade.aigrading.service.impl;

import com.edu.common.exception.ApiException;
import com.edu.domain.grade.aigrading.dto.response.ExamDocumentResponse;
import com.edu.domain.grade.aigrading.mapper.ExamDocumentMapper;
import com.edu.domain.grade.aigrading.service.ExamDocumentService;
import com.edu.domain.grade.aigrading.vo.ExamDocumentVo;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.mapper.UserMapper;
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
import java.util.Set;
import java.util.UUID;

/**
 * 시험자료 업로드 구현 (AIG-01).
 * notice_files 업로드 패턴(UUID storedName, {upload-path}/하위폴더 저장)을 그대로 재사용.
 */
@Service
public class ExamDocumentServiceImpl implements ExamDocumentService {

    /** 코드정의 시트 EXAM_DOCUMENT_TYPE 그룹 */
    private static final Set<String> VALID_DOCUMENT_TYPES = Set.of("QUESTION", "ANSWER_KEY");

    /** file.getContentType()이 null일 때(브라우저가 못 채웠을 때) mime_type NOT NULL 제약을 만족시키기 위한 기본값 */
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private final ExamDocumentMapper examDocumentMapper;
    private final UserMapper userMapper;

    /** 파일 저장 루트 (application.yaml part1.upload-path = ${UPLOAD_PATH}) - notice 도메인과 동일 설정값 재사용 */
    private final String uploadPath;

    public ExamDocumentServiceImpl(ExamDocumentMapper examDocumentMapper,
                                    UserMapper userMapper,
                                    @Value("${part1.upload-path}") String uploadPath) {
        this.examDocumentMapper = examDocumentMapper;
        this.userMapper = userMapper;
        this.uploadPath = uploadPath;
    }

    @Override
    @Transactional
    public List<ExamDocumentResponse> uploadDocuments(Long examId, String documentType,
                                                        List<MultipartFile> files, Authentication authentication) {
        if (documentType == null || !VALID_DOCUMENT_TYPES.contains(documentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "documentType은 QUESTION 또는 ANSWER_KEY여야 합니다");
        }
        if (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "업로드할 파일이 없습니다");
        }
        if (!examDocumentMapper.existsExam(examId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 시험입니다. examId=" + examId);
        }

        Long uploaderId = resolveCurrentUserId(authentication);

        Path dir = Paths.get(uploadPath, "exam-document");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "업로드 폴더를 만들 수 없습니다");
        }

        List<ExamDocumentResponse> saved = new ArrayList<>();
        int pageNo = 1;
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";
            String storedName = UUID.randomUUID() + extensionOf(originalName);
            Path target = dir.resolve(storedName);
            try {
                file.transferTo(target.toFile());
            } catch (IOException e) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "파일 저장에 실패했습니다: " + originalName);
            }

            ExamDocumentVo vo = ExamDocumentVo.builder()
                    .examId(examId)
                    .documentType(documentType)
                    .originalName(originalName)
                    .storedName(storedName)
                    .filePath(target.toAbsolutePath().toString())
                    .mimeType(file.getContentType() != null ? file.getContentType() : DEFAULT_MIME_TYPE)
                    .fileSize(file.getSize())
                    .pageNo(pageNo++)
                    .uploadedBy(uploaderId)
                    .build();
            examDocumentMapper.insertDocument(vo);   // examDocumentId 채워짐
            saved.add(ExamDocumentResponse.from(examDocumentMapper.selectByDocumentId(vo.getExamDocumentId())));
        }
        return saved;
    }

    /** 로그인 ID(Authentication.getName())로 업로더 PK 조회 - DiscountPolicyServiceImpl과 동일 패턴 */
    private Long resolveCurrentUserId(Authentication authentication) {
        UserDto user = userMapper.selectByLoginId(authentication.getName());
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다");
        }
        return user.getUserId();
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > -1 ? fileName.substring(dot) : "";
    }
}
