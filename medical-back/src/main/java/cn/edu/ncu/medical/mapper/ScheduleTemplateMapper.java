package cn.edu.ncu.medical.mapper;

import cn.edu.ncu.medical.entity.ScheduleTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description 针对表【schedule_template】的数据库操作Mapper
 */
@Mapper
public interface ScheduleTemplateMapper extends BaseMapper<ScheduleTemplate> {
	List<ScheduleTemplate> findByWeekDay(int weekDay);
}
