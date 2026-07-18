package com.orcalab.realtime.alerta;

import com.orcalab.realtime.config.AuthContext;
import com.orcalab.realtime.room.RoomServiceClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AlertaHistorialController {

    private final AlertaRepository alertaRepository;
    private final RoomServiceClient roomServiceClient;
    private final AuthContext authContext;

    public AlertaHistorialController(AlertaRepository alertaRepository, RoomServiceClient roomServiceClient,
                                      AuthContext authContext) {
        this.alertaRepository = alertaRepository;
        this.roomServiceClient = roomServiceClient;
        this.authContext = authContext;
    }

    @GetMapping("/api/salas/{salaId}/alertas")
    public List<Alerta> historial(@PathVariable Long salaId) {
        if (!roomServiceClient.esMiembro(salaId, authContext.tokenActual())) {
            throw new AccessDeniedException("No eres miembro de esta sala");
        }
        return alertaRepository.findBySalaIdOrderByTimestampDesc(salaId);
    }
}