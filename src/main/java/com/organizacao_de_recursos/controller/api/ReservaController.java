package com.organizacao_de_recursos.controller.api;

import com.organizacao_de_recursos.dto.CriarReservaRequest;
import com.organizacao_de_recursos.dto.EventoAuditoriaResponse;
import com.organizacao_de_recursos.dto.ReservaResponse;
import com.organizacao_de_recursos.model.ReservaEntity;
import com.organizacao_de_recursos.repository.EventoAuditoriaRepository;
import com.organizacao_de_recursos.security.UsuarioPrincipal;
import com.organizacao_de_recursos.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {

    private final ReservaService reservaService;
    private final EventoAuditoriaRepository eventoAuditoriaRepository;

    public ReservaController(ReservaService reservaService, EventoAuditoriaRepository eventoAuditoriaRepository) {
        this.reservaService = reservaService;
        this.eventoAuditoriaRepository = eventoAuditoriaRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('SOLICITANTE')")
    public ResponseEntity<ReservaResponse> criar(@Valid @RequestBody CriarReservaRequest request,
                                                  Authentication authentication) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        ReservaEntity reserva = reservaService.criar(usuario.getId(), request.salaId(), request.inicio(), request.fim());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservaResponse.de(reserva));
    }

    /** Solicitante vê as próprias; Responsável vê as do seu escopo (D6); Administrador vê todas. */
    @GetMapping
    public List<ReservaResponse> listar(Authentication authentication) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        List<ReservaEntity> reservas = switch (usuario.getUsuario().getPerfil()) {
            case ADMINISTRADOR -> reservaService.listarTodas();
            case RESPONSAVEL -> reservaService.listarNoEscopoDoResponsavel(usuario.getId());
            case SOLICITANTE -> reservaService.listarDoSolicitante(usuario.getId());
        };
        return reservas.stream().map(ReservaResponse::de).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@autorizacaoReserva.podeVer(#id, authentication)")
    public ReservaResponse obter(@PathVariable Long id) {
        return ReservaResponse.de(reservaService.buscarPorId(id));
    }

    /** Histórico de auditoria da reserva (RN-09), ordem canônica (ocorrido_em, id). */
    @GetMapping("/{id}/historico")
    @PreAuthorize("@autorizacaoReserva.podeVer(#id, authentication)")
    public List<EventoAuditoriaResponse> historico(@PathVariable Long id) {
        return eventoAuditoriaRepository.findByReservaIdOrderByOcorridoEmAscIdAsc(id).stream()
                .map(EventoAuditoriaResponse::de)
                .toList();
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("@autorizacaoReserva.podeCancelar(#id, authentication)")
    public ReservaResponse cancelar(@PathVariable Long id, Authentication authentication) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        return ReservaResponse.de(reservaService.cancelar(id, usuario.getId()));
    }

    @PostMapping("/{id}/aprovar")
    @PreAuthorize("@autorizacaoReserva.podeAprovarOuRejeitar(#id, authentication)")
    public ReservaResponse aprovar(@PathVariable Long id, Authentication authentication) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        return ReservaResponse.de(reservaService.aprovar(id, usuario.getId()));
    }

    @PostMapping("/{id}/rejeitar")
    @PreAuthorize("@autorizacaoReserva.podeAprovarOuRejeitar(#id, authentication)")
    public ReservaResponse rejeitar(@PathVariable Long id, Authentication authentication) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        return ReservaResponse.de(reservaService.rejeitar(id, usuario.getId()));
    }

    @PostMapping("/{id}/iniciar")
    @PreAuthorize("hasAnyRole('RESPONSAVEL', 'ADMINISTRADOR')")
    public ReservaResponse iniciar(@PathVariable Long id, Authentication authentication) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        return ReservaResponse.de(reservaService.iniciar(id, usuario.getId()));
    }

    @PostMapping("/{id}/concluir")
    @PreAuthorize("hasAnyRole('RESPONSAVEL', 'ADMINISTRADOR')")
    public ReservaResponse concluir(@PathVariable Long id, Authentication authentication) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        return ReservaResponse.de(reservaService.concluir(id, usuario.getId()));
    }

    @PostMapping("/{id}/nao-compareceu")
    @PreAuthorize("hasAnyRole('RESPONSAVEL', 'ADMINISTRADOR')")
    public ReservaResponse naoCompareceu(@PathVariable Long id, Authentication authentication) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        return ReservaResponse.de(reservaService.marcarNaoCompareceu(id, usuario.getId()));
    }
}
