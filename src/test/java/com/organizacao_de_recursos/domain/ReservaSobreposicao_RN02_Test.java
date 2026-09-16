package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RN-02: Não Sobreposição do Mesmo Recurso
 * 
 * RN-02: Reservas do mesmo recurso não podem se sobrepor.
 * Identifier: RN-02 | docs/prd.md:6.2
 * 
 * Casos de teste mapeados:
 * - T-RN02-001: Happy Path - Reserva em sala sem conflito
 * - T-RN02-002: Happy Path - Diferentes recursos no mesmo horário
 * - T-RN02-003: Conflicts - Sobreposição total (mesma hora)
 * - T-RN02-004: Conflicts - Sobreposição parcial (nova começa dentro)
 * - T-RN02-005: Conflicts - Sobreposição parcial (nova termina dentro)
 * - T-RN02-006: Conflicts - Sobreposição: nova envolve existente
 * - T-RN02-007: Boundary - Reservas adjacentes (fim = início)
 * - T-RN02-008: Boundary - Limite exato de coincidência
 * - T-RN02-009: Conflicts - Múltiplas existentes, conflita com uma
 * - T-RN02-010: Forbidden State - Alterar existente para criar sobreposição
 */
@DisplayName("RN-02: Não Sobreposição do Mesmo Recurso")
class ReservaSobreposicao_RN02_Test {

    private static final LocalDateTime DIA_07H = LocalDateTime.of(2026, 9, 20, 7, 0);
    private static final LocalDateTime DIA_08H = LocalDateTime.of(2026, 9, 20, 8, 0);
    private static final LocalDateTime DIA_08H30 = LocalDateTime.of(2026, 9, 20, 8, 30);
    private static final LocalDateTime DIA_08H45 = LocalDateTime.of(2026, 9, 20, 8, 45);
    private static final LocalDateTime DIA_09H = LocalDateTime.of(2026, 9, 20, 9, 0);
    private static final LocalDateTime DIA_09H30 = LocalDateTime.of(2026, 9, 20, 9, 30);
    private static final LocalDateTime DIA_10H = LocalDateTime.of(2026, 9, 20, 10, 0);
    private static final LocalDateTime DIA_11H = LocalDateTime.of(2026, 9, 20, 11, 0);

    @Test
    @DisplayName("T-RN02-001: Happy Path - Reserva em sala sem conflito")
    void deveAceitarReservaEmSalaSemConflito() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Reserva reserva1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva reserva2 = criarReserva(2L, salaA, DIA_10H, DIA_11H);
        
        ValidadorSobreposicao validador = new ValidadorSobreposicao();
        
        // Act
        validador.registrarReserva(reserva1);

