package com.orcalab.realtime.event;

import java.time.LocalDateTime;

public class ChatEvento {

    private String tipo;
    private Long salaId;
    private Long usuarioId;
    private String mensajeId;
    private String contenido;
    private Long marcadorId;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ChatEvento() {}

    public static ChatEvento mensajeEnviado(Long salaId, Long usuarioId, String mensajeId, String contenido, Long marcadorId) {
        ChatEvento evento = new ChatEvento();
        evento.tipo = "MensajeEnviado";
        evento.salaId = salaId;
        evento.usuarioId = usuarioId;
        evento.mensajeId = mensajeId;
        evento.contenido = contenido;
        evento.marcadorId = marcadorId;
        return evento;
    }

    public String getTipo() { return tipo; }
    public Long getSalaId() { return salaId; }
    public Long getUsuarioId() { return usuarioId; }
    public String getMensajeId() { return mensajeId; }
    public String getContenido() { return contenido; }
    public Long getMarcadorId() { return marcadorId; }
    public LocalDateTime getTimestamp() { return timestamp; }
}