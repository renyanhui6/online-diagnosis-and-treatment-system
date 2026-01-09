package cn.edu.ncu.medical.netty;

import cn.edu.ncu.medical.entity.ChatMessage;
import cn.edu.ncu.medical.entity.Room;
import cn.edu.ncu.medical.service.ChatMessageService;
import cn.edu.ncu.medical.service.RoomService;
import cn.edu.ncu.medical.websocket.ChatController;
import cn.edu.ncu.medical.websocket.ChatWebSocket;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 文本帧处理：心跳、简单聊天广播、长连接通知。
 */
@Slf4j
public class NettyTextFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final NettySessionRegistry sessionRegistry;
    private final ChatMessageService chatMessageService;
    private final RoomService roomService;

    public NettyTextFrameHandler(NettySessionRegistry sessionRegistry, ChatMessageService chatMessageService, RoomService roomService) {
        this.sessionRegistry = sessionRegistry;
        this.chatMessageService = chatMessageService;
        this.roomService = roomService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String text = frame.text();
        JSONObject json = JSON.parseObject(text);
        String type = json.getString("type");
        String roomId = ctx.channel().attr(NettySessionRegistry.ATTR_ROOM).get();
        Long userId = ctx.channel().attr(NettySessionRegistry.ATTR_USER_ID).get();

        if ("ping".equalsIgnoreCase(type)) {
            Map<String, Object> pong = new HashMap<>();
            pong.put("type", "pong");
            pong.put("timestamp", System.currentTimeMillis());
            ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(pong)));
            return;
        }

        if ("consultation_response".equalsIgnoreCase(type) || "patient_response".equalsIgnoreCase(type)) {
            handleConsultationResponse(json, roomId);
            return;
        }

        if ("room_status_update".equalsIgnoreCase(type)) {
            handleRoomStatusUpdate(json, roomId);
            return;
        }

        if ("status".equalsIgnoreCase(type)) {
            broadcastStatusMessage(json, roomId);
            return;
        }

        if ("ready".equalsIgnoreCase(type)) {
            handlePatientReady(roomId);
            return;
        }

        if ("chat".equalsIgnoreCase(type) || json.containsKey("content")) {
            handleChatMessage(json, roomId, userId);
            return;
        }

        // 未识别类型，原样广播到房间，保证与旧前端兼容
        Long resolvedRoomId = resolveRoomId(json, roomId);
        if (resolvedRoomId != null) {
            String resolvedRoomIdText = String.valueOf(resolvedRoomId);
            sessionRegistry.broadcast(resolvedRoomIdText, text);
            ChatWebSocket.broadcastToChatRoom(resolvedRoomIdText, json);
        }
    }

    private void handleChatMessage(JSONObject json, String roomId, Long userId) {
        if (roomId == null) {
            return;
        }
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setRoomId(safeLong(roomId));
        chatMessage.setSenderId(json.getLong("senderId") != null ? json.getLong("senderId") : userId);
        chatMessage.setSenderType(json.getInteger("senderType") != null ? json.getInteger("senderType") : json.getInteger("sender_type"));
        chatMessage.setMessageType(json.getInteger("messageType") != null ? json.getInteger("messageType") : 1);
        chatMessage.setContent(json.getString("content"));
        chatMessage.setCreateTime(new Date());

        try {
            chatMessageService.save(chatMessage);
        } catch (Exception e) {
            log.error("保存聊天消息失败", e);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "chat");
        payload.put("roomId", roomId);
        payload.put("senderId", chatMessage.getSenderId());
        payload.put("senderType", chatMessage.getSenderType());
        payload.put("messageType", chatMessage.getMessageType());
        payload.put("content", chatMessage.getContent());
        payload.put("createTime", chatMessage.getCreateTime());

        sessionRegistry.broadcast(roomId, JSON.toJSONString(payload));
        // 灰度并存：同步广播到旧的 Spring WebSocket 房间，避免两端用户无法互通
        ChatWebSocket.broadcastToChatRoom(roomId, payload);
    }

    private void handleConsultationResponse(JSONObject json, String fallbackRoomId) {
        String response = resolveResponse(json);
        if (response == null) {
            return;
        }
        Long registrationId = resolveRegistrationId(json);
        if (registrationId == null) {
            Long roomId = resolveRoomId(json, fallbackRoomId);
            if (roomId != null) {
                Room room = roomService.getById(roomId);
                if (room != null) {
                    registrationId = room.getRegistrationId();
                }
            }
        }
        if (registrationId == null) {
            return;
        }
        ChatController.respondToConsultationInternal(registrationId, response);
    }

    private void handleRoomStatusUpdate(JSONObject json, String fallbackRoomId) {
        Integer status = resolveRoomStatus(json);
        Long roomId = resolveRoomId(json, fallbackRoomId);
        if (status == null || roomId == null) {
            return;
        }

        try {
            Room room = roomService.getById(roomId);
            if (room != null) {
                room.setRoomStatus(status);
                room.setUpdateTime(new Date());
                roomService.updateById(room);
            }
        } catch (Exception e) {
            log.error("更新房间状态失败, roomId={}, status={}", roomId, status, e);
        }

        Map<String, Object> statusMessage = new HashMap<>();
        statusMessage.put("type", "room_status_update");
        statusMessage.put("room_status", status);
        statusMessage.put("roomId", roomId);
        statusMessage.put("timestamp", new Date());

        String roomIdText = String.valueOf(roomId);
        sessionRegistry.broadcast(roomIdText, JSON.toJSONString(statusMessage));
        ChatWebSocket.broadcastToChatRoom(roomIdText, statusMessage);
    }

    private void broadcastStatusMessage(JSONObject json, String fallbackRoomId) {
        Long roomId = resolveRoomId(json, fallbackRoomId);
        if (roomId == null) {
            return;
        }
        String roomIdText = String.valueOf(roomId);
        sessionRegistry.broadcast(roomIdText, JSON.toJSONString(json));
        ChatWebSocket.broadcastToChatRoom(roomIdText, json);
    }

    private void handlePatientReady(String fallbackRoomId) {
        Long roomId = safeLong(fallbackRoomId);
        if (roomId == null) {
            return;
        }
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "patient_ready");
        notification.put("roomId", roomId);
        notification.put("timestamp", new Date());

        String roomIdText = String.valueOf(roomId);
        sessionRegistry.broadcast(roomIdText, JSON.toJSONString(notification));
        ChatWebSocket.broadcastToChatRoom(roomIdText, notification);
    }

    private Long resolveRoomId(JSONObject json, String fallbackRoomId) {
        Long roomId = json.getLong("roomId");
        if (roomId != null) {
            return roomId;
        }
        JSONObject data = json.getJSONObject("data");
        if (data != null) {
            roomId = data.getLong("roomId");
            if (roomId != null) {
                return roomId;
            }
        }
        return safeLong(fallbackRoomId);
    }

    private Long resolveRegistrationId(JSONObject json) {
        Long registrationId = json.getLong("registrationId");
        if (registrationId != null) {
            return registrationId;
        }
        registrationId = json.getLong("consultationId");
        if (registrationId != null) {
            return registrationId;
        }
        JSONObject data = json.getJSONObject("data");
        if (data == null) {
            return null;
        }
        registrationId = data.getLong("registrationId");
        if (registrationId != null) {
            return registrationId;
        }
        return data.getLong("consultationId");
    }

    private String resolveResponse(JSONObject json) {
        String response = json.getString("response");
        if (response != null) {
            return response;
        }
        JSONObject data = json.getJSONObject("data");
        if (data == null) {
            return null;
        }
        return data.getString("response");
    }

    private Integer resolveRoomStatus(JSONObject json) {
        Integer status = json.getInteger("room_status");
        if (status != null) {
            return status;
        }
        status = json.getInteger("roomStatus");
        if (status != null) {
            return status;
        }
        JSONObject data = json.getJSONObject("data");
        if (data == null) {
            return null;
        }
        status = data.getInteger("room_status");
        if (status != null) {
            return status;
        }
        return data.getInteger("roomStatus");
    }

    private Long safeLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            log.info("连接空闲，关闭 channel {}", ctx.channel().id());
            ctx.close();
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        sessionRegistry.remove(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket 处理异常", cause);
        ctx.close();
    }
}
