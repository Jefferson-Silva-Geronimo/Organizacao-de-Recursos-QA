package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Validador para RN-02: Não Sobreposição do Mesmo Recurso
 * Reservas do mesmo recurso (sala ou material anexado à reserva) não podem se sobrepor.
 */
public class ValidadorSobreposicao {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final List<Reserva> reservasRegistradas = new ArrayList<>();

    /**
     * Registra uma reserva como existente
     */
    public synchronized void registrarReserva(Reserva reserva) {
        reservasRegistradas.add(reserva);
    }

    /**
     * Valida se a nova reserva tem sobreposição com alguma existente
     *
     * @param novaReserva Reserva a validar
     * @throws ReservaSobreposicaoException se houver sobreposição
     */
    public synchronized void validarSobreposicao(Reserva novaReserva) {
        for (Reserva existente : reservasRegistradas) {
            Recurso emConflito = recursoEmComum(existente, novaReserva);
            // Verifica se há recurso em comum e sobreposição temporal
            if (emConflito != null && temSobreposicao(existente, novaReserva)) {
                String mensagem = mesmoPeriodo(existente, novaReserva)
                        ? "Recurso indisponível no período"
                        : "Conflito de horário";
                String periodoExistente = HORA.format(existente.getInicio()) + "-" + HORA.format(existente.getFim());
                throw new ReservaSobreposicaoException(
                        mensagem + ". Conflita com reserva " + periodoExistente, emConflito);
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

    public synchronized void validarAlteracaoReserva(Reserva reserva, LocalDateTime novoInicio, LocalDateTime novoFim) {
        for (Reserva existente : reservasRegistradas) {
            if (existente == reserva || (existente.getId() != null && existente.getId().equals(reserva.getId()))) {
                continue;
            }
            if (recursoEmComum(existente, reserva) != null
                    && temSobreposicao(existente, novoInicio, novoFim)) {
                throw new ReservaSobreposicaoException("Alteração causaria sobreposição");
            }
        }
    }

    /** Primeiro recurso (sala ou material) que as duas reservas têm em comum, ou null. */
    private Recurso recursoEmComum(Reserva primeira, Reserva segunda) {
        for (Recurso recursoPrimeira : recursosDe(primeira)) {
            for (Recurso recursoSegunda : recursosDe(segunda)) {
                if (recursoPrimeira.getId() != null && recursoPrimeira.getId().equals(recursoSegunda.getId())) {
                    return recursoSegunda;
                }
            }
        }
        return null;
    }

    private List<Recurso> recursosDe(Reserva reserva) {
        List<Recurso> recursos = new ArrayList<>();
        if (reserva.getRecurso() != null) {
            recursos.add(reserva.getRecurso());
        }
        recursos.addAll(reserva.getMateriais());
        return recursos;
    }

    private boolean mesmoPeriodo(Reserva primeira, Reserva segunda) {
        return primeira.getInicio().equals(segunda.getInicio())
                && primeira.getFim().equals(segunda.getFim());
    }

    private boolean temSobreposicao(Reserva existente, LocalDateTime novoInicio, LocalDateTime novoFim) {
        return existente.getInicio().isBefore(novoFim) && existente.getFim().isAfter(novoInicio);
    }
}
