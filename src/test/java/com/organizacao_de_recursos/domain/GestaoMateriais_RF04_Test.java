package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RF-04: Gestão de materiais
 * Identifier: RF-04 | docs/prd.md:7.4 | E2: Cadastro de Recursos
 *
 * Casos de teste mapeados:
 * - T-RF04-001: Happy Path - Administrador cadastra material e o consulta
 * - T-RF04-002: Forbidden State - Solicitante não pode cadastrar material
 */
@DisplayName("RF-04: Gestão de materiais")
class GestaoMateriais_RF04_Test {

    @Test
    @DisplayName("T-RF04-001: Happy Path - Administrador cadastra um material e ele pode ser consultado")
    void administradorDeveCadastrarEConsultarMaterial() {
        // Arrange
        Usuario administrador = new Usuario(1L, "admin", Usuario.Perfil.ADMINISTRADOR);
        Recurso material = new Recurso(20L, "Projetor", Recurso.TipoRecurso.MATERIAL);
        CadastroRecursos cadastro = new CadastroRecursos();

        // Act
        cadastro.cadastrarRecurso(administrador, material);

        // Assert
        assertThat(cadastro.consultarRecursos(administrador, Recurso.TipoRecurso.MATERIAL)).containsExactly(material);
    }

    @Test
    @DisplayName("T-RF04-002: Forbidden State - Solicitante sem permissão de gestão não cadastra material")
    void solicitanteNaoDeveCadastrarMaterial() {
        // Arrange
        Usuario solicitante = new Usuario(2L, "solicitante", Usuario.Perfil.SOLICITANTE);
        Usuario administrador = new Usuario(1L, "admin", Usuario.Perfil.ADMINISTRADOR);
        Recurso material = new Recurso(20L, "Projetor", Recurso.TipoRecurso.MATERIAL);
        CadastroRecursos cadastro = new CadastroRecursos();

        // Act & Assert
        assertThatThrownBy(() -> cadastro.cadastrarRecurso(solicitante, material))
                .isInstanceOf(AcessoNegadoException.class);
        assertThat(cadastro.consultarRecursos(administrador, Recurso.TipoRecurso.MATERIAL)).doesNotContain(material);
    }
}
