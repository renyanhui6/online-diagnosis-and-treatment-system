package cn.edu.ncu.medical.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek（OpenAI 兼容）接口配置：未配置 key 或调用失败时，AI 接口返回“服务不可用”。
 */
@Component
@ConfigurationProperties(prefix = "ai.deepseek")
@Data
public class DeepSeekProperties {
    /**
     * 例如 https://api.deepseek.com
     */
    private String baseUrl = "https://api.deepseek.com";

    /**
     * 建议通过环境变量 DEEPSEEK_API_KEY 注入，不要写死在仓库中
     */
    private String apiKey;

    /**
     * 默认模型名（按 DeepSeek 实际可用模型调整）
     */
    private String model = "deepseek-chat";

    /**
     * 请求超时（秒）
     */
    private int timeoutSeconds = 20;
}
