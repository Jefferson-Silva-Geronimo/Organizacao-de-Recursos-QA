package com.organizacao_de_recursos.controller.api;

import com.organizacao_de_recursos.dto.BloqueioRequest;
import com.organizacao_de_recursos.dto.BloqueioResponse;
import com.organizacao_de_recursos.model.BloqueioEntity;
import com.organizacao_de_recursos.repository.BloqueioRepository;
import com.organizacao_de_recursos.security.UsuarioPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Bloqueios de recurso (manutenção/administrativo) - somente Administrador (D5). */
@RestController
@RequestMapping("/api/v1/bloqueios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class BloqueioController {

    private final BloqueioRepository bloqueioRepository;

    public BloqueioController(BloqueioRepository bloqueioRepository) {
        this.bloqueioRepository = bloqueioRepository;
    }

    @GetMapping
    public List<BloqueioResponse> listar() {
        return bloqueioRepository.findAll().stream().map(BloqueioResponse::de).toList();
    }

    @PostMapping
    public ResponseEntity<BloqueioResponse> criar(@Valid @RequestBody BloqueioRequest request, Authentication authentication) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        BloqueioEntity bloqueio = bloqueioRepository.save(new BloqueioEntity(request.tipoRecurso(), request.recursoId(),
                request.inicio(), request.fim(), request.motivo(), usuario.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(BloqueioResponse.de(bloqueio));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        bloqueioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
