package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;

/**
 * Contrato mínimo (fase RED, RF-06): bloqueios de recursos criados pelo Administrador.
 * Sem lógica de negócio. Implementação pendente.
 */
public class GestaoBloqueios {

    /** Registra um bloqueio do recurso no período (somente Administrador). */
    public void registrarBloqueio(Usuario usuario, Recurso recurso, LocalDateTime inicio, LocalDateTime fim) {
        // não implementado
    }

    /** Informa se o recurso está livre de bloqueio no período. */
    public boolean estaDisponivel(Recurso recurso, LocalDateTime inicio, LocalDateTime fim) {
        return true;
    }
}
