package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

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
 * - T-RF10-010: Boundary - Período RN-01 válido (fim > início)
 * - T-RF10-011: Invalid Input - Fim anterior ao início (RN-01)
 * - T-RF10-012: Invalid Input - Recurso inexistente
 * - T-RF10-013: Forbidden State - Solicitante cria para outro Solicitante
 * - T-RF10-014: Happy Path - Auditoria criada (RN-09)
 * - T-RF10-015: Happy Path - Persistência em banco
 */
@DisplayName("RF-10: Criação de Reserva")
class ReservaCriacao_RF10_Test {

    private static final LocalDateTime DIA_08H = LocalDateTime.of(2026, 9, 25, 8, 0);
    private static final LocalDateTime DIA_09H = LocalDateTime.of(2026, 9, 25, 9, 0);
    private static final LocalDateTime DIA_08H30 = LocalDateTime.of(2026, 9, 25, 8, 30);

    @Test
    @DisplayName("T-RF10-001: Happy Path - Criar reserva simples (sala comum)")
    void deveCriarReservaSimplesEmSalaComum() {
        // Arrange
        Usuario solicitante = new Usuario(1L, "solicitante1", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        salaA.setRestrito(false);

        Reserva reserva = new Reserva();
        reserva.setRecurso(salaA);
        reserva.setInicio(DIA_08H);
        reserva.setFim(DIA_09H);
        reserva.setUsuarioSolicitante(solicitante);

        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act
        Reserva criada = servico.criarReserva(solicitante, reserva);

        // Assert
        assertThat(criada).isNotNull();
        assertThat(criada.getEstado()).isEqualTo("SOLICITADA");
    }

    @Test
    @DisplayName("T-RF10-002: Happy Path - Criar com múltiplos recursos (sala + material)")
    void deveCriarReservaComMultiplosRecursos() {
        // Arrange
        Usuario solicitante = new Usuario(1L, "solicitante1", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Recurso material = new Recurso(2L, "Projetor", Recurso.TipoRecurso.MATERIAL);

        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act
        Reserva criada = servico.criarReservaComRecursos(solicitante, Arrays.asList(salaA, material), DIA_08H, DIA_09H);

        // Assert
        assertThat(criada).isNotNull();
        assertThat(criada.getEstado()).isEqualTo("SOLICITADA");
    }

    @Test
    @DisplayName("T-RF10-003: Happy Path - Criar com professor")
    void deveCriarReservaComProfessor() {
        // Arrange
        Usuario solicitante = new Usuario(1L, "solicitante1", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Professor professor = new Professor(1L, "Prof Carlos");

        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act
        Reserva criada = servico.criarReservaComProfessor(solicitante, salaA, professor, DIA_08H, DIA_09H);

        // Assert
        assertThat(criada).isNotNull();
        assertThat(criada.getEstado()).isEqualTo("SOLICITADA");
    }

    @Test
    @DisplayName("T-RF10-004: Happy Path - Recurso restrito aguarda aprovação")
    void deveCriarReservaRestritaAguardandoAprovacao() {
        // Arrange
        Usuario solicitante = new Usuario(1L, "solicitante1", Usuario.Perfil.SOLICITANTE);
        Recurso salaRestrita = new Recurso(3L, "Auditório Nobre", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);

        Reserva reserva = new Reserva();
        reserva.setRecurso(salaRestrita);
        reserva.setInicio(DIA_08H);
        reserva.setFim(DIA_09H);
        reserva.setUsuarioSolicitante(solicitante);

        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act
        Reserva criada = servico.criarReserva(solicitante, reserva);

        // Assert
        assertThat(criada).isNotNull();
        assertThat(criada.getEstado()).isEqualTo("SOLICITADA");
        assertThat(criada.isApprovalRequired()).isTrue();
    }

    @Test
    @DisplayName("T-RF10-005: Conflicts - Sobreposição em sala deve recusar")
    void deveRecusarCriacaoComSobreposicaoEmSala() {
        // Arrange
        Usuario solicitante = new Usuario(1L, "solicitante1", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        
        Reserva rExistente = new Reserva();
        rExistente.setRecurso(salaA);
        rExistente.setInicio(DIA_08H);
        rExistente.setFim(DIA_09H);

        Reserva nova = new Reserva();
        nova.setRecurso(salaA);
        nova.setInicio(DIA_08H30);
        nova.setFim(DIA_09H.plusHours(1));

        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act & Assert
        assertThatThrownBy(() -> servico.criarReserva(solicitante, nova))
                .isInstanceOf(ReservaCriacaoException.class)
                .hasMessageContaining("conflito em sala");
    }

    @Test
    @DisplayName("T-RF10-006: Conflicts - Sobreposição em material deve recusar")
    void deveRecusarCriacaoComSobreposicaoEmMaterial() {
        // Arrange
        Usuario solicitante = new Usuario(1L, "solicitante1", Usuario.Perfil.SOLICITANTE);
        Recurso material = new Recurso(2L, "Projetor", Recurso.TipoRecurso.MATERIAL);

        Reserva nova = new Reserva();
        nova.setRecurso(material);
        nova.setInicio(DIA_08H30);
        nova.setFim(DIA_09H);

        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act & Assert
        assertThatThrownBy(() -> servico.criarReserva(solicitante, nova))
                .isInstanceOf(ReservaCriacaoException.class)
                .hasMessageContaining("conflito em material");
    }

    @Test
    @DisplayName("T-RF10-007: Conflicts - Sobreposição em professor deve recusar (RN-03)")
    void deveRecusarCriacaoComSobreposicaoEmProfessor() {
        // Arrange
        Usuario solicitante = new Usuario(1L, "solicitante1", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Professor professor = new Professor(1L, "Prof Carlos");
        professor.adicionarAgenda(DIA_08H, DIA_09H);

        Reserva nova = new Reserva();
        nova.setRecurso(salaA);
        nova.setProfessor(professor);
        nova.setInicio(DIA_08H30);
        nova.setFim(DIA_09H);

        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act & Assert
        assertThatThrownBy(() -> servico.criarReserva(solicitante, nova))
                .isInstanceOf(ReservaCriacaoException.class)
                .hasMessageContaining("conflito professor");
    }

    @Test
    @DisplayName("T-RF10-008: Conflicts - Recurso em manutenção deve recusar (RN-05)")
    void deveRecusarCriacaoEmRecursoEmManutencao() {
        // Arrange
        Usuario solicitante = new Usuario(1L, "solicitante1", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);

        Reserva nova = new Reserva();
        nova.setRecurso(salaA);
        nova.setInicio(DIA_08H30);
        nova.setFim(DIA_09H);

        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act & Assert
        assertThatThrownBy(() -> servico.criarReserva(solicitante, nova))
                .isInstanceOf(ReservaCriacaoException.class)
                .hasMessageContaining("manutenção");
    }

    @Test
    @DisplayName("T-RF10-009: Conflicts - Dupla simultânea uma aceita uma recusada (RN-04)")
    void deveGerenciarDuplaSimultaneaNaCriacao() {
        // Arrange
        Usuario sol1 = new Usuario(1L, "user1", Usuario.Perfil.SOLICITANTE);
        Usuario sol2 = new Usuario(2L, "user2", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);

        Reserva r1 = new Reserva();
        r1.setRecurso(salaA);
        r1.setInicio(DIA_08H);
        r1.setFim(DIA_09H);

        Reserva r2 = new Reserva();
        r2.setRecurso(salaA);
        r2.setInicio(DIA_08H);
        r2.setFim(DIA_09H);

        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act & Assert
        Reserva res1 = servico.criarReserva(sol1, r1);
        assertThatThrownBy(() -> servico.criarReserva(sol2, r2))
                .isInstanceOf(ReservaCriacaoException.class);
    }

    @Test
    @DisplayName("T-RF10-010: Boundary - Período RN-01 válido (fim > início)")
    void deveAceitarCriacaoComPeriodoValidoMinimo() {
        // Arrange
        Usuario solicitante = new Usuario(1L, "solicitante1", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);

        Reserva reserva = new Reserva();
        reserva.setRecurso(salaA);
        reserva.setInicio(DIA_08H);
        reserva.setFim(DIA_08H.plusMinutes(1));

        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act
        Reserva criada = servico.criarReserva(solicitante, reserva);

        // Assert
        assertThat(criada).isNotNull();
    }

    @Test
    @DisplayName("T-RF10-011: Invalid Input - Fim anterior ao início deve recusar por RN-01")
    void deveRecusarCriacaoComFimAnteriorAoInicio() {
        // Arrange
        Usuario solicitante = new Usuario(1L, "solicitante1", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);

        Reserva reserva = new Reserva();
        reserva.setRecurso(salaA);
        reserva.setInicio(DIA_09H);
        reserva.setFim(DIA_08H);

        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act & Assert
        assertThatThrownBy(() -> servico.criarReserva(solicitante, reserva))
                .isInstanceOf(ReservaTemporalException.class);
    }

    @Test
    @DisplayName("T-RF10-012: Invalid Input - Recurso inexistente deve recusar")
    void deveRecusarCriacaoComRecursoInexistente() {
        // Arrange
        Usuario solicitante = new Usuario(1L, "solicitante1", Usuario.Perfil.SOLICITANTE);
        Reserva reserva = new Reserva();
        reserva.setRecurso(null);
        reserva.setInicio(DIA_08H);
        reserva.setFim(DIA_09H);

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
        Usuario solicitanteA = new Usuario(1L, "solicitanteA", Usuario.Perfil.SOLICITANTE);
        Usuario solicitanteB = new Usuario(2L, "solicitanteB", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);

        Reserva reserva = new Reserva();
        reserva.setRecurso(salaA);
        reserva.setInicio(DIA_08H);
        reserva.setFim(DIA_09H);
        reserva.setUsuarioSolicitante(solicitanteB); // Solicitante A tenta passar B como owner

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
        Usuario solicitante = new Usuario(1L, "solicitante1", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);

        Reserva reserva = new Reserva();
        reserva.setId(100L);
        reserva.setRecurso(salaA);
        reserva.setInicio(DIA_08H);
        reserva.setFim(DIA_09H);

        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();
        ValidadorAuditoria validadorAuditoria = new ValidadorAuditoria();

        // Act
        servico.criarReserva(solicitante, reserva);

        // Assert
        assertThat(validadorAuditoria.obterAuditorias(100L)).isNotEmpty();
    }

    @Test
    @DisplayName("T-RF10-015: Happy Path - Persistência em banco e consulta")
    void deveRecuperarReservaCriadaDoRepositorio() {
        // Arrange
        Usuario solicitante = new Usuario(1L, "solicitante1", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);

        Reserva reserva = new Reserva();
        reserva.setId(200L);
        reserva.setRecurso(salaA);
        reserva.setInicio(DIA_08H);
        reserva.setFim(DIA_09H);

        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        // Act
        servico.criarReserva(solicitante, reserva);
        Reserva encontrada = servico.buscarPorId(200L);

        // Assert
        assertThat(encontrada).isNotNull();
        assertThat(encontrada.getId()).isEqualTo(200L);
    }
}
