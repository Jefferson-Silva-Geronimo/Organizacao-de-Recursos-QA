package com.organizacao_de_recursos.repository;

import com.organizacao_de_recursos.model.ReservaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<ReservaEntity, Long> {
}
