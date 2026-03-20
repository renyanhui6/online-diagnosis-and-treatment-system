package cn.edu.ncu.medical.service;

import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.entity.dto.AppointmentCreateRequest;
import cn.edu.ncu.medical.entity.dto.AppointmentReservationMessage;
import cn.edu.ncu.medical.entity.dto.RegistrationCondition;
import cn.edu.ncu.medical.entity.vo.AppointmentReservationStatusVo;
import cn.edu.ncu.medical.entity.vo.RegistrationInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author star
* @description 针对表【registration】的数据库操作Service
* @createDate 2025-07-24 17:44:39
*/
public interface RegistrationService extends IService<Registration> {

    /**
     * 医生根据自己id和状态查询排队中的患者（号)
     * @param doctorId
     * @param status
     *               挂号状态(int)0 - 'pending_payment'（待支付，简化后不再使用）
     * 1-‘已支付’
     * 2 - 'queuing'（排队中）
     * 3 - in_progress - 问诊中
     * 4- 'completed'（已完成）
     *
     * 5- suspended '（患者未及时响应，暂时挂起，等待后续处理）
     * 6-“已回归”
     * 7-‘等待患者确认’
     * 8-“失效”（正常过期失效，和退款失效）
     * @return 挂号信息
     */
    //**************************************
    /**
     * 医生获取挂号列表
     * @return 挂号列表
     */
    IPage<RegistrationInfo> getRegistrationList(Long doctorId,Page<RegistrationInfo> page,RegistrationCondition registrationCondition);




    /**
     * 获取患者的挂号信息
     * @param userId
     * @return 返回用户所有的挂号详细信息
     */
    IPage<RegistrationInfo> getRegistrationInfoList(Long userId, Page<RegistrationInfo> page, RegistrationCondition registrationCondition);


    /**
     * 获取医生的所有挂号信息
     * @param doctorId
     * @return
     */
    IPage<RegistrationInfo> getAllRegistrationList(Long doctorId, Page<RegistrationInfo> page,
                                                          RegistrationCondition condition);

    /**
     * 创建挂号记录（简化为直接创建，不再走支付流程）
     * @param registration 挂号信息
     * @param userId 当前登录用户ID
     * @return 挂号ID
     */
    AppointmentReservationStatusVo createRegistration(AppointmentCreateRequest request, Long userId);

    AppointmentReservationStatusVo getReservationStatus(String token, Long userId);

    void consumeReservedRegistration(AppointmentReservationMessage message);

    void failReservation(AppointmentReservationMessage message, String reason);

    void releaseExpiredReservation(Long scheduleId, String token);





    /**
     * 根据挂号id查询挂号信息
     * @param registrationId
     * @return
     */
    RegistrationInfo getRegistrationById(Long registrationId);




    /**
     * 修改挂号状态
     * @param registrationId 挂号id
     * @param status 新状态
     */
    void changeStatus(Long registrationId, Integer status);



}
