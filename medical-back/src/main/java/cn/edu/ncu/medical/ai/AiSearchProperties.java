package cn.edu.ncu.medical.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.search")
@Data
public class AiSearchProperties {
    private boolean enabled;
    private String provider = "tavily";
    private String endpoint = "https://api.tavily.com/search";
    private String apiKey;
    private int maxResults = 5;
    private int timeoutSeconds = 15;
}
