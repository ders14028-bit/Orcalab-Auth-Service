package com.orcalab.realtime.broadcast;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Envoltorio publicado en el canal Redis Pub/Sub que reemplaza al broker STOMP
 * en memoria (enableSimpleBroker) como mecanismo de difusión entre instancias.
 * Cada instancia de realtime-service escucha este canal y reenvia el contenido
 * a su broker local (SimpMessagingTemplate), que a su vez lo entrega solo a
 * las sesiones WebSocket conectadas a ESA instancia.
 */
public class BroadcastEnvelope {

    public enum Tipo { TOPIC, USUARIO }

    private final Tipo tipo;
    private final String destino;
    private final String usuarioId; // solo relevante si tipo == USUARIO
    private final JsonNode payload;

    @JsonCreator
    public BroadcastEnvelope(
            @JsonProperty("tipo") Tipo tipo,
            @JsonProperty("destino") String destino,
            @JsonProperty("usuarioId") String usuarioId,
            @JsonProperty("payload") JsonNode payload) {
        this.tipo = tipo;
        this.destino = destino;
        this.usuarioId = usuarioId;
        this.payload = payload;
    }

    public Tipo getTipo() { return tipo; }
    public String getDestino() { return destino; }
    public String getUsuarioId() { return usuarioId; }
    public JsonNode getPayload() { return payload; }
}
