package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RN-04: Unicidade Sob Concorrência
 * 
 * RN-04: Duas solicitações simultâneas para o mesmo recurso e período 
 * devem produzir somente uma reserva aceita.
 * Identifier: RN-04 | docs/prd.md:6.4
 * 
 * Casos de teste mapeados:
 * - T-RN04-001: Conflicts - Dupla simultânea: apenas uma aceita
 * - T-RN04-002: Conflicts - Tripla simultânea: exatamente 1 aceita, 2 recusadas
 * - T-RN04-003: Happy Path - Sequencial (não simultâneo)
 * - T-RN04-004: Conflicts - Dupla em recursos diferentes
 * - T-RN04-005: Conflicts - Dupla em períodos adjacentes
 * - T-RN04-006: Conflicts - Dupla: um com recurso restrito, um sem
 * - T-RN04-007: Conflicts - Garantir consistência após aceitar uma
 * - T-RN04-008: Conflicts - Sem race condition em auditoria
 */
@DisplayName("RN-04: Unicidade Sob Concorrência")
class ReservaUnicidadeConcorrencia_RN04_Test {

    private static final LocalDateTime DIA_08H = LocalDateTime.of(2026, 9, 23, 8, 0);
    private static final LocalDateTime DIA_09H = LocalDateTime.of(2026, 9, 23, 9, 0);
    private static final LocalDateTime DIA_10H = LocalDateTime.of(2026, 9, 23, 10, 0);

    @Test
    @DisplayName("T-RN04-001: Conflicts - Dupla simultânea: apenas uma deve ser aceita")
    void duasSolicitacoesSimultaneasDevemResultarEmApenasUmaAceita() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorConcorrencia validador = new ValidadorConcorrencia();
        
        Reserva reserva1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva reserva2 = criarReserva(2L, salaA, DIA_08H, DIA_09H);

        // Act
        boolean resultado1 = validador.procesarReservaSimultanea(reserva1);
        boolean resultado2 = validador.procesarReservaSimultanea(reserva2);

        // Assert
        assertThat(resultado1 || resultado2).isTrue();
        assertThat(resultado1 && resultado2).isFalse();
        assertThat(resultado1 ^ resultado2).isTrue();
    }

    @Test
    @DisplayName("T-RN04-002: Conflicts - Tripla simultânea: exatamente 1 aceita, 2 recusadas")
    void triplaSimultaneaDeveAceitarApenasUma() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorConcorrencia validador = new ValidadorConcorrencia();
        
        Reserva r1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva r2 = criarReserva(2L, salaA, DIA_08H, DIA_09H);
        Reserva r3 = criarReserva(3L, salaA, DIA_08H, DIA_09H);

        // Act
        int aceitas = validador.processarTriplaSimultanea(r1, r2, r3);

        // Assert
        assertThat(aceitas).isEqualTo(1);
    }

    @Test
    @DisplayName("T-RN04-003: Happy Path - Sequencial (não simultâneo) - segunda recusada por sobreposição")
    void solicitacoesSequenciaisDevemAplicarRN02() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorConcorrencia validador = new ValidadorConcorrencia();
        
        Reserva reserva1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva reserva2 = criarReserva(2L, salaA, DIA_08H, DIA_09H);

        // Act
        boolean resultado1 = validador.procesarReservaSimultanea(reserva1);
        
        ValidadorSobreposicao validadorSobreposicao = new ValidadorSobreposicao();
        if (resultado1) {
            validadorSobreposicao.registrarReserva(reserva1);
        }

        // Assert
        assertThat(resultado1).isTrue();
        assertThatThrownBy(() -> validadorSobreposicao.validarSobreposicao(reserva2))
                .isInstanceOf(ReservaSobreposicaoException.class);
    }

    @Test
    @DisplayName("T-RN04-004: Conflicts - Dupla em recursos diferentes - ambas aceitas")
    void duplicaEmRecursosDiferentesDevemSerAmbosAceitos() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Recurso salaB = new Recurso(2L, "Sala B", Recurso.TipoRecurso.SALA);
        ValidadorConcorrencia validador = new ValidadorConcorrencia();
        
        Reserva reserva1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva reserva2 = criarReserva(2L, salaB, DIA_08H, DIA_09H);

        // Act
        boolean resultado1 = validador.procesarReservaSimultanea(reserva1);
        boolean resultado2 = validador.procesarReservaSimultanea(reserva2);

        // Assert
        assertThat(resultado1).isTrue();
        assertThat(resultado2).isTrue();
    }

    @Test
    @DisplayName("T-RN04-005: Conflicts - Dupla em períodos adjacentes simultâneas")
    void duplaEmPeriodosAdjacentesSimultaneasDevemSerAceitas() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorConcorrencia validador = new ValidadorConcorrencia();
        
        Reserva r1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva r2 = criarReserva(2L, salaA, DIA_09H, DIA_10H);

        // Act
        boolean ambosAceitos = validador.processarDuplaPeriodosAdjacentes(r1, r2);

        // Assert
        assertThat(ambosAceitos).isTrue();
    }

    @Test
    @DisplayName("T-RN04-006: Conflicts - Dupla: um com recurso restrito, um sem")
    void duplaComRecursoRestritoESemRestricao() {
        // Arrange
        Recurso salaRestrita = new Recurso(1L, "Sala Especial", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Recurso salaComum = new Recurso(2L, "Sala Comum", Recurso.TipoRecurso.SALA);
        salaComum.setRestrito(false);
        
        ValidadorConcorrencia validador = new ValidadorConcorrencia();
        Reserva rRestrita = criarReserva(1L, salaRestrita, DIA_08H, DIA_09H);
        Reserva rComum = criarReserva(2L, salaComum, DIA_08H, DIA_09H);

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.processarReservaComRestricao(rRestrita, rComum));
    }

    @Test
    @DisplayName("T-RN04-007: Conflicts - Garantir consistência após aceitar uma")
    void deveGarantirConsistenciaAposAceitarUma() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorConcorrencia validador = new ValidadorConcorrencia();
        
        Reserva r1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva r2 = criarReserva(2L, salaA, DIA_08H, DIA_09H);

        // Act
        boolean consistente = validador.verificarConsistenciaAposConcorrencia(r1, r2);

        // Assert
        assertThat(consistente).isTrue();
    }

    @Test
    @DisplayName("T-RN04-008: Conflicts - Sem race condition em auditoria")
    void naoDeveHaverRaceConditionEmAuditoria() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorConcorrencia validador = new ValidadorConcorrencia();
        ValidadorAuditoria validadorAuditoria = new ValidadorAuditoria();
        
        Reserva r1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva r2 = criarReserva(2L, salaA, DIA_08H, DIA_09H);

        // Act
        boolean ordenadoCorretamente = validador.validarSequenciaAuditoriaConcorrente(r1, r2, validadorAuditoria);

        // Assert
        assertThat(ordenadoCorretamente).isTrue();
    }

    private Reserva criarReserva(Long id, Recurso recurso, LocalDateTime inicio, LocalDateTime fim) {
        Reserva r = new Reserva();
        r.setId(id);
        r.setRecurso(recurso);
        r.setInicio(inicio);
        r.setFim(fim);
        return r;
    }
}
