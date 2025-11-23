package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.entity.DoctorDetail;
import cn.edu.ncu.medical.entity.PatientAttendant;
import cn.edu.ncu.medical.entity.dto.SystemUserDoctorDetail;
import cn.edu.ncu.medical.entity.SystemUser;
import cn.edu.ncu.medical.entity.vo.SystemUserPage;
import cn.edu.ncu.medical.exception.LoginException;
import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.mapper.DoctorDetailMapper;
import cn.edu.ncu.medical.mapper.PatientAttendantMapper;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.mapper.SystemUserMapper;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.SystemUserService;
import cn.edu.ncu.medical.utils.FormatValidator;
import cn.edu.ncu.medical.utils.SHA256Util;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.edu.ncu.medical.entity.SystemUser;
import cn.edu.ncu.medical.service.SystemUserService;
import cn.edu.ncu.medical.mapper.SystemUserMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* @author star
* @description 针对表【system_user】的数据库操作Service实现
* @createDate 2025-07-24 17:44:39
*/
@Transactional
@Service
public class SystemUserServiceImpl extends ServiceImpl<SystemUserMapper, SystemUser>
    implements SystemUserService{
	@Autowired
	private SystemUserMapper systemUserMapper;
	@Autowired
	private PatientAttendantMapper patientAttendantMapper;
	@Autowired
	private DoctorDetailMapper doctorDetailMapper;
	@Override
	public void register(SystemUser systemUser) {
		//如果信息不全抛异常
		if (systemUser==null||systemUser.getUsername()==null||systemUser.getPassword()==null||systemUser.getEmail()==null) {
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}
		if(systemUser.getUsername().trim().isEmpty()||systemUser.getPassword().trim().isEmpty()||systemUser.getEmail().trim().isEmpty()){
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}
		//如果注册类型不是患者，不能注册，否则初始化角色为患者
		if (systemUser.getRegisterType()!=1) {
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}else {
			systemUser.setType(1);
		}
		//检查邮箱格式
		if (!FormatValidator.isValidEmail(systemUser.getEmail())||systemUser.getPassword().length()<6) {
			throw new LoginException(ResultCodeEnum.FRONT_PATTERN_ERROR);
		}
		//检查用户名或者邮箱是否重复
		LambdaQueryWrapper<SystemUser> queryWrapper=new LambdaQueryWrapper<>();
		queryWrapper.eq(SystemUser::getUsername,systemUser.getUsername()).or().eq(SystemUser::getEmail,systemUser.getEmail());
		SystemUser one = systemUserMapper.selectOne(queryWrapper);

		if (one!=null) {
			throw new LoginException(ResultCodeEnum.FRONT_ACCOUNT_EXIST_ERROR);
		}
		systemUser.setPassword(SHA256Util.encrypt(systemUser.getPassword()));
		systemUserMapper.insert(systemUser);

		PatientAttendant patientAttendant=new PatientAttendant();
		patientAttendant.setSystemUserId(systemUser.getId());
		patientAttendant.setNickname(systemUser.getUsername());
		patientAttendant.setIsMaster(1);
		patientAttendantMapper.insert(patientAttendant);
	}

	@Override
	public Result modifyStatus(SystemUser systemUser) {
		//直接根据对应账号id修改可用状态
		if (systemUser==null||systemUser.getId()==null||systemUser.getStatus()==null) {
			throw new LoginException(ResultCodeEnum.PARAM_ERROR);
		}
		LambdaUpdateWrapper<SystemUser> updateWrapper=new LambdaUpdateWrapper<>();
		updateWrapper.eq(SystemUser::getId,systemUser.getId()).
				set(SystemUser::getStatus,systemUser.getStatus());
		systemUserMapper.update(updateWrapper);
		return Result.ok();
	}


	@Override
	public IPage<SystemUser> getList(IPage<SystemUser> page) {
		return systemUserMapper.selectDoctorList(page);
	}

	@Override
	public IPage<SystemUserPage> findAll(IPage<SystemUserPage> page, SystemUserPage condition) {
		IPage<SystemUserPage> systemUserPage= systemUserMapper.selectByCondition(page, condition);
		return systemUserPage;
	}

	@Override
	public void insert(SystemUser systemUser) {
		systemUserMapper.insert(systemUser);
	}



}




