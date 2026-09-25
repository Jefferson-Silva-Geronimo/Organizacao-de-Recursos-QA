package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RF-20: Notificação ou integração externa
 * Identifier: RF-20 | docs/prd.md:7.20 | E9: Notificações e Integrações
 *
 * Cobre somente a parte verificável do requisito: o evento concluído produz a notificação simulada e a
 * falha do canal (API que não responde) é registrada de forma segura e observável. Quais eventos notificam,
 * o canal real (WireMock), o destinatário e o conteúdo seguem pendentes (arquitetura J15).
 *
 * Casos de teste mapeados:
 * - T-RF20-001: Happy Path - Evento concluído produz a notificação simulada
 * - T-RF20-002: Invalid Input - Canal que não responde tem a falha registrada sem expor detalhes internos
 */
@DisplayName("RF-20: Notificação ou integração externa")
class NotificacaoReserva_RF20_Test {

    @Test
    @DisplayName("T-RF20-001: Happy Path - Evento concluído produz a notificação simulada")
    void eventoConcluidoDeveProduzirNotificacaoSimulada() {
        // Arrange
        NotificacaoSimulada canal = new NotificacaoSimulada();
        ServicoNotificacao servico = new ServicoNotificacao(canal);
        EventoNotificacao evento = new EventoNotificacao(2001L, "APROVADA");

        // Act
        ResultadoNotificacao resultado = servico.notificar(evento);

        // Assert
        assertThat(canal.enviados()).containsExactly(evento);
        assertThat(resultado.evento()).isEqualTo(evento);
        assertThat(resultado.entregue()).isTrue();
        assertThat(servico.registros()).containsExactly(resultado);
    }

    @Test
    @DisplayName("T-RF20-002: Invalid Input - Canal que não responde tem a falha registrada sem expor detalhes internos")
    void falhaDoCanalDeveSerRegistradaDeFormaSegura() {
        // Arrange - a API externa não responde; a exceção carrega detalhes internos que não podem vazar
        String detalheInterno = "Connection timed out: https://api-interna.exemplo/notificacoes?token=segredo123"
                + " em br.exemplo.ClienteHttp.enviar(ClienteHttp.java:42)";
        CanalNotificacao canalSemResposta = evento -> {
            throw new IllegalStateException(detalheInterno);
        };
        ServicoNotificacao servico = new ServicoNotificacao(canalSemResposta);
        EventoNotificacao evento = new EventoNotificacao(2002L, "APROVADA");

        // Act
        ResultadoNotificacao resultado = servico.notificar(evento);

        // Assert - falha observável no registro, sem propagar e sem detalhes internos
        assertThat(resultado.evento()).isEqualTo(evento);
        assertThat(resultado.entregue()).isFalse();
        assertThat(servico.registros()).containsExactly(resultado);
        assertThat(resultado.mensagem())
                .isNotBlank()
                .doesNotContain("segredo123", "https://", "ClienteHttp", "IllegalStateException", "timed out");
    }
}
