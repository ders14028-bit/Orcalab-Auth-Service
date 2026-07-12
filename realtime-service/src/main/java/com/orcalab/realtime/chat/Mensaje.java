package com.orcalab.realtime.chat;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "mensajes")
public class Mensaje {

    @Id
    private String id;

    private Long salaId;
    private String canalId;
    private Long usuarioId;
    private String contenido;
    private Long marcadorId; // opcional: id del marcador del mapa al que está vinculado (null si no aplica)
    private LocalDateTime timestamp = LocalDateTime.now();

    public Mensaje() {}

    public Mensaje(Long salaId, String canalId, Long usuarioId, String contenido, Long marcadorId) {
        this.salaId = salaId;
        this.canalId = canalId;
        this.usuarioId = usuarioId;
        this.contenido = contenido;
        this.marcadorId = marcadorId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getSalaId() { return salaId; }
    public void setSalaId(Long salaId) { this.salaId = salaId; }

    public String getCanalId() { return canalId; }
    public void setCanalId(String canalId) { this.canalId = canalId; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public Long getMarcadorId() { return marcadorId; }
    public void setMarcadorId(Long marcadorId) { this.marcadorId = marcadorId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}