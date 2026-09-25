package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

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
 * - T-RN03-005: Conflicts - Alterar reserva criando conflito de agenda
 * - T-RN03-006: Boundary - Professor com agenda até o minuto exato de início [BLOQUEADO_POR_LACUNA]
 * - T-RN03-007: Invalid Input - Professor inexistente
 * - T-RN03-008: Invalid Input - Agenda com formato inválido [BLOQUEADO_POR_LACUNA]
 * - T-RN03-009: Forbidden State - Tentar sobrepor agenda de professor já alocado [BLOQUEADO_POR_LACUNA]
 * - T-RN03-010: Conflicts - Professor simultaneamente em duas reservas (dias diferentes)
 */
@DisplayName("RN-03: Agenda do Professor")
class ReservaAgendaProfessor_RN03_Test {

    private static final LocalDateTime SEG_08H = LocalDateTime.of(2026, 9, 21, 8, 0);
    private static final LocalDateTime SEG_09H = LocalDateTime.of(2026, 9, 21, 9, 0);
    private static final LocalDateTime SEG_10H = LocalDateTime.of(2026, 9, 21, 10, 0);
    private static final LocalDateTime SEG_11H = LocalDateTime.of(2026, 9, 21, 11, 0);
    private static final LocalDateTime TER_08H = LocalDateTime.of(2026, 9, 22, 8, 0);
    private static final LocalDateTime TER_09H = LocalDateTime.of(2026, 9, 22, 9, 0);
    private static final LocalDateTime TER_10H = LocalDateTime.of(2026, 9, 22, 10, 0);
    private static final LocalDateTime TER_11H = LocalDateTime.of(2026, 9, 22, 11, 0);

    @Test
    @DisplayName("T-RN03-001: Happy Path - Professor sem conflito de agenda")
    void deveAceitarReservaComProfessorSemConflito() {
        // Arrange - Prof X com agenda Seg 08:00-09:00; nova reserva Seg 10:00-11:00
        Professor professorX = new Professor(1L, "Prof X");
        professorX.adicionarAgenda(SEG_08H, SEG_09H);

        Reserva reserva = new Reserva();
        reserva.setProfessor(professorX);

        ValidadorAgendaProfessor validador = new ValidadorAgendaProfessor();

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.validarAgenda(reserva, SEG_10H, SEG_11H));
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
        professorX.adicionarAgenda(SEG_08H, SEG_09H);
        
        Reserva reserva = new Reserva();
        reserva.setProfessor(professorX);
        
        ValidadorAgendaProfessor validador = new ValidadorAgendaProfessor();

