package cn.edu.ncu.medical.entity.dto;

import cn.edu.ncu.medical.entity.vo.Captcha;
import lombok.Data;

@Data
public class UserLogin {
	//前三个登录用
	private String username;
	private String password;
	private Captcha captcha;
	//改密码用
	private String newPassword;
	//找回密码用
	private String email;
	private String email_code;
}
