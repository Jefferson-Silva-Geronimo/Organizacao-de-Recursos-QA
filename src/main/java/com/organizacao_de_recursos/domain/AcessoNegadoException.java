package com.organizacao_de_recursos.domain;

/**
 * Exceção lançada quando acesso a funcionalidade é negado por perfil (RF-01)
 */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String message) {
        super(message);
    }
}
