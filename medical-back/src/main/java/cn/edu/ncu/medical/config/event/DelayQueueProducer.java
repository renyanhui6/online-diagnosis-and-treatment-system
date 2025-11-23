package cn.edu.ncu.medical.config.event;

import cn.edu.ncu.medical.service.AppointmentPaymentRecordService;
import cn.edu.ncu.medical.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;
/**

 延迟队列生产者：负责将订单 ID 添加到 Redis ZSet，或从 ZSet 中移除
 */
@Component
public class DelayQueueProducer {
    @Autowired
    private RedisCache redisCache;

    // 订单过期键的前缀（用于区分其他键）
    public static final String ORDER_REGISTER_PREFIX = "order:register:";
    public static final String ORDERME_MEDICINE_PREFIX = "order:medicine:";
    /**
     * 创建订单时，设置过期键（15分钟后过期，触发通知）
     */
    public void addRegisterOrder(Long orderId) {
        // 键名：order:expire:1001（包含订单ID，便于解析）
        String key = ORDER_REGISTER_PREFIX + orderId;
        // 存入任意值（值不重要，关键是键名和过期时间）
        redisCache.setString(key, "expire");
        // 设置2分钟过期（到期后Redis会发送过期事件）
        redisCache.setExpire(key, 1, TimeUnit.MINUTES);
    }

    /**
     * 订单支付成功后，删除过期键（避免触发通知）
     */
    public void removeRegisterOrder(Long orderId) {
        String key = ORDER_REGISTER_PREFIX + orderId;
        redisCache.delete(key);
    }


    //药品处理
    public void addMedicineOrder(Long orderId) {
        // 键名：order:expire:1001（包含订单ID，便于解析）
        String key = ORDERME_MEDICINE_PREFIX + orderId;
        // 存入任意值（值不重要，关键是键名和过期时间）
        redisCache.setString(key, "expire");
        // 设置2分钟过期（到期后Redis会发送过期事件）
        redisCache.setExpire(key, 1, TimeUnit.MINUTES);
    }

    public void removeMedicineOrder(Long orderId) {
        String key = ORDERME_MEDICINE_PREFIX + orderId;
        redisCache.delete(key);
    }

}