package com.orcalab.reporting.repository;

import com.orcalab.reporting.model.Observacion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ObservacionRepository extends MongoRepository<Observacion, String> {
    List<Observacion> findBySalaId(Long salaId);
    List<Observacion> findBySalaIdAndTipo(Long salaId, String tipo);
}