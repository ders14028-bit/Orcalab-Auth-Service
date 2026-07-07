package com.orcalab.reporting.controller;

import com.orcalab.reporting.model.Observacion;
import com.orcalab.reporting.repository.ObservacionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ObservacionController {

    private final ObservacionRepository observacionRepository;

    public ObservacionController(ObservacionRepository observacionRepository) {
        this.observacionRepository = observacionRepository;
    }

    @GetMapping("/api/reportes/salas/{salaId}/observaciones")
    public List<Observacion> listar(@PathVariable Long salaId,
                                     @RequestParam(required = false) String tipo) {
        if (tipo != null && !tipo.isBlank()) {
            return observacionRepository.findBySalaIdAndTipo(salaId, tipo);
        }
        return observacionRepository.findBySalaId(salaId);
    }
}