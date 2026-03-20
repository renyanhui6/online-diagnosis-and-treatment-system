package cn.edu.ncu.medical.registration;

import cn.edu.ncu.medical.constant.RegistrationStatus;
import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.service.RegistrationService;
import cn.edu.ncu.medical.service.ScheduleService;
import cn.edu.ncu.medical.utils.ScheduleTimePolicy;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.registration", name = "reconcile-enabled", havingValue = "true", matchIfMissing = true)
public class RgistrationTimedTask {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private ScheduleService scheduleService;

    /**
     * 旧实现依赖“精确命中 08:00/12:00/14:00/18:00”去推进状态：
     * - 服务如果宕机/错过定时点，会导致状态永远卡住。
     *
     * 新实现改为“补偿式对账”：固定间隔扫描近几天排班，按当前时间推导应当推进的状态并批量更新。
     */
    private final Clock clock = Clock.systemDefaultZone();

    @EventListener(ApplicationReadyEvent.class)
    public void reconcileOnStartup() {
        reconcileRegistrationStatus("startup");
    }

    @Scheduled(fixedDelayString = "${app.registration.reconcile-interval-ms:300000}")
    public void reconcilePeriodically() {
        reconcileRegistrationStatus("periodic");
    }

    private void reconcileRegistrationStatus(String trigger) {
        try {
            ZoneId zoneId = clock.getZone();
            LocalDateTime now = LocalDateTime.now(clock);

            LocalDate today = LocalDate.now(clock);
            LocalDate from = today.minusDays(7);

            Date fromDate = java.sql.Date.valueOf(from);
            Date toDate = java.sql.Date.valueOf(today);

            LambdaQueryWrapper<Schedule> scheduleQuery = new LambdaQueryWrapper<>();
            scheduleQuery.eq(Schedule::getIsDeleted, 0)
                    .ge(Schedule::getScheduleDate, fromDate)
                    .le(Schedule::getScheduleDate, toDate);

            List<Schedule> schedules = scheduleService.list(scheduleQuery);
            if (schedules == null || schedules.isEmpty()) {
                return;
            }

            for (Schedule schedule : schedules) {
                if (schedule == null || schedule.getScheduleDate() == null) {
                    continue;
                }

                ScheduleTimePolicy.Session session;
                try {
                    session = ScheduleTimePolicy.resolveSession(schedule);
                } catch (IllegalArgumentException e) {
                    log.warn("Skip invalid schedule session flags, scheduleId={}", schedule.getId());
                    continue;
                }

                LocalDate scheduleDate = ScheduleTimePolicy.toLocalDate(schedule.getScheduleDate(), zoneId);
                LocalDateTime start = ScheduleTimePolicy.sessionStart(scheduleDate, session);
                LocalDateTime end = ScheduleTimePolicy.sessionEnd(scheduleDate, session);

                // 1) 开诊后：PAID/RESUMED -> QUEUING
                if (!now.isBefore(start) && now.isBefore(end)) {
                    LambdaUpdateWrapper<Registration> toQueuing = new LambdaUpdateWrapper<>();
                    toQueuing.eq(Registration::getScheduleId, schedule.getId())
                            .eq(Registration::getIsDeleted, 0)
                            .in(Registration::getRegistrationStatus,
                                    RegistrationStatus.PAID.getCode(),
                                    RegistrationStatus.RESUMED.getCode())
                            .set(Registration::getRegistrationStatus, RegistrationStatus.QUEUING.getCode())
                            .set(Registration::getUpdateTime, new Date());
                    registrationService.update(toQueuing);
                }

                // 2) 下诊后：把仍未完成的号统一置为 INVALID，避免“错过定时点后永久卡死”
                if (!now.isBefore(end)) {
                    LambdaUpdateWrapper<Registration> toInvalid = new LambdaUpdateWrapper<>();
                    toInvalid.eq(Registration::getScheduleId, schedule.getId())
                            .eq(Registration::getIsDeleted, 0)
                            .in(Registration::getRegistrationStatus,
                                    RegistrationStatus.PENDING_PAYMENT.getCode(),
                                    RegistrationStatus.PAID.getCode(),
                                    RegistrationStatus.QUEUING.getCode(),
                                    RegistrationStatus.SUSPENDED.getCode(),
                                    RegistrationStatus.RESUMED.getCode(),
                                    RegistrationStatus.WAITING_CONFIRM.getCode())
                            .set(Registration::getRegistrationStatus, RegistrationStatus.INVALID.getCode())
                            .set(Registration::getUpdateTime, new Date());
                    registrationService.update(toInvalid);
                }
            }
        } catch (Exception e) {
            log.warn("Reconcile registration status failed (trigger={})", trigger, e);
        }
    }
}
