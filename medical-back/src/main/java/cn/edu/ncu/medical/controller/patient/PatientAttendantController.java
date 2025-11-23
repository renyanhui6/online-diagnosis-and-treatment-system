package cn.edu.ncu.medical.controller.patient;

import cn.edu.ncu.medical.entity.PatientAttendant;
import cn.edu.ncu.medical.entity.dto.IdCard;
import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.PatientAttendantService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 处理患者端的基本信息：详细信息+实名认证
 * 提供完善信息或修改信息，实名除外
 * 一个账号可以绑定多个用例实名，但前提主体确定后
 */
@RestController
@RequestMapping("/front/patient/attendant")
public class PatientAttendantController {
	@Autowired
	private PatientAttendantService patientAttendantService;

	/**
	 * 患者账号主人完善自己的个人信息
	 * 不包括身份证实名部分
	 *
	 * @param patientAttendant
	 * @return
	 */
	@PostMapping("/updateInfo")
	public Result updateInfo(@RequestBody PatientAttendant patientAttendant) {
		patientAttendantService.updateInfo(patientAttendant);
		return Result.ok();
	}

	/**
	 *
	 *
	 * @return
	 */
	@GetMapping("/getInfo")
	public Result getInfo() {
		return Result.ok(patientAttendantService.getInfo());
	}

	/**
	 * 患者账号主人完善自己的身份证实名信息
	 *
	 * @param idCard
	 * @return
	 */
	@PostMapping("/addIdCard")
	public Result addIdCard(@RequestBody IdCard idCard) {
		patientAttendantService.addIdCard(idCard);
		return Result.ok();
	}

	/**
	 * 患者账号主人绑定其他用例实名信息
	 *
	 * @param idCard
	 * @return
	 */
	@PostMapping("/addPatientAttendant")
	public Result addPatientAttendant(@RequestBody IdCard idCard) {
		return patientAttendantService.addPatientAttendant(idCard);
	}
	/**
	 *获取就诊人列表
 	 */

	@GetMapping("/getPatientList")
	public Result getPatientList() {
		return patientAttendantService.getPatientList();
	}
	/**
	 *删除用例
	 */
	@GetMapping("/removePatientAttendant")
	public Result removePatientAttendant(@RequestParam("patientAttendantId") Long patientAttendantId) {
		patientAttendantService.removePatientAttendant(patientAttendantId);
		return Result.ok();
	}
}
