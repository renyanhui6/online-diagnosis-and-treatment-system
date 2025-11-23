package cn.edu.ncu.medical.service;

import cn.edu.ncu.medical.entity.ScheduleTemplate;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author star
* @description 针对表【schedule_template】的数据库操作Service
* @createDate 2025-07-31 00:43:27
*/
public interface ScheduleTemplateService extends IService<ScheduleTemplate> {

	void removeTemplate(Long id);

	IPage<ScheduleTemplate> findByPage(IPage<ScheduleTemplate> page, Long doctorId);

	void modifyStatus(Long id,Integer isActive);
}
