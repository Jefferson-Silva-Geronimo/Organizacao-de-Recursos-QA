package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RF-22: Interface responsiva e erros compreensíveis
 * Identifier: RF-22 | docs/prd.md:7.22 | E11: Interface e Erros
 *
 * Cobre o critério de aceitação 2 (mensagem que identifica a causa observável e não expõe informação
 * sensível: stack trace, credenciais ou detalhes internos - docs/fluxos-personas.md FLX-01, RNF-15 e RNF-17).
 * O critério 1 (viewports) segue bloqueado: não existe interface e os viewports não foram definidos.
 *
 * Casos de teste mapeados:
 * - T-RF22-001: [BLOQUEADO_POR_LACUNA] responsividade em viewports
 * - T-RF22-002: Recusas conhecidas informam a causa; falha interna não expõe detalhes
 */
@DisplayName("RF-22: Interface responsiva e erros compreensíveis")
class ErrosCompreensiveis_RF22_Test {

    private static final LocalDateTime INICIO = LocalDateTime.now().plusDays(10)
            .withHour(8).withMinute(0).withSecond(0).withNano(0);

    enum CausaDeRecusa {
        PERIODO_INVALIDO("Fim anterior ao início"),
        CONFLITO("conflito"),
        MANUTENCAO("manutenção"),
        FALTA_DE_AUTORIZACAO("Acesso negado");

        final String causaObservavel;

        CausaDeRecusa(String causaObservavel) {
            this.causaObservavel = causaObservavel;
        }
    }

    private RuntimeException recusaReal(CausaDeRecusa causa) {
        Usuario solicitante = new Usuario(1L, "solicitante", Usuario.Perfil.SOLICITANTE);
        Recurso sala = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorManutencao manutencao = new ValidadorManutencao();
        ServicoCriacaoReserva servico = new ServicoCriacaoReserva(manutencao);
        Throwable recusa = catchThrowable(() -> {
            switch (causa) {
                case PERIODO_INVALIDO -> new Reserva().validarTemporalidade(INICIO.plusHours(1), INICIO);
                case CONFLITO -> {
                    servico.criarReserva(new Usuario(2L, "outro", Usuario.Perfil.SOLICITANTE), reservaEm(sala));
                    servico.criarReserva(solicitante, reservaEm(sala));
                }
                case MANUTENCAO -> {
                    manutencao.registrarManutencao(sala, INICIO, INICIO.plusHours(1));
                    servico.criarReserva(solicitante, reservaEm(sala));
                }
                case FALTA_DE_AUTORIZACAO -> new ValidadorAutorizacao().validarAcesso(solicitante, "GERENCIAR_USUARIOS");
            }
        });
        return (RuntimeException) recusa;
    }

    private Reserva reservaEm(Recurso sala) {
        Reserva reserva = new Reserva();
        reserva.setRecurso(sala);
        reserva.setInicio(INICIO);
        reserva.setFim(INICIO.plusHours(1));
        return reserva;
    }

    @ParameterizedTest(name = "recusa por {0} informa a causa observável")
    @EnumSource(CausaDeRecusa.class)
    @DisplayName("T-RF22-002: Recusas conhecidas informam a causa observável sem expor informação sensível")
    void recusaConhecidaDeveInformarCausaSemExporDetalhes(CausaDeRecusa causa) {
        // Arrange
        RuntimeException recusa = recusaReal(causa);
        TradutorErros tradutor = new TradutorErros();

        // Act
        String mensagem = tradutor.mensagemParaUsuario(recusa);

        // Assert
        assertThat(recusa).as("a recusa real foi provocada").isNotNull();
        assertThat(mensagem).contains(causa.causaObservavel);
        assertThat(mensagem).doesNotContain("Exception", "java.", ".java", "at com.");
    }

    @Test
    @DisplayName("T-RF22-002: Falha interna e causa encadeada não expõem informação sensível")
    void falhaInternaNaoDeveExporDetalhesInternos() {
        // Arrange
        String detalheInterno = "jdbc:postgresql://db-interno:5432/reservas user=app password=segredo123";
        RuntimeException falhaInterna = new NullPointerException(
                "Cannot invoke Usuario.getPerfil() because u is null; " + detalheInterno);
        RuntimeException recusaComCausaInterna =
                new ReservaCriacaoException("conflito em sala", new RuntimeException(detalheInterno));
        TradutorErros tradutor = new TradutorErros();

        // Act
        String mensagemDaFalha = tradutor.mensagemParaUsuario(falhaInterna);
        String mensagemDaRecusa = tradutor.mensagemParaUsuario(recusaComCausaInterna);

        // Assert - falha interna: mensagem genérica sem detalhes; recusa conhecida: só a causa observável
        assertThat(mensagemDaFalha).isNotBlank()
                .doesNotContain("jdbc", "segredo123", "db-interno", "NullPointerException", "getPerfil");
        assertThat(mensagemDaRecusa).contains("conflito em sala")
                .doesNotContain("jdbc", "segredo123", "db-interno");
    }
}
