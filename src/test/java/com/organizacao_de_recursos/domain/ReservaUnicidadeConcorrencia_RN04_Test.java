package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

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
 * - T-RN04-005: Conflicts - Dupla em períodos adjacentes [BLOQUEADO_POR_LACUNA]
 * - T-RN04-006: Conflicts - Dupla: um com recurso restrito, um sem [BLOQUEADO_POR_LACUNA]
 * - T-RN04-007: Conflicts - Garantir consistência após aceitar uma
 * - T-RN04-008: Conflicts - Sem race condition em auditoria [BLOQUEADO_POR_LACUNA]
 *
 * As solicitações "simultâneas" são disparadas em threads distintas, liberadas ao mesmo
 * tempo por um CountDownLatch, contra o ValidadorConcorrencia.
 */
@DisplayName("RN-04: Unicidade Sob Concorrência")
class ReservaUnicidadeConcorrencia_RN04_Test {

    private static final LocalDateTime DIA_08H = LocalDateTime.of(2026, 9, 23, 8, 0);
    private static final LocalDateTime DIA_09H = LocalDateTime.of(2026, 9, 23, 9, 0);

    @Test
    @DisplayName("T-RN04-001: Conflicts - Dupla simultânea: apenas uma deve ser aceita")
    void duasSolicitacoesSimultaneasDevemResultarEmApenasUmaAceita() throws Exception {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorConcorrencia validador = new ValidadorConcorrencia();
        Reserva reserva1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva reserva2 = criarReserva(2L, salaA, DIA_08H, DIA_09H);

        // Act
        List<Boolean> resultados = processarSimultaneamente(validador, reserva1, reserva2);

        // Assert
        assertThat(resultados).containsExactlyInAnyOrder(true, false);
    }

    @Test
    @DisplayName("T-RN04-002: Conflicts - Tripla simultânea: exatamente 1 aceita, 2 recusadas")
    void triplaSimultaneaDeveAceitarApenasUma() throws Exception {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorConcorrencia validador = new ValidadorConcorrencia();
        Reserva r1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva r2 = criarReserva(2L, salaA, DIA_08H, DIA_09H);
        Reserva r3 = criarReserva(3L, salaA, DIA_08H, DIA_09H);

        // Act
        List<Boolean> resultados = processarSimultaneamente(validador, r1, r2, r3);

        // Assert
        assertThat(resultados).containsExactlyInAnyOrder(true, false, false);
    }

    @Test
    @DisplayName("T-RN04-003: Happy Path - Sequencial (não simultâneo) - segunda recusada por sobreposição")
    void solicitacoesSequenciaisDevemAplicarRN02() {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorConcorrencia validador = new ValidadorConcorrencia();
        ValidadorSobreposicao validadorSobreposicao = new ValidadorSobreposicao();
        Reserva reserva1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva reserva2 = criarReserva(2L, salaA, DIA_08H, DIA_09H);

        // Act
        boolean resultado1 = validador.procesarReservaSimultanea(reserva1);
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
    void duplicaEmRecursosDiferentesDevemSerAmbosAceitos() throws Exception {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Recurso salaB = new Recurso(2L, "Sala B", Recurso.TipoRecurso.SALA);
        ValidadorConcorrencia validador = new ValidadorConcorrencia();
        Reserva reserva1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva reserva2 = criarReserva(2L, salaB, DIA_08H, DIA_09H);

        // Act
        List<Boolean> resultados = processarSimultaneamente(validador, reserva1, reserva2);

        // Assert
        assertThat(resultados).containsExactly(true, true);
    }

    @Test
    @Disabled("BLOQUEADO_POR_LACUNA: política de adjacência indefinida (Q-002) - resultado 'Ambas aceitas OU conforme política de adjacência (PENDENTE)'")
    @DisplayName("T-RN04-005: Conflicts - Dupla em períodos adjacentes simultâneas")
    void duplaEmPeriodosAdjacentesSimultaneasDevemSerAceitas() {
        fail("Caso bloqueado: política de adjacência indefinida no plano (Q-002)");
    }

    @Test
    @Disabled("BLOQUEADO_POR_LACUNA: resultado 'Request 2 pode ser aceita conforme status' e 'Prof restrito' sem critério de recurso restrito (Q-003)")
    @DisplayName("T-RN04-006: Conflicts - Dupla: um com recurso restrito, um sem")
    void duplaComRecursoRestritoESemRestricao() {
        fail("Caso bloqueado: resultado da Request 2 e critério de recurso restrito indefinidos no plano (Q-003)");
    }

    @Test
    @DisplayName("T-RN04-007: Conflicts - Garantir consistência após aceitar uma")
    void deveGarantirConsistenciaAposAceitarUma() throws Exception {
        // Arrange
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorConcorrencia validador = new ValidadorConcorrencia();
        Reserva r1 = criarReserva(1L, salaA, DIA_08H, DIA_09H);
        Reserva r2 = criarReserva(2L, salaA, DIA_08H, DIA_09H);

        // Act
        List<Boolean> resultados = processarSimultaneamente(validador, r1, r2);
        boolean tentativaPosterior = validador.procesarReservaSimultanea(criarReserva(3L, salaA, DIA_08H, DIA_09H));

        // Assert - apenas uma reserva ficou confirmada; a perdedora e qualquer nova tentativa são recusadas
        assertThat(resultados).containsExactlyInAnyOrder(true, false);
        assertThat(tentativaPosterior).isFalse();
    }

    @Test
    @Disabled("BLOQUEADO_POR_LACUNA: 'PENDENTE: política de timestamp' (§4.2) - ordem/sequência da auditoria sob concorrência não definida")
    @DisplayName("T-RN04-008: Conflicts - Sem race condition em auditoria")
    void naoDeveHaverRaceConditionEmAuditoria() {
        fail("Caso bloqueado: política de timestamp/sequência de auditoria indefinida no plano (§4.2)");
    }

    /** Dispara uma solicitação por reserva, todas liberadas ao mesmo tempo, e devolve os resultados (true = aceita). */
    private List<Boolean> processarSimultaneamente(ValidadorConcorrencia validador, Reserva... reservas) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(reservas.length);
        CountDownLatch prontas = new CountDownLatch(reservas.length);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Boolean>> futuros = new ArrayList<>();
        try {
            for (Reserva reserva : reservas) {
                futuros.add(executor.submit(() -> {
                    prontas.countDown();
                    largada.await();
                    return validador.procesarReservaSimultanea(reserva);
                }));
            }
            prontas.await(10, TimeUnit.SECONDS);
            largada.countDown();

            List<Boolean> resultados = new ArrayList<>();
            for (Future<Boolean> futuro : futuros) {
                resultados.add(futuro.get(10, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            executor.shutdownNow();
        }
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
