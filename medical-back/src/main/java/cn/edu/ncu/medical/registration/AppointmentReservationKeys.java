package cn.edu.ncu.medical.registration;

import cn.edu.ncu.medical.exception.AppointmentException;
import cn.edu.ncu.medical.result.ResultCodeEnum;

import java.util.UUID;

public final class AppointmentReservationKeys {
    private static final String STOCK_PREFIX = "appointment:stock:{";
    private static final String ONCE_PREFIX = "appointment:once:{";
    private static final String RESV_PREFIX = "appointment:resv:{";
    private static final String ZEXP_PREFIX = "appointment:zexp:{";
    private static final String TOKEN_SEPARATOR = ".";

    private AppointmentReservationKeys() {
    }

    public static String stockKey(Long scheduleId) {
        return STOCK_PREFIX + scheduleId + "}";
    }

    public static String onceKey(Long scheduleId, String personKey) {
        return ONCE_PREFIX + scheduleId + "}:" + personKey;
    }

    public static String reservationKey(Long scheduleId, String token) {
        return RESV_PREFIX + scheduleId + "}:" + token;
    }

    public static String expireKey(Long scheduleId) {
        return ZEXP_PREFIX + scheduleId + "}";
    }

    public static String newToken(Long scheduleId) {
        return scheduleId + TOKEN_SEPARATOR + UUID.randomUUID().toString().replace("-", "");
    }

    public static Long parseScheduleId(String token) {
        if (token == null || token.isBlank()) {
            throw new AppointmentException(ResultCodeEnum.PARAM_ERROR);
        }
        int index = token.indexOf(TOKEN_SEPARATOR);
        if (index <= 0) {
            throw new AppointmentException(ResultCodeEnum.PARAM_ERROR.getCode(), "预约 token 不合法");
        }
        try {
            return Long.parseLong(token.substring(0, index));
        } catch (NumberFormatException ex) {
            throw new AppointmentException(ResultCodeEnum.PARAM_ERROR.getCode(), "预约 token 不合法");
        }
    }
}
