package cn.edu.ncu.medical.ai;

import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.utils.RedisCache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiRateLimiterTest {

    @Test
    void rateLimiterBlocksAfterThreshold() {
        AiGuardProperties properties = new AiGuardProperties();
        properties.setRateLimitEnabled(true);
        properties.setRateLimitWindowSeconds(60);
        properties.setRateLimitMaxRequests(2);
        RedisCache redisCache = new RedisCache(null, true);
        AiRateLimiter limiter = new AiRateLimiter(properties, redisCache);

        AiRequestContext context = new AiRequestContext(
                "req-1",
                "doctor_assist",
                "127.0.0.1",
                1L,
                "/treat/ai/doctor/assist",
                "JUnit"
        );

        limiter.check(context);
        limiter.check(context);
        MyRuntimeException ex = assertThrows(MyRuntimeException.class, () -> limiter.check(context));
        assertEquals(9002, ex.getCode());
    }
}
