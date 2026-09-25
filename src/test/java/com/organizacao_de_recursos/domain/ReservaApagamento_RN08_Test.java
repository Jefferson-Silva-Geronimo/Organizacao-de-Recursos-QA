package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RN-08: Reserva Iniciada Não Pode Ser Apagada
 * RN-08: Reservas iniciadas não podem ser apagadas.
 * Identifier: RN-08 | docs/prd.md:6.8
 * 
 * Casos de teste mapeados:
 * - T-RN08-001: Happy Path - Cancelar antes de iniciar
 * - T-RN08-002: Forbidden State - Cancelar após iniciação
 * - T-RN08-003: Forbidden State - Cancelar após conclusão
 * - T-RN08-004: Forbidden State - Apagar registro (DELETE) de reserva iniciada
 * - T-RN08-005: Forbidden State - Alterar de EM_USO para CANCELADA
 * - T-RN08-006: Boundary - Alteração imediatamente após iniciação
 * - T-RN08-007: Invalid Input - Tentar apagar com ID inválido
 * - T-RN08-008: Conflicts - Garantir auditoria de tentativa de apagamento
 * - T-RN08-009: Forbidden State - Admin não pode contornar proteção para CONCLUIDA
 * - T-RN08-010: Forbidden State - Rejeitar após iniciada
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
    @DisplayName("T-RN08-004: Forbidden State - Apagar registro (DELETE) de reserva iniciada")
    void adminNaoPodeApagarRegistroDeReservaIniciada() {
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

    @Test
    @DisplayName("T-RN08-005: Forbidden State - Alterar de EM_USO para CANCELADA")
    void deveRecusarForcarCancelamentoDeReservaEmUso() {
        // Arrange
        ValidadorApagamento validador = new ValidadorApagamento();
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setEstado("EM_USO");

        // Act & Assert
        assertThatThrownBy(() -> validador.forcarCancelamento(reserva))
                .isInstanceOf(ReservaApagamentoException.class)
                .hasMessageContaining("Reserva em uso não pode ser cancelada");
    }

    @Test
    @DisplayName("T-RN08-006: Boundary - Alteração imediatamente após iniciação")
    void deveRecusarCancelamentoImediatamenteAposIniciacao() {
        // Arrange - reserva inicia no segundo 00 e o cancelamento é tentado no segundo 01
        ValidadorApagamento validador = new ValidadorApagamento();
        LocalDateTime inicio = LocalDateTime.now().withNano(0);
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setEstado("EM_USO");
        reserva.setInicio(inicio);
        LocalDateTime momentoCancelamento = inicio.plusSeconds(1);

        // Act & Assert
        assertThatThrownBy(() -> validador.validarCancelamentoNoInicio(reserva, momentoCancelamento))
                .isInstanceOf(ReservaApagamentoException.class)
                .hasMessageContaining("Reserva já iniciada");
    }
    @Test
    @DisplayName("T-RN08-007: Invalid Input - Tentar apagar com ID inválido")
    void deveRecusarApagarComIdInvalido() {
        // Arrange
        ValidadorApagamento validador = new ValidadorApagamento();

        // Act & Assert - ID nulo e número negativo
        assertThatThrownBy(() -> validador.apagarPorId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID inválido");
        assertThatThrownBy(() -> validador.apagarPorId(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID inválido");
    }
    @Test
    @DisplayName("T-RN08-008: Conflicts - Garantir auditoria de tentativa de apagamento")
    void deveRegistrarAuditoriaAoTentarApagarReservaIniciada() {
        // Arrange
        ValidadorApagamento validador = new ValidadorApagamento();
        ValidadorAuditoria auditoria = new ValidadorAuditoria();
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setEstado("EM_USO");
        Usuario usuario = new Usuario(1L, "user1", Usuario.Perfil.SOLICITANTE);

        // Act
        assertThatThrownBy(() -> validador.tentarApagarComAuditoria(reserva, usuario, auditoria))
                .isInstanceOf(ReservaApagamentoException.class);

        // Assert - a tentativa recusada ficou registrada com usuário e timestamp
        assertThat(auditoria.obterAuditorias(1L)).hasSize(1);
        Auditoria registro = auditoria.obterAuditorias(1L).get(0);
        assertThat(registro.getUsuario()).isEqualTo("user1");
        assertThat(registro.getTimestamp()).isNotNull();
    }
    @Test
    @DisplayName("T-RN08-009: Forbidden State - Admin não pode contornar proteção (CONCLUIDA ou EM_USO)")
    void adminNaoPodeApagarReservaConcluida() {
        // Arrange
        ValidadorApagamento validador = new ValidadorApagamento();
        Reserva concluida = new Reserva();
        concluida.setId(1L);
        concluida.setEstado("CONCLUIDA");
        Reserva emUso = new Reserva();
        emUso.setId(2L);
        emUso.setEstado("EM_USO");
        Usuario admin = new Usuario(1L, "admin", Usuario.Perfil.ADMINISTRADOR);

        // Act & Assert
        assertThatThrownBy(() -> validador.validarApagamentoAdmin(concluida, admin))
                .isInstanceOf(ReservaApagamentoException.class)
                .hasMessageContaining("Registros de reserva iniciada não podem ser removidos");
        assertThatThrownBy(() -> validador.validarApagamentoAdmin(emUso, admin))
                .isInstanceOf(ReservaApagamentoException.class)
                .hasMessageContaining("Registros de reserva iniciada não podem ser removidos");
    }
    @Test
    @DisplayName("T-RN08-010: Forbidden State - Rejeitar após iniciada")
    void deveRecusarRejeitarReservaJaIniciada() {
        // Arrange
        ValidadorApagamento validador = new ValidadorApagamento();
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setEstado("EM_USO");

        // Act & Assert - plano aceita "Transição não permitida" OU "Reserva já iniciada não pode ser alterada"
        assertThatThrownBy(() -> validador.validarRejeicaoAposIniciada(reserva))
                .isInstanceOf(ReservaApagamentoException.class)
                .satisfies(erro -> assertThat(erro.getMessage())
                        .containsAnyOf("Transição não permitida", "Reserva já iniciada não pode ser alterada"));
    }
}
