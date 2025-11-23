package cn.edu.ncu.medical.schedule;

import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.service.ScheduleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 每天的上午下班后之前的排班删除
 * 每天的下午下班后之前的排班删除
 */
@Component
public class ScheduleDeleted {

	@Autowired
	private ScheduleService scheduleService;
	/**
	 * 每天的上午下班后之前的排班删除
	 */
	// 中午12点删除当天上午排班
	@Scheduled(cron = "0 0 12 * * ?")
	public void deleteMorningSchedule(){
		LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Schedule::getIsMorning, 1)
				.eq(Schedule::getScheduleDate, new Date()); // 仅处理当天
		scheduleService.remove(wrapper); // 批量删除
	}

	// 下午6点删除当天下午排班
	@Scheduled(cron = "0 0 18 * * ?")
	public void deleteAfternoonSchedule(){
		LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Schedule::getIsAfternoon, 1)
				.eq(Schedule::getScheduleDate, new Date()); // 仅处理当天
		scheduleService.remove(wrapper); // 批量删除
	}

}
