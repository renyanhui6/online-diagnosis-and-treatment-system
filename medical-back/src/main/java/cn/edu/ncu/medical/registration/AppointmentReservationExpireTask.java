package cn.edu.ncu.medical.registration;

import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.service.RegistrationService;
import cn.edu.ncu.medical.service.ScheduleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.registration.reservation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AppointmentReservationExpireTask {
    private final AppointmentReservationRedisService reservationRedisService;
    private final RegistrationService registrationService;
    private final ScheduleService scheduleService;
    private final AppointmentReservationProperties properties;
    private final Clock clock = Clock.systemDefaultZone();

    public AppointmentReservationExpireTask(AppointmentReservationRedisService reservationRedisService,
                                            RegistrationService registrationService,
                                            ScheduleService scheduleService,
                                            AppointmentReservationProperties properties) {
        this.reservationRedisService = reservationRedisService;
        this.registrationService = registrationService;
        this.scheduleService = scheduleService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.registration.reservation.expire-scan-interval-ms:1000}")
    public void releaseExpiredReservations() {
        LocalDate today = LocalDate.now(clock);
        Date from = java.sql.Date.valueOf(today.minusDays(properties.getScanPastDays()));
        Date to = java.sql.Date.valueOf(today.plusDays(properties.getScanFutureDays()));

        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Schedule::getIsDeleted, 0)
                .ge(Schedule::getScheduleDate, from)
                .le(Schedule::getScheduleDate, to);

        List<Schedule> schedules = scheduleService.list(wrapper);
        if (schedules == null || schedules.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        for (Schedule schedule : schedules) {
            try {
                Set<String> expiredTokens = reservationRedisService.listExpiredTokens(
                        schedule.getId(),
                        now,
                        properties.getScanBatchSize()
                );
                for (String token : expiredTokens) {
                    registrationService.releaseExpiredReservation(schedule.getId(), token);
                }
            } catch (Exception ex) {
                log.warn("Scan expired appointment reservations failed, scheduleId={}", schedule.getId(), ex);
            }
        }
    }
}
