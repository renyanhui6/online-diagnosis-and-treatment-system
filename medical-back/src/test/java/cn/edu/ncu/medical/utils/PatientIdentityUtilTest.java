package cn.edu.ncu.medical.utils;

import cn.edu.ncu.medical.entity.PatientAttendant;
import cn.edu.ncu.medical.exception.AppointmentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatientIdentityUtilTest {

    @Test
    void normalizeIdCardShouldTrimAndUpperCase() {
        assertEquals("36010220000101123X", PatientIdentityUtil.normalizeIdCard(" 36010220000101123x "));
    }

    @Test
    void normalizeIdCardShouldReturnNullForBlank() {
        assertNull(PatientIdentityUtil.normalizeIdCard("   "));
        assertNull(PatientIdentityUtil.normalizeIdCard(null));
    }

    @Test
    void buildPersonKeyShouldBeStableAfterNormalization() {
        String one = PatientIdentityUtil.buildPersonKey("36010220000101123x");
        String two = PatientIdentityUtil.buildPersonKey(" 36010220000101123X ");

        assertEquals(one, two);
        assertFalse(one.isBlank());
    }

    @Test
    void buildPersonKeyShouldSupportPatientAttendant() {
        PatientAttendant patientAttendant = new PatientAttendant();
        patientAttendant.setIdCard("36010220000101123x");

        String personKey = PatientIdentityUtil.buildPersonKey(patientAttendant);

        assertFalse(personKey.isBlank());
        assertEquals(PatientIdentityUtil.buildPersonKey("36010220000101123X"), personKey);
    }

    @Test
    void buildPersonKeyShouldRejectInvalidInput() {
        assertThrows(AppointmentException.class, () -> PatientIdentityUtil.buildPersonKey((PatientAttendant) null));
        assertThrows(AppointmentException.class, () -> PatientIdentityUtil.buildPersonKey(" "));
    }
}
