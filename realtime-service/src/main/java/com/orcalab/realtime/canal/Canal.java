package com.orcalab.realtime.canal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "canales")
public class Canal {

    @Id
    private String id;

    private Long salaId;
    private String nombre;
    private TipoCanal tipo;
    private Long creadorId;
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public Canal() {}

    public Canal(Long salaId, String nombre, TipoCanal tipo, Long creadorId) {
        this.salaId = salaId;
        this.nombre = nombre;
        this.tipo = tipo;
        this.creadorId = creadorId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getSalaId() { return salaId; }
    public void setSalaId(Long salaId) { this.salaId = salaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public TipoCanal getTipo() { return tipo; }
    public void setTipo(TipoCanal tipo) { this.tipo = tipo; }

    public Long getCreadorId() { return creadorId; }
    public void setCreadorId(Long creadorId) { this.creadorId = creadorId; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
