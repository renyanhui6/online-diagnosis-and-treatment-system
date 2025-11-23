package cn.edu.ncu.medical.controller.common;

import cn.edu.ncu.medical.entity.SystemUser;
import cn.edu.ncu.medical.entity.dto.UserLogin;
import cn.edu.ncu.medical.entity.vo.Captcha;
import cn.edu.ncu.medical.inteceptor.login.LoginUser;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.service.LoginService;
import com.wf.captcha.SpecCaptcha;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * 本包主要完成医生患者
 * 之间一些共同的操作
 *
 */
@RestController
@RequestMapping("/front/loginAndOut")
public class LoginController {

	@Autowired
	private LoginService loginService;

	/**
	 * 获取登录用的图形验证码
	 * @return 图形验证码
	 */
	@GetMapping("/captchaCode")
	public Result captchaCode(){
		return Result.ok(loginService.getCaptcha());
	}

	/**
	 * 处理患者医生管理员登录
	 * 需要传递用户名账号密码验证码，用户类型
	 * 需要保证用户状态可用才能登录
	 * @param username 用户名
	 * @param password 密码
	 * @param captcha 验证码
	 * @return
	 */
	@PostMapping("/login")
	public Result login(@RequestBody UserLogin userLogin){


		return loginService.login(userLogin);
	}

	/**
	 * 处理用户退出 医生患者管理员
	 * 退出登录需要清除redis中的用户信息
	 */
	@GetMapping("/logout")
	public Result logout(@RequestParam("userId") Long id ){
		return loginService.logout(id);
	}

	@PostMapping("/modifyPassword")
	public Result modifyPassword(@RequestBody UserLogin userLogin){
		loginService.modifyPassword(userLogin);
		return Result.ok();
	}

	/**
	 * 返回给前端对应的id和用户名
	 * 登录完之后与token搭配
	 * @return
	 */
	@GetMapping("/getUserInfo")
	public Result getUserInfo(){
		return Result.ok(LoginUserHolder.getLoginUser());
	}
	@PostMapping("/getEmailCode")
	public Result getEmailCode(@RequestBody UserLogin userLogin){
		return loginService.getEmailCode(userLogin);
	}
	/**
	 * 处理用户找回密码
	 * @param userLogin
	 * @return
	 */
	@PostMapping("/findPassword")
	public Result findPassword(@RequestBody UserLogin userLogin){
		return loginService.findPassword(userLogin);
	}
}
