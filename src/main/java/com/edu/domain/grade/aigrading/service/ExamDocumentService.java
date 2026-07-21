package com.edu.domain.grade.aigrading.service;

import com.edu.domain.grade.aigrading.dto.response.ExamDocumentResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 시험자료(문제지/정답지) 업로드 서비스 (AIG-01)
 */
public interface ExamDocumentService {

    /**
     * 시험자료 업로드. documentType은 QUESTION(문제지) 또는 ANSWER_KEY(정답지).
     * 여러 파일을 한 번에 올리면 업로드 순서대로 pageNo(1부터)를 채운다.
     */
    List<ExamDocumentResponse> uploadDocuments(Long examId, String documentType,
                                                List<MultipartFile> files, Authentication authentication);
}
