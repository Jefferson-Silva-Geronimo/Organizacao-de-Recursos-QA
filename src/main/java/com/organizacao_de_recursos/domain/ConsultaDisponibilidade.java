package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;

/**
 * Contrato mínimo (fase RED, RF-09): pesquisa de disponibilidade que combina reservas, manutenção,
 * bloqueios e agenda do professor. Sem lógica de negócio. Implementação pendente.
 */
public class ConsultaDisponibilidade {
    private final ServicoCriacaoReserva reservas;
    private final ValidadorManutencao manutencao;
    private final GestaoBloqueios bloqueios;

    public ConsultaDisponibilidade(ServicoCriacaoReserva reservas, ValidadorManutencao manutencao, GestaoBloqueios bloqueios) {
        this.reservas = reservas;
        this.manutencao = manutencao;
        this.bloqueios = bloqueios;
    }

    /** Informa se o recurso (e o professor, quando houver) pode ser reservado no período. */
    public boolean estaDisponivel(Recurso recurso, Professor professor, LocalDateTime inicio, LocalDateTime fim) {
        return true;
    }
}
