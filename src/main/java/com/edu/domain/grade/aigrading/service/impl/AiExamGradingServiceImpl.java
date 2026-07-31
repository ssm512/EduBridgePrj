package com.edu.domain.grade.aigrading.service.impl;

import com.edu.domain.ai.mapper.AiMapper;
import com.edu.domain.ai.vo.AiUsageLogVo;
import com.edu.domain.grade.aigrading.dto.response.AnswerGradingItem;
import com.edu.domain.grade.aigrading.mapper.AiGradingMapper;
import com.edu.domain.grade.aigrading.service.AiExamGradingService;
import com.edu.domain.grade.aigrading.service.ExamGradingService;
import com.edu.domain.grade.aigrading.vo.AiGradingRunVo;
import com.edu.domain.grade.aigrading.vo.AnswerResultVo;
import com.edu.domain.grade.aigrading.vo.ExamQuestionVo;
import com.edu.domain.grade.aigrading.vo.ExamSubmissionVo;
import com.edu.domain.grade.aigrading.vo.SubmissionFileVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AIG-06 AI 채점 실행의 실제 처리부 - Gemini 멀티모달 호출 + 결과 저장.
 * ExamGradingServiceImpl(AIG-02 문항 추출)의 멀티모달 호출 패턴을 그대로 재사용한다.
 *
 * {@link ExamGradingService}(mapper/service 통합체, 2026-07-22)와는 별도 빈으로 남아있다 - {@code @Async}는
 * 스프링 프록시를 거쳐야 동작하는데, ExamGradingServiceImpl 안에서 this.gradeSubmissionAsync()로 자기 자신을
 * 부르면 프록시를 우회해 동기로 실행돼버리기 때문이다(자기호출 문제). 그런데 이 클래스는 순수 채점 규칙
 * (sumConfirmedScores/isReviewRequired)을 ExamGradingService에서 빌려 쓰고, ExamGradingServiceImpl은
 * 반대로 AI 채점 실행을 이 클래스에 위임한다 - 서로가 서로를 생성자로 주입받는 순환 참조가 생긴다.
 * ExamGradingService 쪽을 {@code @Lazy}로 주입해 순환을 끊었다(둘 다 즉시주입이면 스프링 기동 시 순환참조 에러).
 *
 * ⚠ 미검증: AIG-02와 달리 이 클래스는 "이미지 여러 장 + 문항 목록 텍스트"를 한 번에 전달해
 * 문항별로 매칭된 JSON을 받아내는 더 복잡한 프롬프트다. 로컬에서 실제 학생 답안 이미지로
 * 1회 이상 검증 필요 (AIG-02처럼 프롬프트/파싱을 조정해야 할 수 있음).
 */
@Service
public class AiExamGradingServiceImpl implements AiExamGradingService {

    private static final String FEATURE_CODE = "GRADE_EXECUTE";
    private static final String MODEL_NAME_FALLBACK = "gemini-2.5-flash";

    /** exam_submissions/ai_grading_runs.error_message 컬럼(VARCHAR(1000)) 제약에 맞춰 자른다 */
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1000;

    private static final String GRADING_PROMPT_HEADER = """
            당신은 학원 시험 답안지를 채점하는 도우미입니다.
            첨부된 이미지는 한 학생의 답안지입니다(페이지 순서대로 첨부됨).
            아래 문항 목록의 정답/배점/채점기준을 참고해 학생이 각 문항에 적은 답을 인식하고 채점하세요.

            문항 목록:
            %s

            학생 답안 인식 및 채점 결과를 아래 JSON 배열 형식으로만 응답하세요. 다른 설명 문장은 포함하지 마세요.

            [
              {
                "questionId": 1,
                "recognizedAnswer": "학생이 적은 답 (판독 불가면 빈 문자열)",
                "score": 5,
                "resultCode": "CORRECT",
                "reason": "채점 근거 (부분점수/오답 사유 등)",
                "confidence": 0.95
              }
            ]

            규칙:
            - questionId는 위 문항 목록에 있는 값을 그대로 사용하세요 (모든 문항에 대해 1건씩 응답).
            - resultCode는 CORRECT(정답), PARTIAL(부분점수), INCORRECT(오답), UNREADABLE(답안 판독 불가) 중 하나여야 합니다.
            - score는 0 이상 해당 문항 배점 이하여야 합니다. UNREADABLE이면 score는 0으로 하세요.
            - confidence는 0과 1 사이의 소수로, 채점 결과에 대한 스스로의 확신도를 나타냅니다.
            - JSON 배열 외의 텍스트(설명, 코드블록 표시 등)는 절대 포함하지 마세요.
            """;

