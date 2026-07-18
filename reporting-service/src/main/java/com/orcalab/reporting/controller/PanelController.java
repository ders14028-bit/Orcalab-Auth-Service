package com.orcalab.reporting.controller;

import com.orcalab.reporting.config.AuthContext;
import com.orcalab.reporting.model.EventoActividad;
import com.orcalab.reporting.repository.EventoActividadRepository;
import com.orcalab.reporting.repository.ObservacionRepository;
import com.orcalab.reporting.repository.SalaRepository;
import com.orcalab.reporting.room.RoomServiceClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class PanelController {

    private final SalaRepository salaRepository;
    private final ObservacionRepository observacionRepository;
    private final EventoActividadRepository eventoActividadRepository;
    private final RoomServiceClient roomServiceClient;
    private final AuthContext authContext;

    public PanelController(SalaRepository salaRepository, ObservacionRepository observacionRepository,
                            EventoActividadRepository eventoActividadRepository, RoomServiceClient roomServiceClient,
                            AuthContext authContext) {
        this.salaRepository = salaRepository;
        this.observacionRepository = observacionRepository;
        this.eventoActividadRepository = eventoActividadRepository;
        this.roomServiceClient = roomServiceClient;
        this.authContext = authContext;
    }

    @GetMapping("/api/reportes/panel")
    public Map<String, Object> panelGeneral() {
        // Conteos agregados (sin detalle por sala): quedan fuera del alcance de este fix,
        // solo se acota el feed detallado, que es lo que expone tipo/salaId/usuarioId por evento.
        long totalSalas = salaRepository.count();
        long totalObservaciones = observacionRepository.count();

        List<Long> misSalasIds = roomServiceClient.misSalaIds(authContext.tokenActual());
        List<EventoActividad> feedReciente = misSalasIds.isEmpty()
                ? List.of()
                : eventoActividadRepository.findTop20BySalaIdInOrderByTimestampDesc(misSalasIds);

        return Map.of(
                "investigacionesEnCurso", totalSalas,
                "totalObservaciones", totalObservaciones,
                "feedEventosRecientes", feedReciente
        );
    }
}