package com.orcalab.realtime.voz;

public class VozSenalMensaje {

    private String tipo;
    private Long salaId;
    private String canalId;
    private Long deUsuarioId;
    private String sdp;
    private Object candidato;

    public VozSenalMensaje() {}

    public static VozSenalMensaje oferta(Long salaId, String canalId, Long deUsuarioId, String sdp) {
        return construir("OFERTA", salaId, canalId, deUsuarioId, sdp, null);
    }

    public static VozSenalMensaje respuesta(Long salaId, String canalId, Long deUsuarioId, String sdp) {
        return construir("RESPUESTA", salaId, canalId, deUsuarioId, sdp, null);
    }

    public static VozSenalMensaje ice(Long salaId, String canalId, Long deUsuarioId, Object candidato) {
        return construir("ICE", salaId, canalId, deUsuarioId, null, candidato);
    }

    private static VozSenalMensaje construir(String tipo, Long salaId, String canalId, Long deUsuarioId,
                                              String sdp, Object candidato) {
        VozSenalMensaje msg = new VozSenalMensaje();
        msg.tipo = tipo;
        msg.salaId = salaId;
        msg.canalId = canalId;
        msg.deUsuarioId = deUsuarioId;
        msg.sdp = sdp;
        msg.candidato = candidato;
        return msg;
    }

    public String getTipo() { return tipo; }
    public Long getSalaId() { return salaId; }
    public String getCanalId() { return canalId; }
    public Long getDeUsuarioId() { return deUsuarioId; }
    public String getSdp() { return sdp; }
    public Object getCandidato() { return candidato; }
}
