package cn.edu.ncu.medical.topic;


import cn.edu.ncu.medical.entity.AppointmentPaymentRecord;
import cn.edu.ncu.medical.entity.OrderPaymentRecord;
import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.entity.dto.Medicine;
import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.service.AppointmentPaymentRecordService;
import cn.edu.ncu.medical.service.OrderPaymentRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.rabbit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitTopicReceiver {
    @Autowired
    private AppointmentPaymentRecordService appointmentPaymentRecordService;
    @Autowired
    private OrderPaymentRecordService orderPaymentRecordService;

    // 监听处方专用队列，参数直接声明为Long（与消息类型匹配）
    @RabbitListener(queues = RabbitTopicConfig.MEDICINE_CREATE_BY_PRESCRIPTION_QUEUE)
    public void receiveByPrescriptionMessage(RabbitMessage<Long> rabbitMessage) {
        Long mId = null;
        Long userId = null;
        try {
            mId = rabbitMessage == null ? null : rabbitMessage.getData();
            userId = rabbitMessage == null ? null : rabbitMessage.getUserId();
            orderPaymentRecordService.createOrderByPrescription(mId, userId);
        } catch (MyRuntimeException e) {
            log.warn("Rabbit 业务异常（不重试）：medicine.create.byPrescription userId={}, prescriptionId={}, code={}, msg={}",
                    userId, mId, e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Rabbit 消费失败（不重试）：medicine.create.byPrescription userId={}, prescriptionId={}", userId, mId, e);
            throw new AmqpRejectAndDontRequeueException("Rabbit listener failed: medicine.create.byPrescription", e);
        }
    }

    // 监听自行购买专用队列，参数直接声明为List<Medicine>（与消息类型匹配）
    @RabbitListener(queues = RabbitTopicConfig.MEDICINE_CREATE_SELF_QUEUE)
    public void receiveSelfPurchaseMessage(RabbitMessage<List<Medicine>> rabbitMessage) {
        List<Medicine> medicineList = null;
        Long userId = null;
        try {
            medicineList = rabbitMessage == null ? null : rabbitMessage.getData();
            userId = rabbitMessage == null ? null : rabbitMessage.getUserId();
            orderPaymentRecordService.createOrder(medicineList, userId);
        } catch (MyRuntimeException e) {
            int size = medicineList == null ? 0 : medicineList.size();
            log.warn("Rabbit 业务异常（不重试）：medicine.create.selfPurchase userId={}, items={}, code={}, msg={}",
                    userId, size, e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Rabbit 消费失败（不重试）：medicine.create.selfPurchase userId={}", userId, e);
            throw new AmqpRejectAndDontRequeueException("Rabbit listener failed: medicine.create.selfPurchase", e);
        }
    }

    // 监听订单完成专用队列
    @RabbitListener(queues = RabbitTopicConfig.MEDICINE_ORDER_COMPLETE_QUEUE)
    public void receiveMedicineOrderCompleteMessage(RabbitMessage<OrderPaymentRecord> rabbitMessage) {
        OrderPaymentRecord orderPaymentRecord = null;
        Long userId = null;
        try {
            orderPaymentRecord = rabbitMessage == null ? null : rabbitMessage.getData();
            userId = rabbitMessage == null ? null : rabbitMessage.getUserId();
            orderPaymentRecordService.completeOrder(orderPaymentRecord, userId);
        } catch (MyRuntimeException e) {
            Long orderId = orderPaymentRecord == null ? null : orderPaymentRecord.getId();
            log.warn("Rabbit 业务异常（不重试）：medicine.order.complete userId={}, orderId={}, code={}, msg={}",
                    userId, orderId, e.getCode(), e.getMessage());
        } catch (Exception e) {
            Long orderId = orderPaymentRecord == null ? null : orderPaymentRecord.getId();
            log.error("Rabbit 消费失败（不重试）：medicine.order.complete userId={}, orderId={}", userId, orderId, e);
            throw new AmqpRejectAndDontRequeueException("Rabbit listener failed: medicine.order.complete", e);
        }
    }

    // 监听预约订单创建专用队列
    @RabbitListener(queues = RabbitTopicConfig.APPOINTMENT_CREATE_QUEUE)
    public void receiveAppointmentCreateMessage(RabbitMessage<Registration> rabbitMessage) {
        Registration registration = null;
        Long userId = null;
        try {
            registration = rabbitMessage == null ? null : rabbitMessage.getData();
            userId = rabbitMessage == null ? null : rabbitMessage.getUserId();
            appointmentPaymentRecordService.createAppointmentPayment(registration, userId);
        } catch (MyRuntimeException e) {
            Long scheduleId = registration == null ? null : registration.getScheduleId();
            Long doctorId = registration == null ? null : registration.getDoctorId();
            Long patientId = registration == null ? null : registration.getPatientId();
            log.warn("Rabbit 业务异常（不重试）：appointment.create userId={}, scheduleId={}, doctorId={}, patientId={}, code={}, msg={}",
                    userId, scheduleId, doctorId, patientId, e.getCode(), e.getMessage());
        } catch (Exception e) {
            Long scheduleId = registration == null ? null : registration.getScheduleId();
            log.error("Rabbit 消费失败（不重试）：appointment.create userId={}, scheduleId={}", userId, scheduleId, e);
            throw new AmqpRejectAndDontRequeueException("Rabbit listener failed: appointment.create", e);
        }
    }

    // 监听订单完成专用队列
    @RabbitListener(queues = RabbitTopicConfig.APPOINTMENT_FINISH_QUEUE)
    public void receiveAppointmentFinishMessage(RabbitMessage<AppointmentPaymentRecord> rabbitMessage) {
        AppointmentPaymentRecord record = null;
        Long userId = null;
        try {
            record = rabbitMessage == null ? null : rabbitMessage.getData();
            userId = rabbitMessage == null ? null : rabbitMessage.getUserId();
            appointmentPaymentRecordService.modifyAppointmentPayment(record, userId);
        } catch (MyRuntimeException e) {
            Long orderId = record == null ? null : record.getId();
            log.warn("Rabbit 业务异常（不重试）：appointment.payment.finish userId={}, orderId={}, code={}, msg={}",
                    userId, orderId, e.getCode(), e.getMessage());
        } catch (Exception e) {
            Long orderId = record == null ? null : record.getId();
            log.error("Rabbit 消费失败（不重试）：appointment.payment.finish userId={}, orderId={}", userId, orderId, e);
            throw new AmqpRejectAndDontRequeueException("Rabbit listener failed: appointment.payment.finish", e);
        }
    }


}
