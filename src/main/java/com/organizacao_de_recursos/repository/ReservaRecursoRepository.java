package com.organizacao_de_recursos.repository;

import com.organizacao_de_recursos.model.ReservaRecursoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservaRecursoRepository extends JpaRepository<ReservaRecursoEntity, Long> {

    List<ReservaRecursoEntity> findByReservaId(Long reservaId);
}
