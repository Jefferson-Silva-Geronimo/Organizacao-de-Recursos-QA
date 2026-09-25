package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.assertj.core.api.SoftAssertions;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RF-12: Cancelamento de reservas
 * Identifier: RF-12 | docs/prd.md:7.12 | E4: Reservas e Agenda
 *
 * Casos de teste mapeados:
 * - T-RF12-001: Happy Path - Solicitante cancela a própria reserva não iniciada
 * - T-RF12-002: Forbidden State - Cancelamento recusado (reserva iniciada ou de outro Solicitante)
 */
@DisplayName("RF-12: Cancelamento de reservas")
class CancelamentoReserva_RF12_Test {

    private static final LocalDateTime DIA_08H = LocalDateTime.now().plusDays(7)
            .withHour(8).withMinute(0).withSecond(0).withNano(0);

    private final Usuario dono = new Usuario(1L, "dono", Usuario.Perfil.SOLICITANTE);
    private final Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
    private final ValidadorManutencao manutencao = new ValidadorManutencao();
    private final ServicoCriacaoReserva criacao = new ServicoCriacaoReserva(manutencao);
    private final ServicoGestaoReserva gestao = new ServicoGestaoReserva(criacao, manutencao);

    private Reserva reservaDoDono(long id) {
        Reserva reserva = new Reserva();
        reserva.setId(id);
        reserva.setRecurso(salaA);
        reserva.setInicio(DIA_08H);
        reserva.setFim(DIA_08H.plusHours(1));
        return criacao.criarReserva(dono, reserva);
    }

    @Test
    @DisplayName("T-RF12-001: Happy Path - Solicitante cancela a própria reserva ainda não iniciada")
    void solicitanteDeveCancelarPropriaReservaNaoIniciada() {
        // Arrange
        Reserva reserva = reservaDoDono(1201L);

        // Act
        gestao.cancelarReserva(dono, reserva);

        // Assert - estado CANCELADA e mudança registrada em auditoria
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(reserva.getEstado()).as("estado após cancelamento").isEqualTo("CANCELADA");
            soft.assertThat(new ValidadorAuditoria().obterAuditorias(1201L))
                    .as("auditoria da mudança de estado")
                    .anyMatch(a -> "CANCELADA".equals(a.getEstadoNovo()) && "dono".equals(a.getUsuario()));
        });
    }

    enum Recusa { RESERVA_INICIADA, RESERVA_DE_OUTRO_SOLICITANTE }

    @ParameterizedTest(name = "cancelamento recusado: {0}")
    @EnumSource(Recusa.class)
    @DisplayName("T-RF12-002: Forbidden State - Cancelamento recusado com motivo informado")
    void cancelamentoNaoPermitidoDeveSerRecusadoComMotivo(Recusa recusa) {
        // Arrange
        Reserva reserva = reservaDoDono(1202L + recusa.ordinal());
        Usuario quemCancela = dono;
        if (recusa == Recusa.RESERVA_INICIADA) {
            reserva.setEstado("EM_USO");
        } else {
            quemCancela = new Usuario(2L, "outro", Usuario.Perfil.SOLICITANTE);
        }
        Usuario solicitanteDoCancelamento = quemCancela;
        String estadoAntes = reserva.getEstado();

        // Act & Assert
        assertThatThrownBy(() -> gestao.cancelarReserva(solicitanteDoCancelamento, reserva))
                .isInstanceOf(RuntimeException.class)
                .message().isNotBlank();
        assertThat(reserva.getEstado()).isEqualTo(estadoAntes);
    }
}
