package com.orcalab.reporting.controller;

import com.orcalab.reporting.model.EventoActividad;
import com.orcalab.reporting.repository.EventoActividadRepository;
import com.orcalab.reporting.repository.ObservacionRepository;
import com.orcalab.reporting.repository.SalaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class PanelController {

    private final SalaRepository salaRepository;
    private final ObservacionRepository observacionRepository;
    private final EventoActividadRepository eventoActividadRepository;

    public PanelController(SalaRepository salaRepository, ObservacionRepository observacionRepository,
                            EventoActividadRepository eventoActividadRepository) {
        this.salaRepository = salaRepository;
        this.observacionRepository = observacionRepository;
        this.eventoActividadRepository = eventoActividadRepository;
    }

    @GetMapping("/api/reportes/panel")
    public Map<String, Object> panelGeneral() {
        long totalSalas = salaRepository.count();
        long totalObservaciones = observacionRepository.count();
        List<EventoActividad> feedReciente = eventoActividadRepository.findTop20ByOrderByTimestampDesc();

        return Map.of(
                "investigacionesEnCurso", totalSalas,
                "totalObservaciones", totalObservaciones,
                "feedEventosRecientes", feedReciente
        );
    }
}