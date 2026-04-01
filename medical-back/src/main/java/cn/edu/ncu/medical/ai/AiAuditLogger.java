package cn.edu.ncu.medical.ai;

import cn.edu.ncu.medical.entity.dto.DoctorAiRequest;
import cn.edu.ncu.medical.entity.dto.TriageChatRequest;
import cn.edu.ncu.medical.entity.dto.TriageRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiAuditLogger {

    private final AiGuardProperties properties;

    public void logDoctorAssist(AiRequestContext context, DoctorAiRequest request, AiSanitizeMeta meta) {
        if (!properties.isAuditEnabled()) {
            return;
        }
        JSONObject payload = basePayload(context);
        payload.put("event", "ai_doctor_assist");
        payload.put("roomId", request.getRoomId());
        payload.put("registrationId", request.getRegistrationId());
        payload.put("summaryLength", meta.getSummaryLength());
        payload.put("snippetLength", meta.getSnippetLength());
        payload.put("symptomCount", meta.getSymptomCount());
        payload.put("symptomsTotalLength", meta.getSymptomsTotalLength());
        payload.put("masked", meta.isMasked());
        payload.put("truncated", meta.isTruncated());
        log.info("AI_AUDIT {}", JSON.toJSONString(payload));
    }

    public void logTriage(AiRequestContext context, TriageRequest request, AiSanitizeMeta meta) {
        if (!properties.isAuditEnabled()) {
            return;
        }
        JSONObject payload = basePayload(context);
        payload.put("event", "ai_triage");
        payload.put("age", request.getAge());
        payload.put("gender", request.getGender());
        payload.put("descriptionLength", meta.getDescriptionLength());
        payload.put("symptomCount", meta.getSymptomCount());
        payload.put("symptomsTotalLength", meta.getSymptomsTotalLength());
        payload.put("masked", meta.isMasked());
        payload.put("truncated", meta.isTruncated());
        log.info("AI_AUDIT {}", JSON.toJSONString(payload));
    }

    public void logTriageChat(AiRequestContext context, TriageChatRequest request, AiSanitizeMeta meta) {
        if (!properties.isAuditEnabled()) {
            return;
        }
        JSONObject payload = basePayload(context);
        payload.put("event", "ai_triage_chat");
        payload.put("sessionId", request.getSessionId());
        payload.put("age", request.getAge());
        payload.put("gender", request.getGender());
        payload.put("messageLength", meta.getDescriptionLength());
        payload.put("masked", meta.isMasked());
        payload.put("truncated", meta.isTruncated());
        log.info("AI_AUDIT {}", JSON.toJSONString(payload));
    }

    private JSONObject basePayload(AiRequestContext context) {
        JSONObject payload = new JSONObject();
        payload.put("requestId", context.getRequestId());
        payload.put("type", context.getType());
        payload.put("userId", context.getUserId());
        payload.put("clientIp", context.getClientIp());
        payload.put("path", context.getPath());
        payload.put("userAgent", context.getUserAgent());
        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }
}
