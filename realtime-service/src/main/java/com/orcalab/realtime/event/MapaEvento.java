package com.orcalab.realtime.event;

import java.time.LocalDateTime;

public class MapaEvento {

    private String tipo;
    private Long salaId;
    private Long usuarioId;
    private String elementoId;
    private String tipoMarcador; // ej: AVISTAMIENTO, CRITICO, ZONA_INTERES (null para eventos de ruta)
    private Double latitud;
    private Double longitud;
    private String descripcion;
    private LocalDateTime timestamp = LocalDateTime.now();

    public MapaEvento() {}

    public static MapaEvento marcador(String tipo, Long salaId, Long usuarioId, String marcadorId,
                                       String tipoMarcador, double latitud, double longitud, String descripcion) {
        MapaEvento evento = new MapaEvento();
        evento.tipo = tipo;
        evento.salaId = salaId;
        evento.usuarioId = usuarioId;
        evento.elementoId = marcadorId;
        evento.tipoMarcador = tipoMarcador;
        evento.latitud = latitud;
        evento.longitud = longitud;
        evento.descripcion = descripcion;
        return evento;
    }

    public static MapaEvento ruta(Long salaId, Long usuarioId, String rutaId, String descripcion) {
        MapaEvento evento = new MapaEvento();
        evento.tipo = "RutaTrazada";
        evento.salaId = salaId;
        evento.usuarioId = usuarioId;
        evento.elementoId = rutaId;
        evento.descripcion = descripcion;
        return evento;
    }

    public String getTipo() { return tipo; }
    public Long getSalaId() { return salaId; }
    public Long getUsuarioId() { return usuarioId; }
    public String getElementoId() { return elementoId; }
    public String getTipoMarcador() { return tipoMarcador; }
    public Double getLatitud() { return latitud; }
    public Double getLongitud() { return longitud; }
    public String getDescripcion() { return descripcion; }
    public LocalDateTime getTimestamp() { return timestamp; }
}