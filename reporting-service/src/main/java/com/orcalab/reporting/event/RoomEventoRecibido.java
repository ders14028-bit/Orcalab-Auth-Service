package com.orcalab.reporting.event;

public class RoomEventoRecibido {

    private String tipo;
    private Long salaId;
    private String nombreSala;
    private Long usuarioId;
    private String rolEnSala;
    private String timestamp;

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Long getSalaId() { return salaId; }
    public void setSalaId(Long salaId) { this.salaId = salaId; }

    public String getNombreSala() { return nombreSala; }
    public void setNombreSala(String nombreSala) { this.nombreSala = nombreSala; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getRolEnSala() { return rolEnSala; }
    public void setRolEnSala(String rolEnSala) { this.rolEnSala = rolEnSala; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}