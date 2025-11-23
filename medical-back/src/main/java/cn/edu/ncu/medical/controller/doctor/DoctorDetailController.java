package cn.edu.ncu.medical.controller.doctor;

import cn.edu.ncu.medical.entity.DoctorDetail;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.service.DoctorDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/front/doctor/doctorDetail")
public class DoctorDetailController {
	@Autowired
	private DoctorDetailService doctorDetailService;
	/**
	 * 获取医生信息
	 * @return
	 */
	@GetMapping("/getInfo")
	public Result getInfo() {
		return Result.ok(doctorDetailService.getInfo());
	}
}
