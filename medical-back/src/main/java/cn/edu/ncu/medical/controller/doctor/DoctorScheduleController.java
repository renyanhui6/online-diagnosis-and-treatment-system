package cn.edu.ncu.medical.controller.doctor;

import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.service.DoctorDetailService;
import cn.edu.ncu.medical.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/front/doctor/schedule")
public class DoctorScheduleController {
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private DoctorDetailService doctorDetailService;

    @GetMapping("/getScheduleList")
    public Result getScheduleList() {
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        Long doctorId  = doctorDetailService.getDoctorIdByUserId(userId);
        return Result.ok(scheduleService.getScheduleListByDoctorId(doctorId));
    }
}
