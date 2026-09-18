package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Validador para RN-02: Não Sobreposição do Mesmo Recurso
 * Reservas do mesmo recurso não podem se sobrepor.
 */
public class ValidadorSobreposicao {
    private List<Reserva> reservasRegistradas = new ArrayList<>();

    /**
     * Registra uma reserva como existente
     */
    public void registrarReserva(Reserva reserva) {
        reservasRegistradas.add(reserva);
    }

    /**
     * Valida se a nova reserva tem sobreposição com alguma existente
     *
     * @param novaReserva Reserva a validar
     * @throws ReservaSobreposicaoException se houver sobreposição
     */
    public void validarSobreposicao(Reserva novaReserva) {
        for (Reserva existente : reservasRegistradas) {
            // Verifica se são do mesmo recurso
            if (mesmoRecurso(existente, novaReserva)) {
                // Verifica sobreposição temporal
                if (temSobreposicao(existente, novaReserva)) {
                    String mensagem = mesmoPeriodo(existente, novaReserva)
                            ? "Recurso indisponível no período"
                            : "Conflito de horário";
                    throw new ReservaSobreposicaoException(mensagem);
                }
            }
        }
    }

    /**
     * Verifica se duas reservas tem sobreposição temporal
     * Dois períodos se sobrepõem se:
     * - inicio1 < fim2 AND fim1 > inicio2
     */
    private boolean temSobreposicao(Reserva r1, Reserva r2) {
        return r1.getInicio().isBefore(r2.getFim()) && r1.getFim().isAfter(r2.getInicio());
    }

    public void validarAlteracaoReserva(Reserva reserva, LocalDateTime novoInicio, LocalDateTime novoFim) {
        for (Reserva existente : reservasRegistradas) {
            if (existente == reserva || (existente.getId() != null && existente.getId().equals(reserva.getId()))) {
                continue;
            }
            if (mesmoRecurso(existente, reserva)
                    && temSobreposicao(existente, novoInicio, novoFim)) {
                throw new ReservaSobreposicaoException("Alteração causaria sobreposição");
            }
        }
    }

    private boolean mesmoRecurso(Reserva primeira, Reserva segunda) {
        return primeira.getRecurso() != null && segunda.getRecurso() != null
                && primeira.getRecurso().getId() != null
                && primeira.getRecurso().getId().equals(segunda.getRecurso().getId());
    }

    private boolean mesmoPeriodo(Reserva primeira, Reserva segunda) {
        return primeira.getInicio().equals(segunda.getInicio())
                && primeira.getFim().equals(segunda.getFim());
    }

    private boolean temSobreposicao(Reserva existente, LocalDateTime novoInicio, LocalDateTime novoFim) {
        return existente.getInicio().isBefore(novoFim) && existente.getFim().isAfter(novoInicio);
    }
}
