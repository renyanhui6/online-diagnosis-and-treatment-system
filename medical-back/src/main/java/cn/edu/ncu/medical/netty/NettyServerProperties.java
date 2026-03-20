package cn.edu.ncu.medical.netty;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Netty WebSocket 服务配置
 */
@Component
@ConfigurationProperties(prefix = "netty.ws")
@Data
public class NettyServerProperties {
    /**
     * 监听端口，默认 9001（避免占用应用端口）
     */
    private int port = 9001;

    /**
     * WebSocket 路径前缀（不含 roomId），默认 /netty/ws/chat
     */
    private String path = "/netty/ws/chat";

    /**
     * 空闲检测秒数（读写皆空闲）
     */
    private int idleSeconds = 90;

    /**
     * 是否启用 Netty 服务，便于灰度/回退
     */
    private boolean enabled = true;

    /**
     * 患者响应超时分钟数（问诊请求等待）
     */
    private int patientResponseTimeoutMinutes = 3;
}
