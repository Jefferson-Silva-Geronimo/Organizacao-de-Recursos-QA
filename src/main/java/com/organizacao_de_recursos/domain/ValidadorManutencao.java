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
        validarPeriodoManutencao(inicio, fim);
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
                        String mensagem = reserva.getRecurso().getTipo() == Recurso.TipoRecurso.MATERIAL
                            ? "Material indisponível"
                            : reservaContidaNaManutencao(m, reserva.getInicio(), reserva.getFim())
                            ? "Recurso em manutenção"
                            : "Recurso indisponível no período";
                    throw new ReservaManutencaoException(mensagem);
                }
            }
        }
    }

    public void validarAlteracaoManutencao(Reserva reserva, LocalDateTime novoInicio, LocalDateTime novoFim) {
        if (reserva.getRecurso() == null) {
            return;
        }
        List<Manutencao> manutencoes = manutencoesPorRecurso.get(reserva.getRecurso().getId());
        if (manutencoes != null) {
            for (Manutencao manutencao : manutencoes) {
                if (manutencao.temSobreposicao(novoInicio, novoFim)) {
                    throw new ReservaManutencaoException("Sala em manutenção neste período");
                }
            }
        }
    }

    public boolean verificarDisponibilidade(Recurso recurso, LocalDateTime inicio, LocalDateTime fim) {
        List<Manutencao> manutencoes = manutencoesPorRecurso.get(recurso.getId());
        if (manutencoes == null) {
            return true;
        }
        for (Manutencao manutencao : manutencoes) {
            if (manutencao.temSobreposicao(inicio, fim)) {
                return false;
            }
        }
        return true;
    }

    public void validarPeriodoManutencao(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null) {
            throw new ReservaTemporalException("Período de manutenção obrigatório");
        }
        if (fim.isBefore(inicio)) {
            throw new ReservaTemporalException("Fim anterior ao início");
        }
        if (fim.isEqual(inicio)) {
            throw new ReservaTemporalException("Duração inválida");
        }
    }

    private boolean reservaContidaNaManutencao(Manutencao manutencao, LocalDateTime inicio, LocalDateTime fim) {
        return !inicio.isBefore(manutencao.inicio) && !fim.isAfter(manutencao.fim);
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
