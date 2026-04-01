package cn.edu.ncu.medical.controller.patient;

import cn.edu.ncu.medical.entity.dto.AppointmentCreateRequest;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.payment.RegistrationPaymentService;
import cn.edu.ncu.medical.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import cn.edu.ncu.medical.service.RegistrationService;

@RestController
@RequestMapping("/front/patient/appointment")
public class AppointRegistrationController {
    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private RegistrationPaymentService registrationPaymentService;

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

    @GetMapping("/payment/form")
    public Result paymentForm(@RequestParam("registrationId") Long registrationId) {
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        return Result.ok(registrationPaymentService.buildPaymentForm(registrationId, userId));
    }

    @GetMapping("/payment/result")
    public Result paymentResult(@RequestParam("outTradeNo") String outTradeNo) {
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        return Result.ok(registrationPaymentService.resolveReturnResult(outTradeNo, userId));
    }

    @PostMapping("/payment/mock/success")
    public Result mockPaySuccess(@RequestParam("outTradeNo") String outTradeNo) {
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        return Result.ok(registrationPaymentService.simulatePaySuccess(outTradeNo, userId));
    }

    @PostMapping("/payment/mock/cancel")
    public Result mockPayCancel(@RequestParam("outTradeNo") String outTradeNo) {
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        return Result.ok(registrationPaymentService.simulateCancel(outTradeNo, userId));
    }
}
