package com.organizacao_de_recursos.exception;

import com.organizacao_de_recursos.domain.ReservaFluxoEstadosException;

/**
 * Transição de estado inválida na camada persistida (seção 3 do plano de migração; HTTP 409).
 * Estende a exceção de domínio equivalente para reaproveitar o tratamento seguro de mensagens
 * já existente em {@code TradutorErros}.
 */
public class TransicaoInvalidaException extends ReservaFluxoEstadosException {
    public TransicaoInvalidaException(String message) {
        super(message);
    }
}
