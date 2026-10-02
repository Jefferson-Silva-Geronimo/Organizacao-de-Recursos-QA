package com.organizacao_de_recursos.service;

import com.organizacao_de_recursos.domain.AcessoNegadoException;
import com.organizacao_de_recursos.domain.ReservaAprovacaoException;
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
import com.organizacao_de_recursos.notification.ReservaNotificacaoEvent;
import com.organizacao_de_recursos.repository.BloqueioRepository;
import com.organizacao_de_recursos.repository.EventoAuditoriaRepository;
import com.organizacao_de_recursos.repository.ReservaRecursoRepository;
import com.organizacao_de_recursos.repository.ReservaRepository;
import com.organizacao_de_recursos.repository.SalaRepository;
import com.organizacao_de_recursos.repository.UsuarioRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

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
    private final ConflitoEvitadoService conflitoEvitadoService;
    private final MaquinaDeEstados maquinaDeEstados;
    private final Clock clock;
    private final ApplicationEventPublisher eventos;

    public ReservaService(ReservaRepository reservaRepository,
                           ReservaRecursoRepository reservaRecursoRepository,
                           UsuarioRepository usuarioRepository,
                           SalaRepository salaRepository,
                           BloqueioRepository bloqueioRepository,
                           EventoAuditoriaRepository eventoAuditoriaRepository,
                           ConflitoEvitadoService conflitoEvitadoService,
                           MaquinaDeEstados maquinaDeEstados,
                           Clock clock,
                           ApplicationEventPublisher eventos) {
        this.reservaRepository = reservaRepository;
        this.reservaRecursoRepository = reservaRecursoRepository;
        this.usuarioRepository = usuarioRepository;
        this.salaRepository = salaRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.eventoAuditoriaRepository = eventoAuditoriaRepository;
        this.conflitoEvitadoService = conflitoEvitadoService;
        this.maquinaDeEstados = maquinaDeEstados;
        this.clock = clock;
        this.eventos = eventos;
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
        } catch (DataAccessException exclusaoVioladaOuDeadlock) {
            // Sob concorrência pesada, o Postgres pode responder com deadlock (40P01) em vez da
            // violação da constraint de exclusão (23P01) - ambos significam a mesma coisa aqui:
            // a reserva não pode ser confirmada porque outra concorrente ganhou o recurso.
            conflitoEvitadoService.registrar(TipoRecursoReserva.SALA, salaId, solicitanteId);
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

    public ReservaEntity buscarPorId(Long id) {
        return reservaRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Reserva não encontrada"));
    }

    public List<ReservaEntity> listarDoSolicitante(Long solicitanteId) {
        return reservaRepository.findBySolicitanteId(solicitanteId);
    }

    public List<ReservaEntity> listarTodas() {
        return reservaRepository.findAll();
    }

    /** Reservas dos recursos atribuídos ao Responsável (D6). */
    public List<ReservaEntity> listarNoEscopoDoResponsavel(Long responsavelId) {
        return reservaRepository.findAll().stream()
                .filter(reserva -> escopoDoResponsavel(reserva, responsavelId))
                .toList();
    }

    /** Cancela a reserva do próprio solicitante; libera o recurso (D4, corrige P2). */
    @Transactional
    public ReservaEntity cancelar(Long reservaId, Long solicitanteId) {
        ReservaEntity reserva = buscarPorId(reservaId);
        if (!Objects.equals(reserva.getSolicitanteId(), solicitanteId)) {
            throw new AcessoNegadoException("Acesso negado. Somente o solicitante da reserva pode cancelá-la");
        }
        EstadoReserva anterior = reserva.getEstado();
        maquinaDeEstados.validarTransicao(anterior, EstadoReserva.CANCELADA);
        reserva.setEstado(EstadoReserva.CANCELADA);
        for (ReservaRecursoEntity recurso : reservaRecursoRepository.findByReservaId(reservaId)) {
            recurso.setOcupa(false);
        }
        registrarAuditoria(reservaId, anterior, EstadoReserva.CANCELADA, solicitanteId);
        eventos.publishEvent(new ReservaNotificacaoEvent(reservaId, "CANCELADA"));
        return reserva;
    }

    /** Aprova a reserva; só o Responsável do recurso pode (D6, corrige P12). */
    @Transactional
    public ReservaEntity aprovar(Long reservaId, Long responsavelId) {
        UsuarioEntity responsavel = usuarioRepository.findById(responsavelId)
                .orElseThrow(() -> new AcessoNegadoException("Acesso negado"));
        if (responsavel.getPerfil() != Usuario.Perfil.RESPONSAVEL) {
            throw new AcessoNegadoException("Acesso negado. Apenas Responsável pode aprovar");
        }
        ReservaEntity reserva = buscarPorId(reservaId);
        if (!escopoDoResponsavel(reserva, responsavelId)) {
            throw new ReservaAprovacaoException("Recurso fora de sua responsabilidade");
        }
        EstadoReserva anterior = reserva.getEstado();
        maquinaDeEstados.validarTransicao(anterior, EstadoReserva.APROVADA);
        reserva.setEstado(EstadoReserva.APROVADA);
        reserva.setAprovadorId(responsavelId);
        registrarAuditoria(reservaId, anterior, EstadoReserva.APROVADA, responsavelId);
        eventos.publishEvent(new ReservaNotificacaoEvent(reservaId, "APROVADA"));
        return reserva;
    }

    /** Rejeita a reserva; só o Responsável do recurso pode (D6). */
    @Transactional
    public ReservaEntity rejeitar(Long reservaId, Long responsavelId) {
        UsuarioEntity responsavel = usuarioRepository.findById(responsavelId)
                .orElseThrow(() -> new AcessoNegadoException("Acesso negado"));
        if (responsavel.getPerfil() != Usuario.Perfil.RESPONSAVEL) {
            throw new AcessoNegadoException("Acesso negado. Apenas Responsável pode rejeitar");
        }
        ReservaEntity reserva = buscarPorId(reservaId);
        if (!escopoDoResponsavel(reserva, responsavelId)) {
            throw new ReservaAprovacaoException("Recurso fora de sua responsabilidade");
        }
        EstadoReserva anterior = reserva.getEstado();
        maquinaDeEstados.validarTransicao(anterior, EstadoReserva.REJEITADA);
        reserva.setEstado(EstadoReserva.REJEITADA);
        reserva.setAprovadorId(responsavelId);
        for (ReservaRecursoEntity recurso : reservaRecursoRepository.findByReservaId(reservaId)) {
            recurso.setOcupa(false);
        }
        registrarAuditoria(reservaId, anterior, EstadoReserva.REJEITADA, responsavelId);
        eventos.publishEvent(new ReservaNotificacaoEvent(reservaId, "REJEITADA"));
        return reserva;
    }

    /** Inicia o uso da reserva aprovada (D3); RESPONSAVEL (no escopo) ou ADMINISTRADOR. */
    @Transactional
    public ReservaEntity iniciar(Long reservaId, Long atorId) {
        ReservaEntity reserva = buscarPorId(reservaId);
        verificarPodeOperarUso(reserva, atorId);
        EstadoReserva anterior = reserva.getEstado();
        maquinaDeEstados.validarTransicao(anterior, EstadoReserva.EM_USO);
        reserva.setEstado(EstadoReserva.EM_USO);
        registrarAuditoria(reservaId, anterior, EstadoReserva.EM_USO, atorId);
        return reserva;
    }

    /** Conclui a reserva em uso; libera o recurso (D3). */
    @Transactional
    public ReservaEntity concluir(Long reservaId, Long atorId) {
        ReservaEntity reserva = buscarPorId(reservaId);
        verificarPodeOperarUso(reserva, atorId);
        EstadoReserva anterior = reserva.getEstado();
        maquinaDeEstados.validarTransicao(anterior, EstadoReserva.CONCLUIDA);
        reserva.setEstado(EstadoReserva.CONCLUIDA);
        for (ReservaRecursoEntity recurso : reservaRecursoRepository.findByReservaId(reservaId)) {
            recurso.setOcupa(false);
        }
        registrarAuditoria(reservaId, anterior, EstadoReserva.CONCLUIDA, atorId);
        return reserva;
    }

    /** Marca não comparecimento a partir de APROVADA (não existe transição direta de SOLICITADA, RN-07). */
    @Transactional
    public ReservaEntity marcarNaoCompareceu(Long reservaId, Long atorId) {
        ReservaEntity reserva = buscarPorId(reservaId);
        verificarPodeOperarUso(reserva, atorId);
        EstadoReserva anterior = reserva.getEstado();
        maquinaDeEstados.validarTransicao(anterior, EstadoReserva.NAO_COMPARECEU);
        reserva.setEstado(EstadoReserva.NAO_COMPARECEU);
        for (ReservaRecursoEntity recurso : reservaRecursoRepository.findByReservaId(reservaId)) {
            recurso.setOcupa(false);
        }
        registrarAuditoria(reservaId, anterior, EstadoReserva.NAO_COMPARECEU, atorId);
        return reserva;
    }

    private void verificarPodeOperarUso(ReservaEntity reserva, Long atorId) {
        UsuarioEntity ator = usuarioRepository.findById(atorId)
                .orElseThrow(() -> new AcessoNegadoException("Acesso negado"));
        if (ator.getPerfil() == Usuario.Perfil.ADMINISTRADOR) {
            return;
        }
        if (ator.getPerfil() == Usuario.Perfil.RESPONSAVEL && escopoDoResponsavel(reserva, atorId)) {
            return;
        }
        throw new AcessoNegadoException("Acesso negado");
    }

    /** Escopo do Responsável (D6): só responde pelas salas que lhe foram atribuídas. */
    boolean escopoDoResponsavel(ReservaEntity reserva, Long responsavelId) {
        for (ReservaRecursoEntity recurso : reservaRecursoRepository.findByReservaId(reserva.getId())) {
            if (recurso.getTipoRecurso() == TipoRecursoReserva.SALA) {
                SalaEntity sala = salaRepository.findById(recurso.getRecursoId()).orElse(null);
                if (sala != null && responsavelId.equals(sala.getResponsavelId())) {
                    return true;
                }
            }
        }
        return false;
    }
}
