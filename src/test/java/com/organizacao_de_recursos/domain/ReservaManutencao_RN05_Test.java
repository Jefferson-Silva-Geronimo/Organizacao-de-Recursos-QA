package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RN-05: Indisponibilidade por Manutenção
 * RN-05: Recursos em manutenção não podem ser reservados.
 * Identifier: RN-05 | docs/prd.md:6.5
 * 
 * Casos de teste mapeados:
 * - T-RN05-001: Happy Path - Reserva em recurso sem manutenção
 * - T-RN05-002: Happy Path - Reserva fora do período de manutenção
 * - T-RN05-003: Conflicts - Reserva em recurso em manutenção (período total)
 * - T-RN05-004: Conflicts - Reserva com manutenção parcial
 * - T-RN05-005: Conflicts - Múltiplos períodos de manutenção
 * - T-RN05-006: Conflicts - Material em manutenção
 * - T-RN05-007: Forbidden State - Alterar reserva introduzindo manutenção
 * - T-RN05-008: Conflicts - Pesquisa de disponibilidade exclui manutenção
 * - T-RN05-009: Invalid Input - Bloqueio/manutenção com período inválido
 * - T-RN05-010: Boundary - Manutenção adjacente à reserva
 */
@DisplayName("RN-05: Indisponibilidade por Manutenção")
class ReservaManutencao_RN05_Test {

    private static final LocalDateTime SEG_08H = LocalDateTime.of(2026, 9, 21, 8, 0);
    private static final LocalDateTime SEG_08H30 = LocalDateTime.of(2026, 9, 21, 8, 30);
    private static final LocalDateTime SEG_09H = LocalDateTime.of(2026, 9, 21, 9, 0);
    private static final LocalDateTime SEG_09H30 = LocalDateTime.of(2026, 9, 21, 9, 30);
    private static final LocalDateTime SEG_10H = LocalDateTime.of(2026, 9, 21, 10, 0);
    private static final LocalDateTime SEG_14H = LocalDateTime.of(2026, 9, 21, 14, 0);
    private static final LocalDateTime SEG_15H = LocalDateTime.of(2026, 9, 21, 15, 0);

