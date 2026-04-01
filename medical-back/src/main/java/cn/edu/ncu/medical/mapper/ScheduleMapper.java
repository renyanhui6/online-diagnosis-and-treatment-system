package cn.edu.ncu.medical.mapper;

import cn.edu.ncu.medical.entity.Schedule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Date;

/**
* @author star
* @description 针对表【schedule】的数据库操作Mapper
* @createDate 2025-07-24 17:44:39
* @Entity cn.edu.ncu.medical.entity.Schedule
*/
@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {


	Integer existsSchedule(@Param("doctorId") Long doctorId,@Param("scheduleDate") Date scheduleDate,@Param("isMorning") boolean isMorning);

	int takeAppointmentSlot(@Param("scheduleId") Long scheduleId);

	int releaseAppointmentSlot(@Param("scheduleId") Long scheduleId);

}


