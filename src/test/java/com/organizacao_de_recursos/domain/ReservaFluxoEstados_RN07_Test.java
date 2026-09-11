package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RN-07: Fluxo de Estados
 * RN-07: Fluxo principal é SOLICITADA -> APROVADA -> EM_USO -> CONCLUIDA
 * Estados alternativos: REJEITADA, CANCELADA, NAO_COMPARECEU
 * Identifier: RN-07 | docs/prd.md:6.7
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
}
