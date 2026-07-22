package com.edu.domain.grade.aigrading.service.impl;

import com.edu.common.exception.ApiException;
import com.edu.domain.ai.mapper.AiMapper;
import com.edu.domain.ai.vo.AiUsageLogVo;
import com.edu.domain.grade.aigrading.dto.request.AnswerReviewItemRequest;
import com.edu.domain.grade.aigrading.dto.request.ExamQuestionItemRequest;
import com.edu.domain.grade.aigrading.dto.response.AnswerResultResponse;
import com.edu.domain.grade.aigrading.dto.response.ExamDocumentResponse;
import com.edu.domain.grade.aigrading.dto.response.ExamQuestionExtractResponse;
import com.edu.domain.grade.aigrading.dto.response.ExamQuestionResponse;
import com.edu.domain.grade.aigrading.dto.response.ExamSubmissionResponse;
import com.edu.domain.grade.aigrading.dto.response.ExtractedQuestionItem;
import com.edu.domain.grade.aigrading.dto.response.GradeConfirmResponse;
import com.edu.domain.grade.aigrading.dto.response.SubmissionFileResponse;
import com.edu.domain.grade.aigrading.mapper.AiGradingMapper;
import com.edu.domain.grade.aigrading.service.AiExamGradingService;
import com.edu.domain.grade.aigrading.service.ExamGradingService;
import com.edu.domain.grade.aigrading.vo.AiGradingRunVo;
import com.edu.domain.grade.aigrading.vo.AnswerResultVo;
import com.edu.domain.grade.aigrading.vo.ExamDocumentVo;
import com.edu.domain.grade.aigrading.vo.ExamQuestionVo;
import com.edu.domain.grade.aigrading.vo.ExamSubmissionVo;
import com.edu.domain.grade.aigrading.vo.SubmissionFileVo;
import com.edu.domain.grade.mapper.GradeMapper;
import com.edu.domain.grade.vo.GradeVo;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.mapper.UserMapper;
import com.edu.domain.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI 시험지 자동채점(AIG-01~10) 통합 서비스 구현.
 *
 * 원래 ExamDocumentServiceImpl/ExamQuestionServiceImpl/ExamQuestionExtractServiceImpl/
 * ExamGradingRuleServiceImpl/ExamSubmissionServiceImpl 5개 클래스로 나뉘어 있던 걸 하나로 합쳤다(2026-07-22).
 * AiExamGradingServiceImpl(AIG-06 비동기 Gemini 채점 호출부)만 `@Async` 자기호출 문제 때문에 별도로 남아있다.
 */
@Service
public class ExamGradingServiceImpl implements ExamGradingService {

    // ── 공통 ──────────────────────────────────────────────────────────
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    // ── AIG-01 시험자료 업로드 ────────────────────────────────────────
    /** 코드정의 시트 EXAM_DOCUMENT_TYPE 그룹 */
    private static final Set<String> VALID_DOCUMENT_TYPES = Set.of("QUESTION", "ANSWER_KEY");

    // ── AIG-02 문항 자동 추출 ─────────────────────────────────────────
    private static final String EXTRACT_FEATURE_CODE = "GRADE_EXTRACT";
    private static final String MODEL_NAME_FALLBACK = "gemini-2.5-flash";
    private static final String EXTRACTION_PROMPT = """
            당신은 학원 시험지를 분석해 문항을 구조화하는 도우미입니다.
            첨부된 이미지는 시험 문제지 및 정답지입니다 (문제지가 먼저, 정답지가 있다면 뒤에 첨부됩니다).
            이미지 속 문항을 분석해 아래 JSON 배열 형식으로만 응답하세요. 다른 설명 문장은 포함하지 마세요.

            [
              {
                "questionNo": 1,
                "questionType": "MULTIPLE_CHOICE",
                "questionText": "문제 내용",
                "correctAnswer": "정답",
                "maxScore": 5,
                "gradingCriteria": "채점 기준 (서술형이 아니면 빈 문자열 가능)"
              }
            ]

            규칙:
            - questionType은 MULTIPLE_CHOICE(객관식), SHORT_ANSWER(단답형), ESSAY(서술형) 중 하나여야 합니다.
            - 정답지 이미지가 없어서 정답을 알 수 없으면 correctAnswer는 빈 문자열("")로 두세요.
            - 문항 번호(questionNo)는 문제지에 표기된 순서를 따르세요.
            - JSON 배열 외의 텍스트(설명, 코드블록 표시 등)는 절대 포함하지 마세요.
            """;

