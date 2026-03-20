package cn.edu.ncu.medical.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentReservationMessage implements Serializable {
    private String token;
    private Long scheduleId;
}
