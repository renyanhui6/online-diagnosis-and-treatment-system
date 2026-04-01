package cn.edu.ncu.medical.payment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payment.mock")
@Data
public class RegistrationPaymentProperties {
    /**
     * 模拟支付完成后回跳到患者前端的结果页。
     */
    private String returnUrl = "http://127.0.0.1:5173/payment/result";

    /**
     * 支付单超时时间，超时后自动关闭并释放号源。
     */
    private int paymentExpireMinutes = 15;
}
