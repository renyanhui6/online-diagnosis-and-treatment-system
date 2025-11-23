package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.entity.dto.RegistrationCondition;
import cn.edu.ncu.medical.entity.vo.RegistrationInfo;
import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.mapper.PatientAttendantMapper;
import cn.edu.ncu.medical.mapper.ScheduleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.service.RegistrationService;
import cn.edu.ncu.medical.mapper.RegistrationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
* @author star
* @description 针对表【registration】的数据库操作Service实现
* @createDate 2025-07-24 17:44:39
*/
@Service
public class RegistrationServiceImpl extends ServiceImpl<RegistrationMapper, Registration>
    implements RegistrationService{

    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private PatientAttendantMapper patientAttendantMapper;

    /**
     * 医生根据自己id和状态查询排队中的患者（号)
     *
     * 挂号状态(int)0 - 'pending_payment'（待支付）
     *      * 1-‘已支付’
     *      * 2 - 'queuing'（排队中）
     *      * 3 - in_progress - 问诊中
     *      * 4- 'completed'（已完成）
     *      *
     *      * 5- suspended '（患者未及时响应，暂时挂起，等待后续处理）
     *      * 6-“已回归”
     *
     *      * 7-“失效”（正常过期失效，和退款失效）
     * @return 挂号信息
     */







    /**
     *
     * 获取患者的挂号信息
     * @param userId
     * @return 返回用户所有的挂号详细信息
     */
    public IPage<RegistrationInfo> getRegistrationInfoList(Long userId, Page<RegistrationInfo> page, RegistrationCondition registrationCondition) {
        IPage<RegistrationInfo> pageInfo = registrationMapper.selectRegistrationInfoList(userId,page,registrationCondition);
        return pageInfo;
    }




    /**
     * 获取排队中挂号列表,医生根据自己的排班查询当天的挂号
     * @param doctorId
     * @return
     */
    @Override
    public IPage<RegistrationInfo> getRegistrationList(Long doctorId,Page<RegistrationInfo> page,RegistrationCondition registrationCondition) {
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getDoctorId,doctorId);
        LocalDate localDate = LocalDate.now();

        //默认2 - 'queuing'（排队中）
        //     * 3 - in_progress - 问诊中
        //     * 4- 'completed'（已完成）
        //     * 5- suspended '（患者未及时响应，暂时挂起，等待后续处理）
        //     * 6-“已回归”


        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        Integer isMorning = 0;
        Integer isAfternoon = 0;
        if(currentHour < 12)
            isMorning = 1;
        else if(currentHour > 12 && currentHour < 18)
            isAfternoon = 1;
        queryWrapper.eq(Schedule::getIsMorning,isMorning);
        queryWrapper.eq(Schedule::getIsAfternoon,isAfternoon);
        queryWrapper.eq(Schedule::getScheduleDate,localDate);
        Schedule schedule = scheduleMapper.selectOne(queryWrapper);
        if(schedule == null){
            //当前时间没有预约
            return  null;
        }
        Long scheduleId = schedule.getId();

        //根据当前医生排班
        IPage<RegistrationInfo> registrationInfoIPage = registrationMapper.selectRegistrationInfoBySchedule(doctorId,page,scheduleId,registrationCondition);
        return registrationInfoIPage;
    }


    /**
     * 获取医生的所有挂号信息
     * @param doctorId
     * @return
     */
    @Override
    public IPage<RegistrationInfo> getAllRegistrationList(Long doctorId, Page<RegistrationInfo> page,
                                                          RegistrationCondition condition) {
        IPage<RegistrationInfo> registrationInfoIPage = registrationMapper.selectAllRegistrationInfo(doctorId,page,condition);
        return registrationInfoIPage;
    }


    @Override
    public Registration getRegistrationById(Long registrationId) {
        LambdaQueryWrapper<Registration> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Registration::getId,registrationId);
        return registrationMapper.selectOne(lambdaQueryWrapper);
    }


    @Override
    public void changeStatus(Long registrationId,Integer newStatus){

        LambdaUpdateWrapper<Registration> updateWrapper = new LambdaUpdateWrapper<>();
        LambdaUpdateWrapper<Registration> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(Registration::getId,registrationId);

        lambdaUpdateWrapper.set(Registration::getRegistrationStatus,newStatus);

        registrationMapper.update(null,lambdaUpdateWrapper);
    }



}