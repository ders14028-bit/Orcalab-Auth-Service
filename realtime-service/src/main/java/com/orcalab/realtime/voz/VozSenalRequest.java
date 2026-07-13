package com.orcalab.realtime.voz;

public class VozSenalRequest {

    private Long paraUsuarioId;
    private String sdp;
    private Object candidato;

    public Long getParaUsuarioId() { return paraUsuarioId; }
    public void setParaUsuarioId(Long paraUsuarioId) { this.paraUsuarioId = paraUsuarioId; }
    public String getSdp() { return sdp; }
    public void setSdp(String sdp) { this.sdp = sdp; }
    public Object getCandidato() { return candidato; }
    public void setCandidato(Object candidato) { this.candidato = candidato; }
}
