package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RN-01: Ordem Temporal da Reserva
 * 
 * RN-01: O término da reserva deve ser posterior ao início.
 * Identifier: RN-01 | docs/prd.md:6.1
 * 
 * Casos de teste mapeados:
 * - T-RN01-001: Happy Path - Reserva com intervalo válido
 * - T-RN01-004: Boundary - Intervalo zero (início = fim)
 * - T-RN01-005: Invalid Input - Fim anterior ao início
 * - T-RN01-006: Invalid Input - Data passada
 * - T-RN01-007: Invalid Input - Início nulo
 * - T-RN01-008: Invalid Input - Fim nulo
 */
@DisplayName("RN-01: Ordem Temporal da Reserva")
class ReservaTemporal_RN01_Test {

    private static final LocalDateTime HOJE_08H = LocalDateTime.of(2026, 9, 11, 8, 0);
    private static final LocalDateTime HOJE_09H = LocalDateTime.of(2026, 9, 11, 9, 0);
    private static final LocalDateTime HOJE_10H = LocalDateTime.of(2026, 9, 11, 10, 0);
    private static final LocalDateTime ONTEM = LocalDateTime.of(2026, 9, 10, 8, 0);

    @Test
    @DisplayName("T-RN01-001: Happy Path - Reserva com intervalo válido (08:00 a 09:00)")
    void deveAceitarReservaComIntervaloValido() {
        // Arrange
        LocalDateTime inicio = HOJE_08H;
        LocalDateTime fim = HOJE_09H;
        Reserva reserva = new Reserva();

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> reserva.validarTemporalidade(inicio, fim));
    }

    @Test
    @DisplayName("T-RN01-004: Boundary - Intervalo zero (início = fim) deve ser recusado")
    void deveRecusarIntervaloZero() {
        // Arrange
        LocalDateTime inicio = HOJE_08H;
        LocalDateTime fim = HOJE_08H;
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
        LocalDateTime inicio = HOJE_09H;
        LocalDateTime fim = HOJE_08H;
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
        LocalDateTime fim = HOJE_09H;
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
        LocalDateTime inicio = HOJE_08H;
        LocalDateTime fim = null;
        Reserva reserva = new Reserva();

        // Act & Assert
        assertThatThrownBy(() -> reserva.validarTemporalidade(inicio, fim))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fim é obrigatório");
    }

    @Test
    @DisplayName("T-RN01-002: Happy Path - Intervalo longo (08:00 a 18:00)")
    void deveAceitarIntervaloLongo() {
        // Arrange
        LocalDateTime inicio = HOJE_08H;
        LocalDateTime fim = LocalDateTime.of(2026, 9, 11, 18, 0);
        Reserva reserva = new Reserva();

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> reserva.validarTemporalidade(inicio, fim));
    }

    @Test
    @DisplayName("T-RN01-003: Boundary - Intervalo mínimo (1 minuto)")
    void deveAceitarIntervaloMinimo() {
        // Arrange
        LocalDateTime inicio = HOJE_08H;
        LocalDateTime fim = HOJE_08H.plusMinutes(1);
        Reserva reserva = new Reserva();

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> reserva.validarTemporalidade(inicio, fim));
    }
}
