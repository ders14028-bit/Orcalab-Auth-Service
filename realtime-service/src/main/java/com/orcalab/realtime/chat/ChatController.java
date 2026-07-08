package com.orcalab.realtime.chat;

import com.orcalab.realtime.event.ChatEvento;
import com.orcalab.realtime.event.EventPublisher;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatController {

    private final MensajeRepository mensajeRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EventPublisher eventPublisher;

    public ChatController(MensajeRepository mensajeRepository, SimpMessagingTemplate messagingTemplate,
                           EventPublisher eventPublisher) {
        this.mensajeRepository = mensajeRepository;
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
    }

    @MessageMapping("/sala/{salaId}/mensaje")
    public void enviarMensaje(@DestinationVariable Long salaId, @Payload MensajeRequest request,
                               SimpMessageHeaderAccessor headerAccessor) {
        Long usuarioId = extraerUsuarioId(headerAccessor.getUser());

        Mensaje mensaje = new Mensaje(salaId, usuarioId, request.getContenido(), request.getMarcadorId());
        mensaje = mensajeRepository.save(mensaje);

        MensajeResponse response = new MensajeResponse(mensaje);
        messagingTemplate.convertAndSend("/topic/sala/" + salaId + "/chat", response);

        eventPublisher.publicar(ChatEvento.mensajeEnviado(salaId, usuarioId, mensaje.getId(), mensaje.getContenido(), request.getMarcadorId()));
    }

    private Long extraerUsuarioId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token) {
            return (Long) token.getPrincipal();
        }
        throw new IllegalStateException("No se pudo determinar el usuario autenticado");
    }
}