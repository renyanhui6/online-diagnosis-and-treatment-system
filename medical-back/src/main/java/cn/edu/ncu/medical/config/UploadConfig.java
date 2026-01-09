package cn.edu.ncu.medical.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "upload")
@Data
public class UploadConfig {
    /**
     * Endpoint，例如：
     * - http://localhost:9000（MinIO）
     * - https://s3.your-domain.com（S3 兼容）
     */
    private String endpoint;

    private String accessKey;
    private String secretKey;
    private String bucket;

    /**
     * 可选：对外访问前缀（例如你的 MinIO 反向代理域名或 CDN 域名）
     * 为空时默认拼接：{endpoint}/{bucket}/{objectKey}
     */
    private String publicUrlPrefix;
}
