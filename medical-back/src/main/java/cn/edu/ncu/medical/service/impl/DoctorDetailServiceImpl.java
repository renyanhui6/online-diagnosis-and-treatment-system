package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.exception.LoginException;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.utils.FormatValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.edu.ncu.medical.entity.DoctorDetail;
import cn.edu.ncu.medical.service.DoctorDetailService;
import cn.edu.ncu.medical.mapper.DoctorDetailMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author star
* @description 针对表【doctor_detail】的数据库操作Service实现
* @createDate 2025-07-24 17:44:39
*/
@Service
public class DoctorDetailServiceImpl extends ServiceImpl<DoctorDetailMapper, DoctorDetail>
    implements DoctorDetailService{
	@Autowired
	private DoctorDetailMapper doctorDetailMapper;


	@Override
	public DoctorDetail getInfo() {

		LambdaQueryWrapper<DoctorDetail> queryWrapper=new LambdaQueryWrapper<>();
		queryWrapper.eq(DoctorDetail::getSystemUserId, LoginUserHolder.getLoginUser().getUserId());
		return doctorDetailMapper.selectOne(queryWrapper);
	}

	@Override
	public List<DoctorDetail> findDocList(Long subDepartmentId) {
		LambdaQueryWrapper<DoctorDetail> queryWrapper=new LambdaQueryWrapper<>();
		queryWrapper.eq(DoctorDetail::getSubDepartmentId, subDepartmentId);
		return doctorDetailMapper.selectList(queryWrapper);
	}

	@Override
	public Long getDoctorIdByUserId(Long userId) {
		LambdaQueryWrapper<DoctorDetail> queryWrapper=new LambdaQueryWrapper<>();
		queryWrapper.eq(DoctorDetail::getSystemUserId, userId);
		DoctorDetail doctorDetail = doctorDetailMapper.selectOne(queryWrapper);

		return doctorDetail.getId();
	}
}




