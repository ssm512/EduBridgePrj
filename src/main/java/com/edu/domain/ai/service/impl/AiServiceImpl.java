package com.edu.domain.ai.service.impl;

import com.edu.domain.ai.dto.request.CounselingSummaryRequest;
import com.edu.domain.ai.dto.request.MonthlyReportRequest;
import com.edu.domain.ai.dto.request.NoticeDraftRequest;
import com.edu.domain.ai.dto.response.AiGenerateResponse;
import com.edu.domain.ai.mapper.AiMapper;
import com.edu.domain.ai.service.AiService;
import com.edu.domain.ai.vo.AiUsageLogVo;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.mapper.UserMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class AiServiceImpl implements AiService {

    private static final String MODEL_NAME_FALLBACK = "gemini-2.5-flash";

    private final AiMapper aiMapper;
    private final UserMapper userMapper;
    private final ChatClient chatClient;
    private final String apiKey;
    private final String modelName;

    public AiServiceImpl(AiMapper aiMapper,
                         UserMapper userMapper,
                         ChatClient.Builder chatClientBuilder,
                         @Value("${spring.ai.google.genai.api-key:}") String apiKey,
                         @Value("${spring.ai.google.genai.chat.model:gemini-2.5-flash}") String modelName) {
        this.aiMapper = aiMapper;
        this.userMapper = userMapper;
        this.chatClient = chatClientBuilder.build();
        this.apiKey = apiKey;
        this.modelName = modelName == null || modelName.isBlank() ? MODEL_NAME_FALLBACK : modelName;
    }

    @Override
    public AiGenerateResponse generateMonthlyReport(MonthlyReportRequest request, String loginId) {
        validateMonthlyReportRequest(request);
        UserDto loginUser = getLoginUser(loginId);

        Map<String, Object> student = aiMapper.selectStudentReportBase(request.getStudentId());
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "학생 정보를 찾을 수 없습니다.");
        }

        String prompt = buildMonthlyReportPrompt(
                student,
                aiMapper.selectMonthlyAttendanceSummary(request.getStudentId(), request.getTargetMonth()),
                aiMapper.selectMonthlyGradeSummary(request.getStudentId(), request.getTargetMonth()),
                aiMapper.selectMonthlyFeeSummary(request.getStudentId(), request.getTargetMonth()),
                aiMapper.selectMonthlyCounselingSummary(request.getStudentId(), request.getTargetMonth()),
                request
        );

        return generateAndLog(loginUser.getUserId(), "REPORT", prompt);
    }

    @Override
    public AiGenerateResponse summarizeCounseling(CounselingSummaryRequest request, String loginId) {
        UserDto loginUser = getLoginUser(loginId);
        String content = null;

        if (request != null && request.getCounselingId() != null) {
            Map<String, Object> counseling = aiMapper.selectCounselingForSummary(request.getCounselingId());
            if (counseling == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상담 기록을 찾을 수 없습니다.");
            }
            content = String.valueOf(counseling.getOrDefault("content", ""));
        } else if (request != null) {
            content = request.getContent();
        }

        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요약할 상담 내용을 입력해 주세요.");
        }

        String prompt = """
                다음 상담 기록을 학원 관리자가 빠르게 확인할 수 있도록 요약해 주세요.
                출력 형식:
                1. 핵심 요약
                2. 학생 상태/이슈
                3. 후속 조치 제안

                상담 내용:
                %s
                """.formatted(content);

        return generateAndLog(loginUser.getUserId(), "COUNSELING", prompt);
    }

    @Override
    public AiGenerateResponse draftNotice(NoticeDraftRequest request, String loginId) {
        UserDto loginUser = getLoginUser(loginId);
        if (request == null || request.getKeywords() == null || request.getKeywords().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "공지 초안 키워드를 입력해 주세요.");
        }

        String tone = request.getTone() == null || request.getTone().isBlank() ? "정중하고 간결하게" : request.getTone();
        String targetType = request.getTargetType() == null || request.getTargetType().isBlank() ? "ALL" : request.getTargetType();
        String prompt = """
                학원 공지사항 초안을 작성해 주세요.
                대상 코드: %s
                문체: %s
                키워드: %s

                출력 형식:
                제목:
                내용:
                """.formatted(targetType, tone, request.getKeywords());

        return generateAndLog(loginUser.getUserId(), "NOTICE", prompt);
    }

    @Override
    public List<Map<String, Object>> getAiUsageLogs(String featureCode, String fromDate, String toDate, Integer limit) {
        int safeLimit = limit == null ? 30 : Math.max(1, Math.min(limit, 100));
        return aiMapper.selectAiUsageLogs(featureCode, fromDate, toDate, safeLimit);
    }

    @Override
    public List<Map<String, Object>> getStudentOptions() {
        return aiMapper.selectActiveStudentOptions();
    }

    @Override
    public List<Map<String, Object>> getCounselingOptions(String category, String keyword) {
        return aiMapper.selectCounselingOptionsForSummary(category, keyword);
    }

    @Override
    public List<Map<String, Object>> getCounselingSearchSuggestions(String category, String keyword) {
        return aiMapper.selectCounselingSearchSuggestions(category, keyword);
    }

    private AiGenerateResponse generateAndLog(Long userId, String featureCode, String prompt) {
        AiUsageLogVo log = new AiUsageLogVo();
        log.setUserId(userId);
        log.setFeatureCode(featureCode);
        log.setRequestPrompt(prompt);
        log.setModelName(modelName);

        try {
            ensureGeminiKey();
            String result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            log.setResponseText(result);
            log.setSuccessYn("Y");
            aiMapper.insertAiUsageLog(log);
            return new AiGenerateResponse(result, log.getAiLogId());
        } catch (Exception e) {
            log.setSuccessYn("N");
            log.setErrorMessage(e.getMessage());
            aiMapper.insertAiUsageLog(log);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 생성에 실패했습니다. Gemini API 키와 네트워크 상태를 확인해 주세요.", e);
        }
    }

    private void ensureGeminiKey() {
        if (apiKey == null || apiKey.isBlank() || "DUMMY_KEY".equals(apiKey)) {
            throw new IllegalStateException("GEMINI_API_KEY 환경변수가 설정되어 있지 않습니다.");
        }
    }

    private UserDto getLoginUser(String loginId) {
        UserDto loginUser = userMapper.selectByLoginId(loginId);
        if (loginUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보가 없습니다.");
        }
        return loginUser;
    }

    private void validateMonthlyReportRequest(MonthlyReportRequest request) {
        if (request == null || request.getStudentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학생을 선택해 주세요.");
        }
        if (request.getTargetMonth() == null || !request.getTargetMonth().matches("\\d{4}-\\d{2}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "조회 월은 yyyy-MM 형식으로 입력해 주세요.");
        }
    }

    private String buildMonthlyReportPrompt(Map<String, Object> student,
                                            List<Map<String, Object>> attendance,
                                            List<Map<String, Object>> grades,
                                            List<Map<String, Object>> fees,
                                            List<Map<String, Object>> counseling,
                                            MonthlyReportRequest request) {
        return """
                EduBridge 학원 관리 시스템의 월간 학습 리포트 초안을 작성해 주세요.
                대상은 학부모에게 전달될 수 있으므로 객관적이고 정중하게 작성해 주세요.
                없는 데이터는 억지로 만들지 말고, 확인된 데이터 기준으로만 작성해 주세요.

                출력 형식:
                1. 월간 요약
                2. 출석 현황 (지각,조퇴도 엄연히 출석은 한 것 으로 처리해야함) 
                3. 성적 변화 및 학습 코멘트
                4. 회비/납부 참고 사항
                5. 상담 참고 사항
                6. 다음 달 지도 제안
                7. 종합 (추가코멘트가 없다면 무시, 있다면 리포트 요약과 더불어 코멘트 내용도 간략하게 답변)

                학생 기본 정보:
                %s

                대상 월:
                %s

                출석 요약:
                %s

                성적 데이터:
                %s

                회비 데이터:
                %s

                상담 데이터:
                %s

                강사/관리자 추가 코멘트:
                %s
                """.formatted(
                student,
                request.getTargetMonth(),
                attendance,
                grades,
                fees,
                counseling,
                request.getTeacherComment() == null ? "" : request.getTeacherComment()
        );
    }
}
