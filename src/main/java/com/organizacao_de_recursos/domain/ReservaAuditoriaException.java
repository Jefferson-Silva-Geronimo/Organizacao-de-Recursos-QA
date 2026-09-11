package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;

/**
 * Exceção lançada quando há violação de RN-09 (Auditoria)
 */
public class ReservaAuditoriaException extends RuntimeException {
    public ReservaAuditoriaException(String message) {
        super(message);
    }

    public ReservaAuditoriaException(String message, Throwable cause) {
        super(message, cause);
    }
}
