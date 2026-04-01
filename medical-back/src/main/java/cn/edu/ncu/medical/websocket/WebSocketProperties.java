package cn.edu.ncu.medical.websocket;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.websocket")
@Data
public class WebSocketProperties {
    /**
     * 主 WebSocket 路径，不包含 roomId。
     */
    private String path = "/ws/chat";

    /**
     * 等待患者确认的超时时间（分钟）。
     */
    private int patientResponseTimeoutMinutes = 3;
}
