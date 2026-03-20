package cn.edu.ncu.medical.registration;

public enum AppointmentReservationState {
    PENDING("P", "PROCESSING"),
    CONFIRMED("C", "SUCCESS"),
    ROLLED_BACK("R", "FAILED");

    private final String code;
    private final String apiStatus;

    AppointmentReservationState(String code, String apiStatus) {
        this.code = code;
        this.apiStatus = apiStatus;
    }

    public String getCode() {
        return code;
    }

    public String getApiStatus() {
        return apiStatus;
    }

    public static AppointmentReservationState fromCode(String code) {
        for (AppointmentReservationState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown reservation state: " + code);
    }
}
