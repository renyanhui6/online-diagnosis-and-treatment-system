package cn.edu.ncu.medical.controller.common;

import cn.edu.ncu.medical.ai.DeepSeekProperties;
import cn.edu.ncu.medical.ai.AiSearchProperties;
import cn.edu.ncu.medical.result.Result;
import lombok.Data;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/ai")
public class AiStatusController {

    private final DeepSeekProperties deepSeekProperties;
    private final AiSearchProperties aiSearchProperties;
    private final Environment environment;

    public AiStatusController(DeepSeekProperties deepSeekProperties, AiSearchProperties aiSearchProperties, Environment environment) {
        this.deepSeekProperties = deepSeekProperties;
        this.aiSearchProperties = aiSearchProperties;
        this.environment = environment;
    }

    @GetMapping("/status")
    public Result<AiStatusResponse> status() {
        AiStatusResponse response = new AiStatusResponse();
        response.setActiveProfiles(Arrays.asList(environment.getActiveProfiles()));

        if (deepSeekProperties == null) {
            response.setDeepSeekEnabled(false);
            response.setApiKeyPresent(false);
            response.setApiKeyLength(0);
            return Result.ok(response);
        }

        String apiKey = deepSeekProperties.getApiKey();
        response.setBaseUrl(deepSeekProperties.getBaseUrl());
        response.setModel(deepSeekProperties.getModel());
        response.setTimeoutSeconds(deepSeekProperties.getTimeoutSeconds());
        response.setApiKeyPresent(StringUtils.hasText(apiKey));
        response.setApiKeyLength(apiKey == null ? 0 : apiKey.length());
        response.setDeepSeekEnabled(StringUtils.hasText(apiKey));
        response.setExternalSearchEnabled(aiSearchProperties.isEnabled() && StringUtils.hasText(aiSearchProperties.getApiKey()));
        response.setExternalSearchProvider(aiSearchProperties.getProvider());
        return Result.ok(response);
    }

    @Data
    public static class AiStatusResponse {
        private boolean deepSeekEnabled;
        private String baseUrl;
        private String model;
        private int timeoutSeconds;
        private boolean apiKeyPresent;
        private int apiKeyLength;
        private boolean externalSearchEnabled;
        private String externalSearchProvider;
        private List<String> activeProfiles;
    }
}
