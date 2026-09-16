package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validador para RN-05: Indisponibilidade por Manutenção
 * Recursos em manutenção não podem ser reservados.
 */
public class ValidadorManutencao {
    private Map<Long, List<Manutencao>> manutencoesPorRecurso = new HashMap<>();

    /**
     * Registra um período de manutenção para um recurso
     */
    public void registrarManutencao(Recurso recurso, LocalDateTime inicio, LocalDateTime fim) {
        Long recursoId = recurso.getId();
        manutencoesPorRecurso.computeIfAbsent(recursoId, k -> new ArrayList<>())
                .add(new Manutencao(inicio, fim));
    }

    /**
     * Valida se a reserva tem conflito com período de manutenção
     *
     * @param reserva Reserva a validar
     * @throws ReservaManutencaoException se houver manutenção no período
     */
    public void validarManutencao(Reserva reserva) {
        if (reserva.getRecurso() == null) {
            return;
        }

        Long recursoId = reserva.getRecurso().getId();
        List<Manutencao> manutencoes = manutencoesPorRecurso.get(recursoId);

        if (manutencoes != null) {
            for (Manutencao m : manutencoes) {
                if (m.temSobreposicao(reserva.getInicio(), reserva.getFim())) {
                    throw new ReservaManutencaoException("Recurso em manutenção");
                }
            }
        }
    }

    public void validarAlteracaoManutencao(Reserva reserva, LocalDateTime novoInicio, LocalDateTime novoFim) {
        // Assinatura mínima sem regra de negócio (Fase RED TDD)
    }

    public boolean verificarDisponibilidade(Recurso recurso, LocalDateTime inicio, LocalDateTime fim) {
        // Assinatura mínima sem regra de negócio (Fase RED TDD)
        return false;
    }

    public void validarPeriodoManutencao(LocalDateTime inicio, LocalDateTime fim) {
        // Assinatura mínima sem regra de negócio (Fase RED TDD)
    }

    // Classe interna para representar manutenção
    private static class Manutencao {
        private LocalDateTime inicio;
        private LocalDateTime fim;

        public Manutencao(LocalDateTime inicio, LocalDateTime fim) {
            this.inicio = inicio;
            this.fim = fim;
        }

        public boolean temSobreposicao(LocalDateTime i, LocalDateTime f) {
            return inicio.isBefore(f) && fim.isAfter(i);
        }
    }
}
