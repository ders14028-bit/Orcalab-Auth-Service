package com.orcalab.realtime.chat;

import java.time.LocalDateTime;

public class MensajeResponse {

    private String id;
    private Long salaId;
    private String canalId;
    private Long usuarioId;
    private String contenido;
    private Long marcadorId;
    private LocalDateTime timestamp;

    public MensajeResponse(Mensaje mensaje) {
        this.id = mensaje.getId();
        this.salaId = mensaje.getSalaId();
        this.canalId = mensaje.getCanalId();
        this.usuarioId = mensaje.getUsuarioId();
        this.contenido = mensaje.getContenido();
        this.marcadorId = mensaje.getMarcadorId();
        this.timestamp = mensaje.getTimestamp();
    }

    public String getId() { return id; }
    public Long getSalaId() { return salaId; }
    public String getCanalId() { return canalId; }
    public Long getUsuarioId() { return usuarioId; }
    public String getContenido() { return contenido; }
    public Long getMarcadorId() { return marcadorId; }
    public LocalDateTime getTimestamp() { return timestamp; }
}