package cn.edu.ncu.medical.controller.patient;

import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.entity.vo.ScheduleVo;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.service.ScheduleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController("patientScheduleController")
@RequestMapping("/front/patient/schedule")
public class ScheduleController {
	@Autowired
	private ScheduleService scheduleService;

	/**
	 * 患者查看排班
	 * @return
	 */
	@GetMapping("/findList")
	public Result<List<ScheduleVo>> findList( @RequestParam(value = "subDepartmentId", required = false) Long subDepartmentId,
											  @RequestParam(value = "scheduleDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd")  Date scheduleDate
											  ) throws Exception{


		List<ScheduleVo> scheduleIPage = scheduleService.findList(subDepartmentId, scheduleDate);
		return Result.ok(scheduleIPage);
	}


}
