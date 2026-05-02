package cn.edu.ncu.medical.ai;

import cn.edu.ncu.medical.entity.dto.DoctorAiRequest;
import cn.edu.ncu.medical.entity.dto.TriageChatRequest;
import cn.edu.ncu.medical.entity.dto.TriageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class AiRequestSanitizer {

    private static final Pattern PHONE = Pattern.compile("\\b1\\d{10}\\b");
    private static final Pattern LANDLINE = Pattern.compile("\\b\\d{3,4}-\\d{7,8}\\b");
    private static final Pattern ID_CARD = Pattern.compile("\\b\\d{17}[0-9Xx]\\b");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private final AiGuardProperties properties;

    public AiSanitizeResult<DoctorAiRequest> sanitizeDoctorRequest(DoctorAiRequest request) {
        AiSanitizeMeta meta = new AiSanitizeMeta();
        DoctorAiRequest sanitized = new DoctorAiRequest();
        if (request == null) {
            return new AiSanitizeResult<>(sanitized, meta);
        }

        sanitized.setRoomId(request.getRoomId());
        sanitized.setRegistrationId(request.getRegistrationId());

        String summary = sanitizeText(request.getSummary(), properties.getMaxSummaryChars(), meta);
        String snippet = sanitizeText(request.getConversationSnippet(), properties.getMaxSnippetChars(), meta);
        sanitized.setSummary(summary);
        sanitized.setConversationSnippet(snippet);
        meta.setSummaryLength(length(summary));
        meta.setSnippetLength(length(snippet));

        List<String> symptoms = sanitizeSymptoms(request.getSymptoms(), meta);
        sanitized.setSymptoms(symptoms);

        return new AiSanitizeResult<>(sanitized, meta);
    }

    public AiSanitizeResult<TriageRequest> sanitizeTriageRequest(TriageRequest request) {
        AiSanitizeMeta meta = new AiSanitizeMeta();
        TriageRequest sanitized = new TriageRequest();
        if (request == null) {
            return new AiSanitizeResult<>(sanitized, meta);
        }

        sanitized.setAge(request.getAge());

        String description = sanitizeText(request.getDescription(), properties.getMaxDescriptionChars(), meta);
        String gender = sanitizeText(request.getGender(), properties.getMaxGenderChars(), meta);
        sanitized.setDescription(description);
        sanitized.setGender(gender);
        meta.setDescriptionLength(length(description));

        List<String> symptoms = sanitizeSymptoms(request.getSymptoms(), meta);
        sanitized.setSymptoms(symptoms);

        return new AiSanitizeResult<>(sanitized, meta);
    }

    public AiSanitizeResult<TriageChatRequest> sanitizeTriageChatRequest(TriageChatRequest request) {
        AiSanitizeMeta meta = new AiSanitizeMeta();
        TriageChatRequest sanitized = new TriageChatRequest();
        if (request == null) {
            return new AiSanitizeResult<>(sanitized, meta);
        }

        sanitized.setSessionId(sanitizeSessionId(request.getSessionId()));
        sanitized.setPatientAttendantId(request.getPatientAttendantId());
        sanitized.setAge(request.getAge());

        String message = sanitizeText(request.getMessage(), properties.getMaxDescriptionChars(), meta);
        String gender = sanitizeText(request.getGender(), properties.getMaxGenderChars(), meta);
        sanitized.setMessage(message);
        sanitized.setGender(gender);
        meta.setDescriptionLength(length(message));
        return new AiSanitizeResult<>(sanitized, meta);
    }

    private List<String> sanitizeSymptoms(List<String> symptoms, AiSanitizeMeta meta) {
        if (symptoms == null || symptoms.isEmpty()) {
            meta.setSymptomCount(0);
            meta.setSymptomsTotalLength(0);
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        int totalLength = 0;
        for (String symptom : symptoms) {
            if (cleaned.size() >= properties.getMaxSymptomCount()) {
                meta.setTruncated(true);
                break;
            }
            String sanitized = sanitizeText(symptom, properties.getMaxSymptomChars(), meta);
            if (!StringUtils.hasText(sanitized)) {
                continue;
            }
            cleaned.add(sanitized);
            totalLength += sanitized.length();
        }
        meta.setSymptomCount(cleaned.size());
        meta.setSymptomsTotalLength(totalLength);
        return cleaned;
    }

    private String sanitizeText(String input, int maxChars, AiSanitizeMeta meta) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        String trimmed = input.trim();
        String masked = properties.isMaskEnabled() ? maskSensitive(trimmed, meta) : trimmed;
        if (maxChars > 0 && masked.length() > maxChars) {
            meta.setTruncated(true);
            masked = masked.substring(0, maxChars);
        }
        return masked;
    }

    private String maskSensitive(String input, AiSanitizeMeta meta) {
        String result = input;
        result = maskPattern(result, PHONE, "[MASKED_PHONE]", meta);
        result = maskPattern(result, LANDLINE, "[MASKED_PHONE]", meta);
        result = maskPattern(result, ID_CARD, "[MASKED_ID]", meta);
        result = maskPattern(result, EMAIL, "[MASKED_EMAIL]", meta);
        return result;
    }

    private String maskPattern(String input, Pattern pattern, String replacement, AiSanitizeMeta meta) {
        String masked = pattern.matcher(input).replaceAll(replacement);
        if (!masked.equals(input)) {
            meta.setMasked(true);
        }
        return masked;
    }

    private String sanitizeSessionId(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        String trimmed = sessionId.trim();
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }
}
