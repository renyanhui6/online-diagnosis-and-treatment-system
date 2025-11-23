package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.entity.dto.IdCard;
import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.utils.FormatValidator;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.edu.ncu.medical.entity.PatientAttendant;
import cn.edu.ncu.medical.service.PatientAttendantService;
import cn.edu.ncu.medical.mapper.PatientAttendantMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author star
 * @description 针对表【patient_attendant】的数据库操作Service实现
 * @createDate 2025-07-24 17:44:39
 */
@Service
public class PatientAttendantServiceImpl extends ServiceImpl<PatientAttendantMapper, PatientAttendant>
		implements PatientAttendantService {
	@Autowired
	private PatientAttendantMapper patientAttendantMapper;

	@Override
	public void updateInfo(PatientAttendant patientAttendant) {
		//判断信息是否为空,为空直接跳过不改
		if (patientAttendant == null) {
			return;
		}
		//判断一下手机号的格式
		if (!FormatValidator.isValidPhone(patientAttendant.getPhone())) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		//先根据账号和是否主人确定一个主用例然后修改
		LambdaUpdateWrapper<PatientAttendant> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(PatientAttendant::getSystemUserId, LoginUserHolder.getLoginUser().getUserId())
				.eq(PatientAttendant::getIsMaster, 1)
				.set(PatientAttendant::getPhone, patientAttendant.getPhone())
				.set(PatientAttendant::getHomeAddress, patientAttendant.getHomeAddress())
				.set(PatientAttendant::getGender, patientAttendant.getGender());
		patientAttendantMapper.update(null, updateWrapper);
	}

	@Override
	public PatientAttendant getInfo() {
		//根据账号和是否主人确定一个主用例然后查询
		LambdaQueryWrapper<PatientAttendant> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PatientAttendant::getSystemUserId, LoginUserHolder.getLoginUser().getUserId())
				.eq(PatientAttendant::getIsMaster, 1);
		PatientAttendant patientAttendant = patientAttendantMapper.selectOne(queryWrapper);
		//判断一下是否有主用例
		if (patientAttendant == null) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		return patientAttendant;
	}

	@Override
	public void addIdCard(IdCard idCard) {
		//判断一下身份证号的格式
		if (!FormatValidator.isValidIdCardStrict(idCard.getIdCard())) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		//先查看身份证有没有使用过
		//有没有主用例使用过
		LambdaQueryWrapper<PatientAttendant> queryWrapper01 = new LambdaQueryWrapper<>();
		queryWrapper01.eq(PatientAttendant::getIdCard, idCard.getIdCard())
				.eq(PatientAttendant::getIsMaster, 1);
		PatientAttendant ifUsed = patientAttendantMapper.selectOne(queryWrapper01);
		if (ifUsed != null) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		//查出要修改的主用例
		LambdaQueryWrapper<PatientAttendant> queryWrapper02 = new LambdaQueryWrapper<>();
		queryWrapper02.eq(PatientAttendant::getIsMaster, 1);
		queryWrapper02.eq(PatientAttendant::getSystemUserId, LoginUserHolder.getLoginUser().getUserId());
		PatientAttendant one = patientAttendantMapper.selectOne(queryWrapper02);
		//有实名那就不能再加入了
		if (one.getIdCard() != null) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		one.setIdCard(idCard.getIdCard());
		one.setRealName(idCard.getRealName());
		patientAttendantMapper.updateById(one);
	}

	@Override
	public Result addPatientAttendant(IdCard idCard) {
		//判断一下身份证号的格式
		if (!FormatValidator.isValidIdCardStrict(idCard.getIdCard())) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		//先查看身份证有没有使用过
		//查看的是当前用户的用例有没有人使用过
		LambdaQueryWrapper<PatientAttendant> queryWrapper01 = new LambdaQueryWrapper<>();
		queryWrapper01.eq(PatientAttendant::getIdCard, idCard.getIdCard())
				.eq(PatientAttendant::getSystemUserId, LoginUserHolder.getLoginUser().getUserId());
		PatientAttendant ifUsed = patientAttendantMapper.selectOne(queryWrapper01);
		if (ifUsed != null) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		//如果没有使用过,那就直接添加
		PatientAttendant newOne = new PatientAttendant();
		newOne.setIdCard(idCard.getIdCard());
		newOne.setRealName(idCard.getRealName());
		newOne.setSystemUserId(LoginUserHolder.getLoginUser().getUserId());
		newOne.setIsMaster(0);
		patientAttendantMapper.insert(newOne);
		return Result.ok();
	}

	@Override
	public Result getPatientList() {
		//根据账号查询所有用例
		LambdaQueryWrapper<PatientAttendant> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PatientAttendant::getSystemUserId, LoginUserHolder.getLoginUser().getUserId());
		return Result.ok(patientAttendantMapper.selectList(queryWrapper));
	}


	@Override
	public void removePatientAttendant(Long patientAttendantId) {
		//先查看用例是否存在
		LambdaQueryWrapper<PatientAttendant> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PatientAttendant::getId, patientAttendantId);
		PatientAttendant ifExist = patientAttendantMapper.selectOne(queryWrapper);
		if (ifExist == null) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		//主用例也不能删除
		if (ifExist.getIsMaster() == 1) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		//只能删除自己的用例
		if (!ifExist.getSystemUserId().equals(LoginUserHolder.getLoginUser().getUserId())) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		patientAttendantMapper.deleteById(patientAttendantId);
	}
}