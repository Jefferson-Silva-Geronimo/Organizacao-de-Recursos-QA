package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;

/**
 * Contrato mínimo (fase RED, RF-11 e RF-12): alteração e cancelamento de reservas pelo Solicitante.
 * Sem lógica de negócio. Implementação pendente.
 */
public class ServicoGestaoReserva {
    private final ServicoCriacaoReserva reservas;
    private final ValidadorManutencao manutencao;

    public ServicoGestaoReserva(ServicoCriacaoReserva reservas, ValidadorManutencao manutencao) {
        this.reservas = reservas;
        this.manutencao = manutencao;
    }

    /** Altera o período da reserva do próprio solicitante. */
    public void alterarReserva(Usuario solicitante, Reserva reserva, LocalDateTime novoInicio, LocalDateTime novoFim) {
        // não implementado
    }

    /** Cancela a reserva do próprio solicitante. */
    public void cancelarReserva(Usuario usuario, Reserva reserva) {
        // não implementado
    }
}
