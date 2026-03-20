package cn.edu.ncu.medical.entity.dto;

import lombok.Data;

@Data
public class AppointmentCreateRequest {
    private Long patientId;
    private Long scheduleId;
}
