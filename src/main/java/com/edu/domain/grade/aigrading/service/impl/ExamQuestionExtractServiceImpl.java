package com.edu.domain.grade.aigrading.service.impl;

import com.edu.common.exception.ApiException;
import com.edu.domain.ai.mapper.AiMapper;
import com.edu.domain.ai.vo.AiUsageLogVo;
import com.edu.domain.grade.aigrading.dto.response.ExamQuestionExtractResponse;
import com.edu.domain.grade.aigrading.dto.response.ExtractedQuestionItem;
import com.edu.domain.grade.aigrading.mapper.ExamDocumentMapper;
import com.edu.domain.grade.aigrading.service.ExamQuestionExtractService;
import com.edu.domain.grade.aigrading.vo.ExamDocumentVo;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.List;

/**
 * 문항 자동 추출 구현 (AIG-02).
 *
 * {@link #callGeminiForExtraction}의 멀티모달 호출(ChatClient .media(MimeType, Resource))은
 * 2026-07-21 로컬에서 실제 PDF 시험지로 업로드→추출까지 정상 동작 확인됨 (더 이상 미검증 아님).
 * 기존 AiServiceImpl(com.edu.domain.ai)은 텍스트 전용이었는데, 이 클래스가 이 프로젝트 최초의
 * Gemini 멀티모달(이미지/PDF) 연동이다.
 */
@Service
public class ExamQuestionExtractServiceImpl implements ExamQuestionExtractService {

    private static final String FEATURE_CODE = "GRADE_EXTRACT";
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

    private final ExamDocumentMapper examDocumentMapper;
    private final AiMapper aiMapper;
    private final UserMapper userMapper;
    /**
     * Spring이 관리하는 ObjectMapper 빈을 주입받지 않고 직접 생성한다.
     * Spring Boot 4 / Spring 7 부터는 기본 JSON 빈이 Jackson 3(tools.jackson.databind.ObjectMapper)이라
     * 여기서 쓰는 com.fasterxml.jackson.databind.ObjectMapper(Jackson 2)와 타입이 달라 DI가 안 된다
     * (2026-07-21 로컬 실행 시 "No qualifying bean of type ObjectMapper" 에러로 확인됨).
     * 이 클래스에서만 쓰는 단순 파싱 용도라 빈 주입 없이 직접 생성해 문제를 피한다.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatClient chatClient;
    private final String apiKey;
    private final String modelName;

    public ExamQuestionExtractServiceImpl(ExamDocumentMapper examDocumentMapper,
                                           AiMapper aiMapper,
                                           UserMapper userMapper,
                                           ChatClient.Builder chatClientBuilder,
                                           @Value("${spring.ai.google.genai.api-key:}") String apiKey,
                                           @Value("${spring.ai.google.genai.chat.model:gemini-2.5-flash}") String modelName) {
        this.examDocumentMapper = examDocumentMapper;
        this.aiMapper = aiMapper;
        this.userMapper = userMapper;
        this.chatClient = chatClientBuilder.build();
        this.apiKey = apiKey;
        this.modelName = (modelName == null || modelName.isBlank()) ? MODEL_NAME_FALLBACK : modelName;
    }

    @Override
    public ExamQuestionExtractResponse extractQuestions(Long examId, String loginId) {
        if (!examDocumentMapper.existsExam(examId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 시험입니다. examId=" + examId);
        }

        List<ExamDocumentVo> documents = examDocumentMapper.selectByExamId(examId);
        if (documents.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "시험자료(문제지/정답지)가 먼저 업로드되어야 합니다. AIG-01로 업로드해주세요.");
        }

        Long userId = resolveCurrentUserId(loginId);

        AiUsageLogVo log = new AiUsageLogVo();
        log.setUserId(userId);
        log.setFeatureCode(FEATURE_CODE);
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

    /**
     * ⚠ 미검증 - 로컬 실행 전 Spring AI 버전에 맞게 media() 오버로드/패키지를 확인할 것.
     * exam_documents.file_path(로컬 저장 경로)를 그대로 Resource로 읽어 Gemini에 전달한다.
     */
    private String callGeminiForExtraction(List<ExamDocumentVo> documents) {
        return chatClient.prompt()
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
        if (apiKey == null || apiKey.isBlank() || "DUMMY_KEY".equals(apiKey)) {
            throw new IllegalStateException("GEMINI_API_KEY 환경변수가 설정되어 있지 않습니다.");
        }
    }

    private Long resolveCurrentUserId(String loginId) {
        UserDto user = userMapper.selectByLoginId(loginId);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다");
        }
        return user.getUserId();
    }
}
