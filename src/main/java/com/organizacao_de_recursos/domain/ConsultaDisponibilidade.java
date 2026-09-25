package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;

/**
 * Pesquisa de disponibilidade (RF-09): combina reservas, manutenção, bloqueios e agenda do professor.
 * Um recurso reservado, bloqueado, em manutenção ou cujo professor tem conflito de agenda no período
 * não é apresentado como disponível.
 *
 * A pesquisa por filtros (tipo, capacidade, localização e competência) não faz parte deste componente.
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
        if (recurso == null) {
            throw new IllegalArgumentException("Recurso não encontrado");
        }
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Período é obrigatório");
        }
        return !reservas.possuiConflito(recurso, inicio, fim)
                && manutencao.verificarDisponibilidade(recurso, inicio, fim)
                && bloqueios.estaDisponivel(recurso, inicio, fim)
                && (professor == null || !professor.temConflito(inicio, fim));
    }
}