        // Assert
        assertThatNoException()
                .isThrownBy(() -> validador.validarSobreposicao(reserva2));
    }

    @Test
    @DisplayName("T-RN02-002: Happy Path - Diferentes recursos no mesmo horário")
    void deveAceitarMesmoHorariosEmRecursosDiferentes() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Recurso salaB = new Recurso(2L, "Sala B", Recurso.TipoRecurso.SALA);
        Reserva reserva1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva reserva2 = criarReserva(2L, salaB, DIA_08H, DIA_09H);
        
        ValidadorSobreposicao validador = new ValidadorSobreposicao();
        validador.registrarReserva(reserva1);

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.validarSobreposicao(reserva2));
    }

    @Test
    @DisplayName("T-RN02-003: Conflicts - Sobreposição total (mesma hora)")
    void deveRecusarSobreposicaoTotal() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Reserva reserva1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva reserva2 = criarReserva(2L, salaA, DIA_08H, DIA_09H);
        
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
        Reserva reserva1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva reserva2 = criarReserva(2L, salaA, DIA_08H30, DIA_09H30);
        
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
        Reserva reserva1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva reserva2 = criarReserva(2L, salaA, DIA_07H, DIA_08H30);
        
        ValidadorSobreposicao validador = new ValidadorSobreposicao();
        validador.registrarReserva(reserva1);

        // Act & Assert
        assertThatThrownBy(() -> validador.validarSobreposicao(reserva2))
                .isInstanceOf(ReservaSobreposicaoException.class)
                .hasMessageContaining("Conflito de horário");
    }

    @Test
    @DisplayName("T-RN02-006: Conflicts - Sobreposição: nova envolve existente")
    void deveRecusarQuandoNovaEnvolveExistente() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Reserva existente = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva nova = criarReserva(2L, salaA, DIA_07H, DIA_10H);
        
        ValidadorSobreposicao validador = new ValidadorSobreposicao();
        validador.registrarReserva(existente);

        // Act & Assert
        assertThatThrownBy(() -> validador.validarSobreposicao(nova))
                .isInstanceOf(ReservaSobreposicaoException.class)
                .hasMessageContaining("Conflito de horário");
    }

    @Test
    @DisplayName("T-RN02-007: Boundary - Reservas adjacentes (fim = início)")
    void deveAceitarReservasAdjacentesSemSobreposicao() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Reserva existente = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva novaAdjacente = criarReserva(2L, salaA, DIA_09H, DIA_10H);
        
        ValidadorSobreposicao validador = new ValidadorSobreposicao();
        validador.registrarReserva(existente);

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.validarSobreposicao(novaAdjacente));
    }

    @Test
    @DisplayName("T-RN02-008: Boundary - Limite exato de coincidência recusada por RN-01")
    void deveRecusarPorRN01AntesDeVerificarSobreposicao() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Reserva reservaInvalida = criarReserva(2L, salaA, DIA_09H, DIA_09H);

        // Act & Assert
        assertThatThrownBy(() -> reservaInvalida.validarTemporalidade(DIA_09H, DIA_09H))
                .isInstanceOf(ReservaTemporalException.class)
                .hasMessageContaining("Duração inválida");
    }

    @Test
    @DisplayName("T-RN02-009: Conflicts - Múltiplas existentes, conflita com uma")
    void deveRecusarQuandoConflitaComUmaDasMultiplasExistentes() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Reserva r1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva r2 = criarReserva(2L, salaA, DIA_10H, DIA_11H);
        Reserva nova = criarReserva(3L, salaA, DIA_08H30, DIA_08H45);
        
        ValidadorSobreposicao validador = new ValidadorSobreposicao();
        validador.registrarReserva(r1);
        validador.registrarReserva(r2);

        // Act & Assert
        assertThatThrownBy(() -> validador.validarSobreposicao(nova))
                .isInstanceOf(ReservaSobreposicaoException.class)
                .hasMessageContaining("Conflito de horário");
    }

    @Test
    @DisplayName("T-RN02-010: Forbidden State - Alterar existente para criar sobreposição")
    void deveRecusarAlteracaoQueCausariaSobreposicao() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Reserva rA = criarReserva(1L, salaA, DIA_07H, DIA_08H);
        Reserva rB = criarReserva(2L, salaA, DIA_08H, DIA_09H);
        
        ValidadorSobreposicao validador = new ValidadorSobreposicao();
        validador.registrarReserva(rA);
        validador.registrarReserva(rB);

        // Act & Assert - Alterar rA para 08:30-09:00 (conflitando com rB)
        assertThatThrownBy(() -> validador.validarAlteracaoReserva(rA, DIA_08H30, DIA_09H))
                .isInstanceOf(ReservaSobreposicaoException.class)
                .hasMessageContaining("Alteração causaria sobreposição");
    }

    private Reserva criarReserva(Long id, Recurso recurso, LocalDateTime inicio, LocalDateTime fim) {
        Reserva r = new Reserva();
        r.setId(id);
        r.setInicio(inicio);
        r.setFim(fim);
        r.setRecurso(recurso);
        return r;
    }
}
