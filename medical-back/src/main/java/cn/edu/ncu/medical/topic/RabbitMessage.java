package cn.edu.ncu.medical.topic;

import lombok.Data;

@Data
public class RabbitMessage<T> {
    // 业务数据（原始消息内容）
    private T data;
    // 发送消息的用户ID（从ThreadLocal获取）
    private Long userId;
}
