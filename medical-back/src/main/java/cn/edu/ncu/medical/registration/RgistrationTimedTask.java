package cn.edu.ncu.medical.registration;

import cn.edu.ncu.medical.constant.RegistrationStatus;
import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.service.RegistrationService;
import cn.edu.ncu.medical.service.ScheduleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

@Component
public class RgistrationTimedTask {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private ScheduleService scheduleService;
    /**
     * 每天中午12点和下午6点将挂起号变为失效号
     * cron表达式：秒 分 时 日 月 周
     * 0 0 12,18 * * ? 表示每天12:00和18:00执行
     */

    @Scheduled(cron = "0 0 12,18 * * ?")
    public void updateRegistrationStatusToInvalid() {
        //先根据上午加当天日期查询挂号信息
        LocalDate today = LocalDate.now();

        LocalTime currentTime = LocalTime.now();

        // 根据当前时间判断是上午场还是下午场
        boolean isMorningSession = currentTime.equals(LocalTime.of(12, 0));

        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getIsDeleted, 0)
                .eq(Schedule::getScheduleDate, today);

        if(isMorningSession){
            queryWrapper.eq(Schedule::getIsMorning, 1);
        }else{
            queryWrapper.eq(Schedule::getIsAfternoon, 1);
        }


        List<Schedule> scheduleList = scheduleService.list(queryWrapper);
        for (Schedule schedule : scheduleList) {
            LambdaQueryWrapper<Registration> registrationQueryWrapper = new LambdaQueryWrapper<>();
            registrationQueryWrapper.eq(Registration::getScheduleId, schedule.getId())
                    .eq(Registration::getIsDeleted, 0)
                    .eq(Registration::getRegistrationStatus,RegistrationStatus.SUSPENDED.getCode());//挂起号
            List<Registration> registrationList = registrationService.list(registrationQueryWrapper);
            for (Registration registration : registrationList) {
                registration.setRegistrationStatus(RegistrationStatus.INVALID.getCode());//失效号
                registrationService.updateById(registration);
            }
        }
    }

    /**
     * 每天早上8点和下午2点将已支付的挂号变为排队中
     * cron表达式：0 0 8 * * ? 表示每天8:00执行
     */
    @Scheduled(cron = "0 0 8,14 * * ?")
    public void updateRegistrationToQueuing(){

        LocalDate today = LocalDate.now();

        LocalTime currentTime = LocalTime.now();


        // 根据当前时间判断是上午场还是下午场
        boolean isMorningSession = currentTime.equals(LocalTime.of(8, 0));

        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getIsDeleted, 0)
                .eq(Schedule::getScheduleDate, today);

        if(isMorningSession){
            queryWrapper.eq(Schedule::getIsMorning, 1);
        }else{
            queryWrapper.eq(Schedule::getIsAfternoon, 1);
        }


        List<Schedule> scheduleList = scheduleService.list(queryWrapper);
        for (Schedule schedule : scheduleList) {
            LambdaQueryWrapper<Registration> registrationQueryWrapper = new LambdaQueryWrapper<>();
            registrationQueryWrapper.eq(Registration::getScheduleId, schedule.getId())
                    .eq(Registration::getIsDeleted, 0)
                    .eq(Registration::getRegistrationStatus,RegistrationStatus.PAID.getCode());//已支付
            List<Registration> registrationList = registrationService.list(registrationQueryWrapper);
            for (Registration registration : registrationList) {
                registration.setRegistrationStatus(RegistrationStatus.QUEUING.getCode());//排队中
                registrationService.updateById(registration);
            }
        }
    }
}
