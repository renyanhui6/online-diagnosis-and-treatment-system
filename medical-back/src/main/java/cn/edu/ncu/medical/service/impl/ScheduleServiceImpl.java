package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.entity.DoctorDetail;
import cn.edu.ncu.medical.entity.vo.ScheduleVo;
import cn.edu.ncu.medical.service.DoctorDetailService;
import cn.edu.ncu.medical.utils.RedisCache;
import cn.edu.ncu.medical.utils.ScheduleCacheKeys;
import cn.edu.ncu.medical.utils.ScheduleTimePolicy;
import cn.edu.ncu.medical.utils.TimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.service.ScheduleService;
import cn.edu.ncu.medical.mapper.ScheduleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author star
* @description 针对表【schedule】的数据库操作Service实现
* @createDate 2025-07-24 17:44:39
*/
@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule>
    implements ScheduleService{
	@Autowired
	private RedisCache redisCache;
	@Autowired
	private DoctorDetailService doctorDetailService;
	@Autowired
	private ScheduleMapper scheduleMapper;

	@Override
	public List<ScheduleVo> findList(Long subDepartmentId, Date scheduleDate) throws Exception {
		//首先判断条件 如果科室id是空 那就默认为1
		//如果日期为空 默认为当天
		if(subDepartmentId == null){
			subDepartmentId = 1L;
		}
		if(scheduleDate == null){
			scheduleDate = TimeUtil.getZeroDate();
		}
		String key = ScheduleCacheKeys.scheduleListKey(subDepartmentId, scheduleDate);
		//首先从redis查询
		@SuppressWarnings("unchecked")
		List<ScheduleVo> list = (List<ScheduleVo>) redisCache.getObject(key, List.class);
		//将查询结果转换为vo
		if(list != null){
			return list;
		}


		//如果redis中没有，则从数据库查询
		LambdaQueryWrapper<Schedule> wrapper01 = new LambdaQueryWrapper<>();
		wrapper01.eq(Schedule::getSubDepartmentId, subDepartmentId)
				.eq(Schedule::getScheduleDate, scheduleDate)
				.eq(Schedule::getStatus, 1);
		Clock clock = Clock.systemDefaultZone();
		List<ScheduleVo> listVo = this.list(wrapper01).stream().map(s -> {
			ScheduleVo scheduleVo = new ScheduleVo();
			scheduleVo.setId(s.getId());
			scheduleVo.setTemplateId(s.getTemplateId());
			scheduleVo.setSubDepartmentId(s.getSubDepartmentId());
			scheduleVo.setDepartmentName(s.getDepartmentName());
			scheduleVo.setDoctorId(s.getDoctorId());
			scheduleVo.setDoctorName(s.getDoctorName());
			scheduleVo.setScheduleDate(s.getScheduleDate());
			DoctorDetail doctorDetail = doctorDetailService.getOne(new LambdaQueryWrapper<DoctorDetail>().eq(DoctorDetail::getId, s.getDoctorId()));
			if (doctorDetail != null) {
				scheduleVo.setPrice(doctorDetail.getPrice());
			}
			scheduleVo.setIsMorning(s.getIsMorning());
			scheduleVo.setIsAfternoon(s.getIsAfternoon());
			scheduleVo.setStatus(s.getStatus());
			scheduleVo.setCurrentAppointmentCount(s.getCurrentAppointmentCount());
			scheduleVo.setAppointmentLimit(s.getAppointmentLimit());
			scheduleVo.setCanBook(ScheduleTimePolicy.canCreateOrder(s, clock));
			return scheduleVo;
		}).collect(Collectors.toList());
		//将查询结果放入redis
		redisCache.setObject(key, listVo).setExpire(key,5, TimeUnit.MINUTES);
		return listVo;
	}

	@Override
	public List<Schedule> getScheduleListByDoctorId(Long doctorId) {

		LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Schedule::getDoctorId, doctorId);
		List<Schedule> scheduleList = scheduleMapper.selectList(wrapper);
		return scheduleList;
	}
}
