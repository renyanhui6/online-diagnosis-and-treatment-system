package cn.edu.ncu.medical.controller.admin;

import cn.edu.ncu.medical.entity.ScheduleTemplate;
import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.DoctorDetailService;
import cn.edu.ncu.medical.service.ScheduleTemplateService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("back/admin/scheduleTemplate")
public class ScheduleTemplateController {
	@Autowired
	private DoctorDetailService doctorDetailService;
	@Autowired
	private ScheduleTemplateService scheduleTemplateService;

	/**
	 * 获取医生列表
	 * 根据子科室id查询
	 *
	 * @return
	 */
	@GetMapping("/findDocList")
	public Result getList(@RequestParam("subDepartmentId") Long subDepartmentId) {
		return Result.ok(doctorDetailService.findDocList(subDepartmentId));
	}

	/**
	 * 获取排班模板列表
	 * 根据医生id查询模板记录
	 *
	 * @return
	 */
	@GetMapping("/findTemplateByPage")
	public Result<IPage<ScheduleTemplate>> findTemplateByPage(@RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum,
															  @RequestParam(value = "pageSize",defaultValue = "10") Integer pageSize,
															  @RequestParam(value = "doctorId",required = false) Long doctorId) {
		Page page = new Page<>(pageNum, pageSize);
		IPage<ScheduleTemplate> scheduleTemplatePage = scheduleTemplateService.findByPage(page,doctorId);
		return Result.ok(scheduleTemplatePage);
	}

	/**
	 * 添加排班模板
	 * @param scheduleTemplate
	 * @return
	 */
	@PostMapping("/add")
	public Result add(@RequestBody ScheduleTemplate scheduleTemplate) {
		if (scheduleTemplate== null||scheduleTemplate.getDoctorId()==null||scheduleTemplate.getWeekDay()==null||scheduleTemplate.getMorningLimit()==null||scheduleTemplate.getAfternoonLimit()==null||scheduleTemplate.getIsActive()==null) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		return Result.ok(scheduleTemplateService.save(scheduleTemplate));
	}

	/**
	 * 修改模板的状态
	 * 需要同步schedule表的状态
	 *
	 * @return
	 */
	@PostMapping("/modify")
	public Result modify(@RequestParam("scheduleTemplateId")Long scheduleTemplateId,@RequestParam("isActive") Integer isActive) {
		scheduleTemplateService.modifyStatus(scheduleTemplateId,isActive);
		return Result.ok();
	}

	/**
	 * 删除排班模板
	 * 需要同步schedule表的状态
	 * 还需要处理对应预约表
	 * 支付记录表
	 * @param id
	 * @return
	 */
	@GetMapping("/remove")
	public Result remove(@RequestParam("scheduleTemplateId") Long id) {
		scheduleTemplateService.removeTemplate(id);
		return Result.ok();
	}
}
