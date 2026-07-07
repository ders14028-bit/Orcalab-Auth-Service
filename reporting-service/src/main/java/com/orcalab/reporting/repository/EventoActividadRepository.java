package com.orcalab.reporting.repository;

import com.orcalab.reporting.model.EventoActividad;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EventoActividadRepository extends MongoRepository<EventoActividad, String> {
    List<EventoActividad> findTop20ByOrderByTimestampDesc();
}