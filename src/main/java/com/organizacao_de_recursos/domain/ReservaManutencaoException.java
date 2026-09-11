package com.organizacao_de_recursos.domain;

/**
 * Exceção lançada quando há violação de RN-05 (Manutenção)
 */
public class ReservaManutencaoException extends RuntimeException {
    public ReservaManutencaoException(String message) {
        super(message);
    }

    public ReservaManutencaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
