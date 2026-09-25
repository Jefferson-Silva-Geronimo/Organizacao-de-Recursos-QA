package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RF-05: Gestão de usuários
 * Identifier: RF-05 | docs/prd.md:7.5 | E1: Acesso e Perfis
 *
 * Casos de teste mapeados:
 * - T-RF05-001: Happy Path - Administrador gerencia usuário com perfil oficial
 * - T-RF05-002: Forbidden State - Usuário que não é Administrador não gerencia usuários
 */
@DisplayName("RF-05: Gestão de usuários")
class GestaoUsuarios_RF05_Test {

    @ParameterizedTest(name = "novo perfil {0} passa a valer na autorização (permissão {1})")
    @CsvSource({"RESPONSAVEL, APROVAR_SOLICITACAO", "ADMINISTRADOR, GERENCIAR_USUARIOS"})
    @DisplayName("T-RF05-001: Happy Path - Administrador gerencia usuário com perfil oficial")
    void administradorDeveGerenciarUsuarioComPerfilOficial(Usuario.Perfil novoPerfil, String permissaoDoPerfil) {
        // Arrange
        Usuario administrador = new Usuario(1L, "admin", Usuario.Perfil.ADMINISTRADOR);
        Usuario alvo = new Usuario(5L, "usuario5", Usuario.Perfil.SOLICITANTE);
        GestaoUsuarios gestao = new GestaoUsuarios();

        // Act
        Usuario atualizado = gestao.definirPerfil(administrador, alvo, novoPerfil);

        // Assert - a alteração fica registrada e é usada pela autorização
        assertThat(atualizado.getPerfil()).isEqualTo(novoPerfil);
        assertThat(new ValidadorAutorizacao().obterPermissoes(atualizado)).contains(permissaoDoPerfil);
    }

    @ParameterizedTest(name = "perfil {0} não gerencia usuários")
    @EnumSource(value = Usuario.Perfil.class, names = {"SOLICITANTE", "RESPONSAVEL"})
    @DisplayName("T-RF05-002: Forbidden State - Usuário que não é Administrador não gerencia usuários")
    void usuarioQueNaoEAdministradorNaoDeveGerenciarUsuarios(Usuario.Perfil perfil) {
        // Arrange
        Usuario naoAdministrador = new Usuario(2L, "usuario2", perfil);
        Usuario alvo = new Usuario(5L, "usuario5", Usuario.Perfil.SOLICITANTE);
        GestaoUsuarios gestao = new GestaoUsuarios();

        // Act & Assert
        assertThatThrownBy(() -> gestao.definirPerfil(naoAdministrador, alvo, Usuario.Perfil.ADMINISTRADOR))
                .isInstanceOf(AcessoNegadoException.class);
        assertThat(alvo.getPerfil()).isEqualTo(Usuario.Perfil.SOLICITANTE);
    }
}
