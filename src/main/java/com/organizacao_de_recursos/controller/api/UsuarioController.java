package com.organizacao_de_recursos.controller.api;

import com.organizacao_de_recursos.dto.CriarUsuarioRequest;
import com.organizacao_de_recursos.dto.UsuarioResponse;
import com.organizacao_de_recursos.model.UsuarioEntity;
import com.organizacao_de_recursos.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

/** Gestão de usuários - somente Administrador (RF-05). */
@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody CriarUsuarioRequest request) {
        UsuarioEntity usuario = usuarioRepository.save(
                new UsuarioEntity(request.username(), passwordEncoder.encode(request.senha()), request.perfil()));
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResponse(usuario));
    }

    @PatchMapping("/{id}/ativo")
    public UsuarioResponse definirAtivo(@PathVariable Long id, @RequestBody boolean ativo) {
        UsuarioEntity usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado"));
        usuario.setAtivo(ativo);
        return paraResponse(usuarioRepository.save(usuario));
    }

    private UsuarioResponse paraResponse(UsuarioEntity usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getUsername(), usuario.getPerfil().name(), usuario.isAtivo());
    }
}
