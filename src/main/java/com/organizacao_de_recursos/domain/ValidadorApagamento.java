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
        if ("EM_USO".equals(reserva.getEstado()) || 
            "CONCLUIDA".equals(reserva.getEstado())) {
            throw new ReservaApagamentoException("Registros de reserva iniciada não podem ser removidos");
        }
        validarApagamento(reserva);
    }

    public void forcarCancelamento(Reserva reserva) {
        validarApagamento(reserva);
        reserva.setEstado("CANCELADA");
    }

    public void validarCancelamentoNoInicio(Reserva reserva, java.time.LocalDateTime momentoCancelamento) {
        if ("EM_USO".equals(reserva.getEstado())) {
            throw new ReservaApagamentoException("Reserva já iniciada");
        }
        validarApagamento(reserva);
    }

    public void apagarPorId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }
    }

    public void tentarApagarComAuditoria(Reserva reserva, Usuario usuario, ValidadorAuditoria auditoria) {
        auditoria.registrarTentativaApagamentoProibido(reserva, usuario);
        validarApagamento(reserva);
    }

    public void validarRejeicaoAposIniciada(Reserva reserva) {
        if ("EM_USO".equals(reserva.getEstado())) {
            throw new ReservaApagamentoException("Reserva já iniciada não pode ser alterada");
        }
    }
}
