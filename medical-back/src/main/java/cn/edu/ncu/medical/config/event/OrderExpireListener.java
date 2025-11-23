package cn.edu.ncu.medical.config.event;

import cn.edu.ncu.medical.service.AppointmentPaymentRecordService;
import cn.edu.ncu.medical.service.OrderPaymentRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static cn.edu.ncu.medical.config.event.DelayQueueProducer.ORDERME_MEDICINE_PREFIX;
import static cn.edu.ncu.medical.config.event.DelayQueueProducer.ORDER_REGISTER_PREFIX;

@Component
public class OrderExpireListener implements MessageListener {

    @Autowired
    private AppointmentPaymentRecordService appointmentPaymentRecordService;

    @Autowired
    private OrderPaymentRecordService orderPaymentRecordService;

    // 订单过期键的前缀（与生产者一致）


    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 1. 解析过期的键名（message.getBody()是键名字节数组）
        String expiredKey = new String(message.getBody(), StandardCharsets.UTF_8);
        // 示例：expiredKey = "order:expire:1001"

        // 2. 校验键名是否符合订单过期键的格式
        if (expiredKey.startsWith(ORDER_REGISTER_PREFIX)) {
            // 3. 提取订单ID（截取前缀后的部分）
            String orderIdStr = expiredKey.substring(ORDER_REGISTER_PREFIX.length());
            try {
                Long orderId = Long.parseLong(orderIdStr);

                // 4. 执行订单取消逻辑（与之前的业务方法一致）
                appointmentPaymentRecordService.cancelAppointmentPaymentRecordByIdAdmin(orderId);
            } catch (NumberFormatException e) {
                // 键名格式错误，忽略
                e.printStackTrace();
                System.out.println("键名格式错误");
            }
        }

        //药品处理
        if (expiredKey.startsWith(ORDERME_MEDICINE_PREFIX)) {
            // 3. 提取订单ID（截取前缀后的部分）
            String orderIdStr = expiredKey.substring(ORDERME_MEDICINE_PREFIX.length());
            try {
                Long orderId = Long.parseLong(orderIdStr);

                // 4. 执行订单取消逻辑（与之前的业务方法一致）
                orderPaymentRecordService.cancelOrderByAdmin(orderId);
            } catch (NumberFormatException e) {
                // 键名格式错误，忽略
                e.printStackTrace();
                System.out.println("键名格式错误");
            }
        }

    }
}

