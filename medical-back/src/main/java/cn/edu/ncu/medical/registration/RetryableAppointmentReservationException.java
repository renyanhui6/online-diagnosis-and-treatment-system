package cn.edu.ncu.medical.registration;

public class RetryableAppointmentReservationException extends RuntimeException {
    public RetryableAppointmentReservationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetryableAppointmentReservationException(String message) {
        super(message);
    }
}
