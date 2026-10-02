package com.organizacao_de_recursos.repository;

import com.organizacao_de_recursos.model.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
}
