package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TradutorErros: nunca expõe stack trace, SQL ou nome de classe (RNF-15/17)")
class TradutorErrosTest {

    private final TradutorErros tradutor = new TradutorErros();

    private static List<Function<String, RuntimeException>> excecoesDeRegraConhecidas() {
        return List.of(
                ReservaTemporalException::new,
                ReservaManutencaoException::new,
                ReservaCriacaoException::new,
                ReservaAprovacaoException::new,
                ReservaApagamentoException::new,
                ReservaFluxoEstadosException::new,
                ReservaAgendaProfessorException::new,
                ReservaMovimentacaoException::new,
                ReservaAuditoriaException::new,
                AcessoNegadoException::new,
                AutenticacaoException::new,
                IllegalArgumentException::new);
    }

    @ParameterizedTest
    @MethodSource("excecoesDeRegraConhecidas")
    @DisplayName("Exceções de regra conhecidas mantêm a mensagem original (quando segura)")
    void mantemMensagemDeRegraConhecida(Function<String, RuntimeException> construtor) {
        RuntimeException erro = construtor.apply("Mensagem de negócio clara");
        assertThat(tradutor.mensagemParaUsuario(erro)).isEqualTo("Mensagem de negócio clara");
    }

    @Test
    @DisplayName("ReservaSobreposicaoException mantém a mensagem original")
    void mantemMensagemDeSobreposicao() {
        assertThat(tradutor.mensagemParaUsuario(new ReservaSobreposicaoException("Conflito de horário")))
                .isEqualTo("Conflito de horário");
    }

    @Test
    @DisplayName("Exceção desconhecida (ex.: NullPointerException) recebe mensagem genérica")
    void excecaoDesconhecidaRecebeMensagemGenerica() {
        assertThat(tradutor.mensagemParaUsuario(new NullPointerException("detalhe interno sensível")))
                .isEqualTo("Não foi possível concluir a operação. Tente novamente mais tarde.");
    }

    @Test
    @DisplayName("Mensagem que cita 'Exception' é tratada como insegura, mesmo em exceção conhecida")
    void mensagemCitandoExceptionViraGenerica() {
        assertThat(tradutor.mensagemParaUsuario(new ReservaCriacaoException("Caused by SQLException: ...")))
                .isEqualTo("Não foi possível concluir a operação. Tente novamente mais tarde.");
    }

    @Test
    @DisplayName("Mensagem que cita 'java.' é tratada como insegura")
    void mensagemCitandoPacoteJavaViraGenerica() {
        assertThat(tradutor.mensagemParaUsuario(new ReservaCriacaoException("java.lang.NullPointerException")))
                .isEqualTo("Não foi possível concluir a operação. Tente novamente mais tarde.");
    }

    @Test
    @DisplayName("Mensagem nula ou em branco vira genérica, mesmo em exceção conhecida")
    void mensagemNulaOuEmBrancoViraGenerica() {
        assertThat(tradutor.mensagemParaUsuario(new ReservaCriacaoException(null)))
                .isEqualTo("Não foi possível concluir a operação. Tente novamente mais tarde.");
        assertThat(tradutor.mensagemParaUsuario(new ReservaCriacaoException("   ")))
                .isEqualTo("Não foi possível concluir a operação. Tente novamente mais tarde.");
    }

    @Test
    @DisplayName("Erro nulo recebe mensagem genérica")
    void erroNuloRecebeMensagemGenerica() {
        assertThat(tradutor.mensagemParaUsuario(null))
                .isEqualTo("Não foi possível concluir a operação. Tente novamente mais tarde.");
    }
}
