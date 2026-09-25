package com.organizacao_de_recursos.domain;

/**
 * Contrato mínimo (fase RED, RF-16 e RF-17): retirada e devolução de materiais pelo Responsável.
 * Sem lógica de negócio. Implementação pendente.
 */
public class MovimentacaoMateriais {

    /** Registra a retirada do material associado à reserva (somente Responsável). */
    public void registrarRetirada(Usuario usuario, Reserva reserva, Recurso material) {
        // não implementado
    }

    /** Registra a devolução do material; exige retirada correspondente (somente Responsável). */
    public void registrarDevolucao(Usuario usuario, Reserva reserva, Recurso material) {
        // não implementado
    }
}
