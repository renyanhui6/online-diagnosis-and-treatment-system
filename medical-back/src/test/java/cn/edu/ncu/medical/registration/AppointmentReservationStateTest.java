package cn.edu.ncu.medical.registration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentReservationStateTest {

    @Test
    void shouldMapCodeToState() {
        assertSame(AppointmentReservationState.PENDING, AppointmentReservationState.fromCode("P"));
        assertSame(AppointmentReservationState.CONFIRMED, AppointmentReservationState.fromCode("C"));
        assertSame(AppointmentReservationState.ROLLED_BACK, AppointmentReservationState.fromCode("R"));
    }

    @Test
    void shouldExposeExpectedApiStatus() {
        assertEquals("PROCESSING", AppointmentReservationState.PENDING.getApiStatus());
        assertEquals("SUCCESS", AppointmentReservationState.CONFIRMED.getApiStatus());
        assertEquals("FAILED", AppointmentReservationState.ROLLED_BACK.getApiStatus());
    }

    @Test
    void shouldRejectUnknownCode() {
        assertThrows(IllegalArgumentException.class, () -> AppointmentReservationState.fromCode("X"));
    }
}
