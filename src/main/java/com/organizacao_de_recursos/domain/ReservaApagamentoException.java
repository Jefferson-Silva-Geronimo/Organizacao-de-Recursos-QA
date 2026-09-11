package com.organizacao_de_recursos.domain;

/**
 * Exceção lançada quando há violação de RN-08 (Apagamento)
 */
public class ReservaApagamentoException extends RuntimeException {
    public ReservaApagamentoException(String message) {
        super(message);
    }

    public ReservaApagamentoException(String message, Throwable cause) {
        super(message, cause);
    }
}
