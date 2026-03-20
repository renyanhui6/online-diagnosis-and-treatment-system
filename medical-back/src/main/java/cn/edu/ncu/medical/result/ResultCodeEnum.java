package cn.edu.ncu.medical.result;

import lombok.Getter;

/**
 * 统一返回结果状态信息类
 */
@Getter
public enum ResultCodeEnum {

    SUCCESS(200, "成功"),
    FAIL(201, "失败"),
    PARAM_ERROR(202, "参数不正确"),
    SERVICE_ERROR(203, "服务异常"),
    DATA_ERROR(204, "数据异常"),
    ILLEGAL_REQUEST(205, "非法请求"),
    REPEAT_SUBMIT(206, "重复提交"),
    DELETE_ERROR(207, "请先删除子集"),

    FRONT_ACCOUNT_EXIST_ERROR(301, "账号已存在"),
    FRONT_PATTERN_ERROR(305, "密码或邮箱格式错误"),
    ADMIN_CAPTCHA_CODE_ERROR(302, "验证码错误"),
    ADMIN_CAPTCHA_CODE_EXPIRED(303, "验证码已过期"),
    ADMIN_CAPTCHA_CODE_NOT_FOUND(304, "未输入验证码"),


    ADMIN_LOGIN_AUTH(305, "未登陆"),
    ADMIN_ACCOUNT_NOT_EXIST_ERROR(306, "账号不存在"),
    ADMIN_ACCOUNT_ERROR(307, "用户名或密码错误"),
    ADMIN_ACCOUNT_DISABLED_ERROR(308, "该用户已被禁用"),
    ADMIN_ACCESS_FORBIDDEN(309, "无访问权限"),

    FRONT_LOGIN_AUTH(501, "未登陆"),
    FRONT_LOGIN_PHONE_EMPTY(502, "手机号码为空"),
    FRONT_LOGIN_CODE_EMPTY(503, "验证码为空"),
    FRONT_SEND_SMS_TOO_OFTEN(504, "验证法发送过于频繁"),
    FRONT_LOGIN_CODE_EXPIRED(505, "验证码已过期"),
    FRONT_LOGIN_CODE_ERROR(506, "验证码错误"),
    FRONT_ACCOUNT_DISABLED_ERROR(507, "该用户已被禁用"),
    BACK_ACCESS_TYPE_ERROR(508,"请求类型错误"),

    TOKEN_EXPIRED(601, "token过期"),
    TOKEN_INVALID(602, "token非法"),



    // 预约挂号相关错误（700-799区间）
    OPERATION_ERROR(701, "操作错误"),
    ORDER_EMPTY(702, "订单为空"),
    ORDER_NOT_EXIST(703, "订单不存在"),
    ORDER_INFO_EMPTY(704, "订单信息为空"),
    REFUND_ILLEGAL(705, "退款不合法"),
    REMOVE_ORDER_NOT_EXIST(706, "移除的订单不存在"),
    ORDER_STATUS_ILLEGAL(707, "订单状态不合法"),
    SCHEDULE_NOT_EXIST(708, "排班不存在或不合法"),
    SOURCE_INSUFFICIENT(709, "号源数量不足"),
    DOCTOR_NOT_EXIST(710, "医生信息不存在"),
    APPOINTMENT_HANDLER_ERROR(711, "挂号处理错误"),
    REGISTRATION_RECORD_ERROR(712, "就诊记录错误"),
    REGISTRATION_STATUS_ERROR(713, "就诊记录状态错误"),
    REGISTRATION_INFO_EMPTY(714, "挂号信息不能为空"),
    DOCTOR_ID_EMPTY(715, "医生ID不能为空"),
    PATIENT_ID_EMPTY(716, "患者ID不能为空"),
    SCHEDULE_ID_EMPTY(717, "排班ID不能为空"),
    DOCTOR_ID_INVALID(718, "医生ID无效（必须为正数）"),
    PATIENT_ID_INVALID(719, "患者ID无效（必须为正数）"),
    SCHEDULE_ID_INVALID(720, "排班ID无效（必须为正数）"),
    ORDER_CREATE_EXCEPTION(721, "订单表创建异常"),
    USER_NOT_LOGIN(722, "用户未登录"),
    PAYER_ID_INVALID(723, "付款人ID无效"),
    NOT_SUSPENDED(724, "当前状态不是挂起状态，不能设置为已回归"),
    PATIENT_NOT_VERIFIED(725, "就诊人未完成实名认证"),

    MEDICINEORDER_INVALID(800,"药品订单错误"),
    DRUG_QUANTITIES_SHORTAGE(801,"药品库存不足"),


    PRISCRIPTION_ERROR(803,"处方表不存在或已被使用");




    private final Integer code;

    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