    private final AiGradingMapper aiGradingMapper;
    private final AiMapper aiMapper;
    private final ExamGradingService examGradingService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatClient chatClient;
    private final String apiKey;
    private final String modelName;
    /** 검수 필요 여부 판정 임계값. 팀 확인 전까지 0.70을 임시 기본값으로 둔다([[edubridge-ai-grading-plan]] 참고) */
    private final BigDecimal confidenceThreshold;

    public AiExamGradingServiceImpl(AiGradingMapper aiGradingMapper,
                                     AiMapper aiMapper,
                                     @Lazy ExamGradingService examGradingService,
                                     ChatClient.Builder chatClientBuilder,
                                     @Value("${spring.ai.google.genai.api-key:}") String apiKey,
                                     @Value("${spring.ai.google.genai.chat.model:gemini-2.5-flash}") String modelName,
                                     @Value("${grade.ai.confidence-threshold:0.70}") String confidenceThreshold) {
        this.aiGradingMapper = aiGradingMapper;
        this.aiMapper = aiMapper;
        this.examGradingService = examGradingService;
        this.chatClient = chatClientBuilder.build();
        this.apiKey = apiKey;
        this.modelName = (modelName == null || modelName.isBlank()) ? MODEL_NAME_FALLBACK : modelName;
        this.confidenceThreshold = new BigDecimal(confidenceThreshold);
    }

    @Override
    @Async
    @Transactional
    public void gradeSubmissionAsync(Long submissionId, Long gradingRunId, Long triggeredByUserId) {
        Long aiLogId = null;
        try {
            ExamSubmissionVo submission = aiGradingMapper.selectBySubmissionId(submissionId);
            List<ExamQuestionVo> questions = aiGradingMapper.selectQuestionsByExamId(submission.getExamId());
            List<SubmissionFileVo> files = aiGradingMapper.selectFilesBySubmissionId(submissionId);

            AiUsageLogVo log = new AiUsageLogVo();
            log.setUserId(triggeredByUserId);
            log.setFeatureCode(FEATURE_CODE);
            log.setModelName(modelName);
            log.setRequestPrompt("[submissionId=" + submissionId + ", 문항 " + questions.size()
                    + "건, 답안이미지 " + files.size() + "장]");

            String rawResponse;
            try {
                ensureGeminiKey();
                rawResponse = callGeminiForGrading(questions, files);
                log.setResponseText(rawResponse);
                log.setSuccessYn("Y");
            } catch (Exception e) {
                log.setSuccessYn("N");
                log.setErrorMessage(e.getMessage());
                aiMapper.insertAiUsageLog(log);
                aiLogId = log.getAiLogId();
                throw e;
            }
            aiMapper.insertAiUsageLog(log);
            aiLogId = log.getAiLogId();

            List<AnswerGradingItem> items = parseGradingItems(rawResponse);
            if (items.isEmpty()) {
                throw new IllegalStateException("Gemini 응답을 채점 결과로 구조화하지 못했습니다");
            }

            Map<Long, ExamQuestionVo> questionById = questions.stream()
                    .collect(Collectors.toMap(ExamQuestionVo::getQuestionId, Function.identity()));

            // 재채점(AIG-10)이면 이전 실행의 answer_results가 이미 있을 수 있다 - uk_answer_submission_question
            // UNIQUE 제약 위반을 피하기 위해 새 결과를 넣기 전에 이전 결과를 지운다.
            aiGradingMapper.deleteAnswerResultsBySubmissionId(submissionId);

            List<BigDecimal> scores = new ArrayList<>();
            List<BigDecimal> confidences = new ArrayList<>();
            for (AnswerGradingItem item : items) {
                ExamQuestionVo question = questionById.get(item.questionId());
                if (question == null) {
                    continue;   // Gemini가 questionId를 잘못 echo한 경우 방어적으로 건너뜀
                }
                boolean reviewNeeded = "ESSAY".equals(question.getQuestionType())
                        || examGradingService.isReviewRequired(item.confidence(), confidenceThreshold);

                AnswerResultVo resultVo = AnswerResultVo.builder()
                        .submissionId(submissionId)
                        .questionId(question.getQuestionId())
                        .recognizedAnswer(item.recognizedAnswer())
                        .aiScore(item.score())
                        .resultCode(item.resultCode())
                        .aiReason(item.reason())
                        .confidence(item.confidence())
                        .reviewRequiredYn(reviewNeeded ? "Y" : "N")
                        .build();
                aiGradingMapper.insertResult(resultVo);
                scores.add(item.score());
                confidences.add(item.confidence());
            }

            if (scores.isEmpty()) {
                throw new IllegalStateException("문항과 매칭되는 채점 결과가 없습니다 (questionId 불일치)");
            }

            BigDecimal totalScore = examGradingService.sumConfirmedScores(scores);
            BigDecimal avgConfidence = averageConfidence(confidences);

            aiGradingMapper.completeGrading(ExamSubmissionVo.builder()
                    .submissionId(submissionId)
                    .statusCode("REVIEW_REQUIRED")
                    .aiTotalScore(totalScore)
                    .aiConfidence(avgConfidence)
                    .build());

            aiGradingMapper.updateRunResult(AiGradingRunVo.builder()
                    .gradingRunId(gradingRunId)
                    .statusCode("SUCCESS")
                    .rawResponse(rawResponse)
                    .aiLogId(aiLogId)
                    .build());
        } catch (Exception e) {
            handleFailure(submissionId, gradingRunId, aiLogId, e);
        }
    }

