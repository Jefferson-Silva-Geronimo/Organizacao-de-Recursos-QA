package com.organizacao_de_recursos.domain;

/**
 * Componente Erros (RF-22): mensagem apresentada ao usuário para uma recusa ou falha.
 *
 * Recusas de regra conhecidas (período inválido, conflito, manutenção, falta de autorização e demais
 * exceções de regra do domínio) mantêm a mensagem, que identifica a causa observável. Qualquer outra
 * falha recebe uma mensagem genérica: stack trace, credenciais, causa encadeada e demais detalhes internos
 * nunca são expostos (RNF-15 e RNF-17).
 */
public class TradutorErros {
    private static final String MENSAGEM_GENERICA = "Não foi possível concluir a operação. Tente novamente mais tarde.";

    public String mensagemParaUsuario(Throwable erro) {
        if (erro != null && ehRecusaDeRegra(erro) && mensagemSegura(erro.getMessage())) {
            return erro.getMessage();
        }
        return MENSAGEM_GENERICA;
    }

    private boolean ehRecusaDeRegra(Throwable erro) {
        return erro instanceof ReservaTemporalException
                || erro instanceof ReservaSobreposicaoException
                || erro instanceof ReservaCriacaoException
                || erro instanceof ReservaManutencaoException
                || erro instanceof ReservaAprovacaoException
                || erro instanceof ReservaApagamentoException
                || erro instanceof ReservaFluxoEstadosException
                || erro instanceof ReservaAgendaProfessorException
                || erro instanceof ReservaMovimentacaoException
                || erro instanceof ReservaAuditoriaException
                || erro instanceof AcessoNegadoException
                || erro instanceof AutenticacaoException
                || erro.getClass() == IllegalArgumentException.class;
    }

    /** Uma mensagem que cite exceção, classe ou arquivo Java não é apresentada ao usuário. */
    private boolean mensagemSegura(String mensagem) {
        return mensagem != null && !mensagem.isBlank()
                && !mensagem.contains("Exception") && !mensagem.contains("java.") && !mensagem.contains(".java");
    }
}
