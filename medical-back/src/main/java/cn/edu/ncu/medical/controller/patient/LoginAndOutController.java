package cn.edu.ncu.medical.controller.patient;

import cn.edu.ncu.medical.entity.SystemUser;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.service.SystemUserService;
import com.wf.captcha.base.Captcha;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author ：zhenzhou
 * 用于处理患者登录注册以及退出等相关操作
 */

@RestController
@RequestMapping("/front/patient/loginAndOut")
public class LoginAndOutController {
	@Autowired
	private SystemUserService userService;
	/**
	 * 处理用户注册,用户会传来用户名,密码,邮箱,以及注册类型--只支持患者
	 * @return 注册是否成功，只有三个信息都不为空才会注册成功
	 */
	@PostMapping("/register")
	public Result register(@RequestBody SystemUser systemUser) {
		userService.register(systemUser);
		return Result.ok();
	}

}
