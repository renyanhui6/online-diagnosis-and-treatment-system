package cn.edu.ncu.medical.controller.doctor;


import cn.edu.ncu.medical.constant.RegistrationStatus;
import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.entity.dto.RegistrationCondition;
import cn.edu.ncu.medical.entity.vo.RegistrationInfo;
import cn.edu.ncu.medical.exception.AppointmentException;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.DoctorDetailService;
import cn.edu.ncu.medical.service.RegistrationService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/front/doctor/registration")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private DoctorDetailService doctorDetailService;


    /**
     * 查询医生全部的挂号情况
     */
    @PostMapping("/getAllRegistrationinfoList")
    public Result getAllRegistrationList(@RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                         @RequestBody(required = false) RegistrationCondition registrationCondition) {
        Page<RegistrationInfo> page = new Page<>(pageNum,pageSize);

        Long userId = LoginUserHolder.getLoginUser().getUserId();
        //根据userid查询医生id
        Long doctorId = doctorDetailService.getDoctorIdByUserId(userId);
        return Result.ok(registrationService.getAllRegistrationList(doctorId,page,registrationCondition));
    }

    /**
     * 获取挂号列表
     * @return 挂号列表
     */
    @PostMapping("/getRegistrationList")
    public Result getRegistrationList(
                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @RequestBody(required = false) RegistrationCondition registrationCondition){
        Page<RegistrationInfo> page = new Page<>(pageNum,pageSize);
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        //根据userid查询医生id
        Long doctorId = doctorDetailService.getDoctorIdByUserId(userId);
        return Result.ok(registrationService.getRegistrationList(doctorId,page,registrationCondition));
    }



    @GetMapping("/getRegistrationById")
    public Result getRegistrationById(@RequestParam("registrationId") Long registrationId){
        return Result.ok(registrationService.getRegistrationById(registrationId));
    }

    /**
     ** @param registrationId 挂号id
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
    //患者超时，系统自动将其设置为挂起
    @PostMapping("/changeStatusToSuspended")
    public Result changeStatusToSuspended(@RequestParam("registrationId") Long registrationId){
        assertDoctorOwnsRegistration(registrationId);
        registrationService.changeStatus(registrationId, RegistrationStatus.SUSPENDED.getCode());
        return Result.ok();
    }

    //患者开始问诊，将状态设置为问诊中
    @PostMapping("/changeStatusToInProgress")
    public Result changeStatusToInProgress(@RequestParam("registrationId") Long registrationId){
        assertDoctorOwnsRegistration(registrationId);
        registrationService.changeStatus(registrationId,RegistrationStatus.IN_PROGRESS.getCode());
        return Result.ok();
    }



    //患者已完成问诊，将状态设置为已完成
    @PostMapping("/changeStatusToCompleted")
    public Result changeStatusToCompleted(@RequestParam("registrationId") Long registrationId){
        assertDoctorOwnsRegistration(registrationId);
        registrationService.changeStatus(registrationId,RegistrationStatus.COMPLETED.getCode());
        return Result.ok();
    }

    //患者未完成问诊，将状态设置为失效
    @PostMapping("/changeStatusToUncompleted")
    public Result changeStatusToUncompleted(@RequestParam("registrationId") Long registrationId){
        assertDoctorOwnsRegistration(registrationId);
        registrationService.changeStatus(registrationId,RegistrationStatus.INVALID.getCode());
        return Result.ok();
    }

    private void assertDoctorOwnsRegistration(Long registrationId) {
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        Long doctorId = doctorDetailService.getDoctorIdByUserId(userId);
        Registration registration = registrationService.getById(registrationId);
        if (registration == null) {
            throw new AppointmentException(ResultCodeEnum.REGISTRATION_RECORD_ERROR);
        }
        if (doctorId == null || !doctorId.equals(registration.getDoctorId())) {
            throw new AppointmentException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
    }

}
