package cn.edu.ncu.medical.websocket;

import cn.edu.ncu.medical.constant.RedisConstant;
import cn.edu.ncu.medical.utils.JwtUtil;
import cn.edu.ncu.medical.utils.RedisCache;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

@Component
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final RedisCache redisCache;
    private final WebSocketProperties webSocketProperties;
    private final boolean authEnabled;

    public WebSocketAuthHandshakeInterceptor(RedisCache redisCache,
                                             WebSocketProperties webSocketProperties,
                                             @Value("${app.auth.enabled:true}") boolean authEnabled) {
        this.redisCache = redisCache;
        this.webSocketProperties = webSocketProperties;
        this.authEnabled = authEnabled;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        URI uri = request.getURI();
        String roomId = extractRoomId(uri.getPath());
        if (!StringUtils.hasText(roomId)) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        String token = extractToken(request, uri);
        Long userId = null;
        String username = null;

        if (authEnabled) {
            if (!StringUtils.hasText(token)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            try {
                Claims claims = JwtUtil.parseToken(token);
                userId = claims.get("userId", Long.class);
                username = claims.get("username", String.class);
            } catch (Exception ex) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            if (userId == null || !StringUtils.hasText(username)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            String redisToken = redisCache.getString(RedisConstant.LOGIN_TOKEN_PREFIX + userId);
            if (!token.equals(redisToken)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
        } else if (StringUtils.hasText(token)) {
            try {
                Claims claims = JwtUtil.parseToken(token);
                userId = claims.get("userId", Long.class);
                username = claims.get("username", String.class);
            } catch (Exception ignored) {
                userId = null;
                username = null;
            }
        }

        if (userId == null) {
            userId = parseUserIdFromRoom(roomId);
        }
        if (!StringUtils.hasText(username)) {
            username = "dev";
        }

        boolean longConnection = roomId.startsWith("patient_") || roomId.startsWith("doctor_");
        if (longConnection && userId == null) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        attributes.put("roomId", roomId);
        attributes.put("userId", userId);
        attributes.put("username", username);
        attributes.put("longConnection", longConnection);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractToken(ServerHttpRequest request, URI uri) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            if (StringUtils.hasText(token)) {
                return token;
            }
        }
        String query = uri.getQuery();
        if (!StringUtils.hasText(query)) {
            return null;
        }
        for (String part : query.split("&")) {
            if (part.startsWith("token=")) {
                return part.substring("token=".length());
            }
        }
        return null;
    }

    private String extractRoomId(String path) {
        String prefix = webSocketProperties.getPath();
        if (!StringUtils.hasText(prefix)) {
            return null;
        }
        int idx = path.indexOf(prefix + "/");
        if (idx >= 0) {
            return path.substring(idx + prefix.length() + 1);
        }
        if (path.endsWith(prefix)) {
            return null;
        }
        return null;
    }

    private Long parseUserIdFromRoom(String roomId) {
        if (roomId.startsWith("patient_")) {
            return safeParseLong(roomId.substring("patient_".length()));
        }
        if (roomId.startsWith("doctor_")) {
            return safeParseLong(roomId.substring("doctor_".length()));
        }
        return null;
    }

    private Long safeParseLong(String value) {
        try {
            return StringUtils.hasText(value) ? Long.valueOf(value) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
