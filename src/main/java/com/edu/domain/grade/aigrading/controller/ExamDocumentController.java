package com.edu.domain.grade.aigrading.controller;

import com.edu.domain.grade.aigrading.dto.response.ExamDocumentResponse;
import com.edu.domain.grade.aigrading.service.ExamDocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 시험자료(문제지/정답지) 업로드 REST API (AIG-01)
 * base path는 팀 결정(/api 통일, [[edubridge-api-prefix-decision]])에 따라 /api로 고정.
 */
@RestController
@RequestMapping("/api")
public class ExamDocumentController {

    private final ExamDocumentService examDocumentService;

    public ExamDocumentController(ExamDocumentService examDocumentService) {
        this.examDocumentService = examDocumentService;
    }

    /** AIG-01 POST /api/exams/{examId}/documents - 시험자료 업로드 (문제지/정답지, multipart 복수 가능) */
    @PostMapping("/exams/{examId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public List<ExamDocumentResponse> uploadDocuments(@PathVariable Long examId,
                                                       @RequestParam("documentType") String documentType,
                                                       @RequestParam("files") List<MultipartFile> files,
                                                       Authentication authentication) {
        return examDocumentService.uploadDocuments(examId, documentType, files, authentication);
    }
}
