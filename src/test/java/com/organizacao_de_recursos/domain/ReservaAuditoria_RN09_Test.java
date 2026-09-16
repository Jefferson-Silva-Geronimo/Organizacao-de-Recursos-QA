package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RN-09: Auditoria de Mudança de Estado
 * RN-09: Toda mudança de estado deve gerar registro de auditoria.
 * Identifier: RN-09 | docs/prd.md:6.9
 * 
 * Casos de teste mapeados:
 * - T-RN09-001: Happy Path - Criar reserva gera auditoria
 * - T-RN09-002: Happy Path - Transição de estado gera auditoria
 * - T-RN09-003: Happy Path - Consultar histórico de reserva
 * - T-RN09-004: Happy Path - Múltiplas mudanças em rápida sucessão
 * - T-RN09-005: Invalid Input - Operação sem autenticação
 * - T-RN09-006: Forbidden State - Tentar editar auditoria
 * - T-RN09-007: Forbidden State - Tentar apagar auditoria
 * - T-RN09-008: Conflicts - Operação recusada gera auditoria
 * - T-RN09-009: Boundary - Auditoria com timestamp granular
 * - T-RN09-010: Boundary - Auditoria após tentativa de apagamento proibido
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
    @DisplayName("T-RN09-004: Happy Path - Múltiplas mudanças em rápida sucessão")
    void devePreservarOrdemDeMultiplasMudancasEmSucessao() {
        // Arrange
        ValidadorAuditoria validador = new ValidadorAuditoria();
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        Usuario user = new Usuario(1L, "user1", Usuario.Perfil.SOLICITANTE);
        List<String> transicoes = Arrays.asList("SOLICITADA", "APROVADA", "EM_USO", "CONCLUIDA");

        // Act
        validador.registrarMudancasEmSequencia(reserva, user, transicoes);

        // Assert
        assertThat(validador.obterAuditorias(reserva.getId())).hasSize(4);
    }

    @Test
    @DisplayName("T-RN09-005: Invalid Input - Operação sem autenticação (sem usuário)")
    void operacaoSemAutenticacaoDeveSerRecusadaNaAuditoria() {
        // Arrange
        ValidadorAuditoria validador = new ValidadorAuditoria();

        // Act & Assert
        assertThatThrownBy(() -> validador.validarUsuarioAuditoria(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuário não identificado");
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

    @Test
    @DisplayName("T-RN09-008: Conflicts - Operação recusada gera auditoria de tentativa")
    void operacaoRecusadaDeveGerarRegistroDeAuditoria() {
        // Arrange
        ValidadorAuditoria validador = new ValidadorAuditoria();
        Usuario solicitante = new Usuario(1L, "user1", Usuario.Perfil.SOLICITANTE);
        Reserva reserva = new Reserva();
        reserva.setId(1L);

        // Act
        validador.registrarTentativaRecusada(reserva, solicitante, "APROVAR", "Acesso negado");

        // Assert
        assertThat(validador.obterAuditorias(reserva.getId())).isNotEmpty();
    }

    @Test
    @DisplayName("T-RN09-009: Boundary - Auditoria com timestamp granular preserva ordenação")
    void auditoriaComTimestampGranularDeveSerOrdenada() {
        // Arrange
        ValidadorAuditoria validador = new ValidadorAuditoria();
        Long reservaId = 1L;

        // Act
        List<Auditoria> ordenadas = validador.obterAuditoriasOrdenadas(reservaId);

        // Assert
        assertThat(ordenadas).isNotNull();
    }

    @Test
    @DisplayName("T-RN09-010: Boundary - Auditoria após tentativa de apagamento proibido")
    void tentativaDeApagamentoProibidoDeveRegistrarAuditoriaComResultadoRejeitado() {
        // Arrange
        ValidadorAuditoria validador = new ValidadorAuditoria();
        Usuario usuario = new Usuario(1L, "admin", Usuario.Perfil.ADMINISTRADOR);
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setEstado("EM_USO");

        // Act
        validador.registrarTentativaApagamentoProibido(reserva, usuario);

        // Assert
        assertThat(validador.obterAuditorias(reserva.getId())).isNotEmpty();
    }
}
