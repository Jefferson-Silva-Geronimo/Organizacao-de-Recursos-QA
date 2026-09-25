package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Testes para RF-10: Criação de Reserva
 * Identifier: RF-10 | docs/prd.md:7.10 | E4: Reservas e Agenda
 *
 * O sistema deve permitir ao Solicitante criar suas próprias reservas
 * respeitando disponibilidade, conflitos, manutenção e restrições.
 *
 * Casos de teste mapeados:
 * - T-RF10-001: Happy Path - Criar reserva simples (sala comum)
 * - T-RF10-002: Happy Path - Criar com múltiplos recursos (sala + material)
 * - T-RF10-003: Happy Path - Criar com professor
 * - T-RF10-004: Happy Path - Recurso restrito aguarda aprovação
 * - T-RF10-005: Conflicts - Sobreposição em sala
 * - T-RF10-006: Conflicts - Sobreposição em material
 * - T-RF10-007: Conflicts - Sobreposição em professor
 * - T-RF10-008: Conflicts - Recurso em manutenção
 * - T-RF10-009: Conflicts - Dupla simultânea (RN-04)
 * - T-RF10-010: Boundary - Período RN-01 válido (fim > início) [BLOQUEADO_POR_LACUNA]
 * - T-RF10-011: Invalid Input - Fim anterior ao início (RN-01)
 * - T-RF10-012: Invalid Input - Recurso inexistente
 * - T-RF10-013: Forbidden State - Solicitante cria para outro Solicitante
 * - T-RF10-014: Happy Path - Auditoria criada (RN-09)
 * - T-RF10-015: Happy Path - Persistência em banco
 *
 * As datas são sempre relativas ao instante atual (futuro), para que a
 * validação de "data no passado" da RN-01 não interfira nos demais cenários.
 */
@DisplayName("RF-10: Criação de Reserva")
class ReservaCriacao_RF10_Test {

    private static final LocalDateTime DIA_08H = LocalDateTime.now().plusDays(2)
            .withHour(8).withMinute(0).withSecond(0).withNano(0);
    private static final LocalDateTime DIA_08H30 = DIA_08H.plusMinutes(30);
    private static final LocalDateTime DIA_09H = DIA_08H.plusHours(1);

    private Usuario solicitante(long id, String username) {
        return new Usuario(id, username, Usuario.Perfil.SOLICITANTE);
    }

    private Reserva reservaEm(Recurso recurso, LocalDateTime inicio, LocalDateTime fim) {
        Reserva reserva = new Reserva();
        reserva.setRecurso(recurso);
        reserva.setInicio(inicio);
        reserva.setFim(fim);
        return reserva;
    }

    @Test
    @DisplayName("T-RF10-001: Happy Path - Criar reserva simples (sala comum)")
    void deveCriarReservaSimplesEmSalaComum() {
        // Arrange
        Usuario solicitante = solicitante(1L, "solicitante1");
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        salaA.setRestrito(false);
        Reserva reserva = reservaEm(salaA, DIA_08H, DIA_09H);
        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act
        Reserva criada = servico.criarReserva(solicitante, reserva);

        // Assert
        assertThat(criada).isNotNull();
        assertThat(criada.getEstado()).isEqualTo("SOLICITADA");
        assertThat(criada.isApprovalRequired()).isFalse();
    }

    @Test
    @DisplayName("T-RF10-002: Happy Path - Criar com múltiplos recursos (sala + material)")
    void deveCriarReservaComMultiplosRecursos() {
        // Arrange
        Usuario solicitante = solicitante(1L, "solicitante1");
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Recurso material = new Recurso(2L, "Projetor", Recurso.TipoRecurso.MATERIAL);
        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act
        Reserva criada = servico.criarReservaComRecursos(solicitante, Arrays.asList(salaA, material), DIA_08H, DIA_09H);

        // Assert
        assertThat(criada.getEstado()).isEqualTo("SOLICITADA");
        assertThat(criada.getRecurso()).isEqualTo(salaA);
        assertThat(criada.getMateriais()).containsExactly(material);
    }

    @Test
    @DisplayName("T-RF10-003: Happy Path - Criar com professor")
    void deveCriarReservaComProfessor() {
        // Arrange
        Usuario solicitante = solicitante(1L, "solicitante1");
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Professor professor = new Professor(1L, "Prof Carlos");
        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act
        Reserva criada = servico.criarReservaComProfessor(solicitante, salaA, professor, DIA_08H, DIA_09H);

        // Assert
        assertThat(criada.getEstado()).isEqualTo("SOLICITADA");
        assertThat(criada.getProfessor()).isEqualTo(professor);
    }

