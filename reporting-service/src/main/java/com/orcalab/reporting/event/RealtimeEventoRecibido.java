package com.orcalab.reporting.event;

public class RealtimeEventoRecibido {

    private String tipo;
    private Long salaId;
    private Long usuarioId;

    // Campos de eventos de mapa (marcador/ruta/alerta)
    private String elementoId;
    private String tipoMarcador;
    private Double latitud;
    private Double longitud;
    private String descripcion;

    // Campos de eventos de chat
    private String mensajeId;
    private String contenido;
    private Long marcadorId;

    private String timestamp;

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Long getSalaId() { return salaId; }
    public void setSalaId(Long salaId) { this.salaId = salaId; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getElementoId() { return elementoId; }
    public void setElementoId(String elementoId) { this.elementoId = elementoId; }

    public String getTipoMarcador() { return tipoMarcador; }
    public void setTipoMarcador(String tipoMarcador) { this.tipoMarcador = tipoMarcador; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getMensajeId() { return mensajeId; }
    public void setMensajeId(String mensajeId) { this.mensajeId = mensajeId; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public Long getMarcadorId() { return marcadorId; }
    public void setMarcadorId(Long marcadorId) { this.marcadorId = marcadorId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}