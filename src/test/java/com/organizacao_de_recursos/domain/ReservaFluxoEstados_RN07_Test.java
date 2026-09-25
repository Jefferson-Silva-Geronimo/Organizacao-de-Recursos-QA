package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Testes para RN-07: Fluxo de Estados
 * RN-07: Fluxo principal é SOLICITADA -> APROVADA -> EM_USO -> CONCLUIDA
 * Estados alternativos: REJEITADA, CANCELADA, NAO_COMPARECEU
 * Identifier: RN-07 | docs/prd.md:6.7
 * 
 * Casos de teste mapeados:
 * - T-RN07-001: Happy Path - Fluxo completo
 * - T-RN07-002: Happy Path - Cancelamento em SOLICITADA
 * - T-RN07-003: Happy Path - Rejeição em SOLICITADA
 * - T-RN07-004: Forbidden State - Transição não especificada
 * - T-RN07-005: Forbidden State - Estado inválido
 * - T-RN07-006: Forbidden State - Cancelar reserva iniciada
 * - T-RN07-007: Conflicts - Rejeitar após APROVADA (transição retrógrada)
 * - T-RN07-008: Boundary - Transição na fronteira de tempo
 * - T-RN07-009: Invalid Input - Estado null ou vazio
 * - T-RN07-010: Forbidden State - NAO_COMPARECEU sem origem definida [BLOQUEADO_POR_LACUNA]
 */
@DisplayName("RN-07: Fluxo de Estados")
class ReservaFluxoEstados_RN07_Test {

    @Test
    @DisplayName("T-RN07-001: Happy Path - Fluxo completo")
    void deveExecutarFluxoCompletoDeEstados() {
        // Arrange
        ValidadorFluxoEstados validador = new ValidadorFluxoEstados();
        Reserva reserva = new Reserva();
        reserva.setEstado("SOLICITADA");

        // Act & Assert - Verificar transições válidas
        assertThatNoException()
                .isThrownBy(() -> {
                    validador.validarTransicao(reserva, "APROVADA");
                    reserva.setEstado("APROVADA");
                    
                    validador.validarTransicao(reserva, "EM_USO");
                    reserva.setEstado("EM_USO");
                    
                    validador.validarTransicao(reserva, "CONCLUIDA");
                    reserva.setEstado("CONCLUIDA");
                });

        assertThat(reserva.getEstado()).isEqualTo("CONCLUIDA");
    }

    @Test
    @DisplayName("T-RN07-002: Happy Path - Cancelamento em SOLICITADA")
    void deveCancelarReservaEmSolicitada() {
        // Arrange
        ValidadorFluxoEstados validador = new ValidadorFluxoEstados();
        Reserva reserva = new Reserva();
        reserva.setEstado("SOLICITADA");

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.validarTransicao(reserva, "CANCELADA"));