    @Test
    @DisplayName("T-RF10-004: Happy Path - Recurso restrito aguarda aprovação")
    void deveCriarReservaRestritaAguardandoAprovacao() {
        // Arrange
        Usuario solicitante = solicitante(1L, "solicitante1");
        Recurso salaRestrita = new Recurso(3L, "Auditório Nobre", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Reserva reserva = reservaEm(salaRestrita, DIA_08H, DIA_09H);
        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act
        Reserva criada = servico.criarReserva(solicitante, reserva);

        // Assert
        assertThat(criada.getEstado()).isEqualTo("SOLICITADA");
        assertThat(criada.isApprovalRequired()).isTrue();
    }

    @Test
    @DisplayName("T-RF10-005: Conflicts - Sobreposição em sala deve recusar")
    void deveRecusarCriacaoComSobreposicaoEmSala() {
        // Arrange - Sala A já reservada 08:00-09:00
        Usuario solicitante = solicitante(1L, "solicitante1");
        Usuario outro = solicitante(2L, "solicitante2");
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();
        servico.criarReserva(outro, reservaEm(salaA, DIA_08H, DIA_09H));
        Reserva nova = reservaEm(salaA, DIA_08H30, DIA_09H);

        // Act & Assert
        assertThatThrownBy(() -> servico.criarReserva(solicitante, nova))
                .isInstanceOf(ReservaCriacaoException.class)
                .hasMessageContaining("conflito em sala");
    }

    @Test
    @DisplayName("T-RF10-006: Conflicts - Sobreposição em material deve recusar")
    void deveRecusarCriacaoComSobreposicaoEmMaterial() {
        // Arrange - Material X já reservado 08:00-09:00 junto com a Sala A;
        // a nova reserva usa outra sala (B) e o mesmo Material X às 08:30
        Usuario solicitante = solicitante(1L, "solicitante1");
        Usuario outro = solicitante(2L, "solicitante2");
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Recurso salaB = new Recurso(4L, "Sala B", Recurso.TipoRecurso.SALA);
        Recurso materialX = new Recurso(2L, "Projetor", Recurso.TipoRecurso.MATERIAL);
        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();
        servico.criarReservaComRecursos(outro, Arrays.asList(salaA, materialX), DIA_08H, DIA_09H);

        // Act & Assert
        assertThatThrownBy(() -> servico.criarReservaComRecursos(
                solicitante, Arrays.asList(salaB, materialX), DIA_08H30, DIA_09H))
                .isInstanceOf(ReservaCriacaoException.class)
                .hasMessageContaining("conflito em material");
    }

    @Test
    @DisplayName("T-RF10-007: Conflicts - Sobreposição em professor deve recusar (RN-03)")
    void deveRecusarCriacaoComSobreposicaoEmProfessor() {
        // Arrange - Prof Y com agenda 08:00-09:00
        Usuario solicitante = solicitante(1L, "solicitante1");
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Professor professorY = new Professor(1L, "Prof Y");
        professorY.adicionarAgenda(DIA_08H, DIA_09H);
        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act & Assert
        assertThatThrownBy(() -> servico.criarReservaComProfessor(solicitante, salaA, professorY, DIA_08H30, DIA_09H))
                .isInstanceOf(ReservaCriacaoException.class)
                .hasMessageContaining("conflito professor");
    }

    @Test
    @DisplayName("T-RF10-008: Conflicts - Recurso em manutenção deve recusar (RN-05)")
    void deveRecusarCriacaoEmRecursoEmManutencao() {
        // Arrange - Sala A em manutenção 08:00-09:00
        Usuario solicitante = solicitante(1L, "solicitante1");
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ValidadorManutencao manutencao = new ValidadorManutencao();
        manutencao.registrarManutencao(salaA, DIA_08H, DIA_09H);
        ServicoCriacaoReserva servico = new ServicoCriacaoReserva(manutencao);
        Reserva nova = reservaEm(salaA, DIA_08H30, DIA_09H);

        // Act & Assert
        assertThatThrownBy(() -> servico.criarReserva(solicitante, nova))
                .isInstanceOf(ReservaCriacaoException.class)
                .hasMessageContaining("manutenção");
    }

    @Test
    @DisplayName("T-RF10-009: Conflicts - Dupla simultânea uma aceita uma recusada (RN-04)")
    void deveGerenciarDuplaSimultaneaNaCriacao() throws Exception {
        // Arrange - 300 rodadas, cada uma com serviço novo e duas requisições disparadas juntas
        int rodadas = 300;
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<String> rodadasInvalidas = new ArrayList<>();

        try {
            for (int rodada = 0; rodada < rodadas; rodada++) {
                ServicoCriacaoReserva servico = new ServicoCriacaoReserva();
                CountDownLatch largada = new CountDownLatch(1);
                Future<String> primeira = executor.submit(() -> tentarCriar(servico, largada, 1L, salaA));
                Future<String> segunda = executor.submit(() -> tentarCriar(servico, largada, 2L, salaA));

                // Act
                largada.countDown();
                List<String> resultados = Arrays.asList(primeira.get(10, TimeUnit.SECONDS), segunda.get(10, TimeUnit.SECONDS));

                if (!resultados.contains("ACEITA") || !resultados.contains("RECUSADA_POR_CONFLITO")) {
                    rodadasInvalidas.add("rodada " + rodada + ": " + resultados);
                }
            }
        } finally {
            executor.shutdownNow();
        }

        // Assert - em toda rodada exatamente uma requisição é aceita e a outra recusada por conflito
        assertThat(rodadasInvalidas).as("rodadas em que não houve exatamente 1 aceita e 1 recusada").isEmpty();
    }

    private String tentarCriar(ServicoCriacaoReserva servico, CountDownLatch largada, long idUsuario, Recurso sala)
            throws InterruptedException {
        largada.await();
        try {
            servico.criarReserva(solicitante(idUsuario, "user" + idUsuario), reservaEm(sala, DIA_08H, DIA_09H));
            return "ACEITA";
        } catch (ReservaCriacaoException e) {
            return "RECUSADA_POR_CONFLITO";
        } catch (RuntimeException e) {
            return "ERRO_INESPERADO:" + e.getClass().getSimpleName();
        }
    }

    @Test
    @Disabled("BLOQUEADO_POR_LACUNA: plano não define a duração mínima (§4.3) - resultado 'Aceita OU conforme política (PENDENTE)'")
    @DisplayName("T-RF10-010: Boundary - Período RN-01 válido (fim > início)")
    void deveAceitarCriacaoComPeriodoValidoMinimo() {
        fail("Caso bloqueado: duração mínima de reserva indefinida no plano (§4.3)");
    }

    @Test
    @DisplayName("T-RF10-011: Invalid Input - Fim anterior ao início deve recusar por RN-01")
    void deveRecusarCriacaoComFimAnteriorAoInicio() {
        // Arrange
        Usuario solicitante = solicitante(1L, "solicitante1");
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Reserva reserva = reservaEm(salaA, DIA_09H, DIA_08H);
        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act & Assert
        assertThatThrownBy(() -> servico.criarReserva(solicitante, reserva))
                .isInstanceOf(ReservaTemporalException.class)
                .hasMessageContaining("Fim anterior ao início");
    }

    @Test
    @DisplayName("T-RF10-012: Invalid Input - Recurso inexistente deve recusar")
    void deveRecusarCriacaoComRecursoInexistente() {
        // Arrange
        Usuario solicitante = solicitante(1L, "solicitante1");
        Reserva reserva = reservaEm(null, DIA_08H, DIA_09H);
        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act & Assert
        assertThatThrownBy(() -> servico.criarReserva(solicitante, reserva))
                .isInstanceOf(ReservaCriacaoException.class)
                .hasMessageContaining("Recurso não encontrado");
    }

    @Test
    @DisplayName("T-RF10-013: Forbidden State - Solicitante cria para outro Solicitante")
    void solicitanteNaoDevePoderCriarParaOutro() {
        // Arrange
        Usuario solicitanteA = solicitante(1L, "solicitanteA");
        Usuario solicitanteB = solicitante(2L, "solicitanteB");
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Reserva reserva = reservaEm(salaA, DIA_08H, DIA_09H);
        reserva.setUsuarioSolicitante(solicitanteB); // A tenta criar com owner=B
        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act & Assert
        assertThatThrownBy(() -> servico.criarReserva(solicitanteA, reserva))
                .isInstanceOf(ReservaCriacaoException.class)
                .hasMessageContaining("Não pode criar reserva para outro usuário");
    }

    @Test
    @DisplayName("T-RF10-014: Happy Path - Auditoria criada na criação (RN-09)")
    void criacaoDeReservaDeveRegistrarAuditoria() {
        // Arrange
        Usuario solicitante = solicitante(1L, "solicitante1");
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Reserva reserva = reservaEm(salaA, DIA_08H, DIA_09H);
        reserva.setId(100L);
        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();
        ValidadorAuditoria validadorAuditoria = new ValidadorAuditoria();

        // Act
        servico.criarReserva(solicitante, reserva);

        // Assert
        assertThat(validadorAuditoria.obterAuditorias(100L)).hasSize(1);
        Auditoria auditoria = validadorAuditoria.obterAuditorias(100L).get(0);
        assertThat(auditoria.getUsuario()).isEqualTo("solicitante1");
        assertThat(auditoria.getAcao()).isEqualTo("CRIAR");
        assertThat(auditoria.getEstadoNovo()).isEqualTo("SOLICITADA");
        assertThat(auditoria.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("T-RF10-015: Happy Path - Persistência em banco e consulta")
    void deveRecuperarReservaCriadaDoRepositorio() {
        // Arrange
        Usuario solicitante = solicitante(1L, "solicitante1");
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Reserva reserva = reservaEm(salaA, DIA_08H, DIA_09H);
        reserva.setId(200L);
        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act
        servico.criarReserva(solicitante, reserva);
        Reserva encontrada = servico.buscarPorId(200L);

        // Assert
        assertThat(encontrada).isNotNull();
        assertThat(encontrada.getId()).isEqualTo(200L);
        assertThat(encontrada.getRecurso()).isEqualTo(salaA);
        assertThat(encontrada.getInicio()).isEqualTo(DIA_08H);
        assertThat(encontrada.getFim()).isEqualTo(DIA_09H);
        assertThat(encontrada.getEstado()).isEqualTo("SOLICITADA");
    }
}
