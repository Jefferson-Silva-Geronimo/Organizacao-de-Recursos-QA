package com.organizacao_de_recursos.controller.api;

import com.organizacao_de_recursos.dto.SalaRequest;
import com.organizacao_de_recursos.dto.SalaResponse;
import com.organizacao_de_recursos.model.SalaEntity;
import com.organizacao_de_recursos.repository.SalaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/salas")
public class SalaController {

    private final SalaRepository salaRepository;

    public SalaController(SalaRepository salaRepository) {
        this.salaRepository = salaRepository;
    }

    @GetMapping
    public List<SalaResponse> listar() {
        return salaRepository.findAll().stream().map(SalaResponse::de).toList();
    }

    @GetMapping("/{id}")
    public SalaResponse obter(@PathVariable Long id) {
        return SalaResponse.de(buscarOuFalhar(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SalaResponse> criar(@Valid @RequestBody SalaRequest request) {
        SalaEntity sala = salaRepository.save(new SalaEntity(request.nome(), request.restrito(), request.responsavelId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(SalaResponse.de(sala));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public SalaResponse atualizar(@PathVariable Long id, @Valid @RequestBody SalaRequest request) {
        SalaEntity sala = buscarOuFalhar(id);
        sala.setNome(request.nome());
        sala.setRestrito(request.restrito());
        sala.setResponsavelId(request.responsavelId());
        return SalaResponse.de(salaRepository.save(sala));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        buscarOuFalhar(id);
        salaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private SalaEntity buscarOuFalhar(Long id) {
        return salaRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Sala não encontrada"));
    }
}
