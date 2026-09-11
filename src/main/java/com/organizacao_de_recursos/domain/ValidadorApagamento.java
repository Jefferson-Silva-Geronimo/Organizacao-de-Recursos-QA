package com.organizacao_de_recursos.domain;

/**
 * Validador para RN-08: Reserva Iniciada Não Pode Ser Apagada
 * Reservas iniciadas não podem ser apagadas.
 */
public class ValidadorApagamento {

    /**
     * Valida se reserva pode ser apagada/cancelada
     *
     * @param reserva Reserva a validar
     * @throws ReservaApagamentoException se reserva está iniciada
     */
    public void validarApagamento(Reserva reserva) {
        if ("EM_USO".equals(reserva.getEstado())) {
            throw new ReservaApagamentoException("Reserva em uso não pode ser cancelada");
        }

        if ("CONCLUIDA".equals(reserva.getEstado())) {
            throw new ReservaApagamentoException("Operação não permitida para reserva concluída");
        }
    }

    /**
     * Valida apagamento por admin (mesmo admin não pode contornar)
     */
    public void validarApagamentoAdmin(Reserva reserva, Usuario admin) {
        validarApagamento(reserva);
        
        // Admin também não pode remover registros iniciados
        if ("EM_USO".equals(reserva.getEstado()) || 
            "CONCLUIDA".equals(reserva.getEstado())) {
            throw new ReservaApagamentoException("Registros de reserva iniciada não podem ser removidos");
        }
    }
}
