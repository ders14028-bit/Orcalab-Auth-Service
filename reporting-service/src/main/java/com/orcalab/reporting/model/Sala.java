package com.orcalab.reporting.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "salas")
public class Sala {

    @Id
    private String id; // usamos el salaId (Long convertido a String) como _id de Mongo

    private Long salaId;
    private String nombre;
    private Long creadorId;
    private LocalDateTime fechaCreacion;
    private boolean activa = true;

    public Sala() {}

    public Sala(Long salaId, String nombre, Long creadorId, LocalDateTime fechaCreacion) {
        this.id = salaId.toString();
        this.salaId = salaId;
        this.nombre = nombre;
        this.creadorId = creadorId;
        this.fechaCreacion = fechaCreacion;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getSalaId() { return salaId; }
    public void setSalaId(Long salaId) { this.salaId = salaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Long getCreadorId() { return creadorId; }
    public void setCreadorId(Long creadorId) { this.creadorId = creadorId; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}