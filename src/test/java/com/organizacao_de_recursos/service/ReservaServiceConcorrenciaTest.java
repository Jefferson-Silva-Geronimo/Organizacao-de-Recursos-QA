package com.organizacao_de_recursos.service;

import com.organizacao_de_recursos.domain.Usuario;
import com.organizacao_de_recursos.model.ReservaRecursoEntity;
import com.organizacao_de_recursos.model.SalaEntity;
import com.organizacao_de_recursos.model.UsuarioEntity;
import com.organizacao_de_recursos.repository.ReservaRecursoRepository;
import com.organizacao_de_recursos.repository.SalaRepository;
import com.organizacao_de_recursos.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P14 (ciclo-01.md) + requisito de concorrência real da Onda 2 (plano de migração, seção 7.5):
 * 20 threads disparam a mesma reserva pelo serviço real contra o banco do container; exatamente
 * uma deve ser aceita, as demais recusadas por {@code ConflitoDeHorarioException}, e o banco deve
 * conter exatamente uma linha ativa em {@code reserva_recurso}. A garantia é a constraint de
 * exclusão da migration V1 (não um mecanismo em memória) - substitui, para o fluxo persistido,
 * o {@code ValidadorConcorrencia} em memória (que permanece no domínio puro, ver ADR-017).
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class ReservaServiceConcorrenciaTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ReservaService reservaService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private SalaRepository salaRepository;
    @Autowired
    private ReservaRecursoRepository reservaRecursoRepository;

    private static final int THREADS = 20;
    private static final AtomicInteger RODADA = new AtomicInteger(0);

    @RepeatedTest(3)
    @DisplayName("P14 - exatamente uma reserva aceita sob concorrência real (20 threads, banco de verdade)")
    void exatamenteUmaReservaAceitaSobConcorrenciaReal() throws Exception {
        int rodada = RODADA.incrementAndGet();
        UsuarioEntity solicitante = usuarioRepository.save(
                new UsuarioEntity("concorrente" + rodada, "hash", Usuario.Perfil.SOLICITANTE));
        SalaEntity sala = salaRepository.save(new SalaEntity("Sala Concorrencia " + rodada, false, null));
        OffsetDateTime inicio = OffsetDateTime.now().plusDays(5).plusHours(rodada)
                .withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime fim = inicio.plusMinutes(30);

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch prontas = new CountDownLatch(THREADS);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Boolean>> futuros = new ArrayList<>();

        try {
            for (int i = 0; i < THREADS; i++) {
                futuros.add(executor.submit(() -> {
                    prontas.countDown();
                    largada.await();
                    try {
                        reservaService.criar(solicitante.getId(), sala.getId(), inicio, fim);
                        return true;
                    } catch (RuntimeException conflitoOuRecusa) {
                        return false;
                    }
                }));
            }
            prontas.await(10, TimeUnit.SECONDS);
            largada.countDown();

            int aceitas = 0;
            for (Future<Boolean> futuro : futuros) {
                if (futuro.get(15, TimeUnit.SECONDS)) {
                    aceitas++;
                }
            }

            // Assert - exatamente 1 das 20 threads concorrentes foi aceita
            assertThat(aceitas).isEqualTo(1);

            List<ReservaRecursoEntity> ativasNoBanco = reservaRecursoRepository.findAll().stream()
                    .filter(r -> r.getRecursoId().equals(sala.getId()) && r.isOcupa())
                    .toList();
            assertThat(ativasNoBanco).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
