package com.tracktrove.config;

import com.tracktrove.redis.KeyExpiredListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.*;

@Configuration
public class RedisConfig {

    @Bean
    public RedisMessageListenerContainer redisListenerContainer(
        RedisConnectionFactory connectionFactory,
        KeyExpiredListener keyExpiredListener
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
            keyExpiredListener,
            new PatternTopic("__keyevent@0__:expired")
        );
        return container;
    }
}
