package com.orcalab.realtime.config;

import com.orcalab.realtime.broadcast.RealtimeBroadcastSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }

    // Canal Pub/Sub usado por RealtimeBroadcaster/RealtimeBroadcastSubscriber para
    // reemplazar la difusion STOMP en memoria (enableSimpleBroker) por una que
    // llega a todas las instancias de realtime-service, no solo a la que
    // proceso el mensaje original. Independiente del stream "room-events"
    // (app.events.topic) que ya existe: ese es un log durable para eventos
    // entre microservicios, esto es Pub/Sub efimero solo para difundir a
    // clientes WebSocket - no necesita replay ni persistencia.
    @Bean
    public ChannelTopic realtimeBroadcastTopic() {
        return new ChannelTopic("orcalab:realtime:broadcast");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                                         RealtimeBroadcastSubscriber subscriber,
                                                                         ChannelTopic realtimeBroadcastTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, realtimeBroadcastTopic);
        return container;
    }
}