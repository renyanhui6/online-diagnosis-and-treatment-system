package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.entity.ScheduleTemplate;
import cn.edu.ncu.medical.mapper.ScheduleTemplateMapper;
import cn.edu.ncu.medical.service.ScheduleTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @description 针对表【schedule_template】的数据库操作Service实现
 */
@Service
public class ScheduleTemplateServiceImpl extends ServiceImpl<ScheduleTemplateMapper, ScheduleTemplate>
		implements ScheduleTemplateService {

	@Autowired
	private ScheduleTemplateMapper scheduleTemplateMapper;

	@Override
	public IPage<ScheduleTemplate> findByPage(IPage<ScheduleTemplate> page, Long doctorId) {
		LambdaQueryWrapper<ScheduleTemplate> queryWrapper = new LambdaQueryWrapper<>();
		if (doctorId != null) {
			queryWrapper.eq(ScheduleTemplate::getDoctorId, doctorId);
		}
		queryWrapper.orderByDesc(ScheduleTemplate::getId);
		return scheduleTemplateMapper.selectPage(page, queryWrapper);
	}
}
