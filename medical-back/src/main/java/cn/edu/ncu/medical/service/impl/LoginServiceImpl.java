package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.constant.RedisConstant;
import cn.edu.ncu.medical.entity.SystemUser;
import cn.edu.ncu.medical.entity.dto.UserLogin;
import cn.edu.ncu.medical.entity.vo.Captcha;
import cn.edu.ncu.medical.exception.LoginException;
import cn.edu.ncu.medical.inteceptor.login.LoginUser;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.mapper.SystemUserMapper;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.LoginService;
import cn.edu.ncu.medical.utils.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wf.captcha.SpecCaptcha;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LoginServiceImpl implements LoginService {
	@Autowired
	private RedisCache redisCache;
	@Autowired
	private SystemUserMapper systemUserMapper;
	@Autowired
	private EmailCodeUtil emailCodeUtil;
	@Override
	public Captcha getCaptcha() {
		SpecCaptcha specCaptcha = new SpecCaptcha(200,100,4);
		String key=RedisConstant.LOGIN_CAPTCHA_PREFIX+ UUID.randomUUID().toString();
		redisCache.setString(key,specCaptcha.text().toLowerCase()).setExpire(key,RedisConstant.LOGIN_CAPTCHA_TTL,TimeUnit.MINUTES);;
		return new Captcha(key,specCaptcha.toBase64());
	}

	@Override
	public Result login(UserLogin userLogin) {
		if(userLogin==null){
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}
		//首先判断验证码是否有效无效直接失败
		Captcha captcha = userLogin.getCaptcha();
		String realCode = redisCache.getString(captcha.getKey());
		if (realCode==null) {
			//没有认为过期
			throw new LoginException(ResultCodeEnum.FRONT_LOGIN_CODE_EXPIRED);
		}
		if (!realCode.equals(captcha.getCode().toLowerCase())) {
			//不匹配认为错误
			throw new LoginException(ResultCodeEnum.FRONT_LOGIN_CODE_ERROR);
		}
		//判断传来的参数是否为空
		if (userLogin.getUsername()==null||userLogin.getPassword()==null||userLogin.getCaptcha()==null) {
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}
		if(userLogin.getUsername().trim().isEmpty()||userLogin.getPassword().trim().isEmpty()){
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}
		//查找用户判断是否存在，也就是用户名和密码是否正确
		LambdaQueryWrapper<SystemUser> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(SystemUser::getUsername,userLogin.getUsername());
		queryWrapper.eq(SystemUser::getPassword, SHA256Util.encrypt(userLogin.getPassword()));
		SystemUser oneUser = systemUserMapper.selectOne(queryWrapper);
		if(oneUser==null){
			//只要查不出来就认为错误
			throw new LoginException(ResultCodeEnum.ADMIN_ACCOUNT_ERROR);
		}
		//判断用户状态是否正常
		if(oneUser.getStatus()==0){
			//状态0为被禁用
			throw new LoginException(ResultCodeEnum.ADMIN_ACCOUNT_DISABLED_ERROR);
		}
		//生成对应的token
		String token = JwtUtil.createToken(oneUser.getId(), oneUser.getUsername());
		//判断token是否已经存在，防止重复登录
		if(redisCache.getString(RedisConstant.LOGIN_TOKEN_PREFIX+oneUser.getId())!=null){
			//存在就说明重复登录了,那就需要用新的token替换掉原来的token
			redisCache.setString(RedisConstant.LOGIN_TOKEN_PREFIX+oneUser.getId(),token).setExpire(RedisConstant.LOGIN_TOKEN_PREFIX+oneUser.getId(),RedisConstant.LOGIN_TOKEN_TTL,TimeUnit.MINUTES);
		}
		//把token放进redis，然后返回给前端
		redisCache.setString(RedisConstant.LOGIN_TOKEN_PREFIX+oneUser.getId(),token).setExpire(RedisConstant.LOGIN_TOKEN_PREFIX+oneUser.getId(),RedisConstant.LOGIN_TOKEN_TTL,TimeUnit.MINUTES);
		return Result.ok(token);
	}

	@Override
	public Result logout(Long id) {
		//首先到redis删除token
		redisCache.delete(RedisConstant.LOGIN_TOKEN_PREFIX+id);
		return Result.ok();
	}

	@Override
	public void modifyPassword(UserLogin userLogin) {
		//先判断传来的参数是否为空
		if (userLogin==null||userLogin.getPassword()==null||userLogin.getNewPassword()==null) {
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}
		if(userLogin.getPassword().trim().isEmpty()||userLogin.getNewPassword().trim().isEmpty()){
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}
		//检查密码格式是否正确以及新旧密码是否一致
		if(userLogin.getNewPassword().length()<6||userLogin.getPassword().equals(userLogin.getNewPassword())){
			throw new LoginException(ResultCodeEnum.FRONT_PATTERN_ERROR);
		}
		//查询用户判断原密码是否正确，原密码正确才能修改
		LambdaQueryWrapper<SystemUser> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(SystemUser::getId, LoginUserHolder.getLoginUser().getUserId());
		SystemUser oneUser = systemUserMapper.selectOne(queryWrapper);
		if(oneUser==null){
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}
		System.out.println(SHA256Util.encrypt(userLogin.getPassword()));
		if(!oneUser.getPassword().equals(SHA256Util.encrypt(userLogin.getPassword()))){
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}
		//修改密码
		oneUser.setPassword(SHA256Util.encrypt(userLogin.getNewPassword()));
		systemUserMapper.updateById(oneUser);
	}

	@Override
	public Result getEmailCode(UserLogin userLogin) {
		String email = userLogin.getEmail();

		// 1. 邮箱非空校验
		if (StringUtils.isEmpty(email)) {
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}

		// 2. 邮箱格式校验
		if (!FormatValidator.isValidEmail(email)) {
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}

		// 3. 发送频率限制 (60秒内只能发送一次)
		String rateLimitKey = "email_rate_limit:" + email;
		if (redisCache.getString(rateLimitKey) != null) {
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}

		// 4. 发送验证码邮件
		if (emailCodeUtil.sendVerificationCode(email)) {
			// 设置60秒冷却时间
			redisCache.setString(rateLimitKey, "1")
					.setExpire(rateLimitKey, 60, TimeUnit.SECONDS);
			return Result.ok();
		} else {
			throw new LoginException(ResultCodeEnum.FRONT_SEND_SMS_TOO_OFTEN);
		}
	}

	@Transactional
	@Override
	public Result findPassword(UserLogin userLogin) {
		String email = userLogin.getEmail();
		String code = userLogin.getEmail_code();
		String newPassword = userLogin.getNewPassword();

		// 1. 参数校验
		if (StringUtils.isEmpty(email)) {
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}
		if (StringUtils.isEmpty(code)) {
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}
		if (StringUtils.isEmpty(newPassword)) {
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}

		// 2. 验证码校验 (优先验证验证码，避免数据库查询)
		if (!emailCodeUtil.verifyCode(email, code)) {
			throw new LoginException(ResultCodeEnum.ADMIN_CAPTCHA_CODE_ERROR);
		}

		// 3. 用户存在性检查
		LambdaQueryWrapper<SystemUser> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(SystemUser::getEmail, email);
		SystemUser user = systemUserMapper.selectOne(queryWrapper);
		if (user == null) {
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}

		// 4. 更新密码 (加密存储)
		String encryptedPassword = SHA256Util.encrypt(newPassword);
		user.setPassword(encryptedPassword);
		systemUserMapper.updateById(user);

		return Result.ok();
	}
}
