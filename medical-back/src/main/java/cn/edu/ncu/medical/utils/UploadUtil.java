package cn.edu.ncu.medical.utils;

import cn.edu.ncu.medical.config.UploadConfig;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.StringUtils;

import java.io.InputStream;

@Log4j2
public class UploadUtil {

    public static String putPhoto(UploadConfig config, InputStream inputStream, String objectKey) {
        if (config == null) {
            throw new IllegalArgumentException("upload config is null");
        }
        if (!StringUtils.hasText(config.getEndpoint())
                || !StringUtils.hasText(config.getAccessKey())
                || !StringUtils.hasText(config.getSecretKey())
                || !StringUtils.hasText(config.getBucket())) {
            throw new IllegalStateException("upload config missing: endpoint/accessKey/secretKey/bucket");
        }
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream is null");
        }
        if (!StringUtils.hasText(objectKey)) {
            throw new IllegalArgumentException("objectKey is empty");
        }

        try {
            String endpoint = normalizeEndpoint(config.getEndpoint());
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(config.getAccessKey(), config.getSecretKey())
                    .build();

            boolean bucketExists = client.bucketExists(BucketExistsArgs.builder().bucket(config.getBucket()).build());
            if (!bucketExists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(config.getBucket()).build());
            }

            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(config.getBucket())
                            .object(objectKey)
                            .stream(inputStream, -1, 10 * 1024 * 1024)
                            .build()
            );

            return buildPublicUrl(config, endpoint, objectKey);
        } catch (Exception ex) {
            log.error("MinIO upload failed, key={}, endpoint={}", objectKey, config.getEndpoint(), ex);
            throw new RuntimeException("MinIO upload failed: " + ex.getMessage(), ex);
        }
    }

    private static String normalizeEndpoint(String endpoint) {
        String trimmed = endpoint.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    private static String buildPublicUrl(UploadConfig config, String endpointWithScheme, String objectKey) {
        if (StringUtils.hasText(config.getPublicUrlPrefix())) {
            return trimTrailingSlash(config.getPublicUrlPrefix()) + "/" + objectKey;
        }
        String base = trimTrailingSlash(endpointWithScheme);
        return base + "/" + config.getBucket() + "/" + objectKey;
    }

    private static String trimTrailingSlash(String text) {
        String t = text.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }
}
