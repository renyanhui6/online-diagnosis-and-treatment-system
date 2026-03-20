package cn.edu.ncu.medical.controller.patient;

import cn.edu.ncu.medical.entity.dto.AppointmentCreateRequest;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import cn.edu.ncu.medical.service.RegistrationService;

@RestController
@RequestMapping("/front/patient/appointment")
public class AppointRegistrationController {
    @Autowired
    private RegistrationService registrationService;

    @PostMapping("/create")
    public Result create(@RequestBody AppointmentCreateRequest request) {
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        return Result.ok(registrationService.createRegistration(request, userId));
    }

    @GetMapping("/status")
    public Result status(@RequestParam("token") String token) {
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        return Result.ok(registrationService.getReservationStatus(token, userId));
    }
}
