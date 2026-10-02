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
 * - T-RN09-008: removido (decisão de negócio) - evento_auditoria só registra mudanças efetivas,
 *   nunca tentativas recusadas (ver ADR-015)
 * - T-RN09-009: Boundary - Auditoria com timestamp granular (desempate por id sequencial)
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
        assertThat(validador.obterAuditorias(reserva.getId())).hasSize(1);
        Auditoria auditoria = validador.obterAuditorias(reserva.getId()).get(0);
        assertThat(auditoria.getUsuario()).isEqualTo(usuario.getUsername());
        assertThat(auditoria.getAcao()).isEqualTo("CRIAR");
        assertThat(auditoria.getEstadoNovo()).isEqualTo("SOLICITADA");
        assertThat(auditoria.getTimestamp()).isNotNull();
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
        assertThat(validador.obterAuditorias(reserva.getId())).hasSize(1);
        Auditoria auditoria = validador.obterAuditorias(reserva.getId()).get(0);
        assertThat(auditoria.getUsuario()).isEqualTo("user2");
        assertThat(auditoria.getAcao()).isEqualTo("APROVAR");
        assertThat(auditoria.getEstadoAnterior()).isEqualTo("SOLICITADA");
        assertThat(auditoria.getEstadoNovo()).isEqualTo("APROVADA");
        assertThat(auditoria.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("T-RN09-003: Happy Path - Consultar histórico de reserva")
    void devePoderconsultarHistoricoCompleto() {
        // Arrange - reserva com 5 mudanças de estado feitas por atores distintos
        ValidadorAuditoria validador = new ValidadorAuditoria();
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        Usuario solicitante = new Usuario(1L, "solicitante", Usuario.Perfil.SOLICITANTE);
        Usuario responsavel = new Usuario(2L, "responsavel", Usuario.Perfil.RESPONSAVEL);

        // Act
        validador.registrarAuditoria(reserva, solicitante, "CRIAR", "SOLICITADA");
        validador.registrarAuditoria(reserva, responsavel, "APROVAR", "APROVADA", "SOLICITADA");
        validador.registrarAuditoria(reserva, responsavel, "INICIAR", "EM_USO", "APROVADA");
        validador.registrarAuditoria(reserva, responsavel, "CONCLUIR", "CONCLUIDA", "EM_USO");
        validador.registrarAuditoria(reserva, solicitante, "CONSULTAR", "CONCLUIDA", "CONCLUIDA");
        List<Auditoria> historico = validador.obterAuditoriasOrdenadas(reserva.getId());

        // Assert - todas as 5 mudanças, em ordem cronológica, com ator e timestamp
        assertThat(historico).hasSize(5);
        assertThat(historico).extracting(Auditoria::getAcao)
                .containsExactly("CRIAR", "APROVAR", "INICIAR", "CONCLUIR", "CONSULTAR");
        assertThat(historico).extracting(Auditoria::getUsuario)
                .containsExactly("solicitante", "responsavel", "responsavel", "responsavel", "solicitante");
        assertThat(historico).extracting(Auditoria::getTimestamp).doesNotContainNull().isSorted();
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

        // Act - criar -> aprovar -> iniciar -> concluir em sucessão imediata
        validador.registrarMudancasEmSequencia(reserva, user, transicoes);
        List<Auditoria> historico = validador.obterAuditoriasOrdenadas(reserva.getId());

        // Assert - ordem preservada e timestamps não decrescentes
        assertThat(historico).extracting(Auditoria::getEstadoNovo)
                .containsExactly("SOLICITADA", "APROVADA", "EM_USO", "CONCLUIDA");
        assertThat(historico).extracting(Auditoria::getTimestamp).doesNotContainNull().isSorted();
    }

    @Test
    @DisplayName("T-RN09-005: Invalid Input - Operação sem autenticação (sem usuário)")
    void operacaoSemAutenticacaoDeveSerRecusadaNaAuditoria() {
        // Arrange
        ValidadorAuditoria validador = new ValidadorAuditoria();
        Reserva reserva = new Reserva();
        reserva.setId(1L);

        // Act & Assert
        assertThatThrownBy(() -> validador.registrarAuditoria(reserva, null, "CRIAR", "SOLICITADA"))
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

    // T-RN09-008 removido: decisão de negócio (sessão de migração) - evento_auditoria só registra
    // mudanças de estado EFETIVAS (consistente com D2/D3); uma operação recusada nunca chega a mudar
    // de estado, então não gera linha de auditoria de negócio (apenas resposta HTTP 409/422 na API).

    @Test
    @DisplayName("T-RN09-009: Boundary - Auditoria com timestamp granular preserva ordenação")
    void auditoriaComTimestampGranularDeveSerOrdenada() {
        // Arrange - decisão: desempate por id sequencial de inserção quando o timestamp coincide
        ValidadorAuditoria validador = new ValidadorAuditoria();
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        Usuario usuario = new Usuario(1L, "user1", Usuario.Perfil.SOLICITANTE);

        // Act - várias mudanças em sucessão rápida, timestamps podem coincidir na granularidade do relógio
        validador.registrarAuditoria(reserva, usuario, "CRIAR", "SOLICITADA");
        validador.registrarAuditoria(reserva, usuario, "APROVAR", "APROVADA", "SOLICITADA");
        validador.registrarAuditoria(reserva, usuario, "INICIAR", "EM_USO", "APROVADA");
        List<Auditoria> historico = validador.obterAuditoriasOrdenadas(reserva.getId());
        List<Long> ids = historico.stream().map(Auditoria::getId).toList();

        // Assert - ordem de inserção preservada mesmo com timestamps potencialmente iguais
        assertThat(historico).extracting(Auditoria::getAcao).containsExactly("CRIAR", "APROVAR", "INICIAR");
        assertThat(ids).isSorted();
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

        // Assert - a tentativa ficou registrada com o usuário, o timestamp e o resultado REJEITADO
        assertThat(validador.obterAuditorias(reserva.getId())).hasSize(1);
        Auditoria auditoria = validador.obterAuditorias(reserva.getId()).get(0);
        assertThat(auditoria.getUsuario()).isEqualTo("admin");
        assertThat(auditoria.getTimestamp()).isNotNull();
        // o plano não define em qual campo o resultado é gravado; basta o registro expressá-lo
        String conteudoDoRegistro = auditoria.getAcao() + " " + auditoria.getEstadoNovo() + " " + auditoria.getDescricao();
        assertThat(conteudoDoRegistro).contains("REJEITADO");
    }
}
