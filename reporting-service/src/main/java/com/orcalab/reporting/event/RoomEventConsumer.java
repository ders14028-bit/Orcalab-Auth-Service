package com.orcalab.reporting.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orcalab.reporting.model.EventoActividad;
import com.orcalab.reporting.model.Sala;
import com.orcalab.reporting.repository.EventoActividadRepository;
import com.orcalab.reporting.repository.SalaRepository;
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
public class RoomEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RoomEventConsumer.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final SalaRepository salaRepository;
    private final EventoActividadRepository eventoActividadRepository;

    @Value("${app.events.room-topic}")
    private String topic;

    private String ultimoIdLeido = "0";

    public RoomEventConsumer(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper,
                              SalaRepository salaRepository, EventoActividadRepository eventoActividadRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.salaRepository = salaRepository;
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
            RoomEventoRecibido evento = objectMapper.readValue(json, RoomEventoRecibido.class);
            aplicarEvento(evento);
        } catch (Exception e) {
            log.error("Error al procesar registro {}", registro.getId(), e);
        }
    }

    private void aplicarEvento(RoomEventoRecibido evento) {
        if ("SalaCreada".equals(evento.getTipo())) {
            Sala sala = new Sala(evento.getSalaId(), evento.getNombreSala(), evento.getUsuarioId(), LocalDateTime.now());
            salaRepository.save(sala);
        }

        eventoActividadRepository.save(new EventoActividad(evento.getTipo(), evento.getSalaId(),
                evento.getUsuarioId(), LocalDateTime.now()));

        log.info("Evento de room-service aplicado: {} para sala {}", evento.getTipo(), evento.getSalaId());
    }
}