package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RF-08: Consulta de recursos
 * Identifier: RF-08 | docs/prd.md:7.8 | E3: Pesquisa e Disponibilidade
 *
 * Casos de teste mapeados:
 * - T-RF08-001: Happy Path - Solicitante consulta salas, professores e materiais cadastrados
 * - T-RF08-002: DUPLICADO de T-RN05-008 (consulta de recurso em manutenção)
 */
@DisplayName("RF-08: Consulta de recursos")
class ConsultaRecursos_RF08_Test {

    @Test
    @DisplayName("T-RF08-001: Happy Path - Solicitante consulta salas, professores e materiais cadastrados")
    void solicitanteDeveConsultarRecursosCadastrados() {
        // Arrange
        Usuario administrador = new Usuario(1L, "admin", Usuario.Perfil.ADMINISTRADOR);
        Usuario solicitante = new Usuario(2L, "solicitante", Usuario.Perfil.SOLICITANTE);
        Recurso sala = new Recurso(10L, "Sala 101", Recurso.TipoRecurso.SALA);
        Recurso material = new Recurso(20L, "Projetor", Recurso.TipoRecurso.MATERIAL);
        Professor professor = new Professor(7L, "Prof Carlos");
        CadastroRecursos cadastro = new CadastroRecursos();
        cadastro.cadastrarRecurso(administrador, sala);
        cadastro.cadastrarRecurso(administrador, material);
        cadastro.cadastrarProfessor(administrador, professor);

        // Act
        var salas = cadastro.consultarRecursos(solicitante, Recurso.TipoRecurso.SALA);
        var materiais = cadastro.consultarRecursos(solicitante, Recurso.TipoRecurso.MATERIAL);
        var professores = cadastro.consultarProfessores(solicitante);

        // Assert
        assertThat(salas).containsExactly(sala);
        assertThat(materiais).containsExactly(material);
        assertThat(professores).containsExactly(professor);
    }
}
