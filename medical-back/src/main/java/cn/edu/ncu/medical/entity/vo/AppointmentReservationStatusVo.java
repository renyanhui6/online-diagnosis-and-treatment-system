package cn.edu.ncu.medical.entity.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class AppointmentReservationStatusVo {
    private String token;
    private String status;
    private Long registrationId;
    private String message;
    private Boolean paymentRequired;
    private String outTradeNo;
    private String paymentStatus;
    private Date paymentExpireTime;

    public AppointmentReservationStatusVo(String token, String status, Long registrationId, String message) {
        this.token = token;
        this.status = status;
        this.registrationId = registrationId;
        this.message = message;
    }
}
