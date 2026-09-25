package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bloqueios de recursos criados pelo Administrador (RF-06).
 *
 * Exige a permissão GERENCIAR_RECURSOS (ADR-003). O recurso fica indisponível no período do bloqueio.
 * Os bloqueios ficam em memória: a persistência real não faz parte deste componente.
 */
public class GestaoBloqueios {
    private final ValidadorAutorizacao autorizacao = new ValidadorAutorizacao();
    private final Map<Long, List<LocalDateTime[]>> bloqueiosPorRecurso = new HashMap<>();

    /** Registra um bloqueio do recurso no período (somente Administrador). */
    public synchronized void registrarBloqueio(Usuario usuario, Recurso recurso, LocalDateTime inicio, LocalDateTime fim) {
        autorizacao.validarAcesso(usuario, "GERENCIAR_RECURSOS");
        if (recurso == null) {
            throw new IllegalArgumentException("Recurso não encontrado");
        }
        validarPeriodo(inicio, fim);
        bloqueiosPorRecurso.computeIfAbsent(recurso.getId(), id -> new ArrayList<>())
                .add(new LocalDateTime[] {inicio, fim});
    }

    /** Informa se o recurso está livre de bloqueio no período. */
    public synchronized boolean estaDisponivel(Recurso recurso, LocalDateTime inicio, LocalDateTime fim) {
        for (LocalDateTime[] bloqueio : bloqueiosPorRecurso.getOrDefault(recurso.getId(), List.of())) {
            if (bloqueio[0].isBefore(fim) && bloqueio[1].isAfter(inicio)) {
                return false;
            }
        }
        return true;
    }

    private void validarPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null) {
            throw new IllegalArgumentException("Início é obrigatório");
        }
        if (fim == null) {
            throw new IllegalArgumentException("Fim é obrigatório");
        }
        if (fim.isBefore(inicio)) {
            throw new ReservaTemporalException("Fim anterior ao início");
        }
        if (fim.isEqual(inicio)) {
            throw new ReservaTemporalException("Duração inválida");
        }
    }
}
