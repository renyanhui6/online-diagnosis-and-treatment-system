package cn.edu.ncu.medical.constant;

public enum RegistrationStatus {
    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付"),
    QUEUING(2, "排队中"),
    IN_PROGRESS(3, "问诊中"),
    COMPLETED(4, "已完成"),
    SUSPENDED(5, "患者未及时响应，暂时挂起，等待后续处理"),
    RESUMED(6, "已回归"),
    INVALID(7, "失效");

    private final int code;
    private final String description;

    RegistrationStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}