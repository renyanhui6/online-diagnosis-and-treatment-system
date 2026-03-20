package cn.edu.ncu.medical.ai;

import cn.edu.ncu.medical.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class AiRequestContext {
    private final String requestId;
    private final String type;
    private final String clientIp;
    private final Long userId;
    private final String path;
    private final String userAgent;

    public static AiRequestContext from(HttpServletRequest request, String type) {
        String requestId = UUID.randomUUID().toString();
        String ip = resolveClientIp(request);
        Long userId = resolveUserId(request);
        String path = request != null ? request.getRequestURI() : "";
        String userAgent = request != null ? request.getHeader("User-Agent") : "";
        return new AiRequestContext(requestId, type, ip, userId, path, userAgent);
    }

    private static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        String ip = request.getRemoteAddr();
        return StringUtils.hasText(ip) ? ip : "unknown";
    }

    private static Long resolveUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String token = request.getHeader("access-key");
        if (!StringUtils.hasText(token)) {
            String auth = request.getHeader("Authorization");
            if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
                token = auth.substring(7);
            }
        }
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            Claims claims = JwtUtil.parseToken(token);
            return claims.get("userId", Long.class);
        } catch (Exception ignored) {
            return null;
        }
    }
}
