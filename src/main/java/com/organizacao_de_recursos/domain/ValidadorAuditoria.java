package com.organizacao_de_recursos.domain;

import java.util.*;

/**
 * Validador para RN-09: Auditoria de Mudança de Estado
 * Toda mudança de estado deve gerar registro de auditoria.
 */
public class ValidadorAuditoria {
    private Map<Long, List<Auditoria>> auditoriasPorReserva = new HashMap<>();
    private static final Map<Long, List<Auditoria>> auditoriasPendentes = new HashMap<>();

    static void registrarAuditoriaPendente(Auditoria auditoria) {
        synchronized (auditoriasPendentes) {
            auditoriasPendentes.computeIfAbsent(auditoria.getReservaId(), k -> new ArrayList<>()).add(auditoria);
        }
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
        auditoriasPorReserva.computeIfAbsent(reserva.getId(), k -> new ArrayList<>())
                .add(auditoria);
    }

    /**
     * Registra auditoria com estado anterior
     */
    public void registrarAuditoria(Reserva reserva, Usuario usuario, String acao, String estadoNovo, String estadoAnterior) {
        validarUsuarioAuditoria(usuario);
        Auditoria auditoria = new Auditoria(reserva.getId(), usuario.getUsername(), acao, estadoNovo, estadoAnterior);
        auditoriasPorReserva.computeIfAbsent(reserva.getId(), k -> new ArrayList<>())
                .add(auditoria);
    }

    /**
     * Retorna todas as auditorias de uma reserva
     */
    public List<Auditoria> obterAuditorias(Long reservaId) {
        if (!auditoriasPorReserva.containsKey(reservaId)) {
            synchronized (auditoriasPendentes) {
                List<Auditoria> pendentes = auditoriasPendentes.remove(reservaId);
                if (pendentes != null) {
                    auditoriasPorReserva.put(reservaId, pendentes);
                }
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
        auditoriasPorReserva.computeIfAbsent(reserva.getId(), k -> new ArrayList<>()).add(auditoria);
    }

    public List<Auditoria> obterAuditoriasOrdenadas(Long reservaId) {
        List<Auditoria> auditorias = new ArrayList<>(obterAuditorias(reservaId));
        auditorias.sort(java.util.Comparator.comparing(Auditoria::getTimestamp));
        return auditorias;
    }

    public void registrarTentativaApagamentoProibido(Reserva reserva, Usuario usuario) {
        registrarTentativaRecusada(reserva, usuario, "APAGAR", "Operação não permitida");
    }
}
