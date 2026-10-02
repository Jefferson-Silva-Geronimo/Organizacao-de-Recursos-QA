package com.organizacao_de_recursos.service;

import com.organizacao_de_recursos.domain.ReservaCriacaoException;
import com.organizacao_de_recursos.domain.Usuario;
import com.organizacao_de_recursos.domain.estado.EstadoReserva;
import com.organizacao_de_recursos.exception.ConflitoDeHorarioException;
import com.organizacao_de_recursos.model.BloqueioEntity;
import com.organizacao_de_recursos.model.EventoAuditoriaEntity;
import com.organizacao_de_recursos.model.ReservaEntity;
import com.organizacao_de_recursos.model.SalaEntity;
import com.organizacao_de_recursos.model.TipoRecursoReserva;
import com.organizacao_de_recursos.model.UsuarioEntity;
import com.organizacao_de_recursos.repository.BloqueioRepository;
import com.organizacao_de_recursos.repository.EventoAuditoriaRepository;
import com.organizacao_de_recursos.repository.SalaRepository;
import com.organizacao_de_recursos.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regressão das sondas ATAM na camada persistida (ciclo-01.md): P1 (bloqueio), P4/P5 (auditoria
 * sem leitura destrutiva/estado estático), P6 (id sem colisão, via BIGSERIAL) e P13 (estado
 * inicial/auto-aprovação formalizados por D2). P14 e a concorrência real estão em
 * {@code ReservaServiceConcorrenciaIT}.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ReservaServiceIntegracaoTest {

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
    private BloqueioRepository bloqueioRepository;
    @Autowired
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    private UsuarioEntity solicitante;
    private UsuarioEntity admin;

    private static final OffsetDateTime INICIO = OffsetDateTime.now().plusDays(3)
            .withHour(8).withMinute(0).withSecond(0).withNano(0);

    @BeforeEach
    void setUp() {
        solicitante = usuarioRepository.save(new UsuarioEntity("solicitante1", "hash", Usuario.Perfil.SOLICITANTE));
        admin = usuarioRepository.save(new UsuarioEntity("admin1", "hash", Usuario.Perfil.ADMINISTRADOR));
    }

    @Test
    @DisplayName("P13 - sala não restrita é auto-aprovada na mesma transação (D2), com 2 eventos de auditoria")
    void p13_salaNaoRestritaEAutoAprovadaNaMesmaTransacao() {
        SalaEntity salaComum = salaRepository.save(new SalaEntity("Sala Comum", false, null));

        ReservaEntity reserva = reservaService.criar(solicitante.getId(), salaComum.getId(), INICIO, INICIO.plusHours(1));

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.APROVADA);
        List<EventoAuditoriaEntity> eventos = eventoAuditoriaRepository.findByReservaIdOrderByOcorridoEmAscIdAsc(reserva.getId());
        assertThat(eventos).hasSize(2);
        assertThat(eventos.get(0).getEstadoNovo()).isEqualTo(EstadoReserva.SOLICITADA);
        assertThat(eventos.get(1).getEstadoNovo()).isEqualTo(EstadoReserva.APROVADA);
    }

    @Test
    @DisplayName("P13b - sala restrita permanece SOLICITADA aguardando aprovação do Responsável")
    void p13b_salaRestritaPermaneceSolicitada() {
        SalaEntity salaRestrita = salaRepository.save(new SalaEntity("Sala Restrita", true, null));

        ReservaEntity reserva = reservaService.criar(solicitante.getId(), salaRestrita.getId(), INICIO, INICIO.plusHours(1));

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.SOLICITADA);
        assertThat(eventoAuditoriaRepository.findByReservaIdOrderByOcorridoEmAscIdAsc(reserva.getId())).hasSize(1);
    }

    @Test
    @DisplayName("P4/P5 - histórico de auditoria não tem leitura destrutiva nem lista exposta por referência")
    void p04_p05_auditoriaSemLeituraDestrutivaNemExposicaoMutavel() {
        SalaEntity sala = salaRepository.save(new SalaEntity("Sala P4P5", false, null));
        ReservaEntity reserva = reservaService.criar(solicitante.getId(), sala.getId(), INICIO, INICIO.plusHours(1));

        List<EventoAuditoriaEntity> primeiraLeitura = eventoAuditoriaRepository.findByReservaIdOrderByOcorridoEmAscIdAsc(reserva.getId());
        primeiraLeitura.clear(); // mutar o resultado da consulta nunca afeta os dados reais no banco

        List<EventoAuditoriaEntity> segundaLeitura = eventoAuditoriaRepository.findByReservaIdOrderByOcorridoEmAscIdAsc(reserva.getId());
        assertThat(segundaLeitura).hasSize(2);
    }

    @Test
    @DisplayName("P6 - ids de reserva gerados pelo banco nunca colidem entre criações (BIGSERIAL)")
    void p06_idsGeradosPeloBancoNuncaColidem() {
        SalaEntity salaA = salaRepository.save(new SalaEntity("Sala A", false, null));
        SalaEntity salaB = salaRepository.save(new SalaEntity("Sala B", false, null));

        ReservaEntity r1 = reservaService.criar(solicitante.getId(), salaA.getId(), INICIO, INICIO.plusHours(1));
        ReservaEntity r2 = reservaService.criar(solicitante.getId(), salaB.getId(), INICIO, INICIO.plusHours(1));

        assertThat(r1.getId()).isNotEqualTo(r2.getId());
    }

    @Test
    @DisplayName("P1 - bloqueio ativo impede a criação da reserva")
    void p01_bloqueioAtivoImpedeCriacao() {
        SalaEntity sala = salaRepository.save(new SalaEntity("Sala Bloqueada", false, null));
        bloqueioRepository.save(new BloqueioEntity(TipoRecursoReserva.SALA, sala.getId(), INICIO, INICIO.plusHours(1),
                BloqueioEntity.Motivo.MANUTENCAO, admin.getId()));

        assertThatThrownBy(() -> reservaService.criar(solicitante.getId(), sala.getId(), INICIO, INICIO.plusHours(1)))
                .isInstanceOf(ReservaCriacaoException.class)
                .hasMessageContaining("bloqueado");
    }

    @Test
    @DisplayName("Constraint de exclusão: sobreposição parcial entre duas reservas sequenciais é rejeitada")
    void constraintDeExclusaoRejeitaSobreposicaoParcial() {
        SalaEntity sala = salaRepository.save(new SalaEntity("Sala Sobreposicao", false, null));
        reservaService.criar(solicitante.getId(), sala.getId(), INICIO, INICIO.plusHours(2));

        assertThatThrownBy(() -> reservaService.criar(solicitante.getId(), sala.getId(),
                INICIO.plusHours(1), INICIO.plusHours(3)))
                .isInstanceOf(ConflitoDeHorarioException.class);
    }

    @Test
    @DisplayName("Constraint de exclusão: períodos adjacentes não conflitam (D7)")
    void constraintDeExclusaoAceitaPeriodosAdjacentes() {
        SalaEntity sala = salaRepository.save(new SalaEntity("Sala Adjacente", false, null));
        reservaService.criar(solicitante.getId(), sala.getId(), INICIO, INICIO.plusHours(1));

        ReservaEntity segunda = reservaService.criar(solicitante.getId(), sala.getId(),
                INICIO.plusHours(1), INICIO.plusHours(2));

        assertThat(segunda.getEstado()).isEqualTo(EstadoReserva.APROVADA);
    }
}
