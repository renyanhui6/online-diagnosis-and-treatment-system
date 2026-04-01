package cn.edu.ncu.medical.websocket;

import cn.edu.ncu.medical.entity.ChatMessage;
import cn.edu.ncu.medical.entity.Room;
import cn.edu.ncu.medical.service.ChatMessageService;
import cn.edu.ncu.medical.service.RoomService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final SimpleWebSocketSessionRegistry sessionRegistry;
    private final ChatMessageService chatMessageService;
    private final RoomService roomService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = (String) session.getAttributes().get("roomId");
        Long userId = (Long) session.getAttributes().get("userId");
        boolean longConnection = Boolean.TRUE.equals(session.getAttributes().get("longConnection"));
        WebSocketSession activeSession = sessionRegistry.register(roomId, userId, longConnection, session);

        Map<String, Object> connection = new HashMap<>();
        connection.put("type", "connection");
        connection.put("roomId", roomId);
        connection.put("timestamp", new Date());
        activeSession.sendMessage(new TextMessage(JSON.toJSONString(connection)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String text = message.getPayload();
        JSONObject json = JSON.parseObject(text);
        String type = json.getString("type");
        String roomId = (String) session.getAttributes().get("roomId");
        Long userId = (Long) session.getAttributes().get("userId");

        if ("ping".equalsIgnoreCase(type)) {
            Map<String, Object> pong = new HashMap<>();
            pong.put("type", "pong");
            pong.put("timestamp", System.currentTimeMillis());
            session.sendMessage(new TextMessage(JSON.toJSONString(pong)));
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

        Long resolvedRoomId = resolveRoomId(json, roomId);
        if (resolvedRoomId != null) {
            sessionRegistry.broadcast(String.valueOf(resolvedRoomId), text);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error", exception);
        sessionRegistry.remove(session);
        try {
            session.close();
        } catch (Exception ignored) {
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
        chatMessage.setMessageType(json.getInteger("messageType") != null ? json.getInteger("messageType") : json.getInteger("message_type"));
        if (chatMessage.getMessageType() == null) {
            chatMessage.setMessageType(1);
        }
        chatMessage.setContent(json.getString("content"));
        chatMessage.setCreateTime(new Date());
        chatMessageService.save(chatMessage);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "chat");
        payload.put("roomId", roomId);
        payload.put("senderId", chatMessage.getSenderId());
        payload.put("senderType", chatMessage.getSenderType());
        payload.put("messageType", chatMessage.getMessageType());
        payload.put("content", chatMessage.getContent());
        payload.put("createTime", chatMessage.getCreateTime());
        sessionRegistry.broadcast(roomId, JSON.toJSONString(payload));
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
        if (registrationId != null) {
            ChatController.respondToConsultationInternal(registrationId, response);
        }
    }

    private void handleRoomStatusUpdate(JSONObject json, String fallbackRoomId) {
        Integer status = resolveRoomStatus(json);
        Long roomId = resolveRoomId(json, fallbackRoomId);
        if (status == null || roomId == null) {
            return;
        }
        Room room = roomService.getById(roomId);
        if (room != null) {
            room.setRoomStatus(status);
            room.setUpdateTime(new Date());
            roomService.updateById(room);
        }
        Map<String, Object> statusMessage = new HashMap<>();
        statusMessage.put("type", "room_status_update");
        statusMessage.put("room_status", status);
        statusMessage.put("roomId", roomId);
        statusMessage.put("timestamp", new Date());
        sessionRegistry.broadcast(String.valueOf(roomId), JSON.toJSONString(statusMessage));
    }

    private void broadcastStatusMessage(JSONObject json, String fallbackRoomId) {
        Long roomId = resolveRoomId(json, fallbackRoomId);
        if (roomId != null) {
            sessionRegistry.broadcast(String.valueOf(roomId), JSON.toJSONString(json));
        }
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
        sessionRegistry.broadcast(String.valueOf(roomId), JSON.toJSONString(notification));
    }

    private Long resolveRoomId(JSONObject json, String fallbackRoomId) {
        Long roomId = json.getLong("roomId");
        if (roomId != null) {
            return roomId;
        }
        JSONObject data = json.getJSONObject("data");
        if (data != null && data.getLong("roomId") != null) {
            return data.getLong("roomId");
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
        if (data.getLong("registrationId") != null) {
            return data.getLong("registrationId");
        }
        return data.getLong("consultationId");
    }

    private String resolveResponse(JSONObject json) {
        if (json.getString("response") != null) {
            return json.getString("response");
        }
        JSONObject data = json.getJSONObject("data");
        return data == null ? null : data.getString("response");
    }

    private Integer resolveRoomStatus(JSONObject json) {
        if (json.getInteger("room_status") != null) {
            return json.getInteger("room_status");
        }
        if (json.getInteger("roomStatus") != null) {
            return json.getInteger("roomStatus");
        }
        JSONObject data = json.getJSONObject("data");
        if (data == null) {
            return null;
        }
        return data.getInteger("room_status") != null ? data.getInteger("room_status") : data.getInteger("roomStatus");
    }

    private Long safeLong(String value) {
        try {
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
