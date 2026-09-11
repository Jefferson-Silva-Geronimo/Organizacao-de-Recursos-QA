package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RN-08: Reserva Iniciada Não Pode Ser Apagada
 * RN-08: Reservas iniciadas não podem ser apagadas.
 * Identifier: RN-08 | docs/prd.md:6.8
 */
@DisplayName("RN-08: Reserva Iniciada Não Pode Ser Apagada")
class ReservaApagamento_RN08_Test {

    @Test
    @DisplayName("T-RN08-001: Happy Path - Cancelar antes de iniciar")
    void devePodercancelarReservaAntesDeiniciar() {
        // Arrange
        ValidadorApagamento validador = new ValidadorApagamento();
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setEstado("SOLICITADA");

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.validarApagamento(reserva));
    }

    @Test
    @DisplayName("T-RN08-002: Forbidden State - Cancelar após iniciação")
    void deveRecusarCancelamentoAposIniciacao() {
        // Arrange
        ValidadorApagamento validador = new ValidadorApagamento();
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setEstado("EM_USO");

        // Act & Assert
        assertThatThrownBy(() -> validador.validarApagamento(reserva))
                .isInstanceOf(ReservaApagamentoException.class)
                .hasMessageContaining("Reserva em uso não pode ser cancelada");
    }

    @Test
    @DisplayName("T-RN08-003: Forbidden State - Cancelar após conclusão")
    void deveRecusarCancelamentoAposConclusao() {
        // Arrange
        ValidadorApagamento validador = new ValidadorApagamento();
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setEstado("CONCLUIDA");

        // Act & Assert
        assertThatThrownBy(() -> validador.validarApagamento(reserva))
                .isInstanceOf(ReservaApagamentoException.class)
                .hasMessageContaining("Operação não permitida para reserva concluída");
    }

    @Test
    @DisplayName("T-RN08-004: Forbidden State - Admin não pode contornar proteção")
    void adminNaoPodeContornarProtecao() {
        // Arrange
        ValidadorApagamento validador = new ValidadorApagamento();
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setEstado("EM_USO");
        Usuario admin = new Usuario(1L, "admin", Usuario.Perfil.ADMINISTRADOR);

        // Act & Assert
        assertThatThrownBy(() -> validador.validarApagamentoAdmin(reserva, admin))
                .isInstanceOf(ReservaApagamentoException.class)
                .hasMessageContaining("Registros de reserva iniciada não podem ser removidos");
    }
}
