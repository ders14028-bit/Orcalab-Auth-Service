package com.orcalab.realtime.broadcast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

/**
 * Reemplazo drop-in de SimpMessagingTemplate.convertAndSend/convertAndSendToUser
 * para todo lo que deba llegar a clientes conectados a CUALQUIER instancia de
 * realtime-service, no solo a la que proceso el evento. En vez de escribir
 * directo al broker STOMP en memoria (enableSimpleBroker, que solo ve las
 * sesiones de esta instancia), publica en un canal Redis Pub/Sub que todas
 * las instancias escuchan (ver RealtimeBroadcastSubscriber) y que a su vez
 * reenvian a su propio broker local.
 *
 * Ver RedisConfig para el bean del canal y el RedisMessageListenerContainer.
 */
@Component
public class RealtimeBroadcaster {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChannelTopic topic;

    public RealtimeBroadcaster(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper,
                                ChannelTopic realtimeBroadcastTopic) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.topic = realtimeBroadcastTopic;
    }

    /** Equivalente a messagingTemplate.convertAndSend(destino, payload), pero entre instancias. */
    public void broadcast(String destino, Object payload) {
        publicar(new BroadcastEnvelope(BroadcastEnvelope.Tipo.TOPIC, destino, null, objectMapper.valueToTree(payload)));
    }

    /** Equivalente a messagingTemplate.convertAndSendToUser(usuarioId, destino, payload), pero entre instancias. */
    public void broadcastToUser(String usuarioId, String destino, Object payload) {
        publicar(new BroadcastEnvelope(BroadcastEnvelope.Tipo.USUARIO, destino, usuarioId, objectMapper.valueToTree(payload)));
    }

    private void publicar(BroadcastEnvelope envelope) {
        try {
            String json = objectMapper.writeValueAsString(envelope);
            redisTemplate.convertAndSend(topic.getTopic(), json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el mensaje de broadcast", e);
        }
    }
}
