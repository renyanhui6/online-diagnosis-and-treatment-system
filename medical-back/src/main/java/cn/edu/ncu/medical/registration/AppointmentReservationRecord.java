package cn.edu.ncu.medical.registration;

import lombok.Data;

@Data
public class AppointmentReservationRecord {
    private Long userId;
    private Long patientId;
    private String personKey;
    private AppointmentReservationState state;
    private Long createdAt;
    private Long registrationId;
    private String message;
}
