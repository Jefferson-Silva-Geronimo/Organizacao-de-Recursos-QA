package com.organizacao_de_recursos.repository;

import com.organizacao_de_recursos.model.BloqueioEntity;
import com.organizacao_de_recursos.model.TipoRecursoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface BloqueioRepository extends JpaRepository<BloqueioEntity, Long> {

    /** Bloqueios do recurso que se sobrepõem ao período (D5, corrige P1; [início,fim) semiaberto). */
    @Query("SELECT b FROM BloqueioEntity b WHERE b.tipoRecurso = :tipo AND b.recursoId = :recursoId "
            + "AND b.inicio < :fim AND b.fim > :inicio")
    List<BloqueioEntity> buscarSobrepostos(@Param("tipo") TipoRecursoReserva tipo,
                                            @Param("recursoId") Long recursoId,
                                            @Param("inicio") OffsetDateTime inicio,
                                            @Param("fim") OffsetDateTime fim);
}
