package cn.edu.ncu.medical.controller.admin;

import cn.edu.ncu.medical.entity.ScheduleTemplate;
import cn.edu.ncu.medical.exception.AppointmentException;
import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.ScheduleTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/back/admin/scheduleTemplate")
public class ScheduleTemplateController {

	@Autowired
	private ScheduleTemplateService scheduleTemplateService;

	@GetMapping("/findByPage")
	public Result<IPage<ScheduleTemplate>> findByPage(
			@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
			@RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
			@RequestParam(value = "doctorId", required = false) Long doctorId) {
		IPage<ScheduleTemplate> page = new Page<>(pageNum, pageSize);
		return Result.ok(scheduleTemplateService.findByPage(page, doctorId));
	}

	@PostMapping("/add")
	public Result add(@RequestBody ScheduleTemplate scheduleTemplate) {
		validateTemplate(scheduleTemplate, null);
		scheduleTemplateService.save(scheduleTemplate);
		return Result.ok();
	}

	@PostMapping("/update")
	public Result update(@RequestBody ScheduleTemplate scheduleTemplate) {
		if (scheduleTemplate == null || scheduleTemplate.getId() == null) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		validateTemplate(scheduleTemplate, scheduleTemplate.getId());
		scheduleTemplateService.updateById(scheduleTemplate);
		return Result.ok();
	}

	@GetMapping("/remove")
	public Result remove(@RequestParam("templateId") Long templateId) {
		scheduleTemplateService.removeById(templateId);
		return Result.ok();
	}

	private void validateTemplate(ScheduleTemplate scheduleTemplate, Long selfId) {
		if (scheduleTemplate == null || scheduleTemplate.getDoctorId() == null || scheduleTemplate.getDoctorId() <= 0) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		Integer weekDay = scheduleTemplate.getWeekDay();
		if (weekDay == null || weekDay < 1 || weekDay > 7) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}

		Integer morningLimit = scheduleTemplate.getMorningLimit() == null ? 0 : scheduleTemplate.getMorningLimit();
		Integer afternoonLimit = scheduleTemplate.getAfternoonLimit() == null ? 0 : scheduleTemplate.getAfternoonLimit();
		if (morningLimit < 0 || afternoonLimit < 0 || (morningLimit == 0 && afternoonLimit == 0)) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}

		scheduleTemplate.setMorningLimit(morningLimit);
		scheduleTemplate.setAfternoonLimit(afternoonLimit);
		scheduleTemplate.setIsActive(scheduleTemplate.getIsActive() == null ? 1 : scheduleTemplate.getIsActive());

		if (scheduleTemplate.getIsActive() != 1) {
			return;
		}

		LambdaQueryWrapper<ScheduleTemplate> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ScheduleTemplate::getDoctorId, scheduleTemplate.getDoctorId())
				.eq(ScheduleTemplate::getWeekDay, weekDay)
				.eq(ScheduleTemplate::getIsActive, 1);
		if (selfId != null) {
			queryWrapper.ne(ScheduleTemplate::getId, selfId);
		}

		if (scheduleTemplateService.count(queryWrapper) > 0) {
			throw new AppointmentException(ResultCodeEnum.REPEAT_SUBMIT.getCode(), "同一医生同一星期仅允许一个启用模板");
		}
	}
}
