package com.orcalab.reporting.repository;

import com.orcalab.reporting.model.Sala;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SalaRepository extends MongoRepository<Sala, String> {
}