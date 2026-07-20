package com.orcalab.realtime.voz;

import com.orcalab.realtime.broadcast.RealtimeBroadcaster;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Controller
public class VozController {

    private final VozService vozService;
    private final VozBroadcastService vozBroadcastService;
    private final RealtimeBroadcaster broadcaster;

    public VozController(VozService vozService, VozBroadcastService vozBroadcastService,
                          RealtimeBroadcaster broadcaster) {
        this.vozService = vozService;
        this.vozBroadcastService = vozBroadcastService;
        this.broadcaster = broadcaster;
    }

    @MessageMapping("/sala/{salaId}/canal/{canalId}/voz/entrar")
    public void entrar(@DestinationVariable Long salaId, @DestinationVariable String canalId,
                        SimpMessageHeaderAccessor headerAccessor) {
        Long usuarioId = extraerUsuarioId(headerAccessor.getUser());
        vozService.entrar(headerAccessor.getSessionId(), salaId, canalId, usuarioId);
        vozBroadcastService.difundirEntro(salaId, canalId, usuarioId);
    }

    @MessageMapping("/sala/{salaId}/canal/{canalId}/voz/salir")
    public void salir(@DestinationVariable Long salaId, @DestinationVariable String canalId,
                       SimpMessageHeaderAccessor headerAccessor) {
        Long usuarioId = extraerUsuarioId(headerAccessor.getUser());
        vozService.salir(headerAccessor.getSessionId());
        vozBroadcastService.difundirSalio(salaId, canalId, usuarioId);
    }

    @MessageMapping("/sala/{salaId}/canal/{canalId}/voz/silenciar")
    public void silenciar(@DestinationVariable Long salaId, @DestinationVariable String canalId,
                           @Payload VozSilenciarRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long usuarioId = extraerUsuarioId(headerAccessor.getUser());
        vozService.silenciar(salaId, canalId, usuarioId, request.isMuteado());
        vozBroadcastService.difundirSilencioCambiado(salaId, canalId, usuarioId);
    }

    @MessageMapping("/sala/{salaId}/canal/{canalId}/voz/oferta")
    public void oferta(@DestinationVariable Long salaId, @DestinationVariable String canalId,
                        @Payload VozSenalRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long usuarioId = extraerUsuarioId(headerAccessor.getUser());
        broadcaster.broadcastToUser(request.getParaUsuarioId().toString(), "/queue/voz/senal",
                VozSenalMensaje.oferta(salaId, canalId, usuarioId, request.getSdp()));
    }

    @MessageMapping("/sala/{salaId}/canal/{canalId}/voz/respuesta")
    public void respuesta(@DestinationVariable Long salaId, @DestinationVariable String canalId,
                           @Payload VozSenalRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long usuarioId = extraerUsuarioId(headerAccessor.getUser());
        broadcaster.broadcastToUser(request.getParaUsuarioId().toString(), "/queue/voz/senal",
                VozSenalMensaje.respuesta(salaId, canalId, usuarioId, request.getSdp()));
    }

    @MessageMapping("/sala/{salaId}/canal/{canalId}/voz/ice")
    public void ice(@DestinationVariable Long salaId, @DestinationVariable String canalId,
                     @Payload VozSenalRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long usuarioId = extraerUsuarioId(headerAccessor.getUser());
        broadcaster.broadcastToUser(request.getParaUsuarioId().toString(), "/queue/voz/senal",
                VozSenalMensaje.ice(salaId, canalId, usuarioId, request.getCandidato()));
    }

    @EventListener
    public void manejarDesconexion(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        VozService.SesionVozInfo info = vozService.salir(headerAccessor.getSessionId());

        if (info != null) {
            vozBroadcastService.difundirSalio(info.salaId(), info.canalId(), info.usuarioId());
        }
    }

    private Long extraerUsuarioId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token) {
            return (Long) token.getPrincipal();
        }
        throw new IllegalStateException("No se pudo determinar el usuario autenticado");
    }
}