        // Act & Assert
        assertThatThrownBy(() -> validador.validarAgenda(reserva, SEG_08H.plusMinutes(30), SEG_09H))
                .isInstanceOf(ReservaAgendaProfessorException.class)
                .hasMessageContaining("Professor indisponível");
    }

    @Test
    @DisplayName("T-RN03-004: Conflicts - Múltiplos professores, um com conflito")
    void deveRecusarQuandoUmDeProfessoresTemConflito() {
        // Arrange - Profs A, B e C requisitados; apenas B tem conflito
        Professor professorA = new Professor(1L, "Prof A");
        Professor professorB = new Professor(2L, "Prof B");
        Professor professorC = new Professor(3L, "Prof C");
        professorB.adicionarAgenda(SEG_08H, SEG_09H);

        Reserva reserva = new Reserva();
        reserva.adicionarProfessor(professorA);
        reserva.adicionarProfessor(professorB);
        reserva.adicionarProfessor(professorC);

        ValidadorAgendaProfessor validador = new ValidadorAgendaProfessor();

        // Act & Assert
        assertThatThrownBy(() -> validador.validarAgenda(reserva, SEG_08H, SEG_09H))
                .isInstanceOf(ReservaAgendaProfessorException.class)
                .hasMessageContaining("Professor B indisponível");
    }
    @Test
    @DisplayName("T-RN03-005: Conflicts - Alterar reserva criando conflito de agenda")
    void deveRecusarAlteracaoCriandoConflitoDeAgenda() {
        // Arrange - reserva existente do Prof X na Ter 10:00-11:00; agenda do Prof X ocupada Seg 08:00-09:00
        Professor professorX = new Professor(1L, "Prof X");
        professorX.adicionarAgenda(SEG_08H, SEG_09H);

        Reserva reserva = new Reserva();
        reserva.setProfessor(professorX);
        reserva.setInicio(TER_10H);
        reserva.setFim(TER_11H);

        ValidadorAgendaProfessor validador = new ValidadorAgendaProfessor();

        // Act & Assert - alterar para Seg 08:00 (conflito com a agenda do Prof X)
        assertThatThrownBy(() -> validador.validarAlteracaoAgenda(reserva, SEG_08H, SEG_09H))
                .isInstanceOf(ReservaAgendaProfessorException.class)
                .hasMessageContaining("Alteração causaria conflito com agenda");
    }
    @Test
    @Disabled("BLOQUEADO_POR_LACUNA: política de adjacência indefinida (Q-002) - resultado 'Aceita OU Recusada conforme política de adjacência (PENDENTE)'")
    @DisplayName("T-RN03-006: Boundary - Professor com agenda até o minuto exato de início")
    void deveValidarAdjacenciaDeAgenda() {
        fail("Caso bloqueado: política de adjacência indefinida no plano (Q-002)");
    }
    @Test
    @DisplayName("T-RN03-007: Invalid Input - Professor inexistente")
    void deveRecusarProfessorInexistente() {
        // Arrange - professor inexistente (nenhum professor associado à reserva)
        Reserva reserva = new Reserva();
        reserva.setProfessor(null);

        ValidadorAgendaProfessor validador = new ValidadorAgendaProfessor();

        // Act & Assert
        assertThatThrownBy(() -> validador.validarAgenda(reserva, SEG_08H, SEG_09H))
                .hasMessageContaining("Professor não encontrado");
    }
    @Test
    @Disabled("BLOQUEADO_POR_LACUNA: plano admite dois comportamentos incompatíveis - 'trata como sem agenda OU recusa'")
    @DisplayName("T-RN03-008: Invalid Input - Agenda com formato inválido")
    void deveRecusarAgendaComFormatoInvalido() {
        fail("Caso bloqueado: plano não define se agenda inválida é tratada como 'sem agenda' ou recusada");
    }
    @Test
    @Disabled("BLOQUEADO_POR_LACUNA: resultado esperado 'Recusada conforme política' sem política definida no plano")
    @DisplayName("T-RN03-009: Forbidden State - Tentar sobrepor agenda de professor já alocado")
    void deveRecusarSobreporAgendaDeProfessorJaAlocado() {
        fail("Caso bloqueado: política de alteração de reserva confirmada com professor alocado não definida no plano");
    }
    @Test
    @DisplayName("T-RN03-010: Conflicts - Professor simultaneamente em duas reservas (dias diferentes)")
    void deveAceitarMesmoProfessorEmDiasDiferentes() {
        // Arrange - Prof X sem agenda; reservas Seg 08:00-09:00 (sala A) e Ter 08:00-09:00 (sala B)
        Professor professorX = new Professor(1L, "Prof X");

        Reserva reserva1 = new Reserva();
        reserva1.setProfessor(professorX);

        Reserva reserva2 = new Reserva();
        reserva2.setProfessor(professorX);

        ValidadorAgendaProfessor validador = new ValidadorAgendaProfessor();

        // Act & Assert - a segunda é validada depois de a primeira ocupar a agenda do professor
        assertThatNoException()
                .isThrownBy(() -> {
                    validador.validarAgenda(reserva1, SEG_08H, SEG_09H);
                    professorX.adicionarAgenda(SEG_08H, SEG_09H);
                    validador.validarAgenda(reserva2, TER_08H, TER_09H);
                });
    }
}
