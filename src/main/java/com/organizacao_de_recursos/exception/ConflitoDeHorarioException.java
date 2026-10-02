package com.organizacao_de_recursos.exception;

import com.organizacao_de_recursos.domain.ReservaSobreposicaoException;

/**
 * Violação da constraint de exclusão do banco (SQLState 23P01) traduzida para uma mensagem segura
 * (seção 5 do plano de migração; HTTP 409). A garantia de RN-04 é do banco; esta exceção só
 * comunica o resultado ao chamador.
 */
public class ConflitoDeHorarioException extends ReservaSobreposicaoException {
    public ConflitoDeHorarioException(String message) {
        super(message);
    }
}
