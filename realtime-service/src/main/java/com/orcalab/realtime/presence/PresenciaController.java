package com.orcalab.realtime.presence;

import com.orcalab.realtime.state.SalaEstadoService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.List;

@Controller
public class PresenciaController {

    private final PresenciaService presenciaService;
    private final SalaEstadoService salaEstadoService;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenciaController(PresenciaService presenciaService, SalaEstadoService salaEstadoService,
                                SimpMessagingTemplate messagingTemplate) {
        this.presenciaService = presenciaService;
        this.salaEstadoService = salaEstadoService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/sala/{salaId}/entrar")
    public void entrarASala(@DestinationVariable Long salaId, SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        Long usuarioId = extraerUsuarioId(principal);
        String sessionId = headerAccessor.getSessionId();

        presenciaService.registrarEntrada(sessionId, salaId, usuarioId);

        // Guardamos salaId en los atributos de la sesión, para poder recuperarlo en la desconexión
        headerAccessor.getSessionAttributes().put("salaId", salaId);

        var presentes = construirListaPresentes(salaId);
        messagingTemplate.convertAndSend("/topic/sala/" + salaId + "/presencia",
                PresenciaMensaje.entrada(usuarioId, presentes));
    }

    @EventListener
    public void manejarDesconexion(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        System.out.println(">>> Desconexión detectada, sessionId: " + sessionId);

        PresenciaService.SesionInfo info = presenciaService.registrarSalida(sessionId);

        System.out.println(">>> Info recuperada: " + info);

        if (info != null) {
            var presentes = construirListaPresentes(info.salaId());
            messagingTemplate.convertAndSend("/topic/sala/" + info.salaId() + "/presencia",
                    PresenciaMensaje.salida(info.usuarioId(), presentes));
        }
    }

    private List<PresenciaMensaje.UsuarioPresente> construirListaPresentes(Long salaId) {
        return presenciaService.obtenerPresentes(salaId).stream()
                .map(uid -> {
                    var rol = salaEstadoService.obtenerRol(salaId, uid);
                    return new PresenciaMensaje.UsuarioPresente(uid, rol != null ? rol.name() : "DESCONOCIDO");
                })
                .toList();
    }

    private Long extraerUsuarioId(Principal principal) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken token) {
            return (Long) token.getPrincipal();
        }
        throw new IllegalStateException("No se pudo determinar el usuario autenticado");
    }
}