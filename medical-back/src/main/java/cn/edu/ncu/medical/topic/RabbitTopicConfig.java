package cn.edu.ncu.medical.topic;

import com.rabbitmq.client.AMQP;
import org.springframework.amqp.core.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.rabbit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitTopicConfig {
    public static final String ORDER_TOPIC_EXCHANGE = "order.topic.exchange";

    // 拆分后的专用队列（两个专用队列）
    // 1. 处方创建专用队列
    public static final String MEDICINE_CREATE_BY_PRESCRIPTION_QUEUE = "order.medicine.create.byPrescription.queue";
    // 2. 自行购买专用队列
    public static final String MEDICINE_CREATE_SELF_QUEUE = "order.medicine.create.selfPurchase.queue";
    // 3. 订单完成专用队列
    public static final String MEDICINE_ORDER_COMPLETE_QUEUE = "order.medicine.complete.queue";

    //挂号处理队列
    // 预约订单创建专用队列
    public static final String APPOINTMENT_CREATE_QUEUE = "order.appointment.create.queue";

    // 订单完成专用队列
    public static final String APPOINTMENT_FINISH_QUEUE = "appointment.payment.finish.queue";

    // 订单完成路由键
    public static final String ROUTING_KEY_APPOINTMENT_FINISH = "appointment.payment.finish";

    // 预约订单创建路由键
    public static final String ROUTING_KEY_APPOINTMENT_CREATE = "appointment.create";


    // routing key（与业务类型一一对应）
    public static final String ROUTING_KEY_MEDICINE_CREATE_BY_PRESCRIPTION = "medicine.create.byPrescription"; // 通过处方
    public static final String ROUTING_KEY_MEDICINE_CREATE_SELF = "medicine.create.selfPurchase"; // 自行购买

    // 订单完成路由键
    public static final String ROUTING_KEY_MEDICINE_ORDER_COMPLETE = "medicine.order.complete";


    // 声明交换机
    @Bean
    public TopicExchange orderTopicExchange() {
        return new TopicExchange(ORDER_TOPIC_EXCHANGE);
    }

    // 声明处方创建专用队列（对应处方类型消息）
    @Bean
    public Queue medicineCreateByPrescriptionQueue() {
        // 持久化策略（durable=true）
        return QueueBuilder.durable(MEDICINE_CREATE_BY_PRESCRIPTION_QUEUE).build();
    }

    // 声明自行购买专用队列（对应药品列表类型消息）
    @Bean
    public Queue medicineCreateSelfQueue() {
        return QueueBuilder.durable(MEDICINE_CREATE_SELF_QUEUE).build();
    }


    // 声明订单完成专用队列
    @Bean
    public Queue medicineOrderCompleteQueue() {
        return QueueBuilder.durable(MEDICINE_ORDER_COMPLETE_QUEUE).build();
    }

    // 绑定：处方队列 <-> 交换机（精确匹配处方路由键）
    @Bean
    public Binding bindMedicineCreateByPrescriptionQueue(
            TopicExchange orderTopicExchange,
            Queue medicineCreateByPrescriptionQueue) {
        // 使用精确路由键绑定，确保处方消息进入该队列
        return BindingBuilder.bind(medicineCreateByPrescriptionQueue)
                .to(orderTopicExchange)
                .with(ROUTING_KEY_MEDICINE_CREATE_BY_PRESCRIPTION);
    }

    // 绑定：自行购买队列 <-> 交换机（精确匹配自行购买路由键）
    @Bean
    public Binding bindMedicineCreateSelfQueue(
            TopicExchange orderTopicExchange,
            Queue medicineCreateSelfQueue) {
        return BindingBuilder.bind(medicineCreateSelfQueue)
                .to(orderTopicExchange)
                .with(ROUTING_KEY_MEDICINE_CREATE_SELF);
    }


    // 绑定：订单完成队列 <-> 交换机
    @Bean
    public Binding bindMedicineOrderCompleteQueue(
            TopicExchange orderTopicExchange,
            Queue medicineOrderCompleteQueue) {
        return BindingBuilder.bind(medicineOrderCompleteQueue)
                .to(orderTopicExchange)
                .with(ROUTING_KEY_MEDICINE_ORDER_COMPLETE);
    }

    // 声明预约订单创建专用队列
    @Bean
    public Queue appointmentCreateQueue() {
        return QueueBuilder.durable(APPOINTMENT_CREATE_QUEUE).build();
    }

    // 绑定：预约订单队列 <-> 交换机
    @Bean
    public Binding bindAppointmentCreateQueue(
            TopicExchange orderTopicExchange,
            Queue appointmentCreateQueue) {
        return BindingBuilder.bind(appointmentCreateQueue)
                .to(orderTopicExchange)
                .with(ROUTING_KEY_APPOINTMENT_CREATE);
    }



    // 声明订单完成队列
    @Bean
    public Queue appointmentFinishQueue() {
        return QueueBuilder.durable(APPOINTMENT_FINISH_QUEUE).build();
    }

    // 绑定订单完成队列到交换机
    @Bean
    public Binding bindAppointmentFinishQueue(
            TopicExchange orderTopicExchange,
            Queue appointmentFinishQueue) {
        return BindingBuilder.bind(appointmentFinishQueue)
                .to(orderTopicExchange)
                .with(ROUTING_KEY_APPOINTMENT_FINISH);
    }



}
