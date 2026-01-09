package cn.edu.ncu.medical.websocket;

import cn.edu.ncu.medical.entity.ChatMessage;
import cn.edu.ncu.medical.entity.Room;
import cn.edu.ncu.medical.netty.NettySessionRegistry;
import cn.edu.ncu.medical.service.ChatMessageService;
import cn.edu.ncu.medical.service.RoomService;
import cn.edu.ncu.medical.utils.JwtUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@ServerEndpoint("/common/ws/chat/{roomId}")
public class ChatWebSocket {

    private static ChatMessageService chatMessageService;
    private static RoomService roomService;
    private static NettySessionRegistry nettySessionRegistry;


    @Autowired
    public void setChatMessageService(ChatMessageService chatMessageService) {
        ChatWebSocket.chatMessageService = chatMessageService;
    }

    @Autowired
    public void setRoomService(RoomService roomService) {
        ChatWebSocket.roomService = roomService;
    }

    @Autowired
    public void setNettySessionRegistry(NettySessionRegistry nettySessionRegistry) {
        ChatWebSocket.nettySessionRegistry = nettySessionRegistry;
    }


    // 存储患者长连接（用于接收问诊请求）
    private static final Map<Long, Session> patientLongConnections = new ConcurrentHashMap<>();
    // 存储医生长连接（用于接收问诊响应）
    private static final Map<Long, Session> doctorLongConnections = new ConcurrentHashMap<>();
    // 存储聊天房间连接（用于实时聊天）
    private static final Map<String, Set<Session>> chatRoomSessions = new ConcurrentHashMap<>();
    // 存储定时任务
    private static final Map<Long, ScheduledFuture<?>> responseTimers = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("roomId") String roomId) {
        try {
            // 从token中获取用户信息
            String token = extractTokenFromSession(session);
            if (token == null) {
                System.out.println("❌ Token为空，关闭连接");
                session.close();
                return;
            }

            UserInfo userInfo = getUserInfoFromToken(token);
            if (userInfo == null) {
                System.out.println("❌ Token解析失败，关闭连接");
                session.close();
                return;
            }

            // 根据房间ID判断连接类型
            if (roomId.startsWith("patient_")) {
                // 患者长连接
                Long patientId = Long.valueOf(roomId.substring(8));
                patientLongConnections.put(patientId, session);
                System.out.println("✅ 患者长连接已建立，患者ID: " + patientId);

                // 发送连接成功消息
                sendConnectionSuccess(session, "patient", patientId, roomId);

            } else if (roomId.startsWith("doctor_")) {
                // 医生长连接
                Long doctorId = Long.valueOf(roomId.substring(7));
                doctorLongConnections.put(doctorId, session);
                System.out.println("✅ 医生长连接已建立，医生ID: " + doctorId);

                // 发送连接成功消息
                sendConnectionSuccess(session, "doctor", doctorId, roomId);

            } else {
                // 聊天房间连接
                chatRoomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
                System.out.println("✅ 用户 " + userInfo.getUserId() + " 加入聊天房间: " + roomId);

                // 发送连接成功消息
                sendConnectionSuccess(session, userInfo.getRole().toLowerCase(), userInfo.getUserId(), roomId);
            }

        } catch (Exception e) {
            e.printStackTrace();
            try {
                session.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("roomId") String roomId) {
        try {
            JSONObject jsonMessage = JSON.parseObject(message);
            String type = (String) jsonMessage.get("type");

            switch (type) {
                case "ready":
                    handlePatientReady(jsonMessage, session, roomId);
                    break;
                case "consultation_request":
                    handleConsultationRequest(jsonMessage, session, roomId);
                    break;
                case "patient_response":
                    handlePatientResponse(jsonMessage, session, roomId);
                    break;
                case "patient_online":
                    handlePatientOnline(jsonMessage, session, roomId);
                    break;
                case "doctor_online":
                    handleDoctorOnline(jsonMessage, session, roomId);
                    break;
                case "chat":
                    handleChatMessage(jsonMessage, session, roomId);
                    break;
                case "room_status_update":
                    handleRoomStatusUpdate(jsonMessage, session, roomId);
                    break;
                case "status":
                    handleStatusMessage(jsonMessage, session, roomId);
                    break;
                case "ping":
                    handlePingMessage(jsonMessage, session, roomId);
                    break;
                default:
                    System.out.println("⚠️ 未知的消息类型: " + type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
// 处理状态消息
    private void handleStatusMessage(JSONObject message, Session session, String roomId) {
        try {
            System.out.println("📊 收到状态消息，房间ID: " + roomId);
            // 可以在这里处理状态更新逻辑
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 处理ping消息
    private void handlePingMessage(JSONObject message, Session session, String roomId) {
        try {
            System.out.println("�� 收到ping消息，房间ID: " + roomId);
            // 发送pong响应
            Map<String, Object> pongMessage = new HashMap<>();
            pongMessage.put("type", "pong");
            pongMessage.put("timestamp", new Date());
            session.getBasicRemote().sendText(JSON.toJSONString(pongMessage));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @OnClose
    public void onClose(Session session, @PathParam("roomId") String roomId) {
        try {
            System.out.println("�� WebSocket连接关闭，房间ID: " + roomId);

            // 从聊天房间中移除
            Set<Session> sessions = chatRoomSessions.get(roomId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    chatRoomSessions.remove(roomId);
                }
            }

            // 从长连接中移除
            if (roomId.startsWith("patient_")) {
                Long patientId = Long.valueOf(roomId.substring(8));
                patientLongConnections.remove(patientId);
                System.out.println("�� 患者长连接已关闭，患者ID: " + patientId);
            } else if (roomId.startsWith("doctor_")) {
                Long doctorId = Long.valueOf(roomId.substring(7));
                doctorLongConnections.remove(doctorId);
                System.out.println("�� 医生长连接已关闭，医生ID: " + doctorId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("❌ WebSocket错误: " + error.getMessage());
        error.printStackTrace();
    }

    // ========== WebSocket消息处理方法 ==========

    private void handlePatientReady(JSONObject message, Session session, String roomId) {
        try {
            System.out.println("�� 收到患者准备就绪消息，房间ID: " + roomId);

            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "patient_ready");
            notification.put("roomId", roomId);
            notification.put("timestamp", new Date());

            broadcastToChatRoom(roomId, notification);
            broadcastToNettyRoom(roomId, JSON.toJSONString(notification));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleConsultationRequest(JSONObject message, Session session, String roomId) {
        try {
            System.out.println("�� 收到问诊请求WebSocket消息: " + message.toJSONString());
            // 问诊请求应该通过REST API处理，这里只记录日志
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handlePatientResponse(JSONObject message, Session session, String roomId) {
        try {
            System.out.println("�� 收到患者响应WebSocket消息: " + message.toJSONString());

            String response = message.getString("response");
            Long registrationId = message.getLong("registrationId");

            // 调用ChatController的响应方法
            ChatController.respondToConsultationInternal(registrationId, response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handlePatientOnline(JSONObject message, Session session, String roomId) {
        try {
            Long patientId = message.getLong("patientId");
            System.out.println("✅ 患者 " + patientId + " 上线");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDoctorOnline(JSONObject message, Session session, String roomId) {
        try {
            Long doctorId = message.getLong("doctorId");
            System.out.println("✅ 医生 " + doctorId + " 上线");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleChatMessage(JSONObject message, Session session, String roomId) {
        try {
            System.out.println("📨 收到聊天消息，房间ID: " + roomId);

            // 保存消息到数据库
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setRoomId(Long.valueOf(roomId));
            chatMessage.setSenderType(message.getInteger("senderType")); // 必须设置
            chatMessage.setSenderId(message.getLong("senderId"));

            // 使用三元运算符处理默认值
            chatMessage.setMessageType(message.getInteger("messageType") != null ?
                    message.getInteger("messageType") : 1);

            chatMessage.setContent(message.getString("content"));
            chatMessage.setCreateTime(new Date());

            chatMessageService.save(chatMessage);

            // 广播给房间内所有连接
            broadcastToChatRoom(roomId, message);
            broadcastToNettyRoom(roomId, message.toJSONString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRoomStatusUpdate(JSONObject message, Session session, String roomId) {
        try {
            System.out.println("�� 收到房间状态更新，房间ID: " + roomId);

            Integer roomStatus = message.getInteger("room_status");
            Long roomIdLong = Long.valueOf(roomId);

            // 更新房间状态
            Room room = roomService.getById(roomIdLong);
            if (room != null) {
                room.setRoomStatus(roomStatus);
                room.setUpdateTime(new Date());
                roomService.updateById(room);

                // 广播状态更新给房间内所有用户
                Map<String, Object> statusMessage = new HashMap<>();
                statusMessage.put("type", "room_status_update");
                statusMessage.put("room_status", roomStatus);
                statusMessage.put("roomId", roomId);
                statusMessage.put("timestamp", new Date());

                broadcastToChatRoom(roomId, statusMessage);
                broadcastToNettyRoom(roomId, JSON.toJSONString(statusMessage));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void broadcastToNettyRoom(String roomId, String json) {
        if (nettySessionRegistry == null || roomId == null || json == null) {
            return;
        }
        // 长连接（patient_x/doctor_x）不做房间广播，避免误发
        boolean isNumericRoom = roomId.chars().allMatch(Character::isDigit);
        if (!isNumericRoom) {
            return;
        }
        nettySessionRegistry.broadcast(roomId, json);
    }

    // ========== 辅助方法 ==========

    private String extractTokenFromSession(Session session) {
        try {
            // 从查询参数获取token
            String queryString = session.getQueryString();
            if (queryString != null && queryString.contains("token=")) {
                String tokenPart = queryString.split("token=")[1];
                if (tokenPart.contains("&")) {
                    return tokenPart.split("&")[0];
                }
                return tokenPart;
            }

            System.out.println("⚠️ 无法从session中获取token");
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private UserInfo getUserInfoFromToken(String token) {
        try {


            Claims claims = JwtUtil.parseToken(token);

            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);

            if (userId == null || username == null) {
                System.out.println("❌ Token中缺少用户信息");
                return null;
            }

            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(userId);
            userInfo.setUsername(username);

            if (username.contains("doctor") || username.contains("_spec")) {
                userInfo.setRole("DOCTOR");
            } else {
                userInfo.setRole("PATIENT");
            }

            System.out.println("✅ Token解析成功，用户ID: " + userId + ", 用户名: " + username + ", 角色: " + userInfo.getRole());
            return userInfo;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendConnectionSuccess(Session session, String role, Long userId, String roomId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "connection");
            message.put("status", "connected");
            message.put("role", role.toUpperCase());
            message.put("userId", userId);
            message.put("roomId", roomId);
            message.put("timestamp", new Date());
            session.getBasicRemote().sendText(JSON.toJSONString(message));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========== 公共方法（供ChatController调用） ==========

    public static void sendToPatientLongConnection(Long patientId, Map<String, Object> notification) {
        String json = JSON.toJSONString(notification);

        // 同时向 Netty 长连接推送（灰度并存）
        if (nettySessionRegistry != null) {
            nettySessionRegistry.sendToPatient(patientId, json);
        }

        Session session = patientLongConnections.get(patientId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(json);
                System.out.println("📤 向患者 " + patientId + " 发送通知: " + notification.get("type"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ 患者 " + patientId + " 长连接不存在或已关闭");
        }
    }

    public static void sendToDoctorLongConnection(Long doctorId, Map<String, Object> notification) {
        String json = JSON.toJSONString(notification);

        // 同时向 Netty 长连接推送（灰度并存）
        if (nettySessionRegistry != null) {
            nettySessionRegistry.sendToDoctor(doctorId, json);
        }

        Session session = doctorLongConnections.get(doctorId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(json);
                System.out.println("�� 向医生 " + doctorId + " 发送通知: " + notification.get("type"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ 医生 " + doctorId + " 长连接不存在或已关闭");
        }
    }

    public static void broadcastToChatRoom(String roomId, Map<String, Object> message) {
        Set<Session> sessions = chatRoomSessions.get(roomId);
        if (sessions != null) {
            for (Session session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(JSON.toJSONString(message));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("�� 向聊天房间 " + roomId + " 广播消息: " + message.get("type"));
        } else {
            System.out.println("⚠️ 聊天房间 " + roomId + " 不存在或为空");
        }
    }

    public static void schedulePatientResponseTimeout(Long registrationId, Long roomId) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> future = executor.schedule(() -> {
            try {
                LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Room::getRegistrationId, registrationId);
                Room room = roomService.getOne(wrapper);
                if (room != null && room.getRoomStatus() == 1) {
                    room.setRoomStatus(4); // 4-超时
                    room.setUpdateTime(new Date());
                    roomService.updateById(room);

                    Map<String, Object> notification = new HashMap<>();
                    notification.put("type", "patient_timeout");
                    notification.put("roomId", roomId);
                    notification.put("registrationId", registrationId);
                    notification.put("timestamp", new Date());

                    sendToDoctorLongConnection(room.getDoctorId(), notification);
                    System.out.println("⏰ 患者响应超时，预约ID: " + registrationId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 3, TimeUnit.MINUTES);

        responseTimers.put(registrationId, future);
        System.out.println("⏰ 启动患者响应超时定时器，预约ID: " + registrationId);
    }

    // 用户信息内部类
    public static class UserInfo {
        private Long userId;
        private String username;
        private String role;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
