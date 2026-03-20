package cn.edu.ncu.medical.controller.admin;

import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.schedule.ScheduleGenerator;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("adminScheduleController")
@RequestMapping("/back/admin/schedule")
public class ScheduleController {

	@Autowired(required = false)
	private ScheduleGenerator scheduleGenerator;
	@Value("${app.schedule.generate-days:7}")
	private int defaultGenerateDays;

	@PostMapping("/generate")
	public Result<Map<String, Object>> generate(@RequestParam(value = "days", required = false) Integer days,
			@RequestParam(value = "mode", required = false) String mode) {
		int rangeDays = days == null ? Math.max(1, defaultGenerateDays) : Math.max(1, days);
		ScheduleGenerator.GenerateMode generateMode;
		try {
			generateMode = ScheduleGenerator.GenerateMode.fromCode(mode);
		} catch (IllegalArgumentException ex) {
			return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), "补偿模式不正确");
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("days", rangeDays);
		payload.put("mode", generateMode.getCode());
		payload.put("fromDate", LocalDate.now().toString());
		payload.put("toDate", LocalDate.now().plusDays(rangeDays - 1L).toString());
		if (scheduleGenerator == null) {
			payload.put("createdCount", 0);
			payload.put("cleanedCount", 0);
			return Result.ok(payload);
		}
		ScheduleGenerator.GenerationSummary summary = scheduleGenerator.generateNowSummary(days, generateMode);
		payload.put("createdCount", summary.createdCount());
		payload.put("cleanedCount", summary.cleanedCount());
		return Result.ok(payload);
	}
}
