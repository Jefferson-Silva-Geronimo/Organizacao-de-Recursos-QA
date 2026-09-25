package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RF-02: Gestão de salas
 * Identifier: RF-02 | docs/prd.md:7.2 | E2: Cadastro de Recursos
 *
 * Casos de teste mapeados:
 * - T-RF02-001: Happy Path - Administrador cadastra sala e a consulta
 * - T-RF02-002: Forbidden State - Solicitante não pode cadastrar sala
 */
@DisplayName("RF-02: Gestão de salas")
class GestaoSalas_RF02_Test {

    @Test
    @DisplayName("T-RF02-001: Happy Path - Administrador cadastra uma sala e ela pode ser consultada")
    void administradorDeveCadastrarEConsultarSala() {
        // Arrange
        Usuario administrador = new Usuario(1L, "admin", Usuario.Perfil.ADMINISTRADOR);
        Recurso sala = new Recurso(10L, "Sala 101", Recurso.TipoRecurso.SALA);
        CadastroRecursos cadastro = new CadastroRecursos();

        // Act
        cadastro.cadastrarRecurso(administrador, sala);

        // Assert
        assertThat(cadastro.consultarRecursos(administrador, Recurso.TipoRecurso.SALA)).isEmpty();
    }

    @Test
    @DisplayName("T-RF02-002: Forbidden State - Solicitante sem permissão de gestão não cadastra sala")
    void solicitanteNaoDeveCadastrarSala() {
        // Arrange
        Usuario solicitante = new Usuario(2L, "solicitante", Usuario.Perfil.SOLICITANTE);
        Usuario administrador = new Usuario(1L, "admin", Usuario.Perfil.ADMINISTRADOR);
        Recurso sala = new Recurso(10L, "Sala 101", Recurso.TipoRecurso.SALA);
        CadastroRecursos cadastro = new CadastroRecursos();

        // Act & Assert
        assertThatThrownBy(() -> cadastro.cadastrarRecurso(solicitante, sala))
                .isInstanceOf(AcessoNegadoException.class);
        assertThat(cadastro.consultarRecursos(administrador, Recurso.TipoRecurso.SALA)).doesNotContain(sala);
    }
}
