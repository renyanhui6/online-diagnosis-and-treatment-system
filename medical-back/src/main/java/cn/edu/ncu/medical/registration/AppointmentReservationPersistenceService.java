package cn.edu.ncu.medical.registration;

import cn.edu.ncu.medical.constant.RegistrationStatus;
import cn.edu.ncu.medical.entity.PatientAttendant;
import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.entity.RegistrationPersonLock;
import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.mapper.RegistrationPersonLockMapper;
import cn.edu.ncu.medical.mapper.RegistrationMapper;
import cn.edu.ncu.medical.mapper.ScheduleMapper;
import cn.edu.ncu.medical.payment.RegistrationPaymentService;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.PatientAttendantService;
import cn.edu.ncu.medical.utils.PatientIdentityUtil;
import cn.edu.ncu.medical.utils.ScheduleTimePolicy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.Clock;

@Service
public class AppointmentReservationPersistenceService {
    private final RegistrationMapper registrationMapper;
    private final RegistrationPersonLockMapper registrationPersonLockMapper;
    private final ScheduleMapper scheduleMapper;
    private final PatientAttendantService patientAttendantService;
    private final RegistrationPaymentService registrationPaymentService;
    private final Clock clock = Clock.systemDefaultZone();

    public AppointmentReservationPersistenceService(RegistrationMapper registrationMapper,
                                                   RegistrationPersonLockMapper registrationPersonLockMapper,
                                                   ScheduleMapper scheduleMapper,
                                                   PatientAttendantService patientAttendantService,
                                                   RegistrationPaymentService registrationPaymentService) {
        this.registrationMapper = registrationMapper;
        this.registrationPersonLockMapper = registrationPersonLockMapper;
        this.scheduleMapper = scheduleMapper;
        this.patientAttendantService = patientAttendantService;
        this.registrationPaymentService = registrationPaymentService;
    }

    @Transactional(rollbackFor = Exception.class)
    public PersistResult persist(AppointmentReservationRecord record, Long scheduleId, String token) {
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null || schedule.getScheduleDate() == null || schedule.getStatus() == null || schedule.getStatus() != 1) {
            return PersistResult.failure("排班不存在或不合法");
        }
        if (!ScheduleTimePolicy.canCreateOrder(schedule, clock)) {
            return PersistResult.failure("当前排班已停止挂号");
        }

        PatientAttendant patient = patientAttendantService.getById(record.getPatientId());
        if (patient == null || patient.getIdCard() == null) {
            return PersistResult.failure("就诊人不存在或未实名");
        }
        String currentPersonKey = PatientIdentityUtil.buildPersonKey(patient);
        if (!currentPersonKey.equals(record.getPersonKey())) {
            return PersistResult.failure("就诊人实名信息已变更，请重新提交");
        }

        Registration existingByToken = findActiveByToken(token);
        if (existingByToken != null) {
            return PersistResult.success(existingByToken.getId());
        }

        Registration existingByPerson = findActiveByScheduleAndPerson(scheduleId, record.getPersonKey());
        if (existingByPerson != null) {
            return PersistResult.failure("当前真实就诊人已存在该排班挂号记录");
        }

        try {
            RegistrationPersonLock lock = new RegistrationPersonLock();
            lock.setScheduleId(scheduleId);
            lock.setPersonKey(record.getPersonKey());
            lock.setRequestToken(token);
            registrationPersonLockMapper.insert(lock);

            int updated = scheduleMapper.takeAppointmentSlot(scheduleId);
            if (updated == 0) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return PersistResult.failure(ResultCodeEnum.SOURCE_INSUFFICIENT.getMessage());
            }

            Registration registration = new Registration();
            registration.setDoctorId(schedule.getDoctorId());
            registration.setPatientId(record.getPatientId());
            registration.setScheduleId(scheduleId);
            registration.setRegistrationStatus(RegistrationStatus.PENDING_PAYMENT.getCode());
            registration.setPersonKey(record.getPersonKey());
            registration.setRequestToken(token);
            registrationMapper.insert(registration);
            lock.setRegistrationId(registration.getId());
            registrationPersonLockMapper.updateById(lock);
            registrationPaymentService.createPendingOrder(registration, record.getUserId(), schedule);
            return PersistResult.success(registration.getId());
        } catch (DuplicateKeyException ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            RegistrationPersonLock conflictByToken = findLockByToken(token);
            if (conflictByToken != null && conflictByToken.getRegistrationId() != null) {
                return PersistResult.success(conflictByToken.getRegistrationId());
            }
            RegistrationPersonLock conflictByPerson = findLockByScheduleAndPerson(scheduleId, record.getPersonKey());
            if (conflictByPerson != null) {
                return PersistResult.failure("当前真实就诊人已存在该排班挂号记录");
            }
            Registration tokenRegistration = findActiveByToken(token);
            if (tokenRegistration != null) {
                return PersistResult.success(tokenRegistration.getId());
            }
            Registration personRegistration = findActiveByScheduleAndPerson(scheduleId, record.getPersonKey());
            if (personRegistration != null) {
                return PersistResult.failure("当前真实就诊人已存在该排班挂号记录");
            }
            throw ex;
        }
    }

    public boolean isRetryable(Throwable throwable) {
        return throwable instanceof TransientDataAccessException;
    }

    public Registration findActiveByToken(String token) {
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getRequestToken, token)
                .eq(Registration::getIsDeleted, 0)
                .ne(Registration::getRegistrationStatus, RegistrationStatus.INVALID.getCode())
                .last("limit 1");
        return registrationMapper.selectOne(wrapper);
    }

    public Registration findActiveByScheduleAndPerson(Long scheduleId, String personKey) {
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getScheduleId, scheduleId)
                .eq(Registration::getPersonKey, personKey)
                .eq(Registration::getIsDeleted, 0)
                .ne(Registration::getRegistrationStatus, RegistrationStatus.INVALID.getCode())
                .last("limit 1");
        return registrationMapper.selectOne(wrapper);
    }

    public RegistrationPersonLock findLockByToken(String token) {
        LambdaQueryWrapper<RegistrationPersonLock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RegistrationPersonLock::getRequestToken, token).last("limit 1");
        return registrationPersonLockMapper.selectOne(wrapper);
    }

    public RegistrationPersonLock findLockByScheduleAndPerson(Long scheduleId, String personKey) {
        LambdaQueryWrapper<RegistrationPersonLock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RegistrationPersonLock::getScheduleId, scheduleId)
                .eq(RegistrationPersonLock::getPersonKey, personKey)
                .last("limit 1");
        return registrationPersonLockMapper.selectOne(wrapper);
    }

    public record PersistResult(boolean success, Long registrationId, String message) {
        public static PersistResult success(Long registrationId) {
            return new PersistResult(true, registrationId, "预约创建成功");
        }

        public static PersistResult failure(String message) {
            return new PersistResult(false, null, message);
        }
    }
}
