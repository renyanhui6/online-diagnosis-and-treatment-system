package cn.edu.ncu.medical.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentPaymentResultVo {
    private Long registrationId;
    private String outTradeNo;
    private String paymentStatus;
    private Integer registrationStatus;
    private Boolean paid;
    private String message;
    private BigDecimal paymentAmount;
    private Date paymentTime;
    private Date expireTime;
    private String gatewayTradeNo;
}
