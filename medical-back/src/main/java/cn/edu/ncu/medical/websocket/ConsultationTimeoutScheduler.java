package cn.edu.ncu.medical.websocket;

import cn.edu.ncu.medical.constant.RegistrationStatus;
import cn.edu.ncu.medical.entity.Room;
import cn.edu.ncu.medical.service.RegistrationService;
import cn.edu.ncu.medical.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConsultationTimeoutScheduler implements DisposableBean {

    private final RoomService roomService;
    private final RegistrationService registrationService;
    private final WebSocketProperties webSocketProperties;
    private final SimpleWebSocketSessionRegistry sessionRegistry;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "consultation-timeout");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<Long, ScheduledFuture<?>> timeouts = new ConcurrentHashMap<>();

    public void schedulePatientResponseTimeout(Long registrationId, Long roomId) {
        if (registrationId == null) {
            return;
        }
        cancelPatientResponseTimeout(registrationId);
        ScheduledFuture<?> future = scheduler.schedule(
                () -> handleTimeout(registrationId, roomId),
                webSocketProperties.getPatientResponseTimeoutMinutes(),
                TimeUnit.MINUTES
        );
        timeouts.put(registrationId, future);
    }

    public void cancelPatientResponseTimeout(Long registrationId) {
        if (registrationId == null) {
            return;
        }
        ScheduledFuture<?> future = timeouts.remove(registrationId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void handleTimeout(Long registrationId, Long roomId) {
        try {
            Room room = roomId == null ? null : roomService.getById(roomId);
            if (room == null) {
                room = roomService.getRoomByRegistrationId(registrationId);
            }
            if (room == null || room.getRoomStatus() == null || room.getRoomStatus() != 1) {
                return;
            }

            room.setRoomStatus(4);
            room.setUpdateTime(new Date());
            roomService.updateById(room);
            registrationService.changeStatus(registrationId, RegistrationStatus.SUSPENDED.getCode());

            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "patient_timeout");
            notification.put("roomId", room.getId());
            notification.put("registrationId", registrationId);
            notification.put("timestamp", new Date());
            sessionRegistry.sendToDoctor(room.getDoctorId(), com.alibaba.fastjson.JSON.toJSONString(notification));
        } catch (Exception ex) {
            log.error("Consultation timeout failed, registrationId={}, roomId={}", registrationId, roomId, ex);
        } finally {
            timeouts.remove(registrationId);
        }
    }

    @Override
    public void destroy() {
        scheduler.shutdownNow();
        timeouts.clear();
    }
}
