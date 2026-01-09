package cn.edu.ncu.medical.service.impl;


import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.result.ResultCodeEnum;

import cn.edu.ncu.medical.service.RegistrationService;
import cn.edu.ncu.medical.service.ScheduleService;
import cn.edu.ncu.medical.schedule.ScheduleGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.edu.ncu.medical.entity.ScheduleTemplate;
import cn.edu.ncu.medical.service.ScheduleTemplateService;
import cn.edu.ncu.medical.mapper.ScheduleTemplateMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author star
 * @description 针对表【schedule_template】的数据库操作Service实现
 * @createDate 2025-07-31 00:43:27
 */
@Service
@Slf4j
public class ScheduleTemplateServiceImpl extends ServiceImpl<ScheduleTemplateMapper, ScheduleTemplate> implements ScheduleTemplateService {
	@Autowired
	private ScheduleTemplateMapper scheduleTemplateMapper;
	@Autowired
	private ScheduleService scheduleService;
	@Autowired
	private RegistrationService registrationService;
	@Autowired(required = false)
	private ScheduleGenerator scheduleGenerator;

	@Value("${app.schedule.generate-days-ahead:7}")
	private int scheduleDaysAhead;

	@Override
	public IPage<ScheduleTemplate> findByPage(IPage<ScheduleTemplate> page, Long doctorId) {
		return scheduleTemplateMapper.selectByPage(page, doctorId);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean save(ScheduleTemplate entity) {
		boolean saved = super.save(entity);
		if (saved && scheduleGenerator != null && entity != null && Integer.valueOf(1).equals(entity.getIsActive())) {
			try {
				scheduleGenerator.ensureSchedulesForNextDays(scheduleDaysAhead);
			} catch (Exception e) {
				log.warn("Ensure schedules after template save failed, templateId={}", entity.getId(), e);
			}
		}
		return saved;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void removeTemplate(Long id) {
		// 1. 检查模板是否存在
		if (scheduleTemplateMapper.selectById(id) == null) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}

		// 2. 获取关联排班ID
		LambdaQueryWrapper<Schedule> scheduleQuery = new LambdaQueryWrapper<>();
		scheduleQuery.select(Schedule::getId)
				.eq(Schedule::getTemplateId, id);
		List<Long> scheduleIds = scheduleService.list(scheduleQuery)
				.stream()
				.map(Schedule::getId)
				.collect(Collectors.toList());

		// 新增预约记录存在性检查
		checkForExistingRegistrations(scheduleIds);

		// 3. 删除模板
		if (scheduleTemplateMapper.deleteById(id) == 0) {
			throw new RuntimeException("模板删除失败");
		}

		if (scheduleIds.isEmpty()) return;

		// 4. 批量删除排班
		LambdaQueryWrapper<Schedule> deleteScheduleWrapper = new LambdaQueryWrapper<>();
		deleteScheduleWrapper.in(Schedule::getId, scheduleIds);
		scheduleService.remove(deleteScheduleWrapper);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void modifyStatus(Long id, Integer newStatus) {
		// 1. 获取模板
		ScheduleTemplate template = getById(id);
		if (template == null) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}

		// 2. 状态未变化直接返回
		if (template.getIsActive().equals(newStatus)) {
			return;
		}

		// 3. 获取关联排班ID
		LambdaQueryWrapper<Schedule> scheduleQuery = new LambdaQueryWrapper<>();
		scheduleQuery.select(Schedule::getId)
				.eq(Schedule::getTemplateId, id);
		List<Long> scheduleIds = scheduleService.list(scheduleQuery)
				.stream()
				.map(Schedule::getId)
				.collect(Collectors.toList());

		// 新增状态变更前检查
		if (newStatus == 0) {
			checkForExistingRegistrations(scheduleIds);
		}

		// 4. 更新模板状态
		LambdaUpdateWrapper<ScheduleTemplate> templateUpdate = new LambdaUpdateWrapper<>();
		templateUpdate.eq(ScheduleTemplate::getId, id)
				.set(ScheduleTemplate::getIsActive, newStatus);
		update(templateUpdate);

		// 5. 更新排班状态
		if (!scheduleIds.isEmpty()) {
			LambdaUpdateWrapper<Schedule> scheduleUpdate = new LambdaUpdateWrapper<>();
			scheduleUpdate.in(Schedule::getId, scheduleIds)
					.set(Schedule::getStatus, newStatus == 1 ? 1 : 0);
			scheduleService.update(scheduleUpdate);
		}

		// 6. 若从“禁用 -> 启用”，补齐未来排班（避免错过凌晨任务导致未来几天无排班）
		if (newStatus == 1) {
			if (scheduleGenerator != null) {
				try {
					scheduleGenerator.ensureSchedulesForNextDays(scheduleDaysAhead);
				} catch (Exception e) {
					log.warn("Ensure schedules after template enable failed, templateId={}", id, e);
				}
			}
		}
	}

	private void checkForExistingRegistrations(List<Long> scheduleIds) {
		if (scheduleIds == null || scheduleIds.isEmpty()) {
			return;
		}
		LambdaQueryWrapper<Registration> regQuery = new LambdaQueryWrapper<>();
		regQuery.in(Registration::getScheduleId, scheduleIds)
				.last("LIMIT 1");
		if (registrationService.count(regQuery) > 0) {
			throw new MyRuntimeException(ResultCodeEnum.FAIL);
		}
	}


}
