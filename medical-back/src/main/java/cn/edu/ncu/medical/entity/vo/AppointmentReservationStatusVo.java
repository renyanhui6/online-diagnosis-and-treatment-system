package cn.edu.ncu.medical.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentReservationStatusVo {
    private String token;
    private String status;
    private Long registrationId;
    private String message;
}
