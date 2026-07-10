package com.orcalab.auth.dto;

import com.orcalab.auth.model.Usuario;

// Vista mínima expuesta a cualquier usuario autenticado (no solo administradores),
// para resolver usuarioId -> nombre en presencia/chat/marcadores sin filtrar email ni rol.
public class UsuarioResumenResponse {

    private Long id;
    private String nombre;

    public UsuarioResumenResponse(Usuario usuario) {
        this.id = usuario.getId();
        this.nombre = usuario.getNombre();
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
}
