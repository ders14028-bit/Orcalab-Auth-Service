package com.orcalab.reporting.controller;

import com.orcalab.reporting.config.AuthContext;
import com.orcalab.reporting.model.Observacion;
import com.orcalab.reporting.repository.ObservacionRepository;
import com.orcalab.reporting.room.RoomServiceClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ObservacionController {

    private final ObservacionRepository observacionRepository;
    private final RoomServiceClient roomServiceClient;
    private final AuthContext authContext;

    public ObservacionController(ObservacionRepository observacionRepository, RoomServiceClient roomServiceClient,
                                  AuthContext authContext) {
        this.observacionRepository = observacionRepository;
        this.roomServiceClient = roomServiceClient;
        this.authContext = authContext;
    }

    @GetMapping("/api/reportes/salas/{salaId}/observaciones")
    public List<Observacion> listar(@PathVariable Long salaId,
                                     @RequestParam(required = false) String tipo) {
        if (!roomServiceClient.esMiembro(salaId, authContext.tokenActual())) {
            throw new AccessDeniedException("No eres miembro de esta sala");
        }

        if (tipo != null && !tipo.isBlank()) {
            return observacionRepository.findBySalaIdAndTipo(salaId, tipo);
        }
        return observacionRepository.findBySalaId(salaId);
    }
}