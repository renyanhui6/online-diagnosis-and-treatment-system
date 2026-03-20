package cn.edu.ncu.medical.utils;

import cn.edu.ncu.medical.entity.PatientAttendant;
import cn.edu.ncu.medical.exception.AppointmentException;
import cn.edu.ncu.medical.result.ResultCodeEnum;

public final class PatientIdentityUtil {
    private PatientIdentityUtil() {
    }

    public static String buildPersonKey(PatientAttendant patientAttendant) {
        if (patientAttendant == null) {
            throw new AppointmentException(ResultCodeEnum.PATIENT_ID_INVALID);
        }
        return buildPersonKey(patientAttendant.getIdCard());
    }

    public static String buildPersonKey(String idCard) {
        String normalized = normalizeIdCard(idCard);
        if (normalized == null) {
            throw new AppointmentException(ResultCodeEnum.PATIENT_NOT_VERIFIED);
        }
        return SHA256Util.encrypt(normalized);
    }

    public static String normalizeIdCard(String idCard) {
        if (idCard == null) {
            return null;
        }
        String normalized = idCard.trim().toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }
}
