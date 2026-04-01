package cn.edu.ncu.medical.registration;

import cn.edu.ncu.medical.exception.AppointmentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentReservationKeysTest {

    @Test
    void shouldBuildNamespacedKeys() {
        assertEquals("appointment:stock:{100}", AppointmentReservationKeys.stockKey(100L));
        assertEquals("appointment:once:{100}:person-key", AppointmentReservationKeys.onceKey(100L, "person-key"));
        assertEquals("appointment:resv:{100}:100.token", AppointmentReservationKeys.reservationKey(100L, "100.token"));
        assertEquals("appointment:zexp:{100}", AppointmentReservationKeys.expireKey(100L));
    }

    @Test
    void newTokenShouldContainScheduleIdPrefix() {
        String token = AppointmentReservationKeys.newToken(100L);

        assertTrue(token.startsWith("100."));
        assertEquals(100L, AppointmentReservationKeys.parseScheduleId(token));
    }

    @Test
    void parseScheduleIdShouldRejectInvalidToken() {
        assertThrows(AppointmentException.class, () -> AppointmentReservationKeys.parseScheduleId(null));
        assertThrows(AppointmentException.class, () -> AppointmentReservationKeys.parseScheduleId(""));
        assertThrows(AppointmentException.class, () -> AppointmentReservationKeys.parseScheduleId("abc"));
        assertThrows(AppointmentException.class, () -> AppointmentReservationKeys.parseScheduleId("abc.token"));
    }
}
