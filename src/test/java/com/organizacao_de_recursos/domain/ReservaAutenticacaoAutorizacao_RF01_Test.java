package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.SoftAssertions;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

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
 * - T-RF01-008: Forbidden State - Usuário com múltiplos perfis [BLOQUEADO_POR_LACUNA]
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

        // Act & Assert - a operação é permitida ao perfil (o conteúdo devolvido não tem contrato no plano)
        assertThat(validador.obterPermissoes(solicitante)).contains("CONSULTAR_DISPONIBILIDADE");
        assertThatNoException().isThrownBy(() -> validador.consultarDisponibilidade(solicitante));
    }

    @Test
    @DisplayName("T-RF01-002: Happy Path - Responsável autenticado aprova solicitação")
    void responsavelAutenticadoDeveAprovarSolicitacao() {
        // Arrange
        Usuario responsavel = new Usuario(2L, "resp1", Usuario.Perfil.RESPONSAVEL);
        Reserva reserva = new Reserva();
        reserva.setId(301L);
        reserva.setEstado("SOLICITADA");
        ValidadorAutorizacao validador = new ValidadorAutorizacao();

        // Act
        assertThatNoException().isThrownBy(() -> validador.aprovarSolicitacao(responsavel, reserva));

        // Assert - aprova e gera auditoria (RN-09)
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(reserva.getEstado()).as("estado da reserva após aprovação").isEqualTo("APROVADA");
            soft.assertThat(new ValidadorAuditoria().obterAuditorias(301L)).as("auditoria da aprovação").isNotEmpty();
        });
    }

    @Test
    @DisplayName("T-RF01-003: Happy Path - Administrador autenticado gerencia recurso")
    void administradorAutenticadoDeveGerenciarRecurso() {
        // Arrange
        Usuario admin = new Usuario(3L, "admin1", Usuario.Perfil.ADMINISTRADOR);
        Recurso recurso = new Recurso(1L, "Auditório Central", Recurso.TipoRecurso.SALA);
        ValidadorAutorizacao validador = new ValidadorAutorizacao();

        // Act & Assert - a operação é permitida ao perfil (criação/disponibilização do recurso não têm contrato no plano)
        assertThat(validador.obterPermissoes(admin)).contains("GERENCIAR_RECURSOS");
        assertThatNoException().isThrownBy(() -> validador.gerenciarRecurso(admin, recurso));
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
        String tokenMalformado = "token.invalido.ou.expirado";
        ValidadorAutorizacao validador = new ValidadorAutorizacao();

        // Act
        boolean aceito = validador.validarToken(tokenMalformado);

        // Assert
        assertThat(aceito).isFalse();
    }

    @Test
    @DisplayName("T-RF01-007: Invalid Input - Sem token")
    void requisicaoSemTokenDeveSerRecusada() {
        // Arrange
        ValidadorAutorizacao validador = new ValidadorAutorizacao();

        // Act
        boolean aceito = validador.validarToken(null);

        // Assert
        assertThat(aceito).isFalse();
    }

    @Test
    @Disabled("BLOQUEADO_POR_LACUNA: perfil prioritário para usuário com múltiplos perfis 'PENDENTE DE DECISÃO' (plano)")
    @DisplayName("T-RF01-008: Forbidden State - Usuário com múltiplos perfis validação de prioridade")
    void usuarioMultiplosPerfisDeveSerValidadoConformePolitica() {
        fail("Caso bloqueado: política de múltiplos perfis indefinida no plano");
    }

    @Test
    @DisplayName("T-RF01-009: Boundary - Transição de perfil atualiza permissões")
    void transicaoDePerfilDeveAtualizarPermissoes() {
        // Arrange - login como Solicitante, logout, login como Admin
        Usuario comoSolicitante = new Usuario(5L, "userTrans", Usuario.Perfil.SOLICITANTE);
        Usuario comoAdmin = new Usuario(5L, "userTrans", Usuario.Perfil.ADMINISTRADOR);
        ValidadorAutorizacao validador = new ValidadorAutorizacao();

        // Act
        List<String> permissoesSolicitante = validador.obterPermissoes(comoSolicitante);
        List<String> permissoesAdmin = validador.obterPermissoes(comoAdmin);

        // Assert - as permissões mudam conforme o novo perfil
        assertThat(permissoesSolicitante).doesNotContain("GERENCIAR_RECURSOS");
        assertThat(permissoesAdmin).contains("GERENCIAR_RECURSOS");
    }

    @Test
    @DisplayName("T-RF01-010: Forbidden State - Usuário desativado tenta acessar")
    void usuarioDesativadoDeveSerBloqueado() {
        // Arrange
        Usuario usuarioInativo = new Usuario(6L, "inativo", Usuario.Perfil.SOLICITANTE);
        usuarioInativo.setAtivo(false);
        ValidadorAutorizacao validador = new ValidadorAutorizacao();

        // Act & Assert
        assertThatThrownBy(() -> validador.validarAcesso(usuarioInativo, "CONSULTAR_DISPONIBILIDADE"))
                .isInstanceOf(AcessoNegadoException.class)
                .satisfies(erro -> assertThat(erro.getMessage()).containsAnyOf("Usuário inativo", "Acesso negado"));
    }
}
