package cn.edu.ncu.medical.service;

import cn.edu.ncu.medical.entity.SystemUser;
import cn.edu.ncu.medical.entity.dto.UserLogin;
import cn.edu.ncu.medical.entity.vo.Captcha;
import cn.edu.ncu.medical.result.Result;

public interface LoginService {
	Captcha getCaptcha();

	Result login(UserLogin userLogin);

	Result logout(Long id);

	void modifyPassword(UserLogin userLogin);

	Result findPassword(UserLogin userLogin);

	Result getEmailCode(UserLogin userLogin);
}
