package cn.edu.ncu.medical.ai;

import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.utils.RedisCache;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiRateLimiter {

    private static final int AI_RATE_LIMIT_CODE = 9002;
    private static final String AI_RATE_LIMIT_MESSAGE = "AI 请求过于频繁，请稍后再试";

    private final AiGuardProperties properties;
    private final RedisCache redisCache;

    public void check(AiRequestContext context) {
        if (context == null || !properties.isRateLimitEnabled()) {
            return;
        }
        String identity = context.getUserId() != null ? "user:" + context.getUserId() : "ip:" + safeIdentity(context.getClientIp());
        String key = "ai:rate:" + context.getType() + ":" + identity;
        long count = redisCache.increment(key, 1, properties.getRateLimitWindowSeconds());
        if (count > properties.getRateLimitMaxRequests()) {
            logRateLimit(context, key, count);
            throw new MyRuntimeException(AI_RATE_LIMIT_CODE, AI_RATE_LIMIT_MESSAGE);
        }
    }

    private String safeIdentity(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        return value.replace(":", "_");
    }

    private void logRateLimit(AiRequestContext context, String key, long count) {
        JSONObject payload = new JSONObject();
        payload.put("event", "ai_rate_limit");
        payload.put("requestId", context.getRequestId());
        payload.put("type", context.getType());
        payload.put("userId", context.getUserId());
        payload.put("clientIp", context.getClientIp());
        payload.put("key", key);
        payload.put("count", count);
        payload.put("windowSeconds", properties.getRateLimitWindowSeconds());
        log.warn("AI_RATE_LIMIT {}", JSON.toJSONString(payload));
    }
}
