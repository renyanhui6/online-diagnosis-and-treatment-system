package cn.edu.ncu.medical.netty;

import cn.edu.ncu.medical.constant.RegistrationStatus;
import cn.edu.ncu.medical.entity.Room;
import cn.edu.ncu.medical.service.RegistrationService;
import cn.edu.ncu.medical.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Netty 侧问诊超时调度（替代旧 WebSocket 内部定时器）。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NettyConsultationTimeoutScheduler implements DisposableBean {

    private final RoomService roomService;
    private final RegistrationService registrationService;
    private final NettyServerProperties nettyServerProperties;
    private final NettySessionRegistry nettySessionRegistry;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "netty-consultation-timeout");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<Long, ScheduledFuture<?>> timeouts = new ConcurrentHashMap<>();

    public void schedulePatientResponseTimeout(Long registrationId, Long roomId) {
        if (registrationId == null) {
            return;
        }
        cancelPatientResponseTimeout(registrationId);

        int timeoutMinutes = nettyServerProperties.getPatientResponseTimeoutMinutes();
        ScheduledFuture<?> future = scheduler.schedule(() -> handleTimeout(registrationId, roomId),
                timeoutMinutes, TimeUnit.MINUTES);
        timeouts.put(registrationId, future);
        log.info("Consultation timeout scheduled, registrationId={}, roomId={}, minutes={}",
                registrationId, roomId, timeoutMinutes);
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
            Room room = null;
            if (roomId != null) {
                room = roomService.getById(roomId);
            }
            if (room == null) {
                room = roomService.getRoomByRegistrationId(registrationId);
            }
            if (room == null || room.getRoomStatus() == null || room.getRoomStatus() != 1) {
                return;
            }

            room.setRoomStatus(4); // 4-超时
            room.setUpdateTime(new Date());
            roomService.updateById(room);
            if (registrationService != null) {
                registrationService.changeStatus(registrationId, RegistrationStatus.SUSPENDED.getCode());
            }

            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "patient_timeout");
            notification.put("roomId", room.getId());
            notification.put("registrationId", registrationId);
            notification.put("timestamp", new Date());

            if (nettySessionRegistry != null) {
                nettySessionRegistry.sendToDoctor(room.getDoctorId(), com.alibaba.fastjson.JSON.toJSONString(notification));
            }
            log.info("Consultation timeout fired, registrationId={}, roomId={}", registrationId, room.getId());
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
