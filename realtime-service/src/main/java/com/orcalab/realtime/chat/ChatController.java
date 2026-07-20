package com.orcalab.realtime.chat;

import com.orcalab.realtime.broadcast.RealtimeBroadcaster;
import com.orcalab.realtime.event.ChatEvento;
import com.orcalab.realtime.event.EventPublisher;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatController {

    private final MensajeRepository mensajeRepository;
    private final RealtimeBroadcaster broadcaster;
    private final EventPublisher eventPublisher;

    public ChatController(MensajeRepository mensajeRepository, RealtimeBroadcaster broadcaster,
                           EventPublisher eventPublisher) {
        this.mensajeRepository = mensajeRepository;
        this.broadcaster = broadcaster;
        this.eventPublisher = eventPublisher;
    }

    @MessageMapping("/sala/{salaId}/canal/{canalId}/mensaje")
    public void enviarMensaje(@DestinationVariable Long salaId, @DestinationVariable String canalId,
                               @Payload MensajeRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long usuarioId = extraerUsuarioId(headerAccessor.getUser());

        Mensaje mensaje = new Mensaje(salaId, canalId, usuarioId, request.getContenido(), request.getMarcadorId());
        mensaje = mensajeRepository.save(mensaje);

        MensajeResponse response = new MensajeResponse(mensaje);
        broadcaster.broadcast("/topic/sala/" + salaId + "/canal/" + canalId + "/chat", response);

        eventPublisher.publicar(ChatEvento.mensajeEnviado(salaId, canalId, usuarioId, mensaje.getId(), mensaje.getContenido(), request.getMarcadorId()));
    }

    private Long extraerUsuarioId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token) {
            return (Long) token.getPrincipal();
        }
        throw new IllegalStateException("No se pudo determinar el usuario autenticado");
    }
}