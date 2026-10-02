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
 * - T-RN03-005: Conflicts - Alterar reserva criando conflito de agenda
 * - T-RN03-006: Boundary - Professor com agenda até o minuto exato de início
 * - T-RN03-007: Invalid Input - Professor inexistente
 * - T-RN03-008: Invalid Input - Agenda com formato inválido
 * - T-RN03-009: Forbidden State - Tentar sobrepor agenda de professor já alocado
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
    @DisplayName("T-RN03-006: Boundary - Professor com agenda até o minuto exato de início")
    void deveValidarAdjacenciaDeAgenda() {
        // Arrange - D7: intervalo semiaberto [início,fim) - agenda adjacente não conflita
        Professor professorX = new Professor(1L, "Prof X");
        professorX.adicionarAgenda(SEG_08H, SEG_09H);

        Reserva reserva = new Reserva();
        reserva.setProfessor(professorX);

        ValidadorAgendaProfessor validador = new ValidadorAgendaProfessor();

        // Act & Assert - nova reserva começa exatamente quando a agenda anterior termina
        assertThatNoException()
                .isThrownBy(() -> validador.validarAgenda(reserva, SEG_09H, SEG_10H));
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
    @DisplayName("T-RN03-008: Invalid Input - Agenda com formato inválido")
    void deveRecusarAgendaComFormatoInvalido() {
        // Arrange - decisão: formato inválido é sempre recusado (rejeitado na borda), nunca tratado como "sem agenda"
        Professor professorX = new Professor(1L, "Prof X");

        // Act & Assert
        assertThatThrownBy(() -> professorX.validarFormatoAgenda("08h30", "09:00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Formato de hora inválido");
    }
    @Test
    @DisplayName("T-RN03-009: Forbidden State - Tentar sobrepor agenda de professor já alocado")
    void deveRecusarSobreporAgendaDeProfessorJaAlocado() {
        // Arrange - Prof X já alocado Seg 08:00-09:00; nova tentativa sobrepõe (não é adjacente)
        Professor professorX = new Professor(1L, "Prof X");
        professorX.adicionarAgenda(SEG_08H, SEG_09H);

        Reserva reserva = new Reserva();
        reserva.setProfessor(professorX);

        ValidadorAgendaProfessor validador = new ValidadorAgendaProfessor();

        // Act & Assert
        assertThatThrownBy(() -> validador.validarAgenda(reserva, SEG_08H.plusMinutes(30), SEG_09H.plusMinutes(30)))
                .isInstanceOf(ReservaAgendaProfessorException.class)
                .hasMessageContaining("Professor indisponível");
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
