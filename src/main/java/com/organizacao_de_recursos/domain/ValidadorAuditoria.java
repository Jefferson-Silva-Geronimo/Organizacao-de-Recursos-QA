package com.organizacao_de_recursos.domain;

import java.util.*;

/**
 * Validador para RN-09: Auditoria de Mudança de Estado
 * Toda mudança de estado deve gerar registro de auditoria.
 */
public class ValidadorAuditoria {
    private Map<Long, List<Auditoria>> auditoriasPorReserva = new HashMap<>();

    /**
     * Registra uma auditoria de mudança de estado
     *
     * @param reserva Reserva
     * @param usuario Usuário que fez a ação
     * @param acao    Ação realizada (CRIAR, APROVAR, etc.)
     * @param estadoNovo Novo estado
     */
    public void registrarAuditoria(Reserva reserva, Usuario usuario, String acao, String estadoNovo) {
        Auditoria auditoria = new Auditoria(reserva.getId(), usuario.getUsername(), acao, estadoNovo);
        auditoriasPorReserva.computeIfAbsent(reserva.getId(), k -> new ArrayList<>())
                .add(auditoria);
    }

    /**
     * Registra auditoria com estado anterior
     */
    public void registrarAuditoria(Reserva reserva, Usuario usuario, String acao, String estadoNovo, String estadoAnterior) {
        Auditoria auditoria = new Auditoria(reserva.getId(), usuario.getUsername(), acao, estadoNovo, estadoAnterior);
        auditoriasPorReserva.computeIfAbsent(reserva.getId(), k -> new ArrayList<>())
                .add(auditoria);
    }

    /**
     * Retorna todas as auditorias de uma reserva
     */
    public List<Auditoria> obterAuditorias(Long reservaId) {
        return auditoriasPorReserva.getOrDefault(reservaId, new ArrayList<>());
    }

    /**
     * Tenta editar auditoria (não permitido)
     */
    public void editarAuditoria(Long reservaId, int indice) {
        throw new ReservaAuditoriaException("Auditoria é imutável");
    }

    /**
     * Tenta apagar auditoria (não permitido)
     */
    public void apagarAuditoria(Long reservaId) {
        throw new ReservaAuditoriaException("Auditoria não pode ser removida");
    }

    public void registrarMudancasEmSequencia(Reserva reserva, Usuario usuario, List<String> transicoes) {
        // Assinatura mínima sem regra de negócio (Fase RED TDD)
    }

    public void validarUsuarioAuditoria(Usuario usuario) {
        // Assinatura mínima sem regra de negócio (Fase RED TDD)
    }

    public void registrarTentativaRecusada(Reserva reserva, Usuario usuario, String operacao, String motivo) {
        // Assinatura mínima sem regra de negócio (Fase RED TDD)
    }

    public List<Auditoria> obterAuditoriasOrdenadas(Long reservaId) {
        // Assinatura mínima sem regra de negócio (Fase RED TDD)
        return Collections.emptyList();
    }

    public void registrarTentativaApagamentoProibido(Reserva reserva, Usuario usuario) {
        // Assinatura mínima sem regra de negócio (Fase RED TDD)
    }
}
