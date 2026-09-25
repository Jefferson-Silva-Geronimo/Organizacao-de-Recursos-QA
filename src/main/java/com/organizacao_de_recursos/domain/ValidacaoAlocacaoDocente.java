package com.organizacao_de_recursos.domain;

/**
 * Contrato mínimo (fase RED, RF-15): validação da alocação de docentes pelo Responsável.
 * Sem lógica de negócio. Implementação pendente.
 */
public class ValidacaoAlocacaoDocente {

    /** Valida a alocação do professor da reserva; conflito com a agenda do professor impede a validação. */
    public void validar(Usuario responsavel, Reserva reserva) {
        // não implementado
    }

    /** Informa se a alocação da reserva foi validada. */
    public boolean foiValidada(Reserva reserva) {
        return false;
    }
}
