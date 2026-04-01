package cn.edu.ncu.medical.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentPaymentFormVo {
    private Long registrationId;
    private String outTradeNo;
    private BigDecimal paymentAmount;
    private Date expireTime;
    private String formHtml;
}
