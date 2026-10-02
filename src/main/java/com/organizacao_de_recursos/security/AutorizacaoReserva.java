package com.organizacao_de_recursos.security;

import com.organizacao_de_recursos.domain.Usuario;
import com.organizacao_de_recursos.model.ReservaEntity;
import com.organizacao_de_recursos.model.ReservaRecursoEntity;
import com.organizacao_de_recursos.model.SalaEntity;
import com.organizacao_de_recursos.model.TipoRecursoReserva;
import com.organizacao_de_recursos.repository.ReservaRecursoRepository;
import com.organizacao_de_recursos.repository.ReservaRepository;
import com.organizacao_de_recursos.repository.SalaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Checagem de autorização por objeto (propriedade e escopo do Responsável, D6) usada em
 * {@code @PreAuthorize} nos controllers - camada "objeto" das três exigidas (ADR-003/ADR-018).
 * A camada autoritativa (que nunca pode ser contornada) é o próprio {@code ReservaService}.
 */
@Component
public class AutorizacaoReserva {

    private final ReservaRepository reservaRepository;
    private final ReservaRecursoRepository reservaRecursoRepository;
    private final SalaRepository salaRepository;

    public AutorizacaoReserva(ReservaRepository reservaRepository,
                               ReservaRecursoRepository reservaRecursoRepository,
                               SalaRepository salaRepository) {
        this.reservaRepository = reservaRepository;
        this.reservaRecursoRepository = reservaRecursoRepository;
        this.salaRepository = salaRepository;
    }

    /** Dono da reserva, ou Responsável/Administrador (que podem consultar via rota própria). */
    public boolean podeVer(Long reservaId, Authentication authentication) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        if (usuario.getUsuario().getPerfil() == Usuario.Perfil.ADMINISTRADOR) {
            return true;
        }
        return reservaRepository.findById(reservaId)
                .map(reserva -> ehDono(reserva, usuario.getId()) || ehResponsavelNoEscopo(reserva, usuario))
                .orElse(true); // reserva inexistente: deixa o controller/service responder 404
    }

    /** Somente o solicitante dono da reserva pode cancelá-la. */
    public boolean podeCancelar(Long reservaId, Authentication authentication) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        return reservaRepository.findById(reservaId)
                .map(reserva -> ehDono(reserva, usuario.getId()))
                .orElse(true);
    }

    /** Somente o Responsável pelo recurso da reserva pode aprovar/rejeitar (D6). */
    public boolean podeAprovarOuRejeitar(Long reservaId, Authentication authentication) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        if (usuario.getUsuario().getPerfil() != Usuario.Perfil.RESPONSAVEL) {
            return false;
        }
        return reservaRepository.findById(reservaId)
                .map(reserva -> ehResponsavelNoEscopo(reserva, usuario))
                .orElse(true);
    }

    private boolean ehDono(ReservaEntity reserva, Long usuarioId) {
        return reserva.getSolicitanteId().equals(usuarioId);
    }

    private boolean ehResponsavelNoEscopo(ReservaEntity reserva, UsuarioPrincipal responsavel) {
        for (ReservaRecursoEntity recurso : reservaRecursoRepository.findByReservaId(reserva.getId())) {
            if (recurso.getTipoRecurso() == TipoRecursoReserva.SALA) {
                SalaEntity sala = salaRepository.findById(recurso.getRecursoId()).orElse(null);
                if (sala != null && responsavel.getId().equals(sala.getResponsavelId())) {
                    return true;
                }
            }
        }
        return false;
    }
}
