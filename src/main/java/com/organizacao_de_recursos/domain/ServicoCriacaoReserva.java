package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço de Criação de Reserva para RF-10
 * Assinatura mínima sem lógica de negócio (Fase RED TDD).
 */
public class ServicoCriacaoReserva {

    public Reserva criarReserva(Usuario solicitante, Reserva reserva) {
        return null;
    }

    public Reserva criarReservaComRecursos(Usuario solicitante, List<Recurso> recursos, LocalDateTime inicio, LocalDateTime fim) {
        return null;
    }

    public Reserva criarReservaComProfessor(Usuario solicitante, Recurso sala, Professor professor, LocalDateTime inicio, LocalDateTime fim) {
        return null;
    }

    public Reserva buscarPorId(Long id) {
        return null;
    }
}
