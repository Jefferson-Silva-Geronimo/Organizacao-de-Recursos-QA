package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RN-09: Auditoria de Mudança de Estado
 * RN-09: Toda mudança de estado deve gerar registro de auditoria.
 * Identifier: RN-09 | docs/prd.md:6.9
 */
@DisplayName("RN-09: Auditoria de Mudança de Estado")
class ReservaAuditoria_RN09_Test {

    @Test
    @DisplayName("T-RN09-001: Happy Path - Criar reserva gera auditoria")
    void criarReservaDeveGerarAuditoria() {
        // Arrange
        ValidadorAuditoria validador = new ValidadorAuditoria();
        Usuario usuario = new Usuario(1L, "user1", Usuario.Perfil.SOLICITANTE);
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setEstado("SOLICITADA");

        // Act
        validador.registrarAuditoria(reserva, usuario, "CRIAR", "SOLICITADA");

        // Assert
        assertThat(validador.obterAuditorias(reserva.getId())).isNotEmpty();
        Auditoria auditoria = validador.obterAuditorias(reserva.getId()).get(0);
        assertThat(auditoria.getAcao()).isEqualTo("CRIAR");
        assertThat(auditoria.getEstadoNovo()).isEqualTo("SOLICITADA");
        assertThat(auditoria.getUsuario()).isEqualTo(usuario.getUsername());
    }

    @Test
    @DisplayName("T-RN09-002: Happy Path - Transição de estado gera auditoria")
    void transicaoDeEstadoDeveGerarAuditoria() {
        // Arrange
        ValidadorAuditoria validador = new ValidadorAuditoria();
        Usuario usuario = new Usuario(2L, "user2", Usuario.Perfil.RESPONSAVEL);
        Reserva reserva = new Reserva();
        reserva.setId(1L);

        // Act
        validador.registrarAuditoria(reserva, usuario, "APROVAR", "APROVADA", "SOLICITADA");

        // Assert
        assertThat(validador.obterAuditorias(reserva.getId())).isNotEmpty();
        Auditoria auditoria = validador.obterAuditorias(reserva.getId()).get(0);
        assertThat(auditoria.getEstadoAnterior()).isEqualTo("SOLICITADA");
        assertThat(auditoria.getEstadoNovo()).isEqualTo("APROVADA");
    }

    @Test
    @DisplayName("T-RN09-003: Happy Path - Consultar histórico de reserva")
    void devePoderconsultarHistoricoCompleto() {
        // Arrange
        ValidadorAuditoria validador = new ValidadorAuditoria();
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        Usuario user1 = new Usuario(1L, "user1", Usuario.Perfil.SOLICITANTE);
        Usuario user2 = new Usuario(2L, "user2", Usuario.Perfil.RESPONSAVEL);

        // Act
        validador.registrarAuditoria(reserva, user1, "CRIAR", "SOLICITADA");
        validador.registrarAuditoria(reserva, user2, "APROVAR", "APROVADA", "SOLICITADA");

        // Assert
        assertThat(validador.obterAuditorias(reserva.getId())).hasSize(2);
    }

    @Test
    @DisplayName("T-RN09-006: Forbidden State - Tentar editar auditoria")
    void naoDevePermitirEditarAuditoria() {
        // Arrange
        ValidadorAuditoria validador = new ValidadorAuditoria();
        Usuario usuario = new Usuario(1L, "user1", Usuario.Perfil.SOLICITANTE);
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        validador.registrarAuditoria(reserva, usuario, "CRIAR", "SOLICITADA");

        // Act & Assert
        assertThatThrownBy(() -> validador.editarAuditoria(reserva.getId(), 0))
                .isInstanceOf(ReservaAuditoriaException.class)
                .hasMessageContaining("Auditoria é imutável");
    }

    @Test
    @DisplayName("T-RN09-007: Forbidden State - Tentar apagar auditoria")
    void naoDevePermitirApagarAuditoria() {
        // Arrange
        ValidadorAuditoria validador = new ValidadorAuditoria();
        Usuario usuario = new Usuario(1L, "user1", Usuario.Perfil.SOLICITANTE);
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        validador.registrarAuditoria(reserva, usuario, "CRIAR", "SOLICITADA");

        // Act & Assert
        assertThatThrownBy(() -> validador.apagarAuditoria(reserva.getId()))
                .isInstanceOf(ReservaAuditoriaException.class)
                .hasMessageContaining("Auditoria não pode ser removida");
    }
}
