package com.organizacao_de_recursos.domain;

/**
 * Exceção lançada quando há violação de RN-03 (Agenda do Professor)
 */
public class ReservaAgendaProfessorException extends RuntimeException {
    public ReservaAgendaProfessorException(String message) {
        super(message);
    }

    public ReservaAgendaProfessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
