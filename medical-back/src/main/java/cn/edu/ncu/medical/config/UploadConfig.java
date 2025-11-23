package cn.edu.ncu.medical.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "upload")
@Data
public class UploadConfig {
    private String accessKey;
    private String secretKey;
    private String bucket;
}