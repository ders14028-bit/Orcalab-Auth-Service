package com.orcalab.auth.dto;

import com.orcalab.auth.model.Rol;
import jakarta.validation.constraints.NotNull;

public class CambiarRolRequest {

    @NotNull
    private Rol nuevoRol;

    public Rol getNuevoRol() { return nuevoRol; }
    public void setNuevoRol(Rol nuevoRol) { this.nuevoRol = nuevoRol; }
}
