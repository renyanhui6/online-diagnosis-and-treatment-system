package cn.edu.ncu.medical.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimpleWebSocketSessionRegistry {

    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

    private final Map<String, Map<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final Map<Long, WebSocketSession> patientSessions = new ConcurrentHashMap<>();
    private final Map<Long, WebSocketSession> doctorSessions = new ConcurrentHashMap<>();

    public WebSocketSession register(String roomId, Long userId, boolean longConnection, WebSocketSession session) {
        WebSocketSession wrapped = wrap(session);
        roomSessions.computeIfAbsent(roomId, key -> new ConcurrentHashMap<>()).put(wrapped.getId(), wrapped);
        if (longConnection && userId != null) {
            if (roomId.startsWith("patient_")) {
                patientSessions.put(userId, wrapped);
            } else if (roomId.startsWith("doctor_")) {
                doctorSessions.put(userId, wrapped);
            }
        }
        return wrapped;
    }

    public void remove(WebSocketSession session) {
        if (session == null) {
            return;
        }
        roomSessions.values().forEach(group -> group.remove(session.getId()));
        roomSessions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        patientSessions.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
        doctorSessions.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
    }

    public void broadcast(String roomId, String payload) {
        Map<String, WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions.values()) {
            send(session, message);
        }
    }

    public void sendToPatient(Long patientId, String payload) {
        if (patientId == null) {
            return;
        }
        send(patientSessions.get(patientId), new TextMessage(payload));
    }

    public void sendToDoctor(Long doctorId, String payload) {
        if (doctorId == null) {
            return;
        }
        send(doctorSessions.get(doctorId), new TextMessage(payload));
    }

    private WebSocketSession wrap(WebSocketSession session) {
        if (session instanceof ConcurrentWebSocketSessionDecorator) {
            return session;
        }
        return new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES);
    }

    private void send(WebSocketSession session, TextMessage message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(message);
        } catch (IOException ex) {
            remove(session);
        }
    }
}
