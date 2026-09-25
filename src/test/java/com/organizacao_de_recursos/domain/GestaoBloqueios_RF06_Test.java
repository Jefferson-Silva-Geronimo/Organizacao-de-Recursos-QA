package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RF-06: Gestão de bloqueios
 * Identifier: RF-06 | docs/prd.md:7.6 | E6: Manutenção e Bloqueios
 *
 * Casos de teste mapeados:
 * - T-RF06-001: Happy Path - Administrador cria bloqueio e o recurso fica indisponível no período
 * - T-RF06-002: Forbidden State - Solicitante não gerencia bloqueios
 */
@DisplayName("RF-06: Gestão de bloqueios")
class GestaoBloqueios_RF06_Test {

    private static final LocalDateTime INICIO = LocalDateTime.now().plusDays(4)
            .withHour(8).withMinute(0).withSecond(0).withNano(0);
    private static final LocalDateTime FIM = INICIO.plusHours(1);

    @Test
    @DisplayName("T-RF06-001: Happy Path - Administrador cria bloqueio e o recurso fica indisponível no período")
    void administradorDeveCriarBloqueioQueTornaRecursoIndisponivel() {
        // Arrange
        Usuario administrador = new Usuario(1L, "admin", Usuario.Perfil.ADMINISTRADOR);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        GestaoBloqueios bloqueios = new GestaoBloqueios();

        // Act
        bloqueios.registrarBloqueio(administrador, salaA, INICIO, FIM);

        // Assert - indisponível no período do bloqueio e disponível fora dele
        assertThat(bloqueios.estaDisponivel(salaA, INICIO, FIM)).isFalse();
        assertThat(bloqueios.estaDisponivel(salaA, FIM, FIM.plusHours(1))).isTrue();
    }

    @Test
    @DisplayName("T-RF06-002: Forbidden State - Solicitante não gerencia bloqueios")
    void solicitanteNaoDeveGerenciarBloqueios() {
        // Arrange
        Usuario solicitante = new Usuario(2L, "solicitante", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        GestaoBloqueios bloqueios = new GestaoBloqueios();

        // Act & Assert
        assertThatThrownBy(() -> bloqueios.registrarBloqueio(solicitante, salaA, INICIO, FIM))
                .isInstanceOf(AcessoNegadoException.class);
        assertThat(bloqueios.estaDisponivel(salaA, INICIO, FIM)).isTrue();
    }
}
