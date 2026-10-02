package com.organizacao_de_recursos.repository;

import com.organizacao_de_recursos.model.ReservaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<ReservaEntity, Long> {

    List<ReservaEntity> findBySolicitanteId(Long solicitanteId);

    List<ReservaEntity> findByInicioGreaterThanEqualAndFimLessThanEqual(OffsetDateTime de, OffsetDateTime ate);
}
