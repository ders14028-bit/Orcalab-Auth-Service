package com.orcalab.realtime.voz;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.BiFunction;

@Service
public class VozBroadcastService {

    private final VozService vozService;
    private final SimpMessagingTemplate messagingTemplate;

    public VozBroadcastService(VozService vozService, SimpMessagingTemplate messagingTemplate) {
        this.vozService = vozService;
        this.messagingTemplate = messagingTemplate;
    }

    public List<VozMensaje.ParticipanteVoz> construirListaParticipantes(Long salaId, String canalId) {
        return vozService.obtenerParticipantes(salaId, canalId).entrySet().stream()
                .map(e -> new VozMensaje.ParticipanteVoz(e.getKey(), e.getValue()))
                .toList();
    }

    private void difundir(Long salaId, String canalId, Long usuarioId,
                           BiFunction<Long, List<VozMensaje.ParticipanteVoz>, VozMensaje> constructor) {
        var participantes = construirListaParticipantes(salaId, canalId);
        messagingTemplate.convertAndSend(
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
