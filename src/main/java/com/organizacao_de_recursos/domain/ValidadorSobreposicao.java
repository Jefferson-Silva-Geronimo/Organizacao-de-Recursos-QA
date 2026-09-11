package com.organizacao_de_recursos.domain;

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
            if (existente.getRecurso().getId().equals(novaReserva.getRecurso().getId())) {
                // Verifica sobreposição temporal
                if (temSobreposicao(existente, novaReserva)) {
                    throw new ReservaSobreposicaoException("Recurso indisponível no período");
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
}
