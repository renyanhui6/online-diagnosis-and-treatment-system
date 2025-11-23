package cn.edu.ncu.medical.topic;


import cn.edu.ncu.medical.entity.AppointmentPaymentRecord;
import cn.edu.ncu.medical.entity.OrderPaymentRecord;
import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.entity.dto.Medicine;
import cn.edu.ncu.medical.service.AppointmentPaymentRecordService;
import cn.edu.ncu.medical.service.OrderPaymentRecordService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class RabbitTopicReceiver {
    @Autowired
    private AppointmentPaymentRecordService appointmentPaymentRecordService;
    @Autowired
    private OrderPaymentRecordService orderPaymentRecordService;

    // 监听处方专用队列，参数直接声明为Long（与消息类型匹配）
    @Transactional(rollbackFor = Exception.class)
    @RabbitListener(queues = RabbitTopicConfig.MEDICINE_CREATE_BY_PRESCRIPTION_QUEUE)
    public void receiveByPrescriptionMessage(RabbitMessage<Long> rabbitMessage) {
        Long mId = rabbitMessage.getData();
        Long userId = rabbitMessage.getUserId();
        orderPaymentRecordService.createOrderByPrescription(mId,userId);
    }

    // 监听自行购买专用队列，参数直接声明为List<Medicine>（与消息类型匹配）
    @Transactional(rollbackFor = Exception.class)
    @RabbitListener(queues = RabbitTopicConfig.MEDICINE_CREATE_SELF_QUEUE)
    public void receiveSelfPurchaseMessage(RabbitMessage<List<Medicine>> rabbitMessage) {
        List<Medicine> medicineList = rabbitMessage.getData();
        Long  userId = rabbitMessage.getUserId();
        orderPaymentRecordService.createOrder(medicineList,userId);
    }

    // 监听订单完成专用队列
    @Transactional(rollbackFor = Exception.class)
    @RabbitListener(queues = RabbitTopicConfig.MEDICINE_ORDER_COMPLETE_QUEUE)
    public void receiveMedicineOrderCompleteMessage(RabbitMessage<OrderPaymentRecord> rabbitMessage) {
        OrderPaymentRecord orderPaymentRecord = rabbitMessage.getData();
        Long userId = rabbitMessage.getUserId();
        orderPaymentRecordService.completeOrder(orderPaymentRecord,userId);
    }

    // 监听预约订单创建专用队列
    @Transactional(rollbackFor = Exception.class)
    @RabbitListener(queues = RabbitTopicConfig.APPOINTMENT_CREATE_QUEUE)
    public void receiveAppointmentCreateMessage(RabbitMessage<Registration> rabbitMessage) {
        Registration registration = rabbitMessage.getData();
        Long userId = rabbitMessage.getUserId();
        appointmentPaymentRecordService.createAppointmentPayment(registration,userId);
    }

    // 监听订单完成专用队列
    @Transactional(rollbackFor = Exception.class)
    @RabbitListener(queues = RabbitTopicConfig.APPOINTMENT_FINISH_QUEUE)
    public void receiveAppointmentFinishMessage(RabbitMessage<AppointmentPaymentRecord> rabbitMessage) {
        AppointmentPaymentRecord record = rabbitMessage.getData();
        Long userId = rabbitMessage.getUserId();
        appointmentPaymentRecordService.modifyAppointmentPayment(record,userId);
    }


}
