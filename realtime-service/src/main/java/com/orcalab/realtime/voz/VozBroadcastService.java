package com.orcalab.realtime.voz;

import com.orcalab.realtime.broadcast.RealtimeBroadcaster;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.BiFunction;

@Service
public class VozBroadcastService {

    private final VozService vozService;
    private final RealtimeBroadcaster broadcaster;

    public VozBroadcastService(VozService vozService, RealtimeBroadcaster broadcaster) {
        this.vozService = vozService;
        this.broadcaster = broadcaster;
    }

    public List<VozMensaje.ParticipanteVoz> construirListaParticipantes(Long salaId, String canalId) {
        return vozService.obtenerParticipantes(salaId, canalId).entrySet().stream()
                .map(e -> new VozMensaje.ParticipanteVoz(e.getKey(), e.getValue()))
                .toList();
    }

    private void difundir(Long salaId, String canalId, Long usuarioId,
                           BiFunction<Long, List<VozMensaje.ParticipanteVoz>, VozMensaje> constructor) {
        var participantes = construirListaParticipantes(salaId, canalId);
        broadcaster.broadcast(
                "/topic/sala/" + salaId + "/canal/" + canalId + "/voz/presentes",
                constructor.apply(usuarioId, participantes));
    }

    public void difundirEntro(Long salaId, String canalId, Long usuarioId) {
        difundir(salaId, canalId, usuarioId, VozMensaje::entro);
    }

    public void difundirSalio(Long salaId, String canalId, Long usuarioId) {
        difundir(salaId, canalId, usuarioId, VozMensaje::salio);
    }

    public void difundirSilencioCambiado(Long salaId, String canalId, Long usuarioId) {
        difundir(salaId, canalId, usuarioId, VozMensaje::silencioCambiado);
    }
}