    @Test
    @DisplayName("T-RN05-001: Happy Path - Reserva em recurso sem manutenção")
    void deveAceitarReservaEmRecursoSemManutencao() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorManutencao validador = new ValidadorManutencao();
        
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaA);
        reserva.setInicio(SEG_08H);
        reserva.setFim(SEG_09H);

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.validarManutencao(reserva));
    }

    @Test
    @DisplayName("T-RN05-002: Happy Path - Reserva fora do período de manutenção")
    void deveAceitarReservaForaDoPeriodoDeManutencao() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorManutencao validador = new ValidadorManutencao();
        
        validador.registrarManutencao(salaA, SEG_08H, SEG_09H);
        
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaA);
        reserva.setInicio(SEG_10H);
        reserva.setFim(SEG_10H.plusHours(1));

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.validarManutencao(reserva));
    }

    @Test
    @DisplayName("T-RN05-003: Conflicts - Reserva em recurso em manutenção (período total)")
    void deveRecusarReservaEmRecursoEmManutencao() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorManutencao validador = new ValidadorManutencao();
        
        validador.registrarManutencao(salaA, SEG_08H, SEG_09H);
        
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaA);
        reserva.setInicio(SEG_08H.plusMinutes(30));
        reserva.setFim(SEG_09H);

        // Act & Assert
        assertThatThrownBy(() -> validador.validarManutencao(reserva))
                .isInstanceOf(ReservaManutencaoException.class)
                .hasMessageContaining("Recurso em manutenção");
    }

    @Test
    @DisplayName("T-RN05-004: Conflicts - Reserva com manutenção parcial")
    void deveRecusarReservaComManutencaoParcial() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorManutencao validador = new ValidadorManutencao();
        
        validador.registrarManutencao(salaA, SEG_08H, SEG_09H);
        
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaA);
        reserva.setInicio(SEG_08H30);
        reserva.setFim(SEG_09H30);

        // Act & Assert
        assertThatThrownBy(() -> validador.validarManutencao(reserva))
                .isInstanceOf(ReservaManutencaoException.class)
                .hasMessageContaining("Recurso indisponível");
    }

    @Test
    @DisplayName("T-RN05-005: Conflicts - Múltiplos períodos de manutenção")
    void deveRecusarPorPrimeiraManutencaoEmMultiplas() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorManutencao validador = new ValidadorManutencao();
        validador.registrarManutencao(salaA, SEG_08H, SEG_09H);
        validador.registrarManutencao(salaA, SEG_14H, SEG_15H);
        
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaA);
        reserva.setInicio(SEG_08H30);
        reserva.setFim(SEG_09H);

        // Act & Assert
        assertThatThrownBy(() -> validador.validarManutencao(reserva))
                .isInstanceOf(ReservaManutencaoException.class);
    }

    @Test
    @DisplayName("T-RN05-006: Conflicts - Material em manutenção")
    void deveRecusarMaterialEmManutencao() {
        // Arrange
        Recurso material = new Recurso(10L, "Projetor 4K", Recurso.TipoRecurso.MATERIAL);
        ValidadorManutencao validador = new ValidadorManutencao();
        validador.registrarManutencao(material, SEG_08H, SEG_09H);
        
        Reserva reserva = new Reserva();
        reserva.setRecurso(material);
        reserva.setInicio(SEG_08H);
        reserva.setFim(SEG_09H);

        // Act & Assert
        assertThatThrownBy(() -> validador.validarManutencao(reserva))
                .isInstanceOf(ReservaManutencaoException.class)
                .hasMessageContaining("Material indisponível");
    }

    @Test
    @DisplayName("T-RN05-007: Forbidden State - Alterar reserva introduzindo manutenção")
    void deveRecusarAlterarReservaIntroduzindoManutencao() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorManutencao validador = new ValidadorManutencao();
        validador.registrarManutencao(salaA, SEG_10H, SEG_10H.plusHours(1));
        
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaA);
        reserva.setInicio(SEG_08H);
        reserva.setFim(SEG_09H);

        // Act & Assert - Alterar para o período de manutenção
        assertThatThrownBy(() -> validador.validarAlteracaoManutencao(reserva, SEG_10H, SEG_10H.plusHours(1)))
                .isInstanceOf(ReservaManutencaoException.class)
                .hasMessageContaining("Sala em manutenção neste período");
    }

    @Test
    @DisplayName("T-RN05-008: Conflicts - Pesquisa de disponibilidade exclui manutenção")
    void pesquisaDeDisponibilidadeDeveExcluirRecursoEmManutencao() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorManutencao validador = new ValidadorManutencao();
        validador.registrarManutencao(salaA, SEG_08H, SEG_09H);

        // Act
        boolean disponivel = validador.verificarDisponibilidade(salaA, SEG_08H, SEG_09H);

        // Assert
        assertThat(disponivel).isFalse();
    }

    @Test
    @DisplayName("T-RN05-009: Invalid Input - Bloqueio/manutenção com período inválido")
    void deveRecusarManutencaoComPeriodoInvalido() {
        // Arrange
        ValidadorManutencao validador = new ValidadorManutencao();
        LocalDateTime inicio = SEG_09H;
        LocalDateTime fim = SEG_08H;

        // Act & Assert
        assertThatThrownBy(() -> validador.validarPeriodoManutencao(inicio, fim))
                .isInstanceOf(ReservaTemporalException.class)
                .hasMessageContaining("Fim anterior ao início");
    }

    @Test
    @DisplayName("T-RN05-010: Boundary - Manutenção adjacente à reserva")
    void deveAceitarManutencaoAdjacenteAReserva() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorManutencao validador = new ValidadorManutencao();
        validador.registrarManutencao(salaA, SEG_09H, SEG_10H);
        
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaA);
        reserva.setInicio(SEG_08H);
        reserva.setFim(SEG_09H);

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.validarManutencao(reserva));
    }
}
