package com.orcalab.auth.dto;

import com.orcalab.auth.model.Rol;
import com.orcalab.auth.model.Usuario;

public class UsuarioResponse {

    private Long id;
    private String email;
    private String nombre;
    private Rol rol;

    public UsuarioResponse(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.nombre = usuario.getNombre();
        this.rol = usuario.getRol();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getNombre() { return nombre; }
    public Rol getRol() { return rol; }
}
