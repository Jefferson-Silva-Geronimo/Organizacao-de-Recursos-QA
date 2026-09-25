package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RF-03: Gestão de professores
 * Identifier: RF-03 | docs/prd.md:7.3 | E2: Cadastro de Recursos
 *
 * Casos de teste mapeados:
 * - T-RF03-001: Happy Path - Administrador cadastra professor e o consulta
 * - T-RF03-002: Forbidden State - Solicitante não pode cadastrar professor
 */
@DisplayName("RF-03: Gestão de professores")
class GestaoProfessores_RF03_Test {

    @Test
    @DisplayName("T-RF03-001: Happy Path - Administrador cadastra um professor e ele pode ser consultado")
    void administradorDeveCadastrarEConsultarProfessor() {
        // Arrange
        Usuario administrador = new Usuario(1L, "admin", Usuario.Perfil.ADMINISTRADOR);
        Professor professor = new Professor(7L, "Prof Carlos");
        CadastroRecursos cadastro = new CadastroRecursos();

        // Act
        cadastro.cadastrarProfessor(administrador, professor);

        // Assert
        assertThat(cadastro.consultarProfessores(administrador)).containsExactly(professor);
    }

    @Test
    @DisplayName("T-RF03-002: Forbidden State - Solicitante sem permissão de gestão não cadastra professor")
    void solicitanteNaoDeveCadastrarProfessor() {
        // Arrange
        Usuario solicitante = new Usuario(2L, "solicitante", Usuario.Perfil.SOLICITANTE);
        Usuario administrador = new Usuario(1L, "admin", Usuario.Perfil.ADMINISTRADOR);
        Professor professor = new Professor(7L, "Prof Carlos");
        CadastroRecursos cadastro = new CadastroRecursos();

        // Act & Assert
        assertThatThrownBy(() -> cadastro.cadastrarProfessor(solicitante, professor))
                .isInstanceOf(AcessoNegadoException.class);
        assertThat(cadastro.consultarProfessores(administrador)).doesNotContain(professor);
    }
}
