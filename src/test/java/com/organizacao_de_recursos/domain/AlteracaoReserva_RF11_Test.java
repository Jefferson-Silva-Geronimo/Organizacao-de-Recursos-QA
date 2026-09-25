package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RF-11: Alteração de reservas
 * Identifier: RF-11 | docs/prd.md:7.11 | E4: Reservas e Agenda
 *
 * Casos de teste mapeados:
 * - T-RF11-001: Happy Path - Solicitante altera a própria reserva, não iniciada, para período livre
 * - T-RF11-002: Forbidden State - Alteração recusada (outro solicitante, iniciada, conflito, manutenção)
 * - T-RF11-003: [BLOQUEADO_POR_LACUNA] - qual alteração muda o estado da reserva é decisão pendente (arquitetura §12, item 6)
 */
@DisplayName("RF-11: Alteração de reservas")
class AlteracaoReserva_RF11_Test {

    private static final LocalDateTime DIA_08H = LocalDateTime.now().plusDays(6)
            .withHour(8).withMinute(0).withSecond(0).withNano(0);
    private static final LocalDateTime DIA_09H = DIA_08H.plusHours(1);
    private static final LocalDateTime DIA_10H = DIA_08H.plusHours(2);
    private static final LocalDateTime DIA_11H = DIA_08H.plusHours(3);

    private final Usuario dono = new Usuario(1L, "dono", Usuario.Perfil.SOLICITANTE);
    private final Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
    private final ValidadorManutencao manutencao = new ValidadorManutencao();
    private final ServicoCriacaoReserva criacao = new ServicoCriacaoReserva(manutencao);
    private final ServicoGestaoReserva gestao = new ServicoGestaoReserva(criacao, manutencao);

    private Reserva reservaDoDono() {
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaA);
        reserva.setInicio(DIA_08H);
        reserva.setFim(DIA_09H);
        return criacao.criarReserva(dono, reserva);
    }

    @Test
    @DisplayName("T-RF11-001: Happy Path - Solicitante altera a própria reserva para um período livre")
    void solicitanteDeveAlterarPropriaReservaParaPeriodoLivre() {
        // Arrange
        Reserva reserva = reservaDoDono();

        // Act
        gestao.alterarReserva(dono, reserva, DIA_10H, DIA_11H);

        // Assert
        assertThat(reserva.getInicio()).isEqualTo(DIA_10H);
        assertThat(reserva.getFim()).isEqualTo(DIA_11H);
    }

    enum Recusa { OUTRO_SOLICITANTE, JA_INICIADA, CONFLITO_COM_OUTRA_RESERVA, RECURSO_EM_MANUTENCAO }

    @ParameterizedTest(name = "alteração recusada: {0}")
    @EnumSource(Recusa.class)
    @DisplayName("T-RF11-002: Forbidden State - Alteração recusada quando não permitida")
    void alteracaoNaoPermitidaDeveSerRecusada(Recusa recusa) {
        // Arrange
        Reserva reserva = reservaDoDono();
        Usuario quemAltera = dono;
        switch (recusa) {
            case OUTRO_SOLICITANTE -> quemAltera = new Usuario(2L, "outro", Usuario.Perfil.SOLICITANTE);
            case JA_INICIADA -> reserva.setEstado("EM_USO");
            case CONFLITO_COM_OUTRA_RESERVA -> {
                Reserva outra = new Reserva();
                outra.setRecurso(salaA);
                outra.setInicio(DIA_10H);
                outra.setFim(DIA_11H);
                criacao.criarReserva(new Usuario(3L, "terceiro", Usuario.Perfil.SOLICITANTE), outra);
            }
            case RECURSO_EM_MANUTENCAO -> manutencao.registrarManutencao(salaA, DIA_10H, DIA_11H);
        }
        Usuario solicitanteDaAlteracao = quemAltera;

        // Act & Assert
        assertThatThrownBy(() -> gestao.alterarReserva(solicitanteDaAlteracao, reserva, DIA_10H, DIA_11H))
                .isInstanceOf(RuntimeException.class);
        assertThat(reserva.getInicio()).isEqualTo(DIA_08H);
        assertThat(reserva.getFim()).isEqualTo(DIA_09H);
    }
}
