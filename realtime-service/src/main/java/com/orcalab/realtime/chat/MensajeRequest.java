package com.orcalab.realtime.chat;

public class MensajeRequest {

    private String contenido;
    private Long marcadorId; // opcional

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public Long getMarcadorId() { return marcadorId; }
    public void setMarcadorId(Long marcadorId) { this.marcadorId = marcadorId; }
}