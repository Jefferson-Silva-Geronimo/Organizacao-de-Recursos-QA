package com.organizacao_de_recursos.domain;

import java.util.*;

/**
 * Validador para RN-09: Auditoria de Mudança de Estado
 * Toda mudança de estado deve gerar registro de auditoria.
 */
public class ValidadorAuditoria {
    // ConcurrentHashMap/CopyOnWriteArrayList: corrige race condition (RN-04) quando a mesma
    // instância é compartilhada entre threads - um HashMap comum pode perder entradas ou
    // corromper sua estrutura interna sob computeIfAbsent/add concorrentes, mesmo em chaves
    // distintas (rehash concorrente).
    private final Map<Long, List<Auditoria>> auditoriasPorReserva = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<Long, List<Auditoria>> auditoriasPendentes = new java.util.concurrent.ConcurrentHashMap<>();

    /** Sentinela para reserva ainda sem id persistido - ConcurrentHashMap não aceita chave nula. */
    private static final Long SEM_ID = -1L;

    static void registrarAuditoriaPendente(Auditoria auditoria) {
        Long chave = auditoria.getReservaId() != null ? auditoria.getReservaId() : SEM_ID;
        auditoriasPendentes.computeIfAbsent(chave, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(auditoria);
    }

    /**
     * Registra uma auditoria de mudança de estado
     *
     * @param reserva Reserva
     * @param usuario Usuário que fez a ação
     * @param acao    Ação realizada (CRIAR, APROVAR, etc.)
     * @param estadoNovo Novo estado
     */
    public void registrarAuditoria(Reserva reserva, Usuario usuario, String acao, String estadoNovo) {
        validarUsuarioAuditoria(usuario);
        Auditoria auditoria = new Auditoria(reserva.getId(), usuario.getUsername(), acao, estadoNovo);
        auditoriasPorReserva.computeIfAbsent(reserva.getId(), k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(auditoria);
    }

    /**
     * Registra auditoria com estado anterior
     */
    public void registrarAuditoria(Reserva reserva, Usuario usuario, String acao, String estadoNovo, String estadoAnterior) {
        validarUsuarioAuditoria(usuario);
        Auditoria auditoria = new Auditoria(reserva.getId(), usuario.getUsername(), acao, estadoNovo, estadoAnterior);
        auditoriasPorReserva.computeIfAbsent(reserva.getId(), k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(auditoria);
    }

    /**
     * Retorna todas as auditorias de uma reserva
     */
    public List<Auditoria> obterAuditorias(Long reservaId) {
        if (!auditoriasPorReserva.containsKey(reservaId)) {
            List<Auditoria> pendentes = auditoriasPendentes.remove(reservaId);
            if (pendentes != null) {
                auditoriasPorReserva.put(reservaId, pendentes);
            }
        }
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
        validarUsuarioAuditoria(usuario);
        String anterior = null;
        for (String transicao : transicoes) {
            if (anterior == null) {
                registrarAuditoria(reserva, usuario, "CRIAR", transicao);
            } else {
                registrarAuditoria(reserva, usuario, "ALTERAR_ESTADO", transicao, anterior);
            }
            anterior = transicao;
        }
    }

    public void validarUsuarioAuditoria(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não identificado");
        }
    }

    public void registrarTentativaRecusada(Reserva reserva, Usuario usuario, String operacao, String motivo) {
        validarUsuarioAuditoria(usuario);
        Auditoria auditoria = new Auditoria(reserva.getId(), usuario.getUsername(), operacao, "RECUSADA");
        auditoria.setDescricao(motivo);
        auditoriasPorReserva.computeIfAbsent(reserva.getId(), k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(auditoria);
    }

    /** Ordena por timestamp e desempata por id (ordem de inserção) quando o timestamp coincide (corrige RN-09/RN-04). */
    public List<Auditoria> obterAuditoriasOrdenadas(Long reservaId) {
        List<Auditoria> auditorias = new ArrayList<>(obterAuditorias(reservaId));
        auditorias.sort(java.util.Comparator.comparing(Auditoria::getTimestamp).thenComparing(Auditoria::getId));
        return auditorias;
    }

    public void registrarTentativaApagamentoProibido(Reserva reserva, Usuario usuario) {
        registrarTentativaRecusada(reserva, usuario, "APAGAR", "REJEITADO - Operação não permitida");
    }
}
