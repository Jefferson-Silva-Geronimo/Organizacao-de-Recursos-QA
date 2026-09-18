package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de Criação de Reserva para RF-10
 * Assinatura mínima sem lógica de negócio (Fase RED TDD).
 */
public class ServicoCriacaoReserva {
    private final Map<Long, Reserva> reservas = new HashMap<>();
    private final ValidadorSobreposicao validadorSobreposicao = new ValidadorSobreposicao();

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
        try {
            validadorSobreposicao.validarSobreposicao(reserva);
        } catch (ReservaSobreposicaoException e) {
            throw new ReservaCriacaoException("conflito em " + tipoRecurso(reserva.getRecurso()), e);
        }
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
        return reservas.get(id);
    }

    private String tipoRecurso(Recurso recurso) {
        return recurso.getTipo() == Recurso.TipoRecurso.MATERIAL ? "material" : "sala";
    }
}

