package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.entity.PatientAttendant;
import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.entity.dto.AppointmentCreateRequest;
import cn.edu.ncu.medical.entity.dto.AppointmentReservationMessage;
import cn.edu.ncu.medical.entity.dto.RegistrationCondition;
import cn.edu.ncu.medical.entity.vo.AppointmentReservationStatusVo;
import cn.edu.ncu.medical.entity.vo.RegistrationInfo;
import cn.edu.ncu.medical.exception.AppointmentException;
import cn.edu.ncu.medical.mapper.RegistrationMapper;
import cn.edu.ncu.medical.mapper.ScheduleMapper;
import cn.edu.ncu.medical.payment.RegistrationPaymentService;
import cn.edu.ncu.medical.registration.AppointmentReservationKeys;
import cn.edu.ncu.medical.registration.AppointmentReservationPersistenceService;
import cn.edu.ncu.medical.registration.AppointmentReservationProperties;
import cn.edu.ncu.medical.registration.AppointmentReservationRabbitConfig;
import cn.edu.ncu.medical.registration.AppointmentReservationRecord;
import cn.edu.ncu.medical.registration.AppointmentReservationRedisService;
import cn.edu.ncu.medical.registration.AppointmentReservationState;
import cn.edu.ncu.medical.registration.RetryableAppointmentReservationException;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.PatientAttendantService;
import cn.edu.ncu.medical.service.RegistrationService;
import cn.edu.ncu.medical.utils.PatientIdentityUtil;
import cn.edu.ncu.medical.utils.RedisCache;
import cn.edu.ncu.medical.utils.ScheduleCacheKeys;
import cn.edu.ncu.medical.utils.ScheduleTimePolicy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RegistrationServiceImpl extends ServiceImpl<RegistrationMapper, Registration>
        implements RegistrationService {

    private static final String PROCESSING_MESSAGE = "挂号请求已提交，正在处理中";
    private static final String SUCCESS_MESSAGE = "预约创建成功";
    private static final String FAILED_MESSAGE = "挂号请求不存在或已过期";

    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private ScheduleMapper scheduleMapper;
    @Autowired
    private RedisCache redisCache;
    @Autowired
    private PatientAttendantService patientAttendantService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private AppointmentReservationRedisService appointmentReservationRedisService;
    @Autowired
    private AppointmentReservationProperties appointmentReservationProperties;
    @Autowired
    private AppointmentReservationPersistenceService appointmentReservationPersistenceService;
    @Autowired
    private RegistrationPaymentService registrationPaymentService;

    private final Clock clock = Clock.systemDefaultZone();

    public IPage<RegistrationInfo> getRegistrationInfoList(Long userId, Page<RegistrationInfo> page,
                                                           RegistrationCondition registrationCondition) {
        return registrationMapper.selectRegistrationInfoList(userId, page, registrationCondition);
    }

    @Override
    public IPage<RegistrationInfo> getRegistrationList(Long doctorId, Page<RegistrationInfo> page,
                                                       RegistrationCondition registrationCondition) {
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getDoctorId, doctorId);
        LocalDate localDate = LocalDate.now();

        LocalTime now = LocalTime.now();
        Integer isMorning = 0;
        Integer isAfternoon = 0;
        if (now.isBefore(LocalTime.NOON)) {
            isMorning = 1;
        } else if (now.isBefore(LocalTime.of(18, 0))) {
            isAfternoon = 1;
        } else {
            Page<RegistrationInfo> empty = new Page<>(page.getCurrent(), page.getSize());
            empty.setRecords(Collections.emptyList());
            empty.setTotal(0);
            return empty;
        }

        queryWrapper.eq(Schedule::getIsMorning, isMorning);
        queryWrapper.eq(Schedule::getIsAfternoon, isAfternoon);
        queryWrapper.eq(Schedule::getScheduleDate, Date.valueOf(localDate));
        Schedule schedule = scheduleMapper.selectOne(queryWrapper);
        if (schedule == null) {
            Page<RegistrationInfo> empty = new Page<>(page.getCurrent(), page.getSize());
            empty.setRecords(Collections.emptyList());
            empty.setTotal(0);
            return empty;
        }
        return registrationMapper.selectRegistrationInfoBySchedule(doctorId, page, schedule.getId(), registrationCondition);
    }

    @Override
    public IPage<RegistrationInfo> getAllRegistrationList(Long doctorId, Page<RegistrationInfo> page,
                                                          RegistrationCondition condition) {
        return registrationMapper.selectAllRegistrationInfo(doctorId, page, condition);
    }

    @Override
    public RegistrationInfo getRegistrationById(Long registrationId) {
        return registrationMapper.selectRegistrationInfoById(registrationId);
    }

    @Override
    public void changeStatus(Long registrationId, Integer newStatus) {
        if (registrationId == null || newStatus == null) {
            throw new AppointmentException(ResultCodeEnum.PARAM_ERROR);
        }
        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null || (registration.getIsDeleted() != null && registration.getIsDeleted() == 1)) {
            throw new AppointmentException(ResultCodeEnum.REGISTRATION_RECORD_ERROR);
        }

        Integer currentStatus = registration.getRegistrationStatus();
        if (Objects.equals(currentStatus, newStatus)) {
            return;
        }
        if (!isStatusTransitionAllowed(currentStatus, newStatus)) {
            throw new AppointmentException(ResultCodeEnum.REGISTRATION_STATUS_ERROR);
        }

        LambdaUpdateWrapper<Registration> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(Registration::getId, registrationId)
                .set(Registration::getRegistrationStatus, newStatus);
        if (currentStatus != null) {
            lambdaUpdateWrapper.eq(Registration::getRegistrationStatus, currentStatus);
        }

        int updated = registrationMapper.update(null, lambdaUpdateWrapper);
        if (updated == 0) {
            throw new AppointmentException(ResultCodeEnum.REGISTRATION_STATUS_ERROR);
        }
    }

    @Override
    public AppointmentReservationStatusVo createRegistration(AppointmentCreateRequest request, Long userId) {
        if (userId == null) {
            throw new AppointmentException(ResultCodeEnum.USER_NOT_LOGIN);
        }
        validateCreateParam(request);

        PatientAttendant patientAttendant = loadAndValidatePatient(request.getPatientId(), userId);
        String personKey = PatientIdentityUtil.buildPersonKey(patientAttendant);
        Long scheduleId = request.getScheduleId();
        Schedule schedule = loadAndValidateSchedule(scheduleId);

        Registration existingRegistration = appointmentReservationPersistenceService
                .findActiveByScheduleAndPerson(scheduleId, personKey);
        if (existingRegistration != null) {
            return resolveExistingRegistration(existingRegistration, userId);
        }

        appointmentReservationRedisService.initializeStockIfAbsent(schedule);

        long processingTtlMillis = TimeUnit.SECONDS.toMillis(appointmentReservationProperties.getProcessingTtlSeconds());
        long expireAtMillis = System.currentTimeMillis() + processingTtlMillis;
        String token = AppointmentReservationKeys.newToken(scheduleId);

        AppointmentReservationRedisService.ReserveResult reserveResult = appointmentReservationRedisService.reserve(
                scheduleId,
                personKey,
                userId,
                patientAttendant.getId(),
                token,
                processingTtlMillis,
                expireAtMillis
        );
        if (reserveResult.noStock()) {
            throw new AppointmentException(ResultCodeEnum.SOURCE_INSUFFICIENT);
        }
        if (reserveResult.duplicate()) {
            return resolveDuplicateReservation(scheduleId, reserveResult.token(), userId);
        }

        try {
            rabbitTemplate.convertAndSend(
                    AppointmentReservationRabbitConfig.APPOINTMENT_EXCHANGE,
                    AppointmentReservationRabbitConfig.APPOINTMENT_ROUTING_KEY,
                    new AppointmentReservationMessage(reserveResult.token(), scheduleId)
            );
        } catch (RuntimeException ex) {
            failReservation(new AppointmentReservationMessage(reserveResult.token(), scheduleId), "挂号消息投递失败");
            throw new AppointmentException(ResultCodeEnum.SERVICE_ERROR.getCode(), "挂号服务暂不可用，请稍后重试");
        }

        return processingStatus(reserveResult.token(), PROCESSING_MESSAGE);
    }

    @Override
    public AppointmentReservationStatusVo getReservationStatus(String token, Long userId) {
        if (userId == null) {
            throw new AppointmentException(ResultCodeEnum.USER_NOT_LOGIN);
        }

        AppointmentReservationRecord record = appointmentReservationRedisService.getReservation(token);
        if (record != null) {
            ensureReservationOwner(record.getUserId(), userId);
            return toStatusVo(token, record);
        }

        Registration registration = appointmentReservationPersistenceService.findActiveByToken(token);
        if (registration != null) {
            validatePatientOwnership(registration.getPatientId(), userId);
            return paymentAwareStatus(token, registration);
        }

        return failedStatus(token, null, FAILED_MESSAGE);
    }

    @Override
    public void consumeReservedRegistration(AppointmentReservationMessage message) {
        Long scheduleId = resolveScheduleId(message);
        String token = message.getToken();
        AppointmentReservationRecord record = appointmentReservationRedisService.getReservation(scheduleId, token);
        if (record == null || record.getState() != AppointmentReservationState.PENDING) {
            return;
        }

        try {
            AppointmentReservationPersistenceService.PersistResult persistResult =
                    appointmentReservationPersistenceService.persist(record, scheduleId, token);
            if (!persistResult.success()) {
                failReservation(message, persistResult.message());
                return;
            }

            Schedule schedule = scheduleMapper.selectById(scheduleId);
            if (schedule != null) {
                String key = ScheduleCacheKeys.scheduleListKey(schedule.getSubDepartmentId(), schedule.getScheduleDate());
                redisCache.delete(key);
            }

            long successKeepMillis = schedule == null
                    ? TimeUnit.HOURS.toMillis(1)
                    : appointmentReservationRedisService.calculateSuccessKeepMillis(schedule);
            boolean confirmed = appointmentReservationRedisService.confirm(
                    scheduleId,
                    record.getPersonKey(),
                    token,
                    persistResult.registrationId(),
                    successKeepMillis
            );
            if (!confirmed) {
                throw new RetryableAppointmentReservationException("预约确认状态写回失败");
            }
        } catch (RetryableAppointmentReservationException ex) {
            throw ex;
        } catch (Exception ex) {
            if (appointmentReservationPersistenceService.isRetryable(ex)) {
                throw new RetryableAppointmentReservationException("预约持久化暂不可用", ex);
            }
            log.warn("Consume appointment reservation failed, token={}", token, ex);
            failReservation(message, defaultMessage(ex.getMessage(), "挂号处理失败"));
        }
    }

    @Override
    public void failReservation(AppointmentReservationMessage message, String reason) {
        Long scheduleId = resolveScheduleId(message);
        String token = message.getToken();
        AppointmentReservationRecord record = appointmentReservationRedisService.getReservation(scheduleId, token);
        if (record == null) {
            appointmentReservationRedisService.removeExpireToken(scheduleId, token);
            return;
        }
        if (record.getState() != AppointmentReservationState.PENDING) {
            return;
        }
        long failedKeepMillis = TimeUnit.SECONDS.toMillis(appointmentReservationProperties.getFailedKeepSeconds());
        appointmentReservationRedisService.rollback(
                scheduleId,
                record.getPersonKey(),
                token,
                defaultMessage(reason, "挂号处理失败"),
                failedKeepMillis
        );
    }

    @Override
    public void releaseExpiredReservation(Long scheduleId, String token) {
        AppointmentReservationRecord record = appointmentReservationRedisService.getReservation(scheduleId, token);
        if (record == null) {
            appointmentReservationRedisService.removeExpireToken(scheduleId, token);
            return;
        }
        if (record.getState() != AppointmentReservationState.PENDING) {
            appointmentReservationRedisService.removeExpireToken(scheduleId, token);
            return;
        }
        long failedKeepMillis = TimeUnit.SECONDS.toMillis(appointmentReservationProperties.getFailedKeepSeconds());
        appointmentReservationRedisService.rollback(
                scheduleId,
                record.getPersonKey(),
                token,
                "TIMEOUT_RELEASED",
                failedKeepMillis
        );
    }

    private AppointmentReservationStatusVo resolveExistingRegistration(Registration registration, Long userId) {
        if (!belongsToUser(registration.getPatientId(), userId)) {
            throw new AppointmentException(ResultCodeEnum.REPEAT_SUBMIT.getCode(), "当前真实就诊人已存在该排班挂号记录");
        }
        if (registration.getRequestToken() != null && !registration.getRequestToken().isBlank()) {
            return paymentAwareStatus(registration.getRequestToken(), registration);
        }
        throw new AppointmentException(ResultCodeEnum.REPEAT_SUBMIT.getCode(), "当前真实就诊人已存在该排班挂号记录");
    }

    private AppointmentReservationStatusVo resolveDuplicateReservation(Long scheduleId, String existingToken, Long userId) {
        if (existingToken == null || existingToken.isBlank()) {
            throw new AppointmentException(ResultCodeEnum.REPEAT_SUBMIT.getCode(), "当前真实就诊人已存在该排班挂号请求");
        }
        AppointmentReservationRecord record = appointmentReservationRedisService.getReservation(scheduleId, existingToken);
        if (record != null) {
            ensureReservationOwner(record.getUserId(), userId);
            return toStatusVo(existingToken, record);
        }
        Registration registration = appointmentReservationPersistenceService.findActiveByToken(existingToken);
        if (registration != null) {
            validatePatientOwnership(registration.getPatientId(), userId);
            return paymentAwareStatus(existingToken, registration);
        }
        throw new AppointmentException(ResultCodeEnum.REPEAT_SUBMIT.getCode(), "当前真实就诊人已存在该排班挂号请求");
    }

    private AppointmentReservationStatusVo toStatusVo(String token, AppointmentReservationRecord record) {
        if (record.getState() == AppointmentReservationState.CONFIRMED) {
            Long registrationId = record.getRegistrationId();
            if (registrationId == null) {
                Registration registration = appointmentReservationPersistenceService.findActiveByToken(token);
                if (registration != null) {
                    registrationId = registration.getId();
                }
            }
            if (registrationId != null) {
                Registration registration = registrationMapper.selectById(registrationId);
                if (registration != null) {
                    return paymentAwareStatus(token, registration);
                }
            }
            return successStatus(token, registrationId, SUCCESS_MESSAGE);
        }
        if (record.getState() == AppointmentReservationState.ROLLED_BACK) {
            return failedStatus(token, null, defaultMessage(record.getMessage(), "挂号失败"));
        }
        return processingStatus(token, defaultMessage(record.getMessage(), PROCESSING_MESSAGE));
    }

    private AppointmentReservationStatusVo paymentAwareStatus(String token, Registration registration) {
        if (registration == null) {
            return failedStatus(token, null, FAILED_MESSAGE);
        }
        if (Objects.equals(registration.getRegistrationStatus(), cn.edu.ncu.medical.constant.RegistrationStatus.PENDING_PAYMENT.getCode())) {
            AppointmentReservationStatusVo statusVo = new AppointmentReservationStatusVo(
                    token,
                    "PAYING",
                    registration.getId(),
                    "挂号已创建，请完成支付"
            );
            statusVo.setPaymentRequired(Boolean.TRUE);
            var order = registrationPaymentService.getLatestOrder(registration.getId());
            if (order != null) {
                statusVo.setOutTradeNo(order.getOutTradeNo());
                statusVo.setPaymentStatus(cn.edu.ncu.medical.constant.RegistrationPaymentStatus.fromCode(order.getPaymentStatus()).name());
                statusVo.setPaymentExpireTime(order.getExpireTime());
            }
            return statusVo;
        }
        return successStatus(token, registration.getId(), SUCCESS_MESSAGE);
    }

    private AppointmentReservationStatusVo processingStatus(String token, String message) {
        return new AppointmentReservationStatusVo(token, "PROCESSING", null, message);
    }

    private AppointmentReservationStatusVo successStatus(String token, Long registrationId, String message) {
        return new AppointmentReservationStatusVo(token, "SUCCESS", registrationId, message);
    }

    private AppointmentReservationStatusVo failedStatus(String token, Long registrationId, String message) {
        return new AppointmentReservationStatusVo(token, "FAILED", registrationId, message);
    }

    private void validateCreateParam(AppointmentCreateRequest request) {
        Assert.notNull(request, ResultCodeEnum.REGISTRATION_INFO_EMPTY.getMessage());
        Assert.notNull(request.getPatientId(), ResultCodeEnum.PATIENT_ID_EMPTY.getMessage());
        Assert.notNull(request.getScheduleId(), ResultCodeEnum.SCHEDULE_ID_EMPTY.getMessage());
        Assert.isTrue(request.getPatientId() > 0, ResultCodeEnum.PATIENT_ID_INVALID.getMessage());
        Assert.isTrue(request.getScheduleId() > 0, ResultCodeEnum.SCHEDULE_ID_INVALID.getMessage());
    }

    private PatientAttendant loadAndValidatePatient(Long patientId, Long userId) {
        PatientAttendant patientAttendant = patientAttendantService.getById(patientId);
        if (patientAttendant == null || patientAttendant.getSystemUserId() == null) {
            throw new AppointmentException(ResultCodeEnum.PATIENT_ID_INVALID);
        }
        if (!Objects.equals(patientAttendant.getSystemUserId(), userId)) {
            throw new AppointmentException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        if (patientAttendant.getIdCard() == null || patientAttendant.getIdCard().isBlank()
                || patientAttendant.getRealName() == null || patientAttendant.getRealName().isBlank()) {
            throw new AppointmentException(ResultCodeEnum.PATIENT_NOT_VERIFIED);
        }
        return patientAttendant;
    }

    private Schedule loadAndValidateSchedule(Long scheduleId) {
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null || schedule.getScheduleDate() == null) {
            throw new AppointmentException(ResultCodeEnum.SCHEDULE_NOT_EXIST);
        }
        if (schedule.getStatus() == null || schedule.getStatus() != 1) {
            throw new AppointmentException(ResultCodeEnum.SCHEDULE_NOT_EXIST);
        }
        if (!ScheduleTimePolicy.canCreateOrder(schedule, clock)) {
            throw new AppointmentException(ResultCodeEnum.SCHEDULE_NOT_EXIST);
        }
        return schedule;
    }

    private void validatePatientOwnership(Long patientId, Long userId) {
        PatientAttendant patientAttendant = patientAttendantService.getById(patientId);
        if (patientAttendant == null || patientAttendant.getSystemUserId() == null) {
            throw new AppointmentException(ResultCodeEnum.PATIENT_ID_INVALID);
        }
        if (!Objects.equals(patientAttendant.getSystemUserId(), userId)) {
            throw new AppointmentException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
    }

    private boolean belongsToUser(Long patientId, Long userId) {
        PatientAttendant patientAttendant = patientAttendantService.getById(patientId);
        return patientAttendant != null && Objects.equals(patientAttendant.getSystemUserId(), userId);
    }

    private void ensureReservationOwner(Long reservationUserId, Long userId) {
        if (!Objects.equals(reservationUserId, userId)) {
            throw new AppointmentException(ResultCodeEnum.REPEAT_SUBMIT.getCode(), "当前真实就诊人已存在该排班挂号请求");
        }
    }

    private Long resolveScheduleId(AppointmentReservationMessage message) {
        if (message == null || message.getToken() == null || message.getToken().isBlank()) {
            throw new AppointmentException(ResultCodeEnum.PARAM_ERROR);
        }
        if (message.getScheduleId() != null && message.getScheduleId() > 0) {
            return message.getScheduleId();
        }
        return AppointmentReservationKeys.parseScheduleId(message.getToken());
    }

    private String defaultMessage(String source, String fallback) {
        return (source == null || source.isBlank()) ? fallback : source;
    }

    private boolean isStatusTransitionAllowed(Integer currentStatus, Integer newStatus) {
        if (newStatus == null) {
            return false;
        }
        if (currentStatus == null) {
            return true;
        }
        return switch (currentStatus) {
            case 0 -> newStatus == 1 || newStatus == 2 || newStatus == 7 || newStatus == 8;
            case 1 -> newStatus == 2 || newStatus == 7 || newStatus == 8;
            case 2 -> newStatus == 7 || newStatus == 3 || newStatus == 5 || newStatus == 8;
            case 3 -> newStatus == 4 || newStatus == 5 || newStatus == 8;
            case 5 -> newStatus == 6 || newStatus == 7 || newStatus == 8;
            case 6 -> newStatus == 2 || newStatus == 7 || newStatus == 3 || newStatus == 8;
            case 7 -> newStatus == 3 || newStatus == 5 || newStatus == 8;
            case 4, 8 -> false;
            default -> false;
        };
    }
}
