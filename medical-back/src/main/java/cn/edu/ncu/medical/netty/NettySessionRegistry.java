package cn.edu.ncu.medical.netty;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理 Netty 连接的简单注册表：房间广播与医生/患者长连接单播
 */
@Component
public class NettySessionRegistry {

    public static final AttributeKey<Long> ATTR_USER_ID = AttributeKey.valueOf("userId");
    public static final AttributeKey<String> ATTR_USERNAME = AttributeKey.valueOf("username");
    public static final AttributeKey<String> ATTR_ROLE = AttributeKey.valueOf("role");
    public static final AttributeKey<String> ATTR_ROOM = AttributeKey.valueOf("roomId");
    public static final AttributeKey<Boolean> ATTR_LONG_CONN = AttributeKey.valueOf("longConnection");

    private final Map<String, ChannelGroup> roomChannels = new ConcurrentHashMap<>();
    private final Map<Long, Channel> patientChannels = new ConcurrentHashMap<>();
    private final Map<Long, Channel> doctorChannels = new ConcurrentHashMap<>();

    public void addRoomChannel(String roomId, Channel channel) {
        roomChannels.computeIfAbsent(roomId, k -> new DefaultChannelGroup(GlobalEventExecutor.INSTANCE))
                .add(channel);
    }

    public void bindLongConnection(String roomId, Long userId, Channel channel) {
        if (roomId.startsWith("patient_")) {
            patientChannels.put(userId, channel);
        } else if (roomId.startsWith("doctor_")) {
            doctorChannels.put(userId, channel);
        }
    }

    public void remove(Channel channel) {
        // 清理房间集合
        roomChannels.values().forEach(group -> group.remove(channel));

        // 清理长连接映射
        patientChannels.values().removeIf(ch -> ch.id().equals(channel.id()));
        doctorChannels.values().removeIf(ch -> ch.id().equals(channel.id()));
    }

    public void broadcast(String roomId, String message) {
        ChannelGroup group = roomChannels.get(roomId);
        if (group != null) {
            group.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(message));
        }
    }

    public void sendToPatient(Long patientId, String message) {
        Channel channel = patientChannels.get(patientId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(message));
        }
    }

    public void sendToDoctor(Long doctorId, String message) {
        Channel channel = doctorChannels.get(doctorId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(message));
        }
    }
}
