package cn.edu.ncu.medical.service;

import cn.edu.ncu.medical.entity.DoctorDetail;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author star
* @description 针对表【doctor_detail】的数据库操作Service
* @createDate 2025-07-24 17:44:39
*/
public interface DoctorDetailService extends IService<DoctorDetail> {


	DoctorDetail getInfo();

	List<DoctorDetail> findDocList(Long subDepartmentId);

	Long getDoctorIdByUserId(Long userId);
}
