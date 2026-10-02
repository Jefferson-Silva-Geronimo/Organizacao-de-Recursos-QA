package com.organizacao_de_recursos.repository;

import com.organizacao_de_recursos.model.ConflitoEvitadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;

public interface ConflitoEvitadoRepository extends JpaRepository<ConflitoEvitadoEntity, Long> {

    long countByTentadoEmBetween(OffsetDateTime de, OffsetDateTime ate);
}
