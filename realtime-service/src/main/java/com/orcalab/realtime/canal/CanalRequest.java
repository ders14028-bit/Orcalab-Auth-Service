package com.orcalab.realtime.canal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CanalRequest {

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotNull
    private TipoCanal tipo;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public TipoCanal getTipo() { return tipo; }
    public void setTipo(TipoCanal tipo) { this.tipo = tipo; }
}
