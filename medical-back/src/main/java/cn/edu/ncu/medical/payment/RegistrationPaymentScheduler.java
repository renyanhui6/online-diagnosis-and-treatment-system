package cn.edu.ncu.medical.payment;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RegistrationPaymentScheduler {
    private final RegistrationPaymentService registrationPaymentService;

    public RegistrationPaymentScheduler(RegistrationPaymentService registrationPaymentService) {
        this.registrationPaymentService = registrationPaymentService;
    }

    @Scheduled(fixedDelayString = "${payment.mock.expire-scan-interval-ms:60000}")
    public void closeExpiredPaymentOrders() {
        registrationPaymentService.closeExpiredOrders();
    }
}
