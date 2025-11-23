package cn.edu.ncu.medical.service;

import cn.edu.ncu.medical.entity.SystemUser;
import cn.edu.ncu.medical.entity.vo.SystemUserPage;
import com.baomidou.mybatisplus.core.metadata.IPage;
import cn.edu.ncu.medical.entity.dto.SystemUserDoctorDetail;
import cn.edu.ncu.medical.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author star
* @description 针对表【system_user】的数据库操作Service
* @createDate 2025-07-24 17:44:39
*/
public interface SystemUserService extends IService<SystemUser> {

	void register(SystemUser systemUser);

	IPage<SystemUserPage> findAll(IPage<SystemUserPage> page, SystemUserPage condition);

	void insert(SystemUser systemUser);


	Result modifyStatus(SystemUser systemUser);



	IPage<SystemUser> getList(IPage<SystemUser> page);
}
