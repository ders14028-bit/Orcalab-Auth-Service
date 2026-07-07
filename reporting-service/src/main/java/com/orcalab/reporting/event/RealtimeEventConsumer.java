package com.orcalab.reporting.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orcalab.reporting.model.EventoActividad;
import com.orcalab.reporting.model.Observacion;
import com.orcalab.reporting.repository.EventoActividadRepository;
import com.orcalab.reporting.repository.ObservacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class RealtimeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEventConsumer.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ObservacionRepository observacionRepository;
    private final EventoActividadRepository eventoActividadRepository;

    @Value("${app.events.realtime-topic}")
    private String topic;

    private String ultimoIdLeido = "0";

    public RealtimeEventConsumer(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper,
                                  ObservacionRepository observacionRepository,
                                  EventoActividadRepository eventoActividadRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.observacionRepository = observacionRepository;
        this.eventoActividadRepository = eventoActividadRepository;
    }

    @Scheduled(fixedDelay = 1000)
    public void consumirEventos() {
        try {
            List<MapRecord<String, Object, Object>> registros = redisTemplate.opsForStream()
                    .read(StreamOffset.create(topic, ReadOffset.from(ultimoIdLeido)));

            if (registros == null || registros.isEmpty()) {
                return;
            }

            for (MapRecord<String, Object, Object> registro : registros) {
                procesarRegistro(registro);
                ultimoIdLeido = registro.getId().getValue();
            }
        } catch (Exception e) {
            log.error("Error al consumir eventos de {}", topic, e);
        }
    }

    private void procesarRegistro(MapRecord<String, Object, Object> registro) {
        try {
            String json = (String) registro.getValue().get("data");
            RealtimeEventoRecibido evento = objectMapper.readValue(json, RealtimeEventoRecibido.class);
            aplicarEvento(evento);
        } catch (Exception e) {
            log.error("Error al procesar registro {}", registro.getId(), e);
        }
    }

    private void aplicarEvento(RealtimeEventoRecibido evento) {
        switch (evento.getTipo()) {
            case "MarcadorAgregado" -> {
                Observacion obs = new Observacion(evento.getElementoId(), evento.getSalaId(), evento.getUsuarioId(),
                        evento.getTipoMarcador(), evento.getLatitud(), evento.getLongitud(),
                        evento.getDescripcion(), LocalDateTime.now());
                observacionRepository.save(obs);
            }
            case "MarcadorEditado" -> observacionRepository.findById(evento.getElementoId()).ifPresent(obs -> {
                obs.setTipo(evento.getTipoMarcador());
                obs.setLatitud(evento.getLatitud());
                obs.setLongitud(evento.getLongitud());
                obs.setDescripcion(evento.getDescripcion());
                observacionRepository.save(obs);
            });
            case "AlertaGenerada" -> observacionRepository.findById(evento.getElementoId()).ifPresent(obs -> {
                // En este caso, elementoId es el id de la alerta, no del marcador original;
                // igual guardamos el marcador asociado si no existe aún.
            });
            default -> { /* MensajeEnviado, RutaTrazada u otros: solo se registran como actividad */ }
        }

        eventoActividadRepository.save(new EventoActividad(evento.getTipo(), evento.getSalaId(),
                evento.getUsuarioId(), LocalDateTime.now()));

        log.info("Evento de realtime-service aplicado: {} para sala {}", evento.getTipo(), evento.getSalaId());
    }
}