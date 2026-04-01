package cn.edu.ncu.medical.payment;

import cn.edu.ncu.medical.constant.RegistrationPaymentStatus;
import cn.edu.ncu.medical.constant.RegistrationStatus;
import cn.edu.ncu.medical.entity.DoctorDetail;
import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.entity.RegistrationPaymentOrder;
import cn.edu.ncu.medical.entity.RegistrationPersonLock;
import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.entity.vo.AppointmentPaymentFormVo;
import cn.edu.ncu.medical.entity.vo.AppointmentPaymentResultVo;
import cn.edu.ncu.medical.exception.AppointmentException;
import cn.edu.ncu.medical.mapper.RegistrationMapper;
import cn.edu.ncu.medical.mapper.RegistrationPaymentOrderMapper;
import cn.edu.ncu.medical.mapper.RegistrationPersonLockMapper;
import cn.edu.ncu.medical.mapper.ScheduleMapper;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.DoctorDetailService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class RegistrationPaymentService {
    private static final String PAYMENT_GATEWAY = "MOCK_PAYMENT";
    private static final String PAYMENT_METHOD = "SIMULATED_CONFIRM";

    private final RegistrationPaymentOrderMapper paymentOrderMapper;
    private final RegistrationMapper registrationMapper;
    private final RegistrationPersonLockMapper registrationPersonLockMapper;
    private final ScheduleMapper scheduleMapper;
    private final DoctorDetailService doctorDetailService;
    private final RegistrationPaymentProperties paymentProperties;
    private final Clock clock = Clock.systemDefaultZone();

    public RegistrationPaymentService(RegistrationPaymentOrderMapper paymentOrderMapper,
                                      RegistrationMapper registrationMapper,
                                      RegistrationPersonLockMapper registrationPersonLockMapper,
                                      ScheduleMapper scheduleMapper,
                                      DoctorDetailService doctorDetailService,
                                      RegistrationPaymentProperties paymentProperties) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.registrationMapper = registrationMapper;
        this.registrationPersonLockMapper = registrationPersonLockMapper;
        this.scheduleMapper = scheduleMapper;
        this.doctorDetailService = doctorDetailService;
        this.paymentProperties = paymentProperties;
    }

    @Transactional(rollbackFor = Exception.class)
    public RegistrationPaymentOrder createPendingOrder(Registration registration, Long payerUserId, Schedule schedule) {
        RegistrationPaymentOrder existing = findByRegistrationId(registration.getId());
        if (existing != null) {
            return existing;
        }

        DoctorDetail doctorDetail = doctorDetailService.getById(schedule.getDoctorId());
        BigDecimal amount = doctorDetail != null && doctorDetail.getPrice() != null
                ? doctorDetail.getPrice().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppointmentException(ResultCodeEnum.ORDER_CREATE_EXCEPTION.getCode(), "当前挂号费用配置异常，无法发起支付");
        }

        RegistrationPaymentOrder order = new RegistrationPaymentOrder();
        order.setRegistrationId(registration.getId());
        order.setPayerUserId(payerUserId);
        order.setOutTradeNo(buildOutTradeNo(registration.getId()));
        order.setPaymentAmount(amount);
        order.setPaymentStatus(RegistrationPaymentStatus.PENDING.getCode());
        order.setSubject(buildSubject(schedule));
        order.setPaymentMethod(PAYMENT_METHOD);
        order.setPaymentGateway(PAYMENT_GATEWAY);
        order.setStatusRemark("WAIT_USER_CONFIRM");
        order.setExpireTime(calculateExpireTime());
        paymentOrderMapper.insert(order);
        return order;
    }

    public RegistrationPaymentOrder findByRegistrationId(Long registrationId) {
        LambdaQueryWrapper<RegistrationPaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RegistrationPaymentOrder::getRegistrationId, registrationId)
                .eq(RegistrationPaymentOrder::getIsDeleted, 0)
                .orderByDesc(RegistrationPaymentOrder::getId)
                .last("limit 1");
        return paymentOrderMapper.selectOne(wrapper);
    }

    public RegistrationPaymentOrder findByOutTradeNo(String outTradeNo) {
        LambdaQueryWrapper<RegistrationPaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RegistrationPaymentOrder::getOutTradeNo, outTradeNo)
                .eq(RegistrationPaymentOrder::getIsDeleted, 0)
                .last("limit 1");
        return paymentOrderMapper.selectOne(wrapper);
    }

    public AppointmentPaymentFormVo buildPaymentForm(Long registrationId, Long userId) {
        RegistrationPaymentOrder order = requireOwnedOrder(registrationId, userId);
        if (order.getPaymentStatus() == RegistrationPaymentStatus.PAID.getCode()) {
            throw new AppointmentException(ResultCodeEnum.ORDER_STATUS_ILLEGAL.getCode(), "当前挂号单已支付，无需重复发起");
        }
        if (order.getPaymentStatus() == RegistrationPaymentStatus.CLOSED.getCode()) {
            throw new AppointmentException(ResultCodeEnum.ORDER_STATUS_ILLEGAL.getCode(), "当前支付单已关闭，请重新挂号");
        }
        if (isExpired(order)) {
            closeOrder(order, "支付超时未完成");
            throw new AppointmentException(ResultCodeEnum.ORDER_STATUS_ILLEGAL.getCode(), "当前支付单已超时关闭，请重新挂号");
        }

        String redirectUrl = appendQueryParam(paymentProperties.getReturnUrl(), "outTradeNo", order.getOutTradeNo());
        String formHtml = buildMockRedirectHtml(redirectUrl);
        return new AppointmentPaymentFormVo(
                registrationId,
                order.getOutTradeNo(),
                order.getPaymentAmount(),
                order.getExpireTime(),
                formHtml
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public AppointmentPaymentResultVo resolveReturnResult(String outTradeNo, Long userId) {
        RegistrationPaymentOrder order = requireOwnedOutTradeNo(outTradeNo, userId);
        if (order.getPaymentStatus() == RegistrationPaymentStatus.PENDING.getCode() && isExpired(order)) {
            closeOrder(order, "支付超时未完成");
            order = findByOutTradeNo(outTradeNo);
        }

        Registration registration = registrationMapper.selectById(order.getRegistrationId());
        return buildResult(order, registration);
    }

    @Transactional(rollbackFor = Exception.class)
    public AppointmentPaymentResultVo simulatePaySuccess(String outTradeNo, Long userId) {
        RegistrationPaymentOrder order = requireOwnedOutTradeNo(outTradeNo, userId);
        if (order.getPaymentStatus() == RegistrationPaymentStatus.PENDING.getCode() && isExpired(order)) {
            closeOrder(order, "支付超时未完成");
            order = findByOutTradeNo(outTradeNo);
        }
        if (order.getPaymentStatus() == RegistrationPaymentStatus.PENDING.getCode()) {
            markPaid(order, "MOCK-" + order.getOutTradeNo(), "mock_user_" + userId, "MOCK_SUCCESS");
            order = findByOutTradeNo(outTradeNo);
        }
        Registration registration = registrationMapper.selectById(order.getRegistrationId());
        return buildResult(order, registration);
    }

    @Transactional(rollbackFor = Exception.class)
    public AppointmentPaymentResultVo simulateCancel(String outTradeNo, Long userId) {
        RegistrationPaymentOrder order = requireOwnedOutTradeNo(outTradeNo, userId);
        if (order.getPaymentStatus() == RegistrationPaymentStatus.PENDING.getCode()) {
            closeOrder(order, "用户取消支付");
            order = findByOutTradeNo(outTradeNo);
        }
        Registration registration = registrationMapper.selectById(order.getRegistrationId());
        return buildResult(order, registration);
    }

    @Transactional(rollbackFor = Exception.class)
    public void closeExpiredOrders() {
        LambdaQueryWrapper<RegistrationPaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RegistrationPaymentOrder::getPaymentStatus, RegistrationPaymentStatus.PENDING.getCode())
                .eq(RegistrationPaymentOrder::getIsDeleted, 0)
                .le(RegistrationPaymentOrder::getExpireTime, new Date());

        for (RegistrationPaymentOrder order : paymentOrderMapper.selectList(wrapper)) {
            closeOrder(order, "支付超时未完成");
        }
    }

    public boolean hasPendingPayment(Long registrationId) {
        RegistrationPaymentOrder order = findByRegistrationId(registrationId);
        return order != null && order.getPaymentStatus() == RegistrationPaymentStatus.PENDING.getCode();
    }

    public RegistrationPaymentOrder getLatestOrder(Long registrationId) {
        return findByRegistrationId(registrationId);
    }

    @Transactional(rollbackFor = Exception.class)
    protected void markPaid(RegistrationPaymentOrder order, String tradeNo, String buyerLogonId, String tradeStatus) {
        LambdaUpdateWrapper<RegistrationPaymentOrder> orderUpdate = new LambdaUpdateWrapper<>();
        orderUpdate.eq(RegistrationPaymentOrder::getId, order.getId())
                .eq(RegistrationPaymentOrder::getPaymentStatus, RegistrationPaymentStatus.PENDING.getCode())
                .set(RegistrationPaymentOrder::getPaymentStatus, RegistrationPaymentStatus.PAID.getCode())
                .set(RegistrationPaymentOrder::getGatewayTradeNo, tradeNo)
                .set(RegistrationPaymentOrder::getBuyerLogonId, buyerLogonId)
                .set(RegistrationPaymentOrder::getPaymentTime, new Date())
                .set(RegistrationPaymentOrder::getStatusRemark, tradeStatus);
        int updated = paymentOrderMapper.update(null, orderUpdate);
        if (updated == 0) {
            return;
        }

        LambdaUpdateWrapper<Registration> registrationUpdate = new LambdaUpdateWrapper<>();
        registrationUpdate.eq(Registration::getId, order.getRegistrationId())
                .eq(Registration::getRegistrationStatus, RegistrationStatus.PENDING_PAYMENT.getCode())
                .set(Registration::getRegistrationStatus, RegistrationStatus.PAID.getCode())
                .set(Registration::getUpdateTime, new Date());
        registrationMapper.update(null, registrationUpdate);
        order.setPaymentStatus(RegistrationPaymentStatus.PAID.getCode());
    }

    @Transactional(rollbackFor = Exception.class)
    protected void closeOrder(RegistrationPaymentOrder order, String reason) {
        if (order.getPaymentStatus() != RegistrationPaymentStatus.PENDING.getCode()) {
            return;
        }

        LambdaUpdateWrapper<RegistrationPaymentOrder> orderUpdate = new LambdaUpdateWrapper<>();
        orderUpdate.eq(RegistrationPaymentOrder::getId, order.getId())
                .eq(RegistrationPaymentOrder::getPaymentStatus, RegistrationPaymentStatus.PENDING.getCode())
                .set(RegistrationPaymentOrder::getPaymentStatus, RegistrationPaymentStatus.CLOSED.getCode())
                .set(RegistrationPaymentOrder::getStatusRemark, reason);
        int updated = paymentOrderMapper.update(null, orderUpdate);
        if (updated == 0) {
            return;
        }

        Registration registration = registrationMapper.selectById(order.getRegistrationId());
        if (registration == null) {
            return;
        }
        if (!Objects.equals(registration.getRegistrationStatus(), RegistrationStatus.PENDING_PAYMENT.getCode())) {
            return;
        }

        LambdaUpdateWrapper<Registration> registrationUpdate = new LambdaUpdateWrapper<>();
        registrationUpdate.eq(Registration::getId, registration.getId())
                .eq(Registration::getRegistrationStatus, RegistrationStatus.PENDING_PAYMENT.getCode())
                .set(Registration::getRegistrationStatus, RegistrationStatus.INVALID.getCode())
                .set(Registration::getUpdateTime, new Date());
        registrationMapper.update(null, registrationUpdate);
        scheduleMapper.releaseAppointmentSlot(registration.getScheduleId());

        LambdaQueryWrapper<RegistrationPersonLock> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(RegistrationPersonLock::getRegistrationId, registration.getId());
        registrationPersonLockMapper.delete(lockWrapper);
        order.setPaymentStatus(RegistrationPaymentStatus.CLOSED.getCode());
    }

    private AppointmentPaymentResultVo buildResult(RegistrationPaymentOrder order, Registration registration) {
        boolean paid = order.getPaymentStatus() == RegistrationPaymentStatus.PAID.getCode()
                && registration != null
                && Objects.equals(registration.getRegistrationStatus(), RegistrationStatus.PAID.getCode());

        String message = switch (order.getPaymentStatus()) {
            case 1 -> "模拟支付成功，挂号已生效";
            case 2 -> "支付已取消或超时关闭，请重新挂号";
            case 3 -> "支付失败，请重新发起支付";
            default -> "待支付，请在页面完成模拟支付确认";
        };

        return new AppointmentPaymentResultVo(
                order.getRegistrationId(),
                order.getOutTradeNo(),
                RegistrationPaymentStatus.fromCode(order.getPaymentStatus()).name(),
                registration == null ? null : registration.getRegistrationStatus(),
                paid,
                message,
                order.getPaymentAmount(),
                order.getPaymentTime(),
                order.getExpireTime(),
                order.getGatewayTradeNo()
        );
    }

    private RegistrationPaymentOrder requireOwnedOrder(Long registrationId, Long userId) {
        RegistrationPaymentOrder order = findByRegistrationId(registrationId);
        if (order == null) {
            throw new AppointmentException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        if (!Objects.equals(order.getPayerUserId(), userId)) {
            throw new AppointmentException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        return order;
    }

    private RegistrationPaymentOrder requireOwnedOutTradeNo(String outTradeNo, Long userId) {
        RegistrationPaymentOrder order = findByOutTradeNo(outTradeNo);
        if (order == null) {
            throw new AppointmentException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        if (!Objects.equals(order.getPayerUserId(), userId)) {
            throw new AppointmentException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        return order;
    }

    private boolean isExpired(RegistrationPaymentOrder order) {
        return order.getExpireTime() != null && order.getExpireTime().before(new Date());
    }

    private Date calculateExpireTime() {
        Instant expireAt = Instant.now(clock).plus(Duration.ofMinutes(paymentProperties.getPaymentExpireMinutes()));
        return Date.from(expireAt);
    }

    private String buildSubject(Schedule schedule) {
        String doctorName = hasText(schedule.getDoctorName()) ? schedule.getDoctorName() : "医生";
        String departmentName = hasText(schedule.getDepartmentName()) ? schedule.getDepartmentName() : "门诊";
        return departmentName + "-" + doctorName + " 挂号费";
    }

    private String buildOutTradeNo(Long registrationId) {
        return "REG" + registrationId + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase();
    }

    private String appendQueryParam(String baseUrl, String key, String value) {
        String joiner = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + joiner + key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String buildMockRedirectHtml(String redirectUrl) {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>正在进入模拟支付</title>
                </head>
                <body style="font-family: sans-serif; padding: 32px; color: #1f2937;">
                  <h2>正在进入模拟支付确认页...</h2>
                  <p>如果页面没有自动跳转，请点击下方按钮继续。</p>
                  <p><a href="%s">进入模拟支付页面</a></p>
                  <script>window.location.replace(%s);</script>
                </body>
                </html>
                """.formatted(redirectUrl, toJsString(redirectUrl));
    }

    private String toJsString(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
