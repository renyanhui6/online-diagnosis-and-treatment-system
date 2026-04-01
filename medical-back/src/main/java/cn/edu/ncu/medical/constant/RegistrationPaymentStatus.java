package cn.edu.ncu.medical.constant;

public enum RegistrationPaymentStatus {
    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    CLOSED(2, "已关闭"),
    FAILED(3, "支付失败");

    private final int code;
    private final String text;

    RegistrationPaymentStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    public static RegistrationPaymentStatus fromCode(Integer code) {
        if (code == null) {
            return PENDING;
        }
        for (RegistrationPaymentStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return PENDING;
    }
}
