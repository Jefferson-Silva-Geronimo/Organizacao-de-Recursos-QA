package com.organizacao_de_recursos.domain;

/**
 * Exceção lançada quando há violação de RN-06 (Aprovação)
 */
public class ReservaAprovacaoException extends RuntimeException {
    public ReservaAprovacaoException(String message) {
        super(message);
    }

    public ReservaAprovacaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
