package com.organizacao_de_recursos.domain;

/**
 * Exceção lançada quando o registro de retirada ou devolução de material viola uma regra (RF-16 e RF-17)
 */
public class ReservaMovimentacaoException extends RuntimeException {
    public ReservaMovimentacaoException(String message) {
        super(message);
    }
}
