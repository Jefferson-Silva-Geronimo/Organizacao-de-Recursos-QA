package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de Criação de Reserva para RF-10.
 *
 * Na confirmação da reserva revalida sala, material, professor e manutenção (ADR-005) e
 * aplica RN-01 a RN-04. A verificação de conflito e o registro da reserva formam uma
 * seção crítica única, de modo que duas solicitações simultâneas para o mesmo recurso e
 * período resultem em uma única reserva aceita (RN-04).
 *
 * O mecanismo de exclusão é local ao processo. O mecanismo definitivo de concorrência sobre
 * persistência (constraint, lock ou isolamento) segue pendente conforme ADR-002.
 */
public class ServicoCriacaoReserva {
    private final Map<Long, Reserva> reservas = new HashMap<>();
    private final ValidadorSobreposicao validadorSobreposicao = new ValidadorSobreposicao();
    private final ValidadorManutencao validadorManutencao;
    private final Object secaoCritica = new Object();

    public ServicoCriacaoReserva() {
        this(new ValidadorManutencao());
    }

    /**
     * @param validadorManutencao fonte dos períodos de manutenção consultados na confirmação (RN-05)
     */
    public ServicoCriacaoReserva(ValidadorManutencao validadorManutencao) {
        this.validadorManutencao = validadorManutencao != null ? validadorManutencao : new ValidadorManutencao();
    }

    public Reserva criarReserva(Usuario solicitante, Reserva reserva) {
        if (solicitante == null || !solicitante.isAtivo()) {
            throw new ReservaCriacaoException("Solicitante inválido");
        }
        if (reserva.getRecurso() == null) {
            throw new ReservaCriacaoException("Recurso não encontrado");
        }
        if (reserva.getUsuarioSolicitante() != null
                && reserva.getUsuarioSolicitante() != solicitante) {
            throw new ReservaCriacaoException("Não pode criar reserva para outro usuário");
        }
        reserva.validarTemporalidade(reserva.getInicio(), reserva.getFim());

        synchronized (secaoCritica) {
            try {
                validadorSobreposicao.validarSobreposicao(reserva);
            } catch (ReservaSobreposicaoException e) {
                throw new ReservaCriacaoException("conflito em " + tipoRecurso(e.getRecursoEmConflito(), reserva), e);
            }
            validarManutencao(reserva);
            if (reserva.getProfessor() != null && reserva.getProfessor().temConflito(reserva.getInicio(), reserva.getFim())) {
                throw new ReservaCriacaoException("conflito professor");
            }
            reserva.setUsuarioSolicitante(solicitante);
            reserva.setEstado("SOLICITADA");
            reserva.setApprovalRequired(reserva.getRecurso().isRestrito());
            if (reserva.getId() == null) {
                reserva.setId((long) (reservas.size() + 1));
            }
            reservas.put(reserva.getId(), reserva);
            validadorSobreposicao.registrarReserva(reserva);
            if (reserva.getProfessor() != null) {
                reserva.getProfessor().adicionarAgenda(reserva.getInicio(), reserva.getFim());
            }
        }
        ValidadorAuditoria.registrarAuditoriaPendente(
                new Auditoria(reserva.getId(), solicitante.getUsername(), "CRIAR", "SOLICITADA"));
        return reserva;
    }

    public Reserva criarReservaComRecursos(Usuario solicitante, List<Recurso> recursos, LocalDateTime inicio, LocalDateTime fim) {
        if (recursos == null || recursos.isEmpty()) {
            throw new ReservaCriacaoException("Recurso não encontrado");
        }
        Reserva reserva = new Reserva();
        reserva.setRecurso(recursos.get(0));
        for (int indice = 1; indice < recursos.size(); indice++) {
            reserva.adicionarMaterial(recursos.get(indice));
        }
        reserva.setInicio(inicio);
        reserva.setFim(fim);
        return criarReserva(solicitante, reserva);
    }

    public Reserva criarReservaComProfessor(Usuario solicitante, Recurso sala, Professor professor, LocalDateTime inicio, LocalDateTime fim) {
        Reserva reserva = new Reserva();
        reserva.setRecurso(sala);
        reserva.setProfessor(professor);
        reserva.setInicio(inicio);
        reserva.setFim(fim);
        return criarReserva(solicitante, reserva);
    }

    public Reserva buscarPorId(Long id) {
        synchronized (secaoCritica) {
            return reservas.get(id);
        }
    }

    /** Recusa a reserva se sala ou material estiver em manutenção no período (RN-05). */
    private void validarManutencao(Reserva reserva) {
        List<Recurso> recursos = new ArrayList<>();
        recursos.add(reserva.getRecurso());
        recursos.addAll(reserva.getMateriais());
        for (Recurso recurso : recursos) {
            if (!validadorManutencao.verificarDisponibilidade(recurso, reserva.getInicio(), reserva.getFim())) {
                throw new ReservaCriacaoException(
                        "Recurso em manutenção no período solicitado: " + recurso.getNome());
            }
        }
    }

    private String tipoRecurso(Recurso recursoEmConflito, Reserva reserva) {
        Recurso recurso = recursoEmConflito != null ? recursoEmConflito : reserva.getRecurso();
        return recurso.getTipo() == Recurso.TipoRecurso.MATERIAL ? "material" : "sala";
    }
}
