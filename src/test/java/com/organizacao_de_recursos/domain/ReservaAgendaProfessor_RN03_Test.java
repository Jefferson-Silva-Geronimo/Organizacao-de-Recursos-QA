package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RN-03: Agenda do Professor
 * 
 * RN-03: A regra de sobreposição também se aplica à agenda do professor alocado.
 * Identifier: RN-03 | docs/prd.md:6.3
 * 
 * Casos de teste mapeados:
 * - T-RN03-001: Happy Path - Professor alocado, agenda sem conflito
 * - T-RN03-002: Happy Path - Professor sem agenda registrada
 * - T-RN03-003: Conflicts - Sobreposição com agenda do professor
 * - T-RN03-004: Conflicts - Múltiplos professores, um com conflito
 */
@DisplayName("RN-03: Agenda do Professor")
class ReservaAgendaProfessor_RN03_Test {

    private static final LocalDateTime SEG_08H = LocalDateTime.of(2026, 9, 14, 8, 0);
    private static final LocalDateTime SEG_09H = LocalDateTime.of(2026, 9, 14, 9, 0);
    private static final LocalDateTime SEG_10H = LocalDateTime.of(2026, 9, 14, 10, 0);
    private static final LocalDateTime TER_08H = LocalDateTime.of(2026, 9, 15, 8, 0);
    private static final LocalDateTime TER_09H = LocalDateTime.of(2026, 9, 15, 9, 0);

    @Test
    @DisplayName("T-RN03-001: Happy Path - Professor sem conflito de agenda")
    void deveAceitarReservaComProfessorSemConflito() {
        // Arrange
        Professor professorX = new Professor(1L, "Prof X");
        professorX.adicionarAgenda(SEG_08H, SEG_09H); // Aula segunda 08:00-09:00
        
        Reserva reserva = new Reserva();
        reserva.setProfessor(professorX);
        
        ValidadorAgendaProfessor validador = new ValidadorAgendaProfessor();

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.validarAgenda(reserva, SEG_10H, TER_09H));
    }

    @Test
    @DisplayName("T-RN03-002: Happy Path - Professor sem agenda registrada")
    void deveAceitarReservaComProfessorSemAgenda() {
        // Arrange
        Professor professorY = new Professor(2L, "Prof Y");
        
        Reserva reserva = new Reserva();
        reserva.setProfessor(professorY);
        
        ValidadorAgendaProfessor validador = new ValidadorAgendaProfessor();

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.validarAgenda(reserva, SEG_08H, SEG_09H));
    }

    @Test
    @DisplayName("T-RN03-003: Conflicts - Sobreposição com agenda do professor")
    void deveRecusarReservaComConflitoDeProfessor() {
        // Arrange
        Professor professorX = new Professor(1L, "Prof X");
        professorX.adicionarAgenda(SEG_08H, SEG_09H); // Aula segunda 08:00-09:00
        
        Reserva reserva = new Reserva();
        reserva.setProfessor(professorX);
        
        ValidadorAgendaProfessor validador = new ValidadorAgendaProfessor();

        // Act & Assert - Tenta reservar no mesmo horário
        assertThatThrownBy(() -> validador.validarAgenda(reserva, SEG_08H.plusMinutes(30), SEG_09H))
                .isInstanceOf(ReservaAgendaProfessorException.class)
                .hasMessageContaining("Professor indisponível");
    }

    @Test
    @DisplayName("T-RN03-004: Conflicts - Múltiplos professores, um com conflito")
    void deveRecusarQuandoUmDeProfessoresTemConflito() {
        // Arrange
        Professor professorA = new Professor(1L, "Prof A");
        Professor professorB = new Professor(2L, "Prof B");
        professorB.adicionarAgenda(SEG_08H, SEG_09H); // Prof B ocupado segunda 08:00-09:00
        
        Reserva reserva = new Reserva();
        reserva.adicionarProfessor(professorA);
        reserva.adicionarProfessor(professorB);
        
        ValidadorAgendaProfessor validador = new ValidadorAgendaProfessor();

        // Act & Assert
        assertThatThrownBy(() -> validador.validarAgenda(reserva, SEG_08H, SEG_09H))
                .isInstanceOf(ReservaAgendaProfessorException.class)
                .hasMessageContaining("Professor B indisponível");
    }

    @Test
    @DisplayName("T-RN03-006: Boundary - Professor com agenda até o minuto exato de início")
    void deveValidarAdjacenciaDeAgenda() {
        // Arrange
        Professor professorX = new Professor(1L, "Prof X");
        professorX.adicionarAgenda(SEG_08H, SEG_09H); // Termina em SEG_09H
        
        Reserva reserva = new Reserva();
        reserva.setProfessor(professorX);
        
        ValidadorAgendaProfessor validador = new ValidadorAgendaProfessor();

        // Act & Assert - Reserva começa exatamente quando agenda termina
        // Comportamento PENDENTE: aceitar ou rejeitar (policy de adjacência)
        // Por enquanto, vamos aceitar (não sobreposição estrita)
        assertThatNoException()
                .isThrownBy(() -> validador.validarAgenda(reserva, SEG_09H, SEG_10H));
    }

    @Test
    @DisplayName("T-RN03-007: Invalid Input - Professor inexistente")
    void deveRecusarProfessorInexistente() {
        // Arrange
        Reserva reserva = new Reserva();
        reserva.setProfessor(null);
        
        ValidadorAgendaProfessor validador = new ValidadorAgendaProfessor();

        // Act & Assert
        assertThatThrownBy(() -> validador.validarAgenda(reserva, SEG_08H, SEG_09H))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Professor não identificado");
    }

    @Test
    @DisplayName("T-RN03-010: Conflicts - Professor simultaneamente em duas reservas (dias diferentes)")
    void deveAceitarMesmoProfessorEmDiasDiferentes() {
        // Arrange
        Professor professorX = new Professor(1L, "Prof X");
        professorX.adicionarAgenda(SEG_08H, SEG_09H);
        professorX.adicionarAgenda(TER_08H, TER_09H);
        
        Reserva reserva1 = new Reserva();
        reserva1.setProfessor(professorX);
        
        Reserva reserva2 = new Reserva();
        reserva2.setProfessor(professorX);
        
        ValidadorAgendaProfessor validador = new ValidadorAgendaProfessor();

        // Act & Assert - Ambas no mesmo horário mas em dias diferentes
        assertThatNoException()
                .isThrownBy(() -> {
                    validador.validarAgenda(reserva1, SEG_08H, SEG_09H);
                    validador.validarAgenda(reserva2, TER_08H, TER_09H);
                });
    }
}
