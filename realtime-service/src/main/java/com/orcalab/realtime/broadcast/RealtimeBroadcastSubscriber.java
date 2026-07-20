package com.orcalab.realtime.broadcast;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Recibe en TODAS las instancias de realtime-service lo que cualquier
 * instancia publico via RealtimeBroadcaster, y lo entrega al broker STOMP
 * local (enableSimpleBroker) para que llegue a las sesiones WebSocket
 * conectadas a ESTA instancia especificamente. Esta es la mitad "consumidor"
 * del relay Redis Pub/Sub que reemplaza la difusion en memoria.
 */
@Component
public class RealtimeBroadcastSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RealtimeBroadcastSubscriber.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public RealtimeBroadcastSubscriber(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            BroadcastEnvelope envelope = objectMapper.readValue(message.getBody(), BroadcastEnvelope.class);
            if (envelope.getTipo() == BroadcastEnvelope.Tipo.USUARIO) {
                // Si el usuario destino no tiene sesion en ESTA instancia, esto es un no-op
                // silencioso (SimpMessagingTemplate resuelve el destino contra el registro
                // LOCAL de usuarios conectados) - asi es como llega a la instancia correcta
                // sin que ninguna instancia necesite saber donde esta conectado cada usuario.
                messagingTemplate.convertAndSendToUser(envelope.getUsuarioId(), envelope.getDestino(), envelope.getPayload());
            } else {
                messagingTemplate.convertAndSend(envelope.getDestino(), envelope.getPayload());
            }
        } catch (Exception e) {
            log.error("Error al procesar mensaje de broadcast Redis", e);
        }
    }
}
