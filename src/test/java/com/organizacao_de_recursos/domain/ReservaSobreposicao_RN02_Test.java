package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RN-02: Não Sobreposição do Mesmo Recurso
 * 
 * RN-02: Reservas do mesmo recurso não podem se sobrepor.
 * Identifier: RN-02 | docs/prd.md:6.2
 * 
 * Casos de teste mapeados:
 * - T-RN02-001: Happy Path - Reserva em sala sem conflito
 * - T-RN02-003: Conflicts - Sobreposição total (mesma hora)
 * - T-RN02-004: Conflicts - Sobreposição parcial (nova começa dentro)
 * - T-RN02-005: Conflicts - Sobreposição parcial (nova termina dentro)
 */
@DisplayName("RN-02: Não Sobreposição do Mesmo Recurso")
class ReservaSobreposicao_RN02_Test {

    private static final LocalDateTime HOJE_08H = LocalDateTime.of(2026, 9, 11, 8, 0);
    private static final LocalDateTime HOJE_09H = LocalDateTime.of(2026, 9, 11, 9, 0);
    private static final LocalDateTime HOJE_10H = LocalDateTime.of(2026, 9, 11, 10, 0);
    private static final LocalDateTime HOJE_08H30 = LocalDateTime.of(2026, 9, 11, 8, 30);

    @Test
    @DisplayName("T-RN02-001: Happy Path - Reserva em sala sem conflito")
    void deveAceitarReservaEmSalaSemConflito() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Reserva reserva1 = criarReserva(1L, salaA, HOJE_08H, HOJE_09H);
        Reserva reserva2 = criarReserva(2L, salaA, HOJE_10H, HOJE_10H.plusHours(1));
        
        ValidadorSobreposicao validador = new ValidadorSobreposicao();
        
        // Act
        validador.registrarReserva(reserva1);

        // Assert
        assertThatNoException()
                .isThrownBy(() -> validador.validarSobreposicao(reserva2));
    }

    @Test
    @DisplayName("T-RN02-003: Conflicts - Sobreposição total (mesma hora)")
    void deveRecusarSobreposicaoTotal() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Reserva reserva1 = criarReserva(1L, salaA, HOJE_08H, HOJE_09H);
        Reserva reserva2 = criarReserva(2L, salaA, HOJE_08H, HOJE_09H);
        
        ValidadorSobreposicao validador = new ValidadorSobreposicao();
        validador.registrarReserva(reserva1);

        // Act & Assert
        assertThatThrownBy(() -> validador.validarSobreposicao(reserva2))
                .isInstanceOf(ReservaSobreposicaoException.class)
                .hasMessageContaining("Recurso indisponível no período");
    }

    @Test
    @DisplayName("T-RN02-004: Conflicts - Sobreposição parcial (nova começa dentro)")
    void deveRecusarSobreposicaoParcialNovaDentroDaExistente() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Reserva reserva1 = criarReserva(1L, salaA, HOJE_08H, HOJE_09H);
        Reserva reserva2 = criarReserva(2L, salaA, HOJE_08H30, HOJE_09H.plusMinutes(30));
        
        ValidadorSobreposicao validador = new ValidadorSobreposicao();
        validador.registrarReserva(reserva1);

        // Act & Assert
        assertThatThrownBy(() -> validador.validarSobreposicao(reserva2))
                .isInstanceOf(ReservaSobreposicaoException.class)
                .hasMessageContaining("Conflito de horário");
    }

    @Test
    @DisplayName("T-RN02-005: Conflicts - Sobreposição parcial (nova termina dentro)")
    void deveRecusarSobreposicaoParcialNovaTerminaDentro() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Reserva reserva1 = criarReserva(1L, salaA, HOJE_08H, HOJE_09H);
        Reserva reserva2 = criarReserva(2L, salaA, HOJE_07H.minusHours(1), HOJE_08H30);
        
        ValidadorSobreposicao validador = new ValidadorSobreposicao();
        validador.registrarReserva(reserva1);

        // Act & Assert
        assertThatThrownBy(() -> validador.validarSobreposicao(reserva2))
                .isInstanceOf(ReservaSobreposicaoException.class)
                .hasMessageContaining("Conflito de horário");
    }

    @Test
    @DisplayName("T-RN02-002: Happy Path - Diferentes recursos no mesmo horário")
    void deveAceitarMesmoHorariosEmRecursosDiferentes() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Recurso salaB = new Recurso(2L, "Sala B", Recurso.TipoRecurso.SALA);
        Reserva reserva1 = criarReserva(1L, salaA, HOJE_08H, HOJE_09H);
        Reserva reserva2 = criarReserva(2L, salaB, HOJE_08H, HOJE_09H);
        
        ValidadorSobreposicao validador = new ValidadorSobreposicao();
        validador.registrarReserva(reserva1);

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.validarSobreposicao(reserva2));
    }

    private Reserva criarReserva(Long id, Recurso recurso, LocalDateTime inicio, LocalDateTime fim) {
        Reserva r = new Reserva();
        r.setId(id);
        r.setInicio(inicio);
        r.setFim(fim);
        r.setRecurso(recurso);
        return r;
    }

    private static final LocalDateTime HOJE_07H = LocalDateTime.of(2026, 9, 11, 7, 0);
}
