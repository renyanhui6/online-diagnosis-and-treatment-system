package cn.edu.ncu.medical.topic;

import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopicSender {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send(Object message, String routingKey) {
        // 1. 校验路由键
        if (routingKey == null || routingKey.trim().isEmpty()) {
            throw new MyRuntimeException("routingKey 不能为空");
        }

        // 2. 获取当前登录用户ID（从ThreadLocal中提取，确保在同步请求线程中执行）
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        if (userId == null) {
            throw new MyRuntimeException(ResultCodeEnum.ADMIN_LOGIN_AUTH);
        }

        // 3. 封装统一消息载体
        RabbitMessage<Object> rabbitMessage = new RabbitMessage<>();
        rabbitMessage.setData(message); // 原始业务数据
        rabbitMessage.setUserId(userId); // 自动填充真实用户ID

        // 4. 发送封装后的消息
        rabbitTemplate.convertAndSend(RabbitTopicConfig.ORDER_TOPIC_EXCHANGE, routingKey, rabbitMessage);
    }
    /**
     * 发送“药品创建-通过处方”消息
     */
    public void sendMedicineCreateByPrescriptionMsg(Object message) {
        send(message, RabbitTopicConfig.ROUTING_KEY_MEDICINE_CREATE_BY_PRESCRIPTION);
    }

    /**
     * 发送“药品创建-自行购买”消息
     */
    public void sendMedicineCreateSelfPurchaseMsg(Object message) {
        send(message, RabbitTopicConfig.ROUTING_KEY_MEDICINE_CREATE_SELF);
    }

    /**
     * 发送药品支付信息
     * @param message
     */
    public void sendMedicineOrderCompleteMsg(Object message) {
        send(message, RabbitTopicConfig.ROUTING_KEY_MEDICINE_ORDER_COMPLETE);
    }

    /**
     * 发送“预约订单创建”消息
     */
    public void sendAppointmentCreateMsg(Object message) {
        send(message, RabbitTopicConfig.ROUTING_KEY_APPOINTMENT_CREATE);
    }

    /**
     * 发送“订单完成”消息
     */
    public void sendAppointmentFinishMsg(Object message) {
        send(message, RabbitTopicConfig.ROUTING_KEY_APPOINTMENT_FINISH);
    }

}
