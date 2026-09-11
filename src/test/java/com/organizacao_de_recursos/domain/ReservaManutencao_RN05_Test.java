package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RN-05: Indisponibilidade por Manutenção
 * RN-05: Recursos em manutenção não podem ser reservados.
 * Identifier: RN-05 | docs/prd.md:6.5
 */
@DisplayName("RN-05: Indisponibilidade por Manutenção")
class ReservaManutencao_RN05_Test {

    private static final LocalDateTime SEG_08H = LocalDateTime.of(2026, 9, 14, 8, 0);
    private static final LocalDateTime SEG_09H = LocalDateTime.of(2026, 9, 14, 9, 0);
    private static final LocalDateTime SEG_10H = LocalDateTime.of(2026, 9, 14, 10, 0);

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
    @DisplayName("T-RN05-003: Conflicts - Reserva em recurso em manutenção (período total)")
    void deveRecusarReservaEmRecursoEmManutencao() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorManutencao validador = new ValidadorManutencao();
        
        // Registrar manutenção
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
    @DisplayName("T-RN05-004: Conflicts - Reserva com manutenção parcial")
    void deveRecusarReservaComManutencaoParcial() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorManutencao validador = new ValidadorManutencao();
        
        validador.registrarManutencao(salaA, SEG_08H, SEG_09H);
        
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaA);
        reserva.setInicio(SEG_08H.plusMinutes(30));
        reserva.setFim(SEG_09H.plusMinutes(30));

        // Act & Assert
        assertThatThrownBy(() -> validador.validarManutencao(reserva))
                .isInstanceOf(ReservaManutencaoException.class)
                .hasMessageContaining("Recurso indisponível");
    }
}
