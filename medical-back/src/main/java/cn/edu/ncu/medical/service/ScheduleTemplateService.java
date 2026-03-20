package cn.edu.ncu.medical.service;

import cn.edu.ncu.medical.entity.ScheduleTemplate;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @description 针对表【schedule_template】的数据库操作Service
 */
public interface ScheduleTemplateService extends IService<ScheduleTemplate> {
	IPage<ScheduleTemplate> findByPage(IPage<ScheduleTemplate> page, Long doctorId);
}
