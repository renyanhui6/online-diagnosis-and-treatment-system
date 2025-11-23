package cn.edu.ncu.medical.config.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;

@Configuration
public class RedisListenerConfig {
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private OrderExpireListener orderExpireListener;

    /**
     * 配置消息监听容器，订阅过期事件
     */
    @Bean
    public RedisMessageListenerContainer listenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);

        // 订阅"键过期事件"频道（__keyevent@0__:expired）
        // 0 是Redis数据库编号，需与你的数据库一致
        Topic topic = new PatternTopic("__keyevent@0__:expired");

        // 绑定监听器（收到过期事件后交给OrderExpireListener处理）
        container.addMessageListener(orderExpireListener, topic);
        return container;
    }
}
