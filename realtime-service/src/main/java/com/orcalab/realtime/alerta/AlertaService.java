package com.orcalab.realtime.alerta;

import com.orcalab.realtime.broadcast.RealtimeBroadcaster;
import com.orcalab.realtime.event.EventPublisher;
import com.orcalab.realtime.event.MapaEvento;
import org.springframework.stereotype.Service;

@Service
public class AlertaService {

    private final AlertaRepository alertaRepository;
    private final RealtimeBroadcaster broadcaster;
    private final EventPublisher eventPublisher;

    public AlertaService(AlertaRepository alertaRepository, RealtimeBroadcaster broadcaster,
                          EventPublisher eventPublisher) {
        this.alertaRepository = alertaRepository;
        this.broadcaster = broadcaster;
        this.eventPublisher = eventPublisher;
    }

    public void generarAlertaPorMarcador(Long salaId, Long usuarioId, String marcadorId,
                                          double latitud, double longitud, String descripcion) {
        Alerta alerta = new Alerta(salaId, usuarioId, marcadorId, latitud, longitud, descripcion);
        alerta = alertaRepository.save(alerta);

        broadcaster.broadcast("/topic/sala/" + salaId + "/alertas", alerta);

        eventPublisher.publicar(MapaEvento.marcador("AlertaGenerada", salaId, usuarioId, alerta.getId(),
                "CRITICO", latitud, longitud, descripcion));
    }
}