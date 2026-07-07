package com.orcalab.realtime.presence;

import java.util.List;

public class PresenciaMensaje {

    private String tipo; // "ENTRADA" o "SALIDA" o "LISTADO"
    private Long usuarioId;
    private List<UsuarioPresente> presentes;

    public PresenciaMensaje() {}

    public static PresenciaMensaje entrada(Long usuarioId, List<UsuarioPresente> presentes) {
        PresenciaMensaje msg = new PresenciaMensaje();
        msg.tipo = "ENTRADA";
        msg.usuarioId = usuarioId;
        msg.presentes = presentes;
        return msg;
    }

    public static PresenciaMensaje salida(Long usuarioId, List<UsuarioPresente> presentes) {
        PresenciaMensaje msg = new PresenciaMensaje();
        msg.tipo = "SALIDA";
        msg.usuarioId = usuarioId;
        msg.presentes = presentes;
        return msg;
    }

    public String getTipo() { return tipo; }
    public Long getUsuarioId() { return usuarioId; }
    public List<UsuarioPresente> getPresentes() { return presentes; }

    public record UsuarioPresente(Long usuarioId, String rolEnSala) {}
}