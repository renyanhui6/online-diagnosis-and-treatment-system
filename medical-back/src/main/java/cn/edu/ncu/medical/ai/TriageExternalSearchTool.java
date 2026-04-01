package cn.edu.ncu.medical.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Log4j2
public class TriageExternalSearchTool {

    private final AiSearchProperties properties;

    @Tool("""
            查询院外公开医疗知识。当院内科室信息不足以判断、症状表述模糊或跨多个科室时可以调用。
            该工具只用于补充分诊常识，不用于做明确诊断。
            """)
    public String searchMedicalKnowledge(String query) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getApiKey())) {
            return "外部医疗知识检索当前未配置，无法提供院外资料。";
        }
        if (!"tavily".equalsIgnoreCase(properties.getProvider())) {
            return "外部医疗知识检索当前未启用受支持的 provider。";
        }

        try {
            JSONObject body = new JSONObject();
            body.put("api_key", properties.getApiKey());
            body.put("query", query);
            body.put("search_depth", "basic");
            body.put("max_results", properties.getMaxResults());
            body.put("topic", "general");

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getEndpoint()))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                return "外部医疗知识检索失败，HTTP " + response.statusCode();
            }

            JSONObject root = JSON.parseObject(response.body());
            JSONArray results = root.getJSONArray("results");
            if (results == null || results.isEmpty()) {
                return "外部医疗知识检索没有返回结果。";
            }

            StringBuilder builder = new StringBuilder("外部医疗知识检索结果：\n");
            for (int i = 0; i < Math.min(results.size(), properties.getMaxResults()); i++) {
                JSONObject item = results.getJSONObject(i);
                builder.append("- 标题：").append(defaultText(item.getString("title")))
                        .append("；摘要：").append(defaultText(item.getString("content")))
                        .append("；链接：").append(defaultText(item.getString("url")))
                        .append("\n");
            }
            return builder.toString();
        } catch (Exception ex) {
            log.warn("External triage search failed: {}", ex.toString());
            return "外部医疗知识检索执行失败。";
        }
    }

    private String defaultText(String text) {
        return StringUtils.hasText(text) ? text : "无";
    }
}
