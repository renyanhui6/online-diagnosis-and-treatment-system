package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.ai.DeepSeekProperties;
import cn.edu.ncu.medical.ai.DoctorCopilotAgent;
import cn.edu.ncu.medical.ai.LocalTriageAdvisor;
import cn.edu.ncu.medical.ai.LocalDoctorCopilotAdvisor;
import cn.edu.ncu.medical.ai.PatientTriageAgent;
import cn.edu.ncu.medical.entity.dto.DoctorAiRequest;
import cn.edu.ncu.medical.entity.dto.DoctorAiResponse;
import cn.edu.ncu.medical.entity.dto.TriageChatRequest;
import cn.edu.ncu.medical.entity.dto.TriageChatResponse;
import cn.edu.ncu.medical.entity.dto.TriageRequest;
import cn.edu.ncu.medical.entity.dto.TriageResponse;
import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.service.AiService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class AiServiceImpl implements AiService {

    private final DeepSeekProperties deepSeekProperties;
    private final PatientTriageAgent patientTriageAgent;
    private final LocalTriageAdvisor localTriageAdvisor;
    private final DoctorCopilotAgent doctorCopilotAgent;
    private final LocalDoctorCopilotAdvisor localDoctorCopilotAdvisor;

    @Override
    public DoctorAiResponse assistDoctor(DoctorAiRequest request) {
        try {
            if (!isDeepSeekEnabled()) {
                return localDoctorCopilotAdvisor.assist(request);
            }
            return agenticDoctorAssist(request);
        } catch (Exception ex) {
            log.warn("Doctor copilot failed, fallback to local advisor: {}", ex.toString());
            return localDoctorCopilotAdvisor.assist(request);
        }
    }

    @Override
    public TriageResponse triage(TriageRequest request) {
        try {
            if (!isDeepSeekEnabled()) {
                return localTriageAdvisor.triage(request);
            }
            TriageChatRequest chatRequest = new TriageChatRequest();
            chatRequest.setSessionId("triage-" + UUID.randomUUID());
            chatRequest.setAge(request.getAge());
            chatRequest.setGender(request.getGender());
            chatRequest.setMessage(buildTriageChatMessage(request));

            TriageChatResponse chatResponse = triageChat(chatRequest);
            TriageResponse response = new TriageResponse();
            response.setRecommendedDepartments(chatResponse.getRecommendedSubDepartments().isEmpty()
                    ? chatResponse.getRecommendedDepartments()
                    : chatResponse.getRecommendedSubDepartments());
            response.setRationale(chatResponse.getAssistantMessage());
            response.setDisclaimer(chatResponse.getDisclaimer());
            response.setSource(chatResponse.getSource());
            return response;
        } catch (Exception ex) {
            log.warn("AI triage failed, fallback to local advisor: {}", ex.toString());
            return localTriageAdvisor.triage(request);
        }
    }

    @Override
    public TriageChatResponse triageChat(TriageChatRequest request) {
        if (!isDeepSeekEnabled()) {
            return localTriageAdvisor.chat(request);
        }
        try {
            TriageChatResponse response = agenticTriage(request);
            response.setSource("langchain-agent");
            return response;
        } catch (Exception ex) {
            log.warn("Agentic triage failed, fallback to local advisor: {}", ex.toString());
            return localTriageAdvisor.chat(request);
        }
    }

    private boolean isDeepSeekEnabled() {
        return deepSeekProperties != null && StringUtils.hasText(deepSeekProperties.getApiKey());
    }

    private TriageChatResponse agenticTriage(TriageChatRequest request) {
        String sessionId = StringUtils.hasText(request.getSessionId()) ? request.getSessionId() : "triage-" + UUID.randomUUID();
        String content = patientTriageAgent.chat(sessionId, request.getMessage(), request.getAge(), request.getGender());
        JSONObject json = tryParseJsonObject(content);

        TriageChatResponse response = new TriageChatResponse();
        response.setSessionId(sessionId);
        response.setAssistantMessage(defaultText(json.getString("assistantMessage"), "请再补充一下主要不适部位、持续时间和伴随症状，我再帮你判断挂号方向。"));
        response.setNeedMoreInfo(Boolean.TRUE.equals(json.getBoolean("needMoreInfo")));
        response.setEmergency(Boolean.TRUE.equals(json.getBoolean("emergency")));
        response.setUsedExternalKnowledge(Boolean.TRUE.equals(json.getBoolean("usedExternalKnowledge")));
        response.setConfidence(json.getInteger("confidence"));
        response.setRationale(defaultText(json.getString("rationale"), response.getAssistantMessage()));
        response.setFollowUpQuestions(toStringList(json.getJSONArray("followUpQuestions")));
        response.setRecommendedDepartments(toStringList(json.getJSONArray("recommendedDepartments")));
        response.setRecommendedSubDepartments(toStringList(json.getJSONArray("recommendedSubDepartments")));
        return response;
    }

    private DoctorAiResponse agenticDoctorAssist(DoctorAiRequest request) {
        String sessionId = "doctor-copilot-" + (request.getRoomId() != null ? request.getRoomId() : UUID.randomUUID());
        String doctorFocus = buildDoctorFocus(request);
        String content = doctorCopilotAgent.analyze(sessionId, request.getRoomId(), request.getRegistrationId(), doctorFocus);
        JSONObject json = tryParseJsonObject(content);

        DoctorAiResponse response = new DoctorAiResponse();
        response.setSuggestion(defaultText(json.getString("suggestion"), "建议先补齐关键病史，再形成最终病历与处方。"));
        response.setFollowUpQuestions(toStringList(json.getJSONArray("followUpQuestions")));
        response.setMissingInfoItems(toStringList(json.getJSONArray("missingInfoItems")));
        response.setRiskAlerts(toStringList(json.getJSONArray("riskAlerts")));
        response.setRecommendedActions(toStringList(json.getJSONArray("recommendedActions")));
        response.setAssessmentFocuses(toStringList(json.getJSONArray("assessmentFocuses")));
        response.setPrescriptionSafetyHints(toStringList(json.getJSONArray("prescriptionSafetyHints")));
        response.setHistoricalRecordHighlights(toStringList(json.getJSONArray("historicalRecordHighlights")));
        response.setUsedDataSources(toStringList(json.getJSONArray("usedDataSources")));
        response.setChiefComplaintDraft(defaultText(json.getString("chiefComplaintDraft"), ""));
        response.setPresentIllnessDraft(defaultText(json.getString("presentIllnessDraft"), ""));
        response.setStructuredRecordDraft(defaultText(json.getString("structuredRecordDraft"), ""));
        response.setConfidence(json.getInteger("confidence"));
        response.setNeedMoreInfo(Boolean.TRUE.equals(json.getBoolean("needMoreInfo")));
        response.setHighRisk(Boolean.TRUE.equals(json.getBoolean("highRisk")));
        response.setSource("langchain-doctor-copilot");
        return response;
    }

    private String buildTriageChatMessage(TriageRequest request) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(request.getDescription())) {
            builder.append("主诉：").append(request.getDescription()).append("。");
        }
        if (request.getSymptoms() != null && !request.getSymptoms().isEmpty()) {
            builder.append("症状：").append(String.join("，", request.getSymptoms())).append("。");
        }
        return builder.toString();
    }

    private String buildDoctorFocus(DoctorAiRequest request) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(request.getSummary())) {
            builder.append("医生补充摘要：").append(request.getSummary()).append("。");
        }
        if (StringUtils.hasText(request.getConversationSnippet())) {
            builder.append("前端传来的对话摘要：").append(request.getConversationSnippet()).append("。");
        }
        if (request.getSymptoms() != null && !request.getSymptoms().isEmpty()) {
            builder.append("前端提取的症状关键词：").append(String.join("，", request.getSymptoms())).append("。");
        }
        return builder.toString();
    }

    private JSONObject tryParseJsonObject(String content) {
        String normalized = extractJsonObject(content);
        try {
            return JSON.parseObject(normalized);
        } catch (Exception ex) {
            log.warn("AI JSON parse failed, contentSnippet={}", safeSnippet(content, 600));
            throw ex;
        }
    }

    private String extractJsonObject(String text) {
        if (!StringUtils.hasText(text)) {
            return "{}";
        }
        String trimmed = text.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private List<String> toStringList(JSONArray array) {
        return array == null ? List.of() : array.toJavaList(String.class);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String safeSnippet(String text, int maxLen) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replace("\r", " ").replace("\n", " ").trim();
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, maxLen) + "...";
    }
}