        reserva.setEstado("CANCELADA");
        assertThat(reserva.getEstado()).isEqualTo("CANCELADA");
    }

    @Test
    @DisplayName("T-RN07-003: Happy Path - Rejeição em SOLICITADA")
    void deveRejeitarReservaEmSolicitada() {
        // Arrange
        ValidadorFluxoEstados validador = new ValidadorFluxoEstados();
        Reserva reserva = new Reserva();
        reserva.setEstado("SOLICITADA");

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.validarTransicao(reserva, "REJEITADA"));

        reserva.setEstado("REJEITADA");
        assertThat(reserva.getEstado()).isEqualTo("REJEITADA");
    }

    @Test
    @DisplayName("T-RN07-004: Forbidden State - Transição não especificada")
    void deveRecusarTransicaoNaoEspecificada() {
        // Arrange
        ValidadorFluxoEstados validador = new ValidadorFluxoEstados();
        Reserva reserva = new Reserva();
        reserva.setEstado("SOLICITADA");

        // Act & Assert - SOLICITADA -> EM_USO direto (sem passar por APROVADA)
        assertThatThrownBy(() -> validador.validarTransicao(reserva, "EM_USO"))
                .isInstanceOf(ReservaFluxoEstadosException.class)
                .hasMessageContaining("Transição não permitida");
    }

    @Test
    @DisplayName("T-RN07-005: Forbidden State - Estado inválido")
    void deveRecusarEstadoInvalido() {
        // Arrange
        ValidadorFluxoEstados validador = new ValidadorFluxoEstados();
        Reserva reserva = new Reserva();
        reserva.setEstado("SOLICITADA");

        // Act & Assert
        assertThatThrownBy(() -> validador.validarTransicao(reserva, "PENDENTE"))
                .isInstanceOf(ReservaFluxoEstadosException.class)
                .hasMessageContaining("Estado inválido");
    }

    @Test
    @DisplayName("T-RN07-006: Forbidden State - Cancelar reserva iniciada")
    void deveRecusarCancelamentoDeReservaIniciada() {
        // Arrange
        ValidadorFluxoEstados validador = new ValidadorFluxoEstados();
        Reserva reserva = new Reserva();
        reserva.setEstado("EM_USO");

        // Act & Assert
        assertThatThrownBy(() -> validador.validarTransicao(reserva, "CANCELADA"))
                .isInstanceOf(ReservaFluxoEstadosException.class)
                .hasMessageContaining("Reserva iniciada não pode ser cancelada");
    }

    @Test
    @DisplayName("T-RN07-007: Conflicts - Rejeitar após APROVADA (transição retrógrada)")
    void deveRecusarRejeicaoAposAprovacao() {
        // Arrange
        ValidadorFluxoEstados validador = new ValidadorFluxoEstados();
        Reserva reserva = new Reserva();
        reserva.setEstado("APROVADA");

        // Act & Assert - APROVADA -> REJEITADA
        assertThatThrownBy(() -> validador.validarTransicao(reserva, "REJEITADA"))
                .isInstanceOf(ReservaFluxoEstadosException.class)
                .hasMessageContaining("Transição não permitida");
    }

    @Test
    @DisplayName("T-RN07-008: Boundary - Transição na fronteira de tempo")
    void deveAceitarTransicaoNaFronteiraDeTempo() {
        // Arrange - início às 23:59:59 de um dia e término às 00:00:00 do dia seguinte
        LocalDateTime inicio = LocalDateTime.now().plusDays(2).withHour(23).withMinute(59).withSecond(59).withNano(0);
        LocalDateTime fim = inicio.plusSeconds(1);
        ValidadorFluxoEstados validador = new ValidadorFluxoEstados();
        Reserva reserva = new Reserva();
        reserva.setEstado("SOLICITADA");

        // Act & Assert - término posterior ao início: período válido e transição SOLICITADA -> APROVADA aceita
        assertThat(fim.toLocalDate()).isAfter(inicio.toLocalDate());
        assertThatNoException().isThrownBy(() -> {
            reserva.validarTemporalidade(inicio, fim);
            validador.validarTransicao(reserva, "APROVADA");
        });
    }
    @Test
    @DisplayName("T-RN07-009: Invalid Input - Estado null ou vazio")
    void deveRecusarEstadoNullOuVazio() {
        // Arrange
        ValidadorFluxoEstados validador = new ValidadorFluxoEstados();
        Reserva reserva = new Reserva();
        reserva.setEstado("SOLICITADA");

        // Act & Assert
        assertThatThrownBy(() -> validador.validarTransicao(reserva, null))
                .isInstanceOf(ReservaFluxoEstadosException.class)
                .hasMessageContaining("Estado é obrigatório");
        assertThatThrownBy(() -> validador.validarTransicao(reserva, ""))
                .isInstanceOf(ReservaFluxoEstadosException.class)
                .hasMessageContaining("Estado é obrigatório");
    }
    @Test
    @Disabled("BLOQUEADO_POR_LACUNA: origem, ator e condição de NAO_COMPARECEU indefinidos (Q-006)")
    @DisplayName("T-RN07-010: Forbidden State - NAO_COMPARECEU sem origem definida")
    void deveRecusarNaoCompareceuDiretoDeSolicitada() {
        fail("Caso bloqueado: fluxo de NAO_COMPARECEU indefinido no plano (Q-006)");
    }
}