    /** 실패 처리 - 부분 저장된 answer_results를 정리하고 FAILED 상태로 반영한다 */
    private void handleFailure(Long submissionId, Long gradingRunId, Long aiLogId, Exception e) {
        aiGradingMapper.deleteAnswerResultsBySubmissionId(submissionId);

        String errorMessage = truncate(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());

        aiGradingMapper.completeGrading(ExamSubmissionVo.builder()
                .submissionId(submissionId)
                .statusCode("FAILED")
                .errorMessage(errorMessage)
                .build());

        aiGradingMapper.updateRunResult(AiGradingRunVo.builder()
                .gradingRunId(gradingRunId)
                .statusCode("FAILED")
                .errorMessage(errorMessage)
                .aiLogId(aiLogId)
                .build());
    }

    /** ⚠ 미검증 - 문항 목록 텍스트 + 답안 이미지 여러 장을 함께 전달하는 멀티모달 호출 */
    private String callGeminiForGrading(List<ExamQuestionVo> questions, List<SubmissionFileVo> files) {
        String questionBlock = formatQuestions(questions);
        String prompt = GRADING_PROMPT_HEADER.formatted(questionBlock);

        return chatClient.prompt()
                .user(userSpec -> {
                    userSpec.text(prompt);
                    for (SubmissionFileVo file : files) {
                        userSpec.media(MimeTypeUtils.parseMimeType(file.getMimeType()),
                                new FileSystemResource(file.getFilePath()));
                    }
                })
                .call()
                .content();
    }

    private String formatQuestions(List<ExamQuestionVo> questions) {
        StringBuilder sb = new StringBuilder();
        for (ExamQuestionVo q : questions) {
            sb.append("- questionId=").append(q.getQuestionId())
                    .append(", 문항").append(q.getQuestionNo())
                    .append(" (").append(q.getQuestionType()).append(")")
                    .append(", 배점=").append(q.getMaxScore())
                    .append(", 정답=").append(q.getCorrectAnswer());
            if (q.getGradingCriteria() != null && !q.getGradingCriteria().isBlank()) {
                sb.append(", 채점기준=").append(q.getGradingCriteria());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /** Gemini 응답을 JSON 배열로 파싱한다. 마크다운 코드펜스가 섞여 와도 벗겨내고 시도한다 - AIG-02와 동일 패턴 */
    private List<AnswerGradingItem> parseGradingItems(String rawResponse) {
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
            AnswerGradingItem[] items = objectMapper.readValue(cleaned, AnswerGradingItem[].class);
            return List.of(items);
        } catch (Exception e) {
            return List.of();
        }
    }

    /** 문항별 confidence의 단순 평균을 제출 건 전체의 ai_confidence로 사용한다 (팀 확인 전 임시 규칙) */
    private BigDecimal averageConfidence(List<BigDecimal> confidences) {
        List<BigDecimal> nonNull = confidences.stream().filter(c -> c != null).toList();
        if (nonNull.isEmpty()) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal c : nonNull) {
            sum = sum.add(c);
        }
        return sum.divide(BigDecimal.valueOf(nonNull.size()), 4, RoundingMode.HALF_UP);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > ERROR_MESSAGE_MAX_LENGTH ? message.substring(0, ERROR_MESSAGE_MAX_LENGTH) : message;
    }

    private void ensureGeminiKey() {
        if (apiKey == null || apiKey.isBlank() || "DUMMY_KEY".equals(apiKey)) {
            throw new IllegalStateException("GEMINI_API_KEY 환경변수가 설정되어 있지 않습니다.");
        }
    }
}
