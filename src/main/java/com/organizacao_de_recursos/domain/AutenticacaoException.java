package com.organizacao_de_recursos.domain;

/**
 * Exceção lançada quando autenticação falha (token inválido/expirado/ausente) (RF-01)
 */
public class AutenticacaoException extends RuntimeException {
    public AutenticacaoException(String message) {
        super(message);
    }
}
