package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;

/**
 * Exceção lançada quando há violação da RN-01 (Ordem Temporal)
 */
public class ReservaTemporalException extends RuntimeException {
    public ReservaTemporalException(String message) {
        super(message);
    }

    public ReservaTemporalException(String message, Throwable cause) {
        super(message, cause);
    }
}
