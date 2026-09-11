package com.organizacao_de_recursos.domain;

/**
 * Exceção lançada quando há violação de RN-07 (Fluxo de Estados)
 */
public class ReservaFluxoEstadosException extends RuntimeException {
    public ReservaFluxoEstadosException(String message) {
        super(message);
    }

    public ReservaFluxoEstadosException(String message, Throwable cause) {
        super(message, cause);
    }
}
