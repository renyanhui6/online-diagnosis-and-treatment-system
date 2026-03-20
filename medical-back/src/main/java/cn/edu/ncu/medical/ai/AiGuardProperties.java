package cn.edu.ncu.medical.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 请求治理配置：截断、脱敏、限流、审计日志。
 */
@Component
@ConfigurationProperties(prefix = "ai.guard")
@Data
public class AiGuardProperties {
    private boolean maskEnabled = true;
    private boolean auditEnabled = true;
    private boolean rateLimitEnabled = true;

    private int maxSummaryChars = 1000;
    private int maxSnippetChars = 2000;
    private int maxDescriptionChars = 500;
    private int maxGenderChars = 10;
    private int maxSymptomChars = 50;
    private int maxSymptomCount = 10;

    private int rateLimitWindowSeconds = 60;
    private int rateLimitMaxRequests = 20;
}