    // ── AIG-09 grades.score 스케일 반올림 ────────────────────────────
    /** grades.score 컬럼 정밀도 (NUMERIC(5,1)) - 소수 첫째자리까지 */
    private static final int GRADE_SCORE_SCALE = 1;

    // ── exam_submissions.status_code ─────────────────────────────────
    private static final String STATUS_UPLOADED = "UPLOADED";
    private static final String STATUS_ANALYZING = "ANALYZING";
    private static final String STATUS_REVIEW_REQUIRED = "REVIEW_REQUIRED";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String RUN_STATUS_RUNNING = "RUNNING";

    /** exam_submissions.status_code 허용 전이 (from -> 허용되는 to 목록) */
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            STATUS_UPLOADED, Set.of(STATUS_ANALYZING),
            STATUS_ANALYZING, Set.of(STATUS_REVIEW_REQUIRED, "FAILED"),
            STATUS_REVIEW_REQUIRED, Set.of(STATUS_CONFIRMED, STATUS_ANALYZING),
            "FAILED", Set.of(STATUS_ANALYZING),
            STATUS_CONFIRMED, Set.of()
    );

    private final AiGradingMapper aiGradingMapper;
    private final GradeMapper gradeMapper;
    private final AiMapper aiMapper;
    private final UserMapper userMapper;
    private final AiExamGradingService aiExamGradingService;

    /** 파일 저장 루트 (application.yaml part1.upload-path = ${UPLOAD_PATH}) - notice 도메인과 동일 설정값 재사용 */
    private final String uploadPath;

    /** AIG-02 추출 로그 기록용 모델명 표기 및 AIG-06 ai_grading_runs.model_name 기록용 - AiExamGradingServiceImpl과 동일 설정값 */
    private final String modelName;

    /**
     * AIG-02 문항 추출용 Gemini 응답 파싱 - Spring이 관리하는 ObjectMapper 빈을 주입받지 않고 직접 생성한다.
     * Spring Boot 4 / Spring 7부터는 기본 JSON 빈이 Jackson 3(tools.jackson.databind.ObjectMapper)이라
     * com.fasterxml.jackson.databind.ObjectMapper(Jackson 2)를 빈 주입받으려 하면 "No qualifying bean" 에러가 난다
     * (2026-07-21 로컬 실행 시 확인됨). 이 클래스에서만 쓰는 단순 파싱 용도라 직접 생성해 문제를 피한다.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();
    /**
     * ChatClient는 생성자에서 바로 만들지 않고(chatClientBuilder.build()) 첫 사용 시점에 지연 생성한다({@link #chatClient()}).
     * 이 클래스는 순수 검증 로직(sumConfirmedScores 등, 원 ExamGradingRuleService)도 함께 갖고 있는데,
     * 그 부분만 테스트하려고 생성자에 실제 ChatClient.Builder 빈을 넘길 필요가 없게 하기 위함이다
     * (ExamGradingServiceTest처럼 나머지 의존성을 전부 null로 넣고 인스턴스화해도 NPE 없이 동작해야 함).
     */
    private final ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private final String geminiApiKey;
    private final NotificationService notificationService;

    public ExamGradingServiceImpl(AiGradingMapper aiGradingMapper,
                                   GradeMapper gradeMapper,
                                   AiMapper aiMapper,
                                   UserMapper userMapper,
                                   AiExamGradingService aiExamGradingService,
                                   ChatClient.Builder chatClientBuilder,
                                   @Value("${part1.upload-path}") String uploadPath,
                                   @Value("${spring.ai.google.genai.api-key:}") String geminiApiKey,
                                   @Value("${spring.ai.google.genai.chat.model:gemini-2.5-flash}") String modelName,
                                   NotificationService notificationService) {
        this.aiGradingMapper = aiGradingMapper;
        this.gradeMapper = gradeMapper;
        this.aiMapper = aiMapper;
        this.userMapper = userMapper;
        this.aiExamGradingService = aiExamGradingService;
        this.chatClientBuilder = chatClientBuilder;
        this.uploadPath = uploadPath;
        this.geminiApiKey = geminiApiKey;
        this.modelName = (modelName == null || modelName.isBlank()) ? MODEL_NAME_FALLBACK : modelName;
        this.notificationService = notificationService;
    }

    /** AIG-02에서만 쓰는 ChatClient를 첫 호출 시 만들어 캐싱한다 (지연 생성 이유는 위 필드 주석 참고) */
    private ChatClient chatClient() {
        if (chatClient == null) {
            chatClient = chatClientBuilder.build();
        }
        return chatClient;
    }

    // =====================================================================
    // AIG-01 시험자료 업로드
    // =====================================================================

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
        if (!aiGradingMapper.existsExam(examId)) {
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
            aiGradingMapper.insertDocument(vo);   // examDocumentId 채워짐
            saved.add(ExamDocumentResponse.from(aiGradingMapper.selectByDocumentId(vo.getExamDocumentId())));
        }
        return saved;
    }

    // =====================================================================
    // AIG-02 문항 자동 추출
    // =====================================================================

    @Override
    public ExamQuestionExtractResponse extractQuestions(Long examId, String loginId) {
        if (!aiGradingMapper.existsExam(examId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 시험입니다. examId=" + examId);
        }

        List<ExamDocumentVo> documents = aiGradingMapper.selectDocumentsByExamId(examId);
        if (documents.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "시험자료(문제지/정답지)가 먼저 업로드되어야 합니다. AIG-01로 업로드해주세요.");
        }

        Long userId = resolveCurrentUserId(loginId);

        AiUsageLogVo log = new AiUsageLogVo();
        log.setUserId(userId);
        log.setFeatureCode(EXTRACT_FEATURE_CODE);
        log.setRequestPrompt(EXTRACTION_PROMPT + "\n[첨부 이미지 " + documents.size() + "장]");
        log.setModelName(modelName);

        String rawResponse;
        try {
            ensureGeminiKey();
            rawResponse = callGeminiForExtraction(documents);
            log.setResponseText(rawResponse);
            log.setSuccessYn("Y");
        } catch (Exception e) {
            log.setSuccessYn("N");
            log.setErrorMessage(e.getMessage());
            aiMapper.insertAiUsageLog(log);
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "AI 문항 추출에 실패했습니다. Gemini API 키/네트워크 상태를 확인해주세요.", e);
        }
        aiMapper.insertAiUsageLog(log);   // aiLogId 채워짐

        List<ExtractedQuestionItem> questions = parseQuestions(rawResponse);
        String status = questions.isEmpty() ? "FAILED" : "SUCCESS";
        return new ExamQuestionExtractResponse(questions, status, log.getAiLogId());
    }

    /** exam_documents.file_path(로컬 저장 경로)를 그대로 Resource로 읽어 Gemini에 전달한다 (멀티모달, 검증 완료) */
    private String callGeminiForExtraction(List<ExamDocumentVo> documents) {
        return chatClient().prompt()
                .user(userSpec -> {
                    userSpec.text(EXTRACTION_PROMPT);
                    for (ExamDocumentVo doc : documents) {
                        userSpec.media(MimeTypeUtils.parseMimeType(doc.getMimeType()),
                                new FileSystemResource(doc.getFilePath()));
                    }
                })
                .call()
                .content();
    }

    /** Gemini 응답을 JSON 배열로 파싱한다. 마크다운 코드펜스(```json ... ```)가 섞여 와도 벗겨내고 시도한다 */
    private List<ExtractedQuestionItem> parseQuestions(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return List.of();
        }
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            int lastFence = cleaned.lastIndexOf("```");
            if (firstNewline > -1 && lastFence > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastFence).trim();
            }
        }
        try {
            ExtractedQuestionItem[] items = objectMapper.readValue(cleaned, ExtractedQuestionItem[].class);
            return List.of(items);
        } catch (Exception e) {
            // 구조화 실패 - 예외를 던지지 않고 빈 목록으로 처리 (FAILED 상태로 응답, 화면에서 수동 입력 유도)
            return List.of();
        }
    }

    private void ensureGeminiKey() {
        if (geminiApiKey == null || geminiApiKey.isBlank() || "DUMMY_KEY".equals(geminiApiKey)) {
            throw new IllegalStateException("GEMINI_API_KEY 환경변수가 설정되어 있지 않습니다.");
        }
    }

    // =====================================================================
    // AIG-03/04 문항 조회/확정
    // =====================================================================

    @Override
    public List<ExamQuestionResponse> getQuestions(Long examId) {
        if (!aiGradingMapper.existsExam(examId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 시험입니다. examId=" + examId);
        }
        return aiGradingMapper.selectQuestionsByExamId(examId).stream()
                .map(ExamQuestionResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public List<ExamQuestionResponse> saveQuestions(Long examId, List<ExamQuestionItemRequest> items) {
        if (!aiGradingMapper.existsExam(examId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 시험입니다. examId=" + examId);
        }
        if (items == null || items.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "저장할 문항이 없습니다");
        }
        items.forEach(this::validateQuestionItem);
        validateNoDuplicateQuestionNo(items);
        if (aiGradingMapper.existsSubmissionForExam(examId)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "이미 학생 답안이 제출된 시험은 문항을 수정할 수 없습니다. examId=" + examId);
        }

        aiGradingMapper.deleteByExamId(examId);
        for (ExamQuestionItemRequest item : items) {
            ExamQuestionVo vo = ExamQuestionVo.builder()
                    .examId(examId)
                    .questionNo(item.getQuestionNo())
                    .questionType(item.getQuestionType())
                    .questionText(item.getQuestionText())
                    .correctAnswer(item.getCorrectAnswer())
                    .maxScore(item.getMaxScore())
                    .gradingCriteria(item.getGradingCriteria())
                    .reviewRequiredYn(resolveQuestionReviewRequired(item.getQuestionType(), item.getReviewRequiredYn()))
                    .sortOrder(item.getSortOrder())
                    .build();
            aiGradingMapper.insertQuestion(vo);
        }

        return getQuestions(examId);
    }

    /**
     * 문항 단건 필수값 검증.
     * 컨트롤러의 @Valid가 List&lt;T&gt; 바디에도 항목별로 적용되긴 하지만, List 파라미터에 대한
     * cascading 검증은 스프링 버전에 따라 동작이 갈릴 수 있어 서비스 레이어에서 한 번 더 방어한다.
     */
    private void validateQuestionItem(ExamQuestionItemRequest item) {
        if (item.getQuestionNo() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "문항 번호를 입력해주세요");
        }
        if (item.getQuestionType() == null
                || !Set.of("MULTIPLE_CHOICE", "SHORT_ANSWER", "ESSAY").contains(item.getQuestionType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "문항 유형은 MULTIPLE_CHOICE, SHORT_ANSWER, ESSAY 중 하나여야 합니다 (questionNo=" + item.getQuestionNo() + ")");
        }
        if (item.getQuestionText() == null || item.getQuestionText().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "문제 내용을 입력해주세요 (questionNo=" + item.getQuestionNo() + ")");
        }
        if (item.getCorrectAnswer() == null || item.getCorrectAnswer().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "정답을 입력해주세요 (questionNo=" + item.getQuestionNo() + ")");
        }
        if (item.getMaxScore() == null || item.getMaxScore().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "배점은 0 이상이어야 합니다 (questionNo=" + item.getQuestionNo() + ")");
        }
        if (item.getSortOrder() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "정렬 순서를 입력해주세요 (questionNo=" + item.getQuestionNo() + ")");
        }
        if (item.getReviewRequiredYn() != null
                && !item.getReviewRequiredYn().equals("Y") && !item.getReviewRequiredYn().equals("N")) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "검수 필요 여부는 Y 또는 N 이어야 합니다 (questionNo=" + item.getQuestionNo() + ")");
        }
    }

    /** 요청 목록 안에서 question_no가 중복되면 DB UNIQUE 제약 위반 전에 400으로 먼저 막는다 */
    private void validateNoDuplicateQuestionNo(List<ExamQuestionItemRequest> items) {
        Set<Integer> seen = new HashSet<>();
        for (ExamQuestionItemRequest item : items) {
            if (!seen.add(item.getQuestionNo())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "문항 번호가 중복되었습니다: " + item.getQuestionNo());
            }
        }
    }

    // =====================================================================
    // AIG-05 학생 답안지 업로드 / 상태 조회
    // =====================================================================

    @Override
    @Transactional
    public ExamSubmissionResponse uploadSubmission(Long examId, Long studentId, List<MultipartFile> files,
                                                    Authentication authentication) {
        if (studentId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "studentId는 필수입니다");
        }
        if (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "업로드할 파일이 없습니다");
        }
        if (!aiGradingMapper.existsExam(examId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 시험입니다. examId=" + examId);
        }
        if (!aiGradingMapper.existsStudent(studentId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 학생입니다. studentId=" + studentId);
        }

        Long uploaderId = resolveCurrentUserId(authentication);

        ExamSubmissionVo submission = aiGradingMapper.selectByExamAndStudent(examId, studentId);
        if (submission == null) {
            submission = ExamSubmissionVo.builder()
                    .examId(examId)
                    .studentId(studentId)
                    .statusCode(STATUS_UPLOADED)
                    .uploadedBy(uploaderId)
                    .build();
            aiGradingMapper.insertSubmission(submission);   // submissionId 채워짐
        } else if (!STATUS_UPLOADED.equals(submission.getStatusCode())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "이미 채점이 시작된 제출은 답안지를 다시 업로드할 수 없습니다. submissionId="
                            + submission.getSubmissionId() + ", statusCode=" + submission.getStatusCode());
        }

        Path dir = Paths.get(uploadPath, "submission-file");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "업로드 폴더를 만들 수 없습니다");
        }

        Integer maxPageNo = aiGradingMapper.selectMaxPageNo(submission.getSubmissionId());
        int pageNo = (maxPageNo != null ? maxPageNo : 0) + 1;

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

            SubmissionFileVo fileVo = SubmissionFileVo.builder()
                    .submissionId(submission.getSubmissionId())
                    .originalName(originalName)
                    .storedName(storedName)
                    .filePath(target.toAbsolutePath().toString())
                    .mimeType(file.getContentType() != null ? file.getContentType() : DEFAULT_MIME_TYPE)
                    .fileSize(file.getSize())
                    .pageNo(pageNo++)
                    .build();
            aiGradingMapper.insertFile(fileVo);
        }

        ExamSubmissionVo saved = aiGradingMapper.selectBySubmissionId(submission.getSubmissionId());
        return ExamSubmissionResponse.from(saved, buildFileResponses(saved.getSubmissionId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ExamSubmissionResponse getSubmission(Long submissionId) {
        ExamSubmissionVo submission = aiGradingMapper.selectBySubmissionId(submissionId);
        if (submission == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 제출입니다. submissionId=" + submissionId);
        }
        return ExamSubmissionResponse.from(submission, buildFileResponses(submissionId));
    }

    @Override
    @Transactional(readOnly = true)
    public ExamSubmissionResponse findSubmission(Long examId, Long studentId) {
        ExamSubmissionVo submission = aiGradingMapper.selectByExamAndStudent(examId, studentId);
        if (submission == null) {
            return null;
        }
        return ExamSubmissionResponse.from(submission, buildFileResponses(submission.getSubmissionId()));
    }

    private List<SubmissionFileResponse> buildFileResponses(Long submissionId) {
        List<SubmissionFileResponse> fileResponses = new ArrayList<>();
        for (SubmissionFileVo fileVo : aiGradingMapper.selectFilesBySubmissionId(submissionId)) {
            fileResponses.add(SubmissionFileResponse.from(fileVo));
        }
        return fileResponses;
    }

    // =====================================================================
    // AIG-06/10 AI 채점 실행 트리거
    // =====================================================================

    @Override
    @Transactional
    public ExamSubmissionResponse startGrading(Long submissionId, Authentication authentication) {
        ExamSubmissionVo submission = aiGradingMapper.selectBySubmissionId(submissionId);
        if (submission == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 제출입니다. submissionId=" + submissionId);
        }
        if (!isValidStatusTransition(submission.getStatusCode(), STATUS_ANALYZING)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "지금 상태(" + submission.getStatusCode() + ")에서는 채점을 시작할 수 없습니다. submissionId=" + submissionId);
        }

        int questionCount = aiGradingMapper.selectQuestionsByExamId(submission.getExamId()).size();
        if (questionCount == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "문항이 먼저 확정 저장되어야 합니다(AIG-04). examId=" + submission.getExamId());
        }
        int fileCount = aiGradingMapper.selectFilesBySubmissionId(submissionId).size();
        if (fileCount == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "답안지 파일이 먼저 업로드되어야 합니다(AIG-05). submissionId=" + submissionId);
        }

        Long triggeredByUserId = resolveCurrentUserId(authentication);

        aiGradingMapper.updateStatusCode(submissionId, STATUS_ANALYZING);

        Integer maxRunNo = aiGradingMapper.selectMaxRunNo(submissionId);
        int runNo = nextRunNo(maxRunNo);

        AiGradingRunVo run = AiGradingRunVo.builder()
                .submissionId(submissionId)
                .modelName(modelName)
                .runNo(runNo)
                .statusCode(RUN_STATUS_RUNNING)
                .requestSummary("문항 " + questionCount + "건, 답안이미지 " + fileCount + "장 채점 요청")
                .build();
        aiGradingMapper.insertRun(run);   // gradingRunId 채워짐

        // @Async는 새 스레드에서 즉시 시작될 수 있어, 이 메서드의 트랜잭션이 커밋되기 전에
        // 비동기 스레드가 방금 쓴 ANALYZING/RUNNING 행을 못 볼 위험이 있다(READ_COMMITTED에서
        // 커밋 전 데이터는 다른 트랜잭션에 안 보임). afterCommit 콜백으로 등록해 커밋이 끝난
        // 뒤에만 비동기 채점을 시작하도록 한다.
        Long gradingRunId = run.getGradingRunId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                aiExamGradingService.gradeSubmissionAsync(submissionId, gradingRunId, triggeredByUserId);
            }
        });

        ExamSubmissionVo updated = aiGradingMapper.selectBySubmissionId(submissionId);
        return ExamSubmissionResponse.from(updated, buildFileResponses(submissionId));
    }

    // =====================================================================
    // AIG-07/08/09 검수 / 최종 확정
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<AnswerResultResponse> getResults(Long submissionId) {
        ExamSubmissionVo submission = aiGradingMapper.selectBySubmissionId(submissionId);
        if (submission == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 제출입니다. submissionId=" + submissionId);
        }
        return buildResultResponses(submission);
    }

    @Override
    @Transactional
    public List<AnswerResultResponse> submitReview(Long submissionId, List<AnswerReviewItemRequest> items,
                                                     Authentication authentication) {
        ExamSubmissionVo submission = aiGradingMapper.selectBySubmissionId(submissionId);
        if (submission == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 제출입니다. submissionId=" + submissionId);
        }
        if (!STATUS_REVIEW_REQUIRED.equals(submission.getStatusCode())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "지금 상태(" + submission.getStatusCode() + ")에서는 검수를 저장할 수 없습니다. submissionId=" + submissionId);
        }
        if (items == null || items.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "저장할 검수 결과가 없습니다");
        }

        Map<Long, ExamQuestionVo> questionById = aiGradingMapper.selectQuestionsByExamId(submission.getExamId()).stream()
                .collect(Collectors.toMap(ExamQuestionVo::getQuestionId, q -> q));
        Map<Long, AnswerResultVo> resultById = aiGradingMapper.selectAnswerResultsBySubmissionId(submissionId).stream()
                .collect(Collectors.toMap(AnswerResultVo::getAnswerResultId, r -> r));

        for (AnswerReviewItemRequest item : items) {
            // List<T> 바디의 cascading @Valid는 스프링 버전별로 불확실해서(saveQuestions와 동일 사유)
            // 서비스 레이어에서 한 번 더 필수값을 확인한다.
            if (item.getAnswerResultId() == null || item.getConfirmedScore() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "answerResultId와 confirmedScore는 필수입니다");
            }
            AnswerResultVo existing = resultById.get(item.getAnswerResultId());
            if (existing == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "이 제출 건에 속하지 않는 채점 결과입니다. answerResultId=" + item.getAnswerResultId());
            }
            ExamQuestionVo question = questionById.get(existing.getQuestionId());
            BigDecimal maxScore = question != null ? question.getMaxScore() : null;
            if (item.getConfirmedScore().compareTo(BigDecimal.ZERO) < 0
                    || (maxScore != null && item.getConfirmedScore().compareTo(maxScore) > 0)) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "확정 점수는 0 이상 배점 이하여야 합니다. answerResultId=" + item.getAnswerResultId());
            }

            int updated = aiGradingMapper.updateTeacherReview(item.getAnswerResultId(), submissionId,
                    item.getConfirmedScore(), item.getTeacherComment());
            if (updated == 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "채점 결과 저장에 실패했습니다. answerResultId=" + item.getAnswerResultId());
            }
        }

        Long reviewerId = resolveCurrentUserId(authentication);
        aiGradingMapper.markReviewed(submissionId, reviewerId);

        return buildResultResponses(aiGradingMapper.selectBySubmissionId(submissionId));
    }

    @Override
    @Transactional
    public GradeConfirmResponse confirmGrade(Long submissionId, Authentication authentication) {
        ExamSubmissionVo submission = aiGradingMapper.selectBySubmissionId(submissionId);
        if (submission == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 제출입니다. submissionId=" + submissionId);
        }
        if (!isValidStatusTransition(submission.getStatusCode(), STATUS_CONFIRMED)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "지금 상태(" + submission.getStatusCode() + ")에서는 확정할 수 없습니다. submissionId=" + submissionId);
        }

        List<AnswerResultVo> results = aiGradingMapper.selectAnswerResultsBySubmissionId(submissionId);
        if (results.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "채점 결과가 없습니다. submissionId=" + submissionId);
        }

        // 검수 불필요 문항은 AI 점수를 자동 확정 점수로 채운다 (이미 채워져 있으면 건드리지 않음)
        for (AnswerResultVo result : results) {
            if ("N".equals(result.getReviewRequiredYn()) && result.getConfirmedScore() == null) {
                aiGradingMapper.confirmAiScoreIfAbsent(result.getAnswerResultId());
            }
        }
        results = aiGradingMapper.selectAnswerResultsBySubmissionId(submissionId);   // 방금 채운 값 반영해서 다시 조회

        List<BigDecimal> effectiveScores = new ArrayList<>();
        for (AnswerResultVo result : results) {
            if (result.getConfirmedScore() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "검수가 끝나지 않은 문항이 있습니다(answerResultId=" + result.getAnswerResultId()
                                + "). AIG-08로 먼저 검수해주세요.");
            }
            effectiveScores.add(result.getConfirmedScore());
        }

        BigDecimal summedScore = sumConfirmedScores(effectiveScores);
        BigDecimal examTotalScore = gradeMapper.selectExamTotalScore(submission.getExamId());
        if (!isWithinExamTotal(summedScore, examTotalScore)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "합산 점수(" + summedScore + ")가 시험 만점(" + examTotalScore + ")을 초과합니다. submissionId=" + submissionId);
        }
        BigDecimal gradeScore = roundToGradeScale(summedScore);

        Long confirmerId = resolveCurrentUserId(authentication);

        GradeVo existingGrade = gradeMapper.selectByExamAndStudent(submission.getExamId(), submission.getStudentId());
        boolean isUpdate = existingGrade != null;
        Long gradeId;
        if (existingGrade == null) {
            GradeVo grade = new GradeVo();
            grade.setExamId(submission.getExamId());
            grade.setStudentId(submission.getStudentId());
            grade.setScore(gradeScore);
            grade.setComment("AI 채점 확정 (submissionId=" + submissionId + ")");
            gradeMapper.insertGrade(grade);   // gradeId 채워짐
            gradeId = grade.getGradeId();
        } else {
            existingGrade.setScore(gradeScore);
            existingGrade.setComment("AI 채점 확정 (submissionId=" + submissionId + ")");
            gradeMapper.updateGrade(existingGrade);
            gradeId = existingGrade.getGradeId();
        }
        gradeMapper.updateGradeRanksByExam(submission.getExamId());
        notifyGradeSaved(submission.getExamId(), submission.getStudentId(), isUpdate);

        aiGradingMapper.confirmSubmission(ExamSubmissionVo.builder()
                .submissionId(submissionId)
                .statusCode(STATUS_CONFIRMED)
                .confirmedScore(gradeScore)
                .gradeId(gradeId)
                .reviewedBy(confirmerId)
                .build());

        ExamSubmissionVo confirmed = aiGradingMapper.selectBySubmissionId(submissionId);
        return new GradeConfirmResponse(submissionId, gradeId, gradeScore, confirmed.getStatusCode(), confirmed.getConfirmedAt());
    }

    /** AIG-07/08 공통 - 채점 결과 + 문항 정보를 조인해 응답 목록을 만든다 */
    private List<AnswerResultResponse> buildResultResponses(ExamSubmissionVo submission) {
        Map<Long, ExamQuestionVo> questionById = new HashMap<>();
        for (ExamQuestionVo q : aiGradingMapper.selectQuestionsByExamId(submission.getExamId())) {
            questionById.put(q.getQuestionId(), q);
        }
        List<AnswerResultResponse> responses = new ArrayList<>();
        for (AnswerResultVo result : aiGradingMapper.selectAnswerResultsBySubmissionId(submission.getSubmissionId())) {
            ExamQuestionVo question = questionById.get(result.getQuestionId());
            if (question != null) {
                responses.add(AnswerResultResponse.from(result, question));
            }
        }
        return responses;
    }

    // =====================================================================
    // 순수 검증/재계산 로직 (원 ExamGradingRuleService)
    // =====================================================================

    @Override
    public BigDecimal sumConfirmedScores(List<BigDecimal> questionScores) {
        if (questionScores == null || questionScores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal score : questionScores) {
            if (score != null) {
                sum = sum.add(score);
            }
        }
        return sum;
    }

    @Override
    public boolean isWithinExamTotal(BigDecimal summedScore, BigDecimal examTotalScore) {
        if (summedScore == null || examTotalScore == null) {
            return false;
        }
        return summedScore.compareTo(examTotalScore) <= 0;
    }

    @Override
    public BigDecimal roundToGradeScale(BigDecimal rawScore) {
        if (rawScore == null) {
            return null;
        }
        return rawScore.setScale(GRADE_SCORE_SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public boolean isReviewRequired(BigDecimal confidence, BigDecimal threshold) {
        if (confidence == null) {
            return true;   // 판독 불가 등으로 신뢰도 자체가 없으면 보수적으로 검수 필요 처리
        }
        if (threshold == null) {
            return true;   // 임계값 미설정 상태에서는 검수 없이 자동 확정되지 않도록 보수적으로 처리
        }
        return confidence.compareTo(threshold) < 0;
    }

    @Override
    public String resolveQuestionReviewRequired(String questionType, String requestedYn) {
        if ("ESSAY".equals(questionType)) {
            return "Y";
        }
        return "Y".equalsIgnoreCase(requestedYn) ? "Y" : "N";
    }

    @Override
    public int nextRunNo(Integer currentMaxRunNo) {
        if (currentMaxRunNo == null || currentMaxRunNo <= 0) {
            return 1;
        }
        return currentMaxRunNo + 1;
    }

    @Override
    public boolean isValidStatusTransition(String fromStatus, String toStatus) {
        if (fromStatus == null || toStatus == null) {
            return false;
        }
        Set<String> allowedNext = ALLOWED_TRANSITIONS.get(fromStatus);
        return allowedNext != null && allowedNext.contains(toStatus);
    }

    // =====================================================================
    // 공통 헬퍼
    // =====================================================================

    /**
     * 성적 입력/수정 알림 - 학생 본인 + 학부모 전원에게 발송한다.
     * "[학생이름 학생]의 [강의명] 강의 [시험명]시험 성적이 입력/수정되었습니다." 형태.
     * GradeController(수기 입력/수정)와 동일한 문구 규칙 - 파일이 달라(도메인 분리) 그대로 재사용은 못 하지만
     * 로직은 동일하게 맞춘다. GradeMapper.selectGradeNotificationContext로 필요한 정보를 한 번에 모은다.
     */
    private void notifyGradeSaved(Long examId, Long studentId, boolean isUpdate) {
        Map<String, Object> context = gradeMapper.selectGradeNotificationContext(examId, studentId);
        if (context == null) {
            return;
        }
        String studentName = String.valueOf(context.get("student_name"));
        String className = String.valueOf(context.get("class_name"));
        String examName = String.valueOf(context.get("exam_name"));
        String action = isUpdate ? "수정" : "입력";

        String title = isUpdate ? "성적 수정 안내" : "성적 입력 안내";
        String message = "[" + studentName + " 학생]의 [" + className + "] 강의 [" + examName + "]시험 성적이 "
                + action + "되었습니다.";

        notificationService.notifyStudentAndParents(studentId, "GRADE", title, message);
    }

    /** 로그인 ID(Authentication.getName())로 업로더 PK 조회 */
    private Long resolveCurrentUserId(Authentication authentication) {
        return resolveCurrentUserId(authentication.getName());
    }

    private Long resolveCurrentUserId(String loginId) {
        UserDto user = userMapper.selectByLoginId(loginId);
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
