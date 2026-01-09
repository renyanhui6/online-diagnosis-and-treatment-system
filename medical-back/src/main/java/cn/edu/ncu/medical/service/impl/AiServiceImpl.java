package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.ai.DeepSeekProperties;
import cn.edu.ncu.medical.entity.dto.DoctorAiRequest;
import cn.edu.ncu.medical.entity.dto.DoctorAiResponse;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class AiServiceImpl implements AiService {

    private static final int AI_UNAVAILABLE_CODE = 9001;
    private static final String AI_UNAVAILABLE_MESSAGE = """
AI 服务不可用：模型 & 价格

模型 & 价格
下表所列模型价格以“百万 tokens”为单位。Token 是模型用来表示自然语言文本的的最小单位，可以是一个词、一个数字或一个标点符号等。我们将根据模型输入和输出的总 token 数进行计量计费。

模型细节
模型\tdeepseek-chat\tdeepseek-reasoner\tdeepseek-reasoner(1)
BASE URL\thttps://api.deepseek.com\thttps://api.deepseek.com/\tv3.2_speciale_expires_on_20251215
模型版本\tDeepSeek-V3.2（非思考模式）\tDeepSeek-V3.2（思考模式）\tDeepSeek-V3.2-Speciale（只支持思考模式）
上下文长度\t128K
输出长度\t默认 4K，最大 8K\t默认 32K，最大 64K\t默认 128K，最大 128K
功能\tJson Output\t支持\t支持\t不支持
\tTool Calls\t支持\t支持\t不支持
\t对话前缀续写（Beta）\t支持\t支持\t不支持
\tFIM 补全（Beta）\t支持\t不支持\t不支持
价格\t百万tokens输入（缓存命中）\t0.2元
\t百万tokens输入（缓存未命中）\t2元
\t百万tokens输出\t3元

(1) 用户可以通过设置 base_url="https://api.deepseek.com/v3.2_speciale_expires_on_20251215" 访问 DeepSeek-V3.2-Speciale 模型。该模型只支持思考模式，支持时间截止至北京时间 2025-12-15 23:59。

扣费规则
扣减费用 = token 消耗量 × 模型单价，对应的费用将直接从充值余额或赠送余额中进行扣减。当充值余额与赠送余额同时存在时，优先扣减赠送余额。

产品价格可能发生变动，DeepSeek 保留修改价格的权利。请您依据实际用量按需充值，定期查看此页面以获知最新价格信息。
""".trim();

    private final DeepSeekProperties deepSeekProperties;

    @Override
    public DoctorAiResponse assistDoctor(DoctorAiRequest request) {
        ensureDeepSeekEnabled();
        try {
            return deepSeekDoctorAssist(request);
        } catch (Exception ex) {
            log.warn("DeepSeek doctorAssist failed: {}", ex.toString());
            throw new MyRuntimeException(AI_UNAVAILABLE_CODE, AI_UNAVAILABLE_MESSAGE);
        }
    }

    @Override
    public TriageResponse triage(TriageRequest request) {
        ensureDeepSeekEnabled();
        try {
            return deepSeekTriage(request);
        } catch (Exception ex) {
            log.warn("DeepSeek triage failed: {}", ex.toString());
            throw new MyRuntimeException(AI_UNAVAILABLE_CODE, AI_UNAVAILABLE_MESSAGE);
        }
    }

    private boolean isDeepSeekEnabled() {
        return deepSeekProperties != null && StringUtils.hasText(deepSeekProperties.getApiKey());
    }

    private void ensureDeepSeekEnabled() {
        if (!isDeepSeekEnabled()) {
            throw new MyRuntimeException(AI_UNAVAILABLE_CODE, AI_UNAVAILABLE_MESSAGE);
        }
    }

    private DoctorAiResponse deepSeekDoctorAssist(DoctorAiRequest request) throws Exception {
        String prompt = buildDoctorPrompt(request);
        String content = deepSeekChat(prompt);
        JSONObject json = tryParseJsonObject(content);

        DoctorAiResponse response = new DoctorAiResponse();
        String suggestion = json.getString("suggestion");
        if (!StringUtils.hasText(suggestion)) {
            throw new IllegalStateException("DeepSeek response missing suggestion");
        }
        response.setSuggestion(suggestion);
        response.setFollowUpQuestions(json.getJSONArray("followUpQuestions") != null
                ? json.getJSONArray("followUpQuestions").toJavaList(String.class)
                : List.of());
        response.setCaution("AI 生成内容仅供参考，请结合临床判断与诊疗规范。");
        response.setSource("deepseek");
        return response;
    }

    private TriageResponse deepSeekTriage(TriageRequest request) throws Exception {
        String prompt = buildTriagePrompt(request);
        String content = deepSeekChat(prompt);
        JSONObject json = tryParseJsonObject(content);

        TriageResponse response = new TriageResponse();
        JSONArray deps = json.getJSONArray("recommendedDepartments");
        if (deps == null || deps.isEmpty()) {
            throw new IllegalStateException("DeepSeek response missing recommendedDepartments");
        }
        response.setRecommendedDepartments(deps.toJavaList(String.class));
        response.setRationale(json.getString("rationale"));
        response.setDisclaimer("AI 推荐仅供参考，请结合实际病情选择科室");
        return response;
    }

    private String buildDoctorPrompt(DoctorAiRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("你是医院线上问诊系统的医生助手。请根据输入给出医生端参考建议。\n");
        builder.append("输出必须是严格 JSON（不要代码块）：");
        builder.append("{\"suggestion\":\"...\",\"followUpQuestions\":[\"...\",\"...\"]}\n\n");

        if (StringUtils.hasText(request.getSummary())) {
            builder.append("病情摘要：").append(request.getSummary()).append("\n");
        }
        if (StringUtils.hasText(request.getConversationSnippet())) {
            builder.append("对话片段：").append(request.getConversationSnippet()).append("\n");
        }
        if (request.getSymptoms() != null && !request.getSymptoms().isEmpty()) {
            builder.append("症状：").append(String.join("，", request.getSymptoms())).append("\n");
        }
        return builder.toString();
    }

    private String buildTriagePrompt(TriageRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("你是医院分诊助手，请根据患者描述推荐就诊科室。\n");
        builder.append("输出必须是严格 JSON（不要代码块）：");
        builder.append("{\"recommendedDepartments\":[\"...\"],\"rationale\":\"...\"}\n\n");

        if (StringUtils.hasText(request.getDescription())) {
            builder.append("主诉：").append(request.getDescription()).append("\n");
        }
        if (request.getSymptoms() != null && !request.getSymptoms().isEmpty()) {
            builder.append("症状：").append(String.join("，", request.getSymptoms())).append("\n");
        }
        if (request.getAge() != null) {
            builder.append("年龄：").append(request.getAge()).append("\n");
        }
        if (StringUtils.hasText(request.getGender())) {
            builder.append("性别：").append(request.getGender()).append("\n");
        }
        builder.append("注意：仅给出 1-3 个推荐科室即可，并简要说明理由。");
        return builder.toString();
    }

    private String deepSeekChat(String userPrompt) throws Exception {
        String baseUrl = StringUtils.hasText(deepSeekProperties.getBaseUrl()) ? deepSeekProperties.getBaseUrl() : "https://api.deepseek.com";
        String endpoint = baseUrl.endsWith("/") ? baseUrl + "v1/chat/completions" : baseUrl + "/v1/chat/completions";

        JSONObject body = new JSONObject();
        body.put("model", deepSeekProperties.getModel());
        body.put("temperature", 0.2);

        JSONArray messages = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是医疗系统内的文本助手，请用中文回答，输出必须可被机器解析。");
        messages.add(systemMessage);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);
        body.put("messages", messages);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(deepSeekProperties.getTimeoutSeconds()))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(deepSeekProperties.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + deepSeekProperties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("DeepSeek HTTP " + response.statusCode() + ": " + safeSnippet(response.body(), 600));
        }

        JSONObject root = JSON.parseObject(response.body());
        JSONArray choices = root.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("DeepSeek response missing choices");
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        if (message == null) {
            throw new IllegalStateException("DeepSeek response missing message");
        }
        String content = message.getString("content");
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("DeepSeek response empty content");
        }
        return content.trim();
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

    private JSONObject tryParseJsonObject(String content) {
        String normalized = extractJsonObject(content);
        try {
            return JSON.parseObject(normalized);
        } catch (Exception ex) {
            log.warn("DeepSeek JSON parse failed, contentSnippet={}", safeSnippet(content, 600));
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
}
