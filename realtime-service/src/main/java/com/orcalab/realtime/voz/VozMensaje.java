package com.orcalab.realtime.voz;

import java.util.List;

public class VozMensaje {

    private String tipo;
    private Long usuarioId;
    private List<ParticipanteVoz> participantes;

    public VozMensaje() {}

    public static VozMensaje entro(Long usuarioId, List<ParticipanteVoz> participantes) {
        return construir("ENTRO", usuarioId, participantes);
    }

    public static VozMensaje salio(Long usuarioId, List<ParticipanteVoz> participantes) {
        return construir("SALIO", usuarioId, participantes);
    }

    public static VozMensaje silencioCambiado(Long usuarioId, List<ParticipanteVoz> participantes) {
        return construir("SILENCIO_CAMBIADO", usuarioId, participantes);
    }

    private static VozMensaje construir(String tipo, Long usuarioId, List<ParticipanteVoz> participantes) {
        VozMensaje msg = new VozMensaje();
        msg.tipo = tipo;
        msg.usuarioId = usuarioId;
        msg.participantes = participantes;
        return msg;
    }

    public String getTipo() { return tipo; }
    public Long getUsuarioId() { return usuarioId; }
    public List<ParticipanteVoz> getParticipantes() { return participantes; }

    public record ParticipanteVoz(Long usuarioId, boolean muteado) {}
}
