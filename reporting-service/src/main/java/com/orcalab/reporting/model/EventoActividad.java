package com.orcalab.reporting.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "eventos_actividad")
public class EventoActividad {

    @Id
    private String id;

    private String tipo; // ej: "SalaCreada", "MarcadorAgregado", "MensajeEnviado"
    private Long salaId;
    private Long usuarioId;
    private LocalDateTime timestamp = LocalDateTime.now();

    public EventoActividad() {}

    public EventoActividad(String tipo, Long salaId, Long usuarioId, LocalDateTime timestamp) {
        this.tipo = tipo;
        this.salaId = salaId;
        this.usuarioId = usuarioId;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Long getSalaId() { return salaId; }
    public void setSalaId(Long salaId) { this.salaId = salaId; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}