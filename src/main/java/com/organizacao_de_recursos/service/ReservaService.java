package com.organizacao_de_recursos.service;

import com.organizacao_de_recursos.domain.ReservaCriacaoException;
import com.organizacao_de_recursos.domain.Usuario;
import com.organizacao_de_recursos.domain.estado.EstadoReserva;
import com.organizacao_de_recursos.domain.estado.MaquinaDeEstados;
import com.organizacao_de_recursos.exception.ConflitoDeHorarioException;
import com.organizacao_de_recursos.model.EventoAuditoriaEntity;
import com.organizacao_de_recursos.model.ReservaEntity;
import com.organizacao_de_recursos.model.ReservaRecursoEntity;
import com.organizacao_de_recursos.model.SalaEntity;
import com.organizacao_de_recursos.model.TipoRecursoReserva;
import com.organizacao_de_recursos.model.UsuarioEntity;
import com.organizacao_de_recursos.repository.BloqueioRepository;
import com.organizacao_de_recursos.repository.EventoAuditoriaRepository;
import com.organizacao_de_recursos.repository.ReservaRecursoRepository;
import com.organizacao_de_recursos.repository.ReservaRepository;
import com.organizacao_de_recursos.repository.SalaRepository;
import com.organizacao_de_recursos.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Criação de reserva na camada persistida (seção 5/6 do plano de migração).
 * Corrige P1 (bloqueio checado na criação), P3 (perfil checado) e P13 (D2: estado inicial
 * SOLICITADA, com auto-aprovação de recurso não restrito na mesma transação). A garantia de
 * RN-04 (P14) é a constraint de exclusão da migration V1 - este serviço só traduz a violação.
 */
@Service
public class ReservaService {

    private static final Duration DURACAO_MINIMA = Duration.ofMinutes(15);
    private static final Duration DURACAO_MAXIMA = Duration.ofHours(8);

    private final ReservaRepository reservaRepository;
    private final ReservaRecursoRepository reservaRecursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SalaRepository salaRepository;
    private final BloqueioRepository bloqueioRepository;
    private final EventoAuditoriaRepository eventoAuditoriaRepository;
    private final MaquinaDeEstados maquinaDeEstados;
    private final Clock clock;

    public ReservaService(ReservaRepository reservaRepository,
                           ReservaRecursoRepository reservaRecursoRepository,
                           UsuarioRepository usuarioRepository,
                           SalaRepository salaRepository,
                           BloqueioRepository bloqueioRepository,
                           EventoAuditoriaRepository eventoAuditoriaRepository,
                           MaquinaDeEstados maquinaDeEstados,
                           Clock clock) {
        this.reservaRepository = reservaRepository;
        this.reservaRecursoRepository = reservaRecursoRepository;
        this.usuarioRepository = usuarioRepository;
        this.salaRepository = salaRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.eventoAuditoriaRepository = eventoAuditoriaRepository;
        this.maquinaDeEstados = maquinaDeEstados;
        this.clock = clock;
    }

    @Transactional
    public ReservaEntity criar(Long solicitanteId, Long salaId, OffsetDateTime inicio, OffsetDateTime fim) {
        UsuarioEntity solicitante = usuarioRepository.findById(solicitanteId)
                .orElseThrow(() -> new ReservaCriacaoException("Solicitante inválido"));
        if (!solicitante.isAtivo()) {
            throw new ReservaCriacaoException("Solicitante inválido");
        }
        if (solicitante.getPerfil() != Usuario.Perfil.SOLICITANTE) {
            throw new ReservaCriacaoException("Apenas Solicitante pode criar reserva");
        }
        SalaEntity sala = salaRepository.findById(salaId)
                .orElseThrow(() -> new ReservaCriacaoException("Recurso não encontrado"));

        validarPeriodo(inicio, fim);
        validarBloqueio(TipoRecursoReserva.SALA, salaId, inicio, fim);

        ReservaEntity reserva = reservaRepository.save(
                new ReservaEntity(solicitanteId, EstadoReserva.SOLICITADA, inicio, fim));
        registrarAuditoria(reserva.getId(), null, EstadoReserva.SOLICITADA, solicitanteId);

        try {
            reservaRecursoRepository.saveAndFlush(
                    new ReservaRecursoEntity(reserva.getId(), TipoRecursoReserva.SALA, salaId, inicio, fim));
        } catch (DataIntegrityViolationException exclusaoViolada) {
            throw new ConflitoDeHorarioException("Recurso indisponível no período solicitado");
        }

        if (!sala.isRestrito()) {
            EstadoReserva anterior = reserva.getEstado();
            maquinaDeEstados.validarTransicao(anterior, EstadoReserva.APROVADA);
            reserva.setEstado(EstadoReserva.APROVADA);
            registrarAuditoria(reserva.getId(), anterior, EstadoReserva.APROVADA, solicitanteId);
        }
        return reserva;
    }

    private void validarPeriodo(OffsetDateTime inicio, OffsetDateTime fim) {
        if (inicio == null || fim == null) {
            throw new ReservaCriacaoException("Período é obrigatório");
        }
        if (!fim.isAfter(inicio)) {
            throw new ReservaCriacaoException("Fim deve ser posterior ao início");
        }
        Duration duracao = Duration.between(inicio, fim);
        if (duracao.compareTo(DURACAO_MINIMA) < 0) {
            throw new ReservaCriacaoException("Duração mínima de 15 minutos");
        }
        if (duracao.compareTo(DURACAO_MAXIMA) > 0) {
            throw new ReservaCriacaoException("Duração máxima de 8 horas excedida");
        }
        if (inicio.isBefore(OffsetDateTime.now(clock))) {
            throw new ReservaCriacaoException("Data no passado");
        }
    }

    private void validarBloqueio(TipoRecursoReserva tipo, Long recursoId, OffsetDateTime inicio, OffsetDateTime fim) {
        if (!bloqueioRepository.buscarSobrepostos(tipo, recursoId, inicio, fim).isEmpty()) {
            throw new ReservaCriacaoException("Recurso bloqueado no período solicitado");
        }
    }

    private void registrarAuditoria(Long reservaId, EstadoReserva anterior, EstadoReserva novo, Long atorId) {
        eventoAuditoriaRepository.save(new EventoAuditoriaEntity(reservaId, anterior, novo, atorId));
    }
}
