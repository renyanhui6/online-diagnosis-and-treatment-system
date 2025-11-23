package cn.edu.ncu.medical.mapper;

import cn.edu.ncu.medical.entity.ScheduleTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
* @author star
* @description 针对表【schedule_template】的数据库操作Mapper
* @createDate 2025-07-31 00:43:27
* @Entity cn.edu.ncu.medical.entity.ScheduleTemplate
*/
public interface ScheduleTemplateMapper extends BaseMapper<ScheduleTemplate> {

	List<ScheduleTemplate> findByWeekDay(int dayOfWeek);

	IPage<ScheduleTemplate> selectByPage(IPage<ScheduleTemplate> page, Long doctorId);
}




