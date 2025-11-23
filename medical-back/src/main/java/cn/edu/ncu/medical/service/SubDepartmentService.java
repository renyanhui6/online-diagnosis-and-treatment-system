package cn.edu.ncu.medical.service;

import cn.edu.ncu.medical.entity.SubDepartment;
import cn.edu.ncu.medical.entity.dto.SubDepartmentModel;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.IOException;

/**
* @author star
* @description 针对表【sub_department】的数据库操作Service
* @createDate 2025-07-24 17:44:39
*/
public interface SubDepartmentService extends IService<SubDepartment> {

	void addSub(SubDepartmentModel subDepartmentModel) throws IOException;
}
