package com.organizacao_de_recursos.domain;

/**
 * Exceção lançada quando há violação de RN-02 (Não Sobreposição)
 */
public class ReservaSobreposicaoException extends RuntimeException {
    private final transient Recurso recursoEmConflito;

    public ReservaSobreposicaoException(String message) {
        super(message);
        this.recursoEmConflito = null;
    }

    public ReservaSobreposicaoException(String message, Recurso recursoEmConflito) {
        super(message);
        this.recursoEmConflito = recursoEmConflito;
    }

    public ReservaSobreposicaoException(String message, Throwable cause) {
        super(message, cause);
        this.recursoEmConflito = null;
    }

    /** Recurso (sala ou material) que causou o conflito, quando conhecido. */
    public Recurso getRecursoEmConflito() {
        return recursoEmConflito;
    }
}
