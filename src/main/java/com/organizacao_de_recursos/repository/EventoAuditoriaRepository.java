package com.organizacao_de_recursos.repository;

import com.organizacao_de_recursos.model.EventoAuditoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoriaEntity, Long> {

    /** Ordenação canônica (ocorrido_em, id) - o id desempata timestamps iguais (RN-09/RN-04). */
    List<EventoAuditoriaEntity> findByReservaIdOrderByOcorridoEmAscIdAsc(Long reservaId);
}
