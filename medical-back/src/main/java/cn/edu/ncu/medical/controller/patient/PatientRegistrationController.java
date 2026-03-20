package cn.edu.ncu.medical.controller.patient;


import cn.edu.ncu.medical.constant.RegistrationStatus;
import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.entity.dto.RegistrationCondition;
import cn.edu.ncu.medical.entity.vo.RegistrationInfo;
import cn.edu.ncu.medical.exception.AppointmentException;
import cn.edu.ncu.medical.exception.SuspendedStatusException;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.RegistrationService;
import com.baomidou.mybatisplus.core.metadata.IPage;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/front/patient/registration")
public class PatientRegistrationController {

    @Autowired
    private RegistrationService registrationService;



    /**
     **
     * 挂号状态(int)
     * 0 - 'pending_payment'（待支付，简化后不再使用）
     * 1-‘已支付’
     * 2 - 'queuing'（排队中）
     * 3 - in_progress - 问诊中
     * 4- 'completed'（已完成）
     * 5- suspended '（患者未及时响应，暂时挂起，等待后续处理）
     * 6-“已回归”
     * 7-“等待患者确认”
     * 8-“失效”（正常过期失效，和退款失效）
     * @return
     */

    @PostMapping("/getRegistrationInfoList")
    public Result<IPage<RegistrationInfo>> getRegistrationInfo(
                                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                                              @RequestBody(required = false) RegistrationCondition registrationCondition){
        Page<RegistrationInfo> page = new Page<>(pageNum, pageSize);
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        IPage<RegistrationInfo> pageInfo = registrationService.getRegistrationInfoList(userId,page,registrationCondition);
        return Result.ok(pageInfo);
    }


    //患者是挂起状态，将状态设置为已回归
    @PostMapping("/changeStatusToResumed")
    public Result changeStatusToResumed(@RequestParam("registrationId") Long registrationId){
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        RegistrationInfo registrationInfo = registrationService.getRegistrationById(registrationId);
        if (registrationInfo == null || registrationInfo.getPatientUserId() == null || !registrationInfo.getPatientUserId().equals(userId)) {
            throw new AppointmentException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        //先判断是否是挂起状态
        Registration registration = registrationService.getById(registrationId);
        if (registration == null) {
            throw new AppointmentException(ResultCodeEnum.REGISTRATION_RECORD_ERROR);
        }
        if(registration.getRegistrationStatus() != RegistrationStatus.SUSPENDED.getCode()){
            throw new SuspendedStatusException();
        }
        registrationService.changeStatus(registrationId, RegistrationStatus.RESUMED.getCode());
        return Result.ok();
    }




}
