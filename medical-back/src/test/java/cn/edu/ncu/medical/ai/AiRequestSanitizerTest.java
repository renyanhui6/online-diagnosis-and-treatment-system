package cn.edu.ncu.medical.ai;

import cn.edu.ncu.medical.entity.dto.DoctorAiRequest;
import cn.edu.ncu.medical.entity.dto.TriageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiRequestSanitizerTest {

    @Test
    void sanitizeDoctorRequestMasksSensitiveContent() {
        AiGuardProperties properties = new AiGuardProperties();
        properties.setMaxSummaryChars(200);
        properties.setMaxSnippetChars(200);
        AiRequestSanitizer sanitizer = new AiRequestSanitizer(properties);

        DoctorAiRequest request = new DoctorAiRequest();
        request.setSummary("患者电话 13800138000，身份证 11010519491231002X");
        request.setConversationSnippet("邮箱 test@example.com");
        request.setSymptoms(List.of("头痛", "发热"));

        AiSanitizeResult<DoctorAiRequest> result = sanitizer.sanitizeDoctorRequest(request);
        DoctorAiRequest sanitized = result.getRequest();
        AiSanitizeMeta meta = result.getMeta();

        assertNotNull(sanitized.getSummary());
        assertNotNull(sanitized.getConversationSnippet());
        assertTrue(sanitized.getSummary().contains("[MASKED_PHONE]"));
        assertTrue(sanitized.getSummary().contains("[MASKED_ID]"));
        assertTrue(sanitized.getConversationSnippet().contains("[MASKED_EMAIL]"));
        assertTrue(meta.isMasked());
        assertFalse(meta.isTruncated());
        assertEquals(2, sanitized.getSymptoms().size());
    }

    @Test
    void sanitizeTriageRequestTruncatesFields() {
        AiGuardProperties properties = new AiGuardProperties();
        properties.setMaxDescriptionChars(8);
        properties.setMaxSymptomChars(4);
        properties.setMaxSymptomCount(2);
        AiRequestSanitizer sanitizer = new AiRequestSanitizer(properties);

        TriageRequest request = new TriageRequest();
        request.setDescription("症状描述过长需要截断");
        request.setGender("female");
        request.setSymptoms(List.of("症状一", "症状二", "症状三"));

        AiSanitizeResult<TriageRequest> result = sanitizer.sanitizeTriageRequest(request);
        TriageRequest sanitized = result.getRequest();
        AiSanitizeMeta meta = result.getMeta();

        assertNotNull(sanitized.getDescription());
        assertTrue(sanitized.getDescription().length() <= 8);
        assertEquals(2, sanitized.getSymptoms().size());
        assertTrue(meta.isTruncated());
    }
}
