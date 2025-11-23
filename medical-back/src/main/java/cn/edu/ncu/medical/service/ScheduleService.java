package cn.edu.ncu.medical.service;

import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.entity.vo.ScheduleVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Date;
import java.util.List;

/**
* @author star
* @description 针对表【schedule】的数据库操作Service
* @createDate 2025-07-24 17:44:39
*/
public interface ScheduleService extends IService<Schedule> {


	List<ScheduleVo> findList(Long subDepartmentId, Date scheduleDate) throws Exception;

	/**
	 * 根据医生id查询排班列表
	 * @param doctorId
	 * @return
	 */
	List<Schedule> getScheduleListByDoctorId(Long doctorId);
}
