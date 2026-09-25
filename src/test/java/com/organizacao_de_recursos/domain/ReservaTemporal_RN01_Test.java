package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Testes para RN-01: Ordem Temporal da Reserva
 * 
 * RN-01: O término da reserva deve ser posterior ao início.
 * Identifier: RN-01 | docs/prd.md:6.1
 * 
 * Casos de teste mapeados:
 * - T-RN01-001: Happy Path - Reserva com intervalo válido
 * - T-RN01-002: Happy Path - Intervalo longo
 * - T-RN01-003: Boundary - Intervalo mínimo (1 minuto) [BLOQUEADO_POR_LACUNA]
 * - T-RN01-004: Boundary - Intervalo zero (início = fim)
 * - T-RN01-005: Invalid Input - Fim anterior ao início
 * - T-RN01-006: Invalid Input - Data passada
 * - T-RN01-007: Invalid Input - Início nulo
 * - T-RN01-008: Invalid Input - Fim nulo
 * - T-RN01-009: Invalid Input - Formato de hora inválido
 * - T-RN01-010: Forbidden State - Alteração de reserva já iniciada
 */
@DisplayName("RN-01: Ordem Temporal da Reserva")
class ReservaTemporal_RN01_Test {

    private static final LocalDateTime FUTURO_08H = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
    private static final LocalDateTime FUTURO_09H = FUTURO_08H.plusHours(1);
    private static final LocalDateTime FUTURO_10H = FUTURO_08H.plusHours(2);
    private static final LocalDateTime FUTURO_18H = FUTURO_08H.plusHours(10);
    private static final LocalDateTime ONTEM = LocalDateTime.now().minusDays(1);

    @Test
    @DisplayName("T-RN01-001: Happy Path - Reserva com intervalo válido (08:00 a 09:00)")
    void deveAceitarReservaComIntervaloValido() {
        // Arrange
        LocalDateTime inicio = FUTURO_08H;
        LocalDateTime fim = FUTURO_09H;
        Reserva reserva = new Reserva();

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> reserva.validarTemporalidade(inicio, fim));
    }

    @Test
    @DisplayName("T-RN01-002: Happy Path - Intervalo longo (08:00 a 18:00)")
    void deveAceitarIntervaloLongo() {
        // Arrange
        LocalDateTime inicio = FUTURO_08H;
        LocalDateTime fim = FUTURO_18H;
        Reserva reserva = new Reserva();

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> reserva.validarTemporalidade(inicio, fim));
    }

    @Test
    @Disabled("BLOQUEADO_POR_LACUNA: plano não define a duração mínima (§4.3) - resultado 'Reserva aceita ou recusada conforme política (PENDENTE)'")
    @DisplayName("T-RN01-003: Boundary - Intervalo mínimo (1 minuto)")
    void deveAceitarIntervaloMinimo() {
        fail("Caso bloqueado: duração mínima de reserva indefinida no plano (§4.3)");
    }

    @Test
    @DisplayName("T-RN01-004: Boundary - Intervalo zero (início = fim) deve ser recusado")
    void deveRecusarIntervaloZero() {
        // Arrange
        LocalDateTime inicio = FUTURO_08H;
        LocalDateTime fim = FUTURO_08H;
        Reserva reserva = new Reserva();

        // Act & Assert
        assertThatThrownBy(() -> reserva.validarTemporalidade(inicio, fim))
                .isInstanceOf(ReservaTemporalException.class)
                .hasMessageContaining("Duração inválida");
    }

    @Test
    @DisplayName("T-RN01-005: Invalid Input - Fim anterior ao início (regressão temporal)")
    void deveRecusarFimAnteriorAoInicio() {
        // Arrange
        LocalDateTime inicio = FUTURO_09H;
        LocalDateTime fim = FUTURO_08H;
        Reserva reserva = new Reserva();

        // Act & Assert
        assertThatThrownBy(() -> reserva.validarTemporalidade(inicio, fim))
                .isInstanceOf(ReservaTemporalException.class)
                .hasMessageContaining("Fim anterior ao início");
    }

    @Test
    @DisplayName("T-RN01-006: Invalid Input - Data passada")
    void deveRecusarDataPassada() {
        // Arrange
        LocalDateTime inicio = ONTEM;
        LocalDateTime fim = ONTEM.plusHours(1);
        Reserva reserva = new Reserva();

        // Act & Assert
        assertThatThrownBy(() -> reserva.validarTemporalidade(inicio, fim))
                .isInstanceOf(ReservaTemporalException.class)
                .hasMessageContaining("Data no passado");
    }

    @Test
    @DisplayName("T-RN01-007: Invalid Input - Início nulo")
    void deveRecusarInicioNulo() {
        // Arrange
        LocalDateTime inicio = null;
        LocalDateTime fim = FUTURO_09H;
        Reserva reserva = new Reserva();

        // Act & Assert
        assertThatThrownBy(() -> reserva.validarTemporalidade(inicio, fim))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Início é obrigatório");
    }

    @Test
    @DisplayName("T-RN01-008: Invalid Input - Fim nulo")
    void deveRecusarFimNulo() {
        // Arrange
        LocalDateTime inicio = FUTURO_08H;
        LocalDateTime fim = null;
        Reserva reserva = new Reserva();

        // Act & Assert
        assertThatThrownBy(() -> reserva.validarTemporalidade(inicio, fim))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fim é obrigatório");
    }

    @Test
    @DisplayName("T-RN01-009: Invalid Input - Formato de hora inválido")
    void deveRecusarFormatoDeHoraInvalido() {
        // Arrange
        String inicioInvalido = "08h30";
        String fimValido = "09:30";
        Reserva reserva = new Reserva();

        // Act & Assert
        assertThatThrownBy(() -> reserva.validarFormatoHora(inicioInvalido, fimValido))
                .isInstanceOf(ReservaTemporalException.class)
                .hasMessageContaining("Formato de hora inválido");
    }

    @Test
    @DisplayName("T-RN01-010: Forbidden State - Alteração de reserva já iniciada")
    void deveRecusarAlteracaoDeReservaJaIniciada() {
        // Arrange
        Reserva reserva = new Reserva();
        reserva.setEstado("EM_USO");
        LocalDateTime novoInicio = FUTURO_09H;
        LocalDateTime novoFim = FUTURO_10H;

        // Act & Assert
        assertThatThrownBy(() -> reserva.alterarHorario(novoInicio, novoFim))
                .isInstanceOf(ReservaTemporalException.class)
                .hasMessageContaining("Reserva já iniciada não pode ser alterada");
    }
}
