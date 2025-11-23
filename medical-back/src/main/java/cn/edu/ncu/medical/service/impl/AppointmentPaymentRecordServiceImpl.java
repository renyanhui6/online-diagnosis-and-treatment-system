package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.config.event.DelayQueueProducer;
import cn.edu.ncu.medical.entity.DoctorDetail;
import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.exception.AppointmentException;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.mapper.DoctorDetailMapper;
import cn.edu.ncu.medical.mapper.RegistrationMapper;
import cn.edu.ncu.medical.mapper.ScheduleMapper;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.utils.RedisCache;
import cn.edu.ncu.medical.utils.TimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.edu.ncu.medical.entity.AppointmentPaymentRecord;
import cn.edu.ncu.medical.service.AppointmentPaymentRecordService;
import cn.edu.ncu.medical.mapper.AppointmentPaymentRecordMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author star
 * @description 针对表【appointment_payment_record】的数据库操作Service实现
 * @createDate 2025-07-24 17:44:39
 */
@Service
public class AppointmentPaymentRecordServiceImpl extends ServiceImpl<AppointmentPaymentRecordMapper, AppointmentPaymentRecord>
        implements AppointmentPaymentRecordService{

    @Autowired
    private AppointmentPaymentRecordMapper appointmentPaymentRecordMapper;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private DoctorDetailMapper doctorDetailMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;


    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private RedisCache redisCache;



    @Autowired
    DelayQueueProducer delayQueueProducer;


    /**
     * 订单创建事件（创建订单后发布）
     */
    public class OrderCreatedEvent {
        private final Long orderId;

        public OrderCreatedEvent(Long orderId) {
            this.orderId = orderId;
        }

        public Long getOrderId() {
            return orderId;
        }
    }

    /**
     * 订单删除事件（删除订单后发布）
     */
    public class OrderDeletedEvent {
        private final Long orderId;

        public OrderDeletedEvent(Long orderId) {
            this.orderId = orderId;
        }

        public Long getOrderId() {
            return orderId;
        }
    }


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void addToDelayQueue(OrderCreatedEvent orderCreatedEvent) {
        delayQueueProducer.addRegisterOrder(orderCreatedEvent.getOrderId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void removefromDelayQueue(OrderDeletedEvent orderDeletedEvent) {
        delayQueueProducer.removeRegisterOrder(orderDeletedEvent.getOrderId());
    }



    private boolean checkOwnerById(Long id) {
        if(id==LoginUserHolder.getLoginUser().getUserId()){
            return true;
        }
        return false;
    }


    @Override
    public IPage<AppointmentPaymentRecord> getAppointmentPaymentRecords(Integer pageNum,  // 页码（从1开始）
             Integer pageSize, // 每页条数
             Date createDate) {

        // 1. 创建分页对象
        IPage<AppointmentPaymentRecord> page = new Page<>(pageNum, pageSize);

        // 2. 构建基础查询条件（无论是否传时间都生效的条件）
        LambdaQueryWrapper<AppointmentPaymentRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppointmentPaymentRecord::getPayerId, LoginUserHolder.getLoginUser().getUserId())  // 固定条件
                .orderByDesc(AppointmentPaymentRecord::getCreateTime);  // 固定排序

        // 3. 动态添加时间范围条件（仅当createDate不为null时才添加）
        if (createDate != null) {
            // 计算当天的起始和结束时间
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(createDate);

            // 当天00:00:00
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date startTime = calendar.getTime();

            // 当天23:59:59
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            Date endTime = calendar.getTime();

            // 添加时间范围条件
            queryWrapper.ge(AppointmentPaymentRecord::getCreateTime, startTime)
                    .le(AppointmentPaymentRecord::getCreateTime, endTime);
        }

        // 4. 执行分页查询
        return appointmentPaymentRecordMapper.selectPage(page, queryWrapper);
    }

    @Override
    public AppointmentPaymentRecord getAppointmentPaymentRecordById(Long id) {
        AppointmentPaymentRecord appointmentPaymentRecord = appointmentPaymentRecordMapper.selectById(id);
        if(appointmentPaymentRecord==null){
            throw new AppointmentException(ResultCodeEnum.ORDER_EMPTY);
        }
        if(!checkOwnerById(appointmentPaymentRecord.getPayerId())){
            throw new  AppointmentException(ResultCodeEnum.OPERATION_ERROR);
        }
        return appointmentPaymentRecord;
    }




    @Transactional(rollbackFor = Exception.class)
    @Override
    public void modifyAppointmentPayment(AppointmentPaymentRecord ap,Long userId) {
        //支付时间为当前时间
        //状态变为已支付
        if(ap==null){
            throw new AppointmentException(ResultCodeEnum.ORDER_INFO_EMPTY);
        }

        AppointmentPaymentRecord appointmentPaymentRecord = appointmentPaymentRecordMapper.selectById(ap.getId());

        if(appointmentPaymentRecord.getPayerId()!=userId){
            throw new  AppointmentException(ResultCodeEnum.OPERATION_ERROR);
        }

        Long registrationId = appointmentPaymentRecord.getRegistrationId();
        //
        Registration registration = new Registration();
        registration.setId(registrationId);
        registration.setRegistrationStatus(1);
        registration.setUpdateTime(new Date());
        registrationMapper.updateById(registration);

        ap.setPaymentStatus(1);
        ap.setPaymentTime(new Date());
        appointmentPaymentRecordMapper.updateById(ap);


        applicationEventPublisher.publishEvent(new OrderDeletedEvent(appointmentPaymentRecord.getId()));
    }



    @Transactional(rollbackFor = Exception.class)
    @Override
    public void RefundAppointmentPayment(Long id) {
        AppointmentPaymentRecord appointmentPaymentRecord = appointmentPaymentRecordMapper.selectById(id);

        if(!checkOwnerById(appointmentPaymentRecord.getPayerId())){
            throw new  AppointmentException(ResultCodeEnum.OPERATION_ERROR);
        }

        if(appointmentPaymentRecord==null){
            throw new AppointmentException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        if(appointmentPaymentRecord.getPaymentStatus()==0||appointmentPaymentRecord.getPaymentStatus()==2){
            throw new AppointmentException(ResultCodeEnum.REFUND_ILLEGAL);
        }
        //变为已退款状态
        appointmentPaymentRecord.setPaymentStatus(2);
        appointmentPaymentRecordMapper.updateById(appointmentPaymentRecord);
        //检查就诊记录
        Long  registrationId = appointmentPaymentRecord.getRegistrationId();
        Registration registration = registrationMapper.selectById(registrationId);
        if(registration==null){
            throw new AppointmentException(ResultCodeEnum.REGISTRATION_RECORD_ERROR);
        }
        Integer status = registration.getRegistrationStatus();
        if(status!=1&&status!=5){
            throw new AppointmentException(ResultCodeEnum.REGISTRATION_STATUS_ERROR);
        }
        //挂号表状态变为7
        registration.setRegistrationStatus(7);
        registration.setUpdateTime(new Date());
        registrationMapper.updateById(registration);

        //当前在挂号的数量减一
        Long scheduleId = registration.getScheduleId();

        try{LambdaUpdateWrapper<Schedule> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Schedule::getId,scheduleId);
            updateWrapper.setSql("current_appointment_count = current_appointment_count - 1");
            scheduleMapper.update(null,updateWrapper);

        }catch (Exception e){
            throw new AppointmentException(ResultCodeEnum.APPOINTMENT_HANDLER_ERROR);
        }

        //清缓存
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        String key = String.join(":",schedule.getSubDepartmentId().toString(),schedule.getDepartmentName(),schedule.getScheduleDate().toString());
        redisCache.delete(key);

        registrationMapper.deleteById(registrationId);


        //执行退款处理


    }




    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createAppointmentPayment(Registration r,Long userId){


        validateRegistrationParam(r);

        Long doctorId = r.getDoctorId();
        Long patientId = r.getPatientId();
        Long scheduleId = r.getScheduleId();
        // 业务检查，医生挂号是否可用
        //用于判断当前医生挂号是否可用可用即挂号数量返回当前可用数量

        LambdaQueryWrapper<Schedule> scheduleQueryWrapper = new LambdaQueryWrapper<>();
        scheduleQueryWrapper.eq(Schedule::getId, scheduleId);
        Schedule schedule = scheduleMapper.selectOne( scheduleQueryWrapper);
        if(schedule==null){
            throw new AppointmentException(ResultCodeEnum.SCHEDULE_NOT_EXIST);
        }
        Date scheduleDate  = schedule.getScheduleDate();
        if(scheduleDate==null){
            throw new AppointmentException(ResultCodeEnum.SCHEDULE_NOT_EXIST);
        }
    // 使用 Calendar 处理日期
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(scheduleDate);

    // 创建当天 8:00 的时间
        Calendar eightOClockCal = (Calendar) calendar.clone();
        eightOClockCal.set(Calendar.HOUR_OF_DAY, 8);
        eightOClockCal.set(Calendar.MINUTE, 0);
        eightOClockCal.set(Calendar.SECOND, 0);
        eightOClockCal.set(Calendar.MILLISECOND, 0);
        Date eightOClock = eightOClockCal.getTime();

    // 创建当天 14:00 的时间
        Calendar twoPMCal = (Calendar) calendar.clone();
        twoPMCal.set(Calendar.HOUR_OF_DAY, 14);
        twoPMCal.set(Calendar.MINUTE, 0);
        twoPMCal.set(Calendar.SECOND, 0);
        twoPMCal.set(Calendar.MILLISECOND, 0);
        Date twoPM = twoPMCal.getTime();

// 获取当前时间
        Date now = new Date();

// 判断当前时间是否在 8:00 之前
        boolean isBeforeEight = now.before(eightOClock);
// 判断当前时间是否在 14:00 之前
        boolean isBeforeTwoPM = now.before(twoPM);
        // 先判断排班类型是否合法（避免既不是上午也不是下午的异常情况）
        boolean isMorning = schedule.getIsMorning() == 1;
        boolean isAfternoon = schedule.getIsAfternoon() == 1;

        if (!isMorning && !isAfternoon) {
            // 既不是上午场也不是下午场，排班类型无效
            throw new AppointmentException(ResultCodeEnum.SCHEDULE_NOT_EXIST);
        }

// 判断预约时间是否符合对应场次的规则
        boolean isMorningValid = isMorning && isBeforeEight; // 上午场且在8点前：有效
        boolean isAfternoonValid = isAfternoon && isBeforeTwoPM; // 下午场且在14点前：有效

        if (!isMorningValid && !isAfternoonValid) {
            // 时间不符合当前排班的规则
            throw new AppointmentException(ResultCodeEnum.SCHEDULE_NOT_EXIST);
        }


        if(schedule.getCurrentAppointmentCount()>=schedule.getAppointmentLimit()) {
            throw new AppointmentException(ResultCodeEnum.SOURCE_INSUFFICIENT);
        }

        //核心业务处理，挂号处理,号源数量减一对schedule字段加一
        try{LambdaUpdateWrapper<Schedule> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Schedule::getId,scheduleId);
            updateWrapper.setSql("current_appointment_count = current_appointment_count + 1");
            scheduleMapper.update(null,updateWrapper);

        }catch (Exception e){
            throw new AppointmentException(ResultCodeEnum.APPOINTMENT_HANDLER_ERROR);
        }

        //核心业务处理，订单表相关创建
        try{r.setRegistrationStatus(0);
            //根据医生id和就诊人id进行插入还有支付金额
            //初始化参数
            //额外参数registration_status
            //插入并回显
            registrationMapper.insert(r);
            Long registrationId = r.getId();



            AppointmentPaymentRecord appointmentPaymentRecord = new AppointmentPaymentRecord();
            //设置挂号订单表的状态为未支付
            appointmentPaymentRecord.setPaymentStatus(0);
            //设置金额从doctor表中查询
            LambdaQueryWrapper<DoctorDetail> doctorDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
            doctorDetailLambdaQueryWrapper.eq(DoctorDetail::getId,r.getDoctorId()).select(DoctorDetail::getPrice);
            DoctorDetail doctorDetail = doctorDetailMapper.selectOne(doctorDetailLambdaQueryWrapper);
            if (doctorDetail == null) {
                throw new AppointmentException(ResultCodeEnum.DOCTOR_NOT_EXIST);
            }
            BigDecimal amount = doctorDetail.getPrice();
            appointmentPaymentRecord.setPaymentAmount(amount);
            //设置registrationId
            appointmentPaymentRecord.setRegistrationId(registrationId);
            //设置付款/拥有者为当前用户
            Long payerId = userId;

            appointmentPaymentRecord.setPayerId(payerId);

            appointmentPaymentRecordMapper.insert(appointmentPaymentRecord);
            //主键回显
            Long appointId= appointmentPaymentRecord.getId();
            //返回值为挂号订单id

            //清缓存
            String key = String.join(":",schedule.getSubDepartmentId().toString(),schedule.getDepartmentName(),TimeUtil.dateToString(schedule.getScheduleDate()));
            redisCache.delete(key);

            applicationEventPublisher.publishEvent(new OrderCreatedEvent(appointId));

            return appointId;
        }catch (Exception e){
            throw new AppointmentException(ResultCodeEnum.ORDER_CREATE_EXCEPTION);
        }

    }



    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancelAppointmentPaymentRecordByIdAdmin(Long id) {


        AppointmentPaymentRecord appointmentPaymentRecord = appointmentPaymentRecordMapper.selectById(id);

        if(appointmentPaymentRecord == null){
            throw new AppointmentException(ResultCodeEnum.REMOVE_ORDER_NOT_EXIST);
        }
        Integer status = appointmentPaymentRecord.getPaymentStatus();

        if (status != 0) {
            throw new AppointmentException(ResultCodeEnum.ORDER_STATUS_ILLEGAL);
        }

        Long registrationId = appointmentPaymentRecord.getRegistrationId();

        Registration registration = registrationMapper.selectById(registrationId);

        if(registration == null){
            throw new AppointmentException(ResultCodeEnum.REGISTRATION_RECORD_ERROR);
        }
        Long scheduleId = registration.getScheduleId();
        //在挂号的数量减一
        try{LambdaUpdateWrapper<Schedule> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Schedule::getId,scheduleId);
            updateWrapper.setSql("current_appointment_count = current_appointment_count - 1");
            scheduleMapper.update(null,updateWrapper);

        }catch (Exception e){
            throw new AppointmentException(ResultCodeEnum.APPOINTMENT_HANDLER_ERROR);
        }
        //清缓存
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        String key = String.join(":",schedule.getSubDepartmentId().toString(),schedule.getDepartmentName(),schedule.getScheduleDate().toString());
        redisCache.delete(key);

        registrationMapper.deleteById(registrationId);


        appointmentPaymentRecordMapper.deleteById(id);

    }



    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancelAppointmentPaymentRecordById(Long id) {


        AppointmentPaymentRecord appointmentPaymentRecord = appointmentPaymentRecordMapper.selectById(id);

        if(appointmentPaymentRecord == null){
            throw new AppointmentException(ResultCodeEnum.REMOVE_ORDER_NOT_EXIST);
        }

        if(!checkOwnerById(appointmentPaymentRecord.getPayerId())){
            throw new  AppointmentException(ResultCodeEnum.OPERATION_ERROR);
        }

        Integer status = appointmentPaymentRecord.getPaymentStatus();

        if (status != 0) {
            throw new AppointmentException(ResultCodeEnum.ORDER_STATUS_ILLEGAL);
        }

        Long registrationId = appointmentPaymentRecord.getRegistrationId();

        Registration registration = registrationMapper.selectById(registrationId);

        if(registration == null){
            throw new AppointmentException(ResultCodeEnum.REGISTRATION_RECORD_ERROR);
        }
        Long scheduleId = registration.getScheduleId();
        //挂号数量加一
        try{LambdaUpdateWrapper<Schedule> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Schedule::getId,scheduleId);
            updateWrapper.setSql("current_appointment_count = current_appointment_count - 1");
            scheduleMapper.update(null,updateWrapper);

        }catch (Exception e){
            throw new AppointmentException(ResultCodeEnum.APPOINTMENT_HANDLER_ERROR);
        }
        //清缓存
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        String key = String.join(":",schedule.getSubDepartmentId().toString(),schedule.getDepartmentName(),schedule.getScheduleDate().toString());
        redisCache.delete(key);


        registrationMapper.deleteById(registrationId);

        appointmentPaymentRecordMapper.deleteById(id);


        applicationEventPublisher.publishEvent(new OrderDeletedEvent(appointmentPaymentRecord.getId()));

    }




    private void validateRegistrationParam(Registration r) {
        // 校验挂号信息非空
        Assert.notNull(r, ResultCodeEnum.REGISTRATION_INFO_EMPTY.getMessage());

        // 校验核心ID非空且有效
        Long doctorId = r.getDoctorId();
        Long patientId = r.getPatientId();
        Long scheduleId = r.getScheduleId();
        Assert.notNull(doctorId, ResultCodeEnum.DOCTOR_ID_EMPTY.getMessage());
        Assert.notNull(patientId, ResultCodeEnum.PATIENT_ID_EMPTY.getMessage());
        Assert.notNull(scheduleId, ResultCodeEnum.SCHEDULE_ID_EMPTY.getMessage());
        Assert.isTrue(doctorId > 0, ResultCodeEnum.DOCTOR_ID_INVALID.getMessage());
        Assert.isTrue(patientId > 0, ResultCodeEnum.PATIENT_ID_INVALID.getMessage());
        Assert.isTrue(scheduleId > 0, ResultCodeEnum.SCHEDULE_ID_INVALID.getMessage());

        // 校验当前登录用户（付款人）有效
/*        Assert.notNull(LoginUserHolder.getLoginUser(), ResultCodeEnum.USER_NOT_LOGIN);
        Assert.notNull(LoginUserHolder.getLoginUser().getUserId(), ResultCodeEnum.PAYER_ID_INVALID);*/
    }

  /*  private DoctorDetail validateDoctorExists(Long doctorId) {
        LambdaQueryWrapper<DoctorDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DoctorDetail::getId, doctorId)
                .select(DoctorDetail::getId, DoctorDetail::getPrice); // 只查需要的字段
        DoctorDetail doctorDetail = doctorDetailMapper.selectOne(queryWrapper);
       if (doctorDetail == null) {
            throw new AppointmentException(ResultCodeEnum.DOCTOR_NOT_EXIST + doctorId);
        }
        return doctorDetail;
    }*/


}