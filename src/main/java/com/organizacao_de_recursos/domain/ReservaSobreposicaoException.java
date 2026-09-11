package com.organizacao_de_recursos.domain;

/**
 * Exceção lançada quando há violação de RN-02 (Não Sobreposição)
 */
public class ReservaSobreposicaoException extends RuntimeException {
    public ReservaSobreposicaoException(String message) {
        super(message);
    }

    public ReservaSobreposicaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
