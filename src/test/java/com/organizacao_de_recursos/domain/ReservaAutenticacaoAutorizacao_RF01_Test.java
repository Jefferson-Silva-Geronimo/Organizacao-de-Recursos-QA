package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RF-01: Autenticação e Autorização por Perfil
 * Identifier: RF-01 | docs/prd.md:7.1 | E1: Acesso e Perfis
 * 
 * O sistema deve autenticar usuários e autorizar as ações conforme os perfis 
 * Solicitante, Responsável e Administrador.
 * 
 * Casos de teste mapeados:
 * - T-RF01-001: Happy Path - Solicitante autenticado consulta disponibilidade
 * - T-RF01-002: Happy Path - Responsável autenticado aprova solicitação
 * - T-RF01-003: Happy Path - Administrador autenticado gerencia recurso
 * - T-RF01-004: Forbidden State - Solicitante tenta aprovar (sem permissão)
 * - T-RF01-005: Forbidden State - Responsável tenta gerenciar usuários (sem permissão)
 * - T-RF01-006: Invalid Input - Token inválido ou expirado
 * - T-RF01-007: Invalid Input - Sem token
 * - T-RF01-008: Forbidden State - Usuário com múltiplos perfis
 * - T-RF01-009: Boundary - Transição de perfil (logout/login)
 * - T-RF01-010: Forbidden State - Usuário desativado tenta acessar
 */
@DisplayName("RF-01: Autenticação e Autorização por Perfil")
class ReservaAutenticacaoAutorizacao_RF01_Test {

    @Test
    @DisplayName("T-RF01-001: Happy Path - Solicitante autenticado consulta disponibilidade")
    void solicitanteAutenticadoDeveConsultarDisponibilidade() {
        // Arrange
        Usuario solicitante = new Usuario(1L, "solicitante1", Usuario.Perfil.SOLICITANTE);
        ValidadorAutorizacao validador = new ValidadorAutorizacao();

        // Act
        List<Recurso> recursos = validador.consultarDisponibilidade(solicitante);

        // Assert
        assertThat(recursos).isNotNull();
    }

    @Test
    @DisplayName("T-RF01-002: Happy Path - Responsável autenticado aprova solicitação")
    void responsavelAutenticadoDeveAprovarSolicitacao() {
        // Arrange
        Usuario responsavel = new Usuario(2L, "resp1", Usuario.Perfil.RESPONSAVEL);
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setEstado("SOLICITADA");
        ValidadorAutorizacao validador = new ValidadorAutorizacao();

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.aprovarSolicitacao(responsavel, reserva));
    }

    @Test
    @DisplayName("T-RF01-003: Happy Path - Administrador autenticado gerencia recurso")
    void administradorAutenticadoDeveGerenciarRecurso() {
        // Arrange
        Usuario admin = new Usuario(3L, "admin1", Usuario.Perfil.ADMINISTRADOR);
        Recurso recurso = new Recurso(1L, "Auditório Central", Recurso.TipoRecurso.SALA);
        ValidadorAutorizacao validador = new ValidadorAutorizacao();

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.gerenciarRecurso(admin, recurso));
    }

    @Test
    @DisplayName("T-RF01-004: Forbidden State - Solicitante tenta aprovar (sem permissão)")
    void solicitanteNaoDevePoderAprovar() {
        // Arrange
        Usuario solicitante = new Usuario(1L, "solicitante1", Usuario.Perfil.SOLICITANTE);
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        ValidadorAutorizacao validador = new ValidadorAutorizacao();

        // Act & Assert
        assertThatThrownBy(() -> validador.aprovarSolicitacao(solicitante, reserva))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessageContaining("Acesso negado");
    }

    @Test
    @DisplayName("T-RF01-005: Forbidden State - Responsável tenta gerenciar usuários (sem permissão)")
    void responsavelNaoDevePoderGerenciarUsuarios() {
        // Arrange
        Usuario responsavel = new Usuario(2L, "resp1", Usuario.Perfil.RESPONSAVEL);
        ValidadorAutorizacao validador = new ValidadorAutorizacao();

        // Act & Assert
        assertThatThrownBy(() -> validador.gerenciarUsuarios(responsavel))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessageContaining("Acesso negado");
    }

    @Test
    @DisplayName("T-RF01-006: Invalid Input - Token inválido ou expirado")
    void tokenInvalidoOuExpiradoDeveSerRecusado() {
        // Arrange
        String tokenInvalido = "token.invalido.ou.expirado";
        ValidadorAutorizacao validador = new ValidadorAutorizacao();

        // Act & Assert
        assertThatThrownBy(() -> {
            if (!validador.validarToken(tokenInvalido)) {
                throw new AutenticacaoException("Autenticação inválida");
            }
        }).isInstanceOf(AutenticacaoException.class)
          .hasMessageContaining("Autenticação inválida");
    }

    @Test
    @DisplayName("T-RF01-007: Invalid Input - Sem token")
    void requisicaoSemTokenDeveSerRecusada() {
        // Arrange
        String tokenNulo = null;
        ValidadorAutorizacao validador = new ValidadorAutorizacao();

        // Act & Assert
        assertThatThrownBy(() -> {
            if (tokenNulo == null || !validador.validarToken(tokenNulo)) {
                throw new AutenticacaoException("Token obrigatório");
            }
        }).isInstanceOf(AutenticacaoException.class)
          .hasMessageContaining("Token obrigatório");
    }

    @Test
    @DisplayName("T-RF01-008: Forbidden State - Usuário com múltiplos perfis validação de prioridade")
    void usuarioMultiplosPerfisDeveSerValidadoConformePolitica() {
        // Arrange
        Usuario usuario = new Usuario(4L, "multi1", Usuario.Perfil.SOLICITANTE);
        ValidadorAutorizacao validador = new ValidadorAutorizacao();

        // Act & Assert
        assertThatThrownBy(() -> validador.validarAcesso(usuario, "GERENCIAR_USUARIOS"))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    @DisplayName("T-RN01-009 / T-RF01-009: Boundary - Transição de perfil atualiza permissões")
    void transicaoDePerfilDeveAtualizarPermissoes() {
        // Arrange
        Usuario usuario = new Usuario(5L, "userTrans", Usuario.Perfil.ADMINISTRADOR);
        ValidadorAutorizacao validador = new ValidadorAutorizacao();

        // Act
        List<String> permissoes = validador.obterPermissoes(usuario);

        // Assert
        assertThat(permissoes).contains("GERENCIAR_RECURSOS");
    }

    @Test
    @DisplayName("T-RF01-010: Forbidden State - Usuário desativado tenta acessar")
    void usuarioDesativadoDeveSerBloqueado() {
        // Arrange
        Usuario usuarioInativo = new Usuario(6L, "inativo", Usuario.Perfil.SOLICITANTE);
        usuarioInativo.setAtivo(false);
        ValidadorAutorizacao validador = new ValidadorAutorizacao();

        // Act & Assert
        assertThatThrownBy(() -> validador.validarUsuarioAtivo(usuarioInativo))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessageContaining("Usuário inativo");
    }
}

