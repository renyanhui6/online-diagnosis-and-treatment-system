package cn.edu.ncu.medical.config;

import cn.edu.ncu.medical.websocket.ChatWebSocketHandler;
import cn.edu.ncu.medical.websocket.WebSocketAuthHandshakeInterceptor;
import cn.edu.ncu.medical.websocket.WebSocketProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class SimpleWebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WebSocketAuthHandshakeInterceptor handshakeInterceptor;
    private final WebSocketProperties webSocketProperties;

    public SimpleWebSocketConfig(ChatWebSocketHandler chatWebSocketHandler,
                                 WebSocketAuthHandshakeInterceptor handshakeInterceptor,
                                 WebSocketProperties webSocketProperties) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.webSocketProperties = webSocketProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String path = webSocketProperties.getPath();
        registry.addHandler(chatWebSocketHandler, path, path + "/**")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
