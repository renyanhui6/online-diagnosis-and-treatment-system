package cn.edu.ncu.medical.ai;

import cn.edu.ncu.medical.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class AiRequestContextTest {

    @Test
    void resolveUserIdFromAccessKey() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String token = JwtUtil.createToken(123L, "doctor_user");
        request.addHeader("access-key", token);
        request.setRequestURI("/treat/ai/doctor/assist");
        request.addHeader("User-Agent", "JUnit");
        request.setRemoteAddr("10.0.0.1");

        AiRequestContext context = AiRequestContext.from(request, "doctor_assist");

        assertEquals(123L, context.getUserId());
        assertEquals("10.0.0.1", context.getClientIp());
        assertEquals("/treat/ai/doctor/assist", context.getPath());
        assertEquals("JUnit", context.getUserAgent());
    }
}
