package com.organizacao_de_recursos.domain;

/**
 * Exceção lançada quando a criação de reserva falha por violação de regra (RF-10)
 */
public class ReservaCriacaoException extends RuntimeException {
    public ReservaCriacaoException(String message) {
        super(message);
    }

    public ReservaCriacaoException(String message, Throwable cause) {
        super(message, cause);
    }
}

