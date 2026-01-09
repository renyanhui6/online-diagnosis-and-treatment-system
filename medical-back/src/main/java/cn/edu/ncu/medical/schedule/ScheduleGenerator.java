package cn.edu.ncu.medical.schedule;

import cn.edu.ncu.medical.entity.*;
import cn.edu.ncu.medical.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.schedule", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScheduleGenerator {
    
    @Autowired
    private ScheduleTemplateMapper templateMapper;
    @Autowired
    private ScheduleMapper scheduleMapper;
    @Autowired
    private DoctorDetailMapper doctorMapper;
    @Autowired
    private SubDepartmentMapper subDepartmentMapper;

    /**
     * 未来多少天的排班需要“保证存在”。
     * 说明：生成逻辑是幂等的（existsSchedule + insert），所以可重复执行做补偿。
     */
    @Value("${app.schedule.generate-days-ahead:7}")
    private int daysAhead;

    @Value("${app.schedule.skip-weekends:true}")
    private boolean skipWeekends;

    /**
     * 启动后先补齐一次（避免错过凌晨定时任务导致当天无排班）。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ensureOnStartup() {
        safeEnsureSchedules("startup");
    }

    /**
     * 定期补偿：即使错过某次 cron，也能在下一次周期补齐。
     */
    @Scheduled(fixedDelayString = "${app.schedule.ensure-interval-ms:3600000}")
    public void ensureSchedulesPeriodically() {
        safeEnsureSchedules("periodic");
    }

    public void ensureSchedulesForNextDays(int daysAhead) {
        LocalDate today = LocalDate.now();
        int safeDaysAhead = Math.max(0, daysAhead);
        for (int i = 0; i <= safeDaysAhead; i++) {
            generateDailySchedules(today.plusDays(i));
        }
    }

    private void safeEnsureSchedules(String trigger) {
        try {
            ensureSchedulesForNextDays(daysAhead);
        } catch (Exception e) {
            log.warn("Ensure schedules failed (trigger={}, daysAhead={})", trigger, daysAhead, e);
        }
    }
    //生成当天的排版
    private void generateDailySchedules(LocalDate date) {
        // 1. 获取星期几 (1-7)
        int dayOfWeek = date.getDayOfWeek().getValue();
        //如果是周六日那么就不生成排版
        if (skipWeekends && (dayOfWeek == 6 || dayOfWeek == 7)) {
            return;
        }
        // 2. 查询当天的排班模板
        List<ScheduleTemplate> templates = templateMapper.findByWeekDay(dayOfWeek);
        
        // 3. 生成排班记录
       templates.stream().filter(t->t.getIsActive()==1)
               .forEach(t->generateForTemplate(t,date));
    }
    
    private void generateForTemplate(ScheduleTemplate template, LocalDate date) {
        DoctorDetail doctor = doctorMapper.selectById(template.getDoctorId());
        if (doctor == null) return;
        
        SubDepartment subDepartment = subDepartmentMapper.selectById(doctor.getSubDepartmentId());
        if (subDepartment == null) return;
        
        // 转换为SQL日期
        Date sqlDate = Date.valueOf(date);
        
        // 生成上午排班
        if (template.getMorningLimit() > 0) {
            createShiftSchedule(sqlDate, true, template, doctor, subDepartment);
        }
        
        // 生成下午排班
        if (template.getAfternoonLimit() > 0) {
            createShiftSchedule(sqlDate, false, template, doctor, subDepartment);
        }
    }
    
    private void createShiftSchedule(Date sqlDate, boolean isMorning, 
                                    ScheduleTemplate template, 
                                    DoctorDetail doctor, SubDepartment subDepartment) {
        // 检查是否已存在相同排班
        int exists = scheduleMapper.existsSchedule(
            doctor.getId(), sqlDate, isMorning);
        
        if (exists == 0) {
            Schedule schedule = new Schedule();
            schedule.setSubDepartmentId(subDepartment.getId());
            schedule.setDepartmentName(subDepartment.getDepartmentName());
            schedule.setDoctorId(doctor.getId());
            schedule.setDoctorName(doctor.getRealName());
            schedule.setScheduleDate(sqlDate);
            schedule.setIsMorning(isMorning ? 1 : 0);
            schedule.setIsAfternoon(isMorning ? 0 : 1);
            schedule.setStatus(1); // 0=正常上班
            schedule.setTemplateId(template.getId());
            schedule.setCurrentAppointmentCount(0);
            schedule.setAppointmentLimit(isMorning ? 
                template.getMorningLimit() : template.getAfternoonLimit());
            scheduleMapper.insert(schedule);
        }
    }
}
