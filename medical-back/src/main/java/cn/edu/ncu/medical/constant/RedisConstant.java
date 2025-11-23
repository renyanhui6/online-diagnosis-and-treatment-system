package cn.edu.ncu.medical.constant;

public class RedisConstant {
	//登录用验证码key前缀，实际还要拼接userId
	public static String LOGIN_CAPTCHA_PREFIX = "login:captcha:";
	public static Integer LOGIN_CAPTCHA_TTL = 3;

	//登录用token前缀，实际还要拼接userId
	public static String LOGIN_TOKEN_PREFIX = "login:user:";
	public static Integer LOGIN_TOKEN_TTL = 60*24;

	//找回密码用的key前缀，实际还要拼接userId
	public static final long CODE_EXPIRE_SECONDS = 300; // 5分钟
	public static final String CODE_PREFIX = "email_code:";



}
