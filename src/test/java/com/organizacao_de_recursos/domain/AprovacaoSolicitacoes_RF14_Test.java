package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.SoftAssertions;

import java.time.LocalDateTime;

/**
 * Testes para RF-14: Aprovação de solicitações especiais
 * Identifier: RF-14 | docs/prd.md:7.14 | E5: Aprovação de Recursos Restritos
 *
 * Casos de teste mapeados:
 * - T-RF14-001: Happy Path - Responsável aprova solicitação pendente (estado APROVADA e auditoria)
 * - T-RF14-002: DUPLICADO de T-RN06-004 e T-RN06-005 (Solicitante ou Administrador não aprovam)
 * - T-RF14-003: Happy Path - Responsável rejeita solicitação (estado REJEITADA e auditoria)
 */
@DisplayName("RF-14: Aprovação de solicitações especiais")
class AprovacaoSolicitacoes_RF14_Test {

    private static final LocalDateTime DIA_08H = LocalDateTime.now().plusDays(8)
            .withHour(8).withMinute(0).withSecond(0).withNano(0);

    private Reserva solicitacaoPendente(long id) {
        Recurso salaRestrita = new Recurso(2L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Reserva reserva = new Reserva();
        reserva.setId(id);
        reserva.setRecurso(salaRestrita);
        reserva.setInicio(DIA_08H);
        reserva.setFim(DIA_08H.plusHours(1));
        Usuario solicitante = new Usuario(1L, "solicitante", Usuario.Perfil.SOLICITANTE);
        return new ServicoCriacaoReserva().criarReserva(solicitante, reserva);
    }

    @Test
    @DisplayName("T-RF14-001: Happy Path - Responsável aprova solicitação especial pendente")
    void responsavelDeveAprovarSolicitacaoEspecialERegistrarMudanca() {
        // Arrange
        Reserva reserva = solicitacaoPendente(1401L);
        Usuario responsavel = new Usuario(2L, "responsavel", Usuario.Perfil.RESPONSAVEL);
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act
        validador.aprovar(reserva, responsavel);

        // Assert - estado APROVADA e mudança de estado registrada
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(reserva.getEstado()).as("estado após aprovação").isEqualTo("APROVADA");
            soft.assertThat(new ValidadorAuditoria().obterAuditorias(1401L))
                    .as("auditoria da aprovação")
                    .anyMatch(a -> "APROVADA".equals(a.getEstadoNovo()) && "responsavel".equals(a.getUsuario()));
        });
    }

    @Test
    @DisplayName("T-RF14-003: Happy Path - Responsável rejeita solicitação especial")
    void responsavelDeveRejeitarSolicitacaoEspecialERegistrarMudanca() {
        // Arrange
        Reserva reserva = solicitacaoPendente(1403L);
        Usuario responsavel = new Usuario(2L, "responsavel", Usuario.Perfil.RESPONSAVEL);
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act
        validador.rejeitar(reserva, responsavel);

        // Assert - estado REJEITADA e mudança de estado registrada
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(reserva.getEstado()).as("estado após rejeição").isEqualTo("REJEITADA");
            soft.assertThat(new ValidadorAuditoria().obterAuditorias(1403L))
                    .as("auditoria da rejeição")
                    .anyMatch(a -> "REJEITADA".equals(a.getEstadoNovo()) && "responsavel".equals(a.getUsuario()));
        });
    }
}
