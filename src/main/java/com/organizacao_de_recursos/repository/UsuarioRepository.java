package com.organizacao_de_recursos.repository;

import com.organizacao_de_recursos.model.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByUsername(String username);
}
