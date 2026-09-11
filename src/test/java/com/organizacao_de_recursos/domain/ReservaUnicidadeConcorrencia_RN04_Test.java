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
 */
@DisplayName("RN-04: Unicidade Sob Concorrência")
class ReservaUnicidadeConcorrencia_RN04_Test {

    private static final LocalDateTime HOJE_08H = LocalDateTime.of(2026, 9, 11, 8, 0);
    private static final LocalDateTime HOJE_09H = LocalDateTime.of(2026, 9, 11, 9, 0);

    @Test
    @DisplayName("T-RN04-001: Dupla simultânea - apenas uma deve ser aceita")
    void duasSolicitacoesSimultaneasDevemResultarEmApenasUmaAceita() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorConcorrencia validador = new ValidadorConcorrencia();
        
        Reserva reserva1 = new Reserva();
        reserva1.setId(1L);
        reserva1.setRecurso(salaA);
        reserva1.setInicio(HOJE_08H);
        reserva1.setFim(HOJE_09H);
        
        Reserva reserva2 = new Reserva();
        reserva2.setId(2L);
        reserva2.setRecurso(salaA);
        reserva2.setInicio(HOJE_08H);
        reserva2.setFim(HOJE_09H);

        // Act
        boolean resultado1 = validador.procesarReservaSimultanea(reserva1);
        boolean resultado2 = validador.procesarReservaSimultanea(reserva2);

        // Assert
        assertThat(resultado1 || resultado2).isTrue();
        assertThat(resultado1 && resultado2).isFalse();
        // Exatamente uma deve ser aceita
        assertThat(resultado1 ^ resultado2).isTrue();
    }

    @Test
    @DisplayName("T-RN04-003: Sequencial (não simultâneo) - segunda recusada por sobreposição")
    void solicitacoesSequenciaisDevemAplicarRN02() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorConcorrencia validador = new ValidadorConcorrencia();
        
        Reserva reserva1 = new Reserva();
        reserva1.setId(1L);
        reserva1.setRecurso(salaA);
        reserva1.setInicio(HOJE_08H);
        reserva1.setFim(HOJE_09H);
        
        Reserva reserva2 = new Reserva();
        reserva2.setId(2L);
        reserva2.setRecurso(salaA);
        reserva2.setInicio(HOJE_08H);
        reserva2.setFim(HOJE_09H);

        // Act
        boolean resultado1 = validador.procesarReservaSimultanea(reserva1);
        
        // Simular processamento sequencial
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
    @DisplayName("T-RN04-004: Dupla em recursos diferentes - ambas aceitas")
    void duplicaEmRecursosDiferentesDevemSerAmbosAceitos() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Recurso salaB = new Recurso(2L, "Sala B", Recurso.TipoRecurso.SALA);
        ValidadorConcorrencia validador = new ValidadorConcorrencia();
        
        Reserva reserva1 = new Reserva();
        reserva1.setId(1L);
        reserva1.setRecurso(salaA);
        reserva1.setInicio(HOJE_08H);
        reserva1.setFim(HOJE_09H);
        
        Reserva reserva2 = new Reserva();
        reserva2.setId(2L);
        reserva2.setRecurso(salaB);
        reserva2.setInicio(HOJE_08H);
        reserva2.setFim(HOJE_09H);

        // Act
        boolean resultado1 = validador.procesarReservaSimultanea(reserva1);
        boolean resultado2 = validador.procesarReservaSimultanea(reserva2);

        // Assert
        assertThat(resultado1).isTrue();
        assertThat(resultado2).isTrue();
    }
}
