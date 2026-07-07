package com.orcalab.reporting.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "observaciones")
public class Observacion {

    @Id
    private String id; // el mismo id del marcador original en realtime-service

    private Long salaId;
    private Long usuarioId;
    private String tipo; // AVISTAMIENTO, CRITICO, ZONA_INTERES, etc.
    private double latitud;
    private double longitud;
    private String descripcion;
    private boolean tieneAlerta = false;
    private LocalDateTime fechaCreacion;

    public Observacion() {}

    public Observacion(String id, Long salaId, Long usuarioId, String tipo, double latitud,
                        double longitud, String descripcion, LocalDateTime fechaCreacion) {
        this.id = id;
        this.salaId = salaId;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.latitud = latitud;
        this.longitud = longitud;
        this.descripcion = descripcion;
        this.fechaCreacion = fechaCreacion;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getSalaId() { return salaId; }
    public void setSalaId(Long salaId) { this.salaId = salaId; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public boolean isTieneAlerta() { return tieneAlerta; }
    public void setTieneAlerta(boolean tieneAlerta) { this.tieneAlerta = tieneAlerta; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}