package com.organizacao_de_recursos.repository;

import com.organizacao_de_recursos.model.SalaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaRepository extends JpaRepository<SalaEntity, Long> {
}
