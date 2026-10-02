package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes de regressão das sondas P1-P15 da avaliação ATAM (docs/avaliacao-arquitetural/ciclo-01.md,
 * branch docs/atam-ciclo-01), ação A-09. Cada teste falhava contra o comportamento do domínio antes
 * das correções desta onda e passa depois. P4, P5, P6, P13 e P14 têm sua evidência real na camada
 * persistida (ReservaServiceIT/ReservaServiceConcorrenciaIT); P6 também é coberta aqui no domínio puro.
 * P11 já era um caso correto no código original (vira apenas um teste positivo, sem correção).
 */
@DisplayName("Regressão ATAM: sondas P1-P15 (ciclo-01)")
class AtamRegressaoTest {

    private static final LocalDateTime FUTURO = LocalDateTime.now().plusDays(10)
            .withHour(8).withMinute(0).withSecond(0).withNano(0);

    private Reserva reservaEm(Recurso recurso, LocalDateTime inicio, LocalDateTime fim) {
        Reserva reserva = new Reserva();
        reserva.setRecurso(recurso);
        reserva.setInicio(inicio);
        reserva.setFim(fim);
        return reserva;
    }

    @Test
    @DisplayName("P1 - bloqueio deve ser checado na criação da reserva (ServicoCriacaoReserva.java:52-59)")
    void p01_bloqueioDeveSerCheckadoNaCriacao() {
        Usuario admin = new Usuario(1L, "admin", Usuario.Perfil.ADMINISTRADOR);
        Usuario solicitante = new Usuario(2L, "solicitante1", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        GestaoBloqueios bloqueios = new GestaoBloqueios();
        bloqueios.registrarBloqueio(admin, salaA, FUTURO, FUTURO.plusHours(1));
        ServicoCriacaoReserva servico = new ServicoCriacaoReserva(new ValidadorManutencao(), bloqueios);

        assertThatThrownBy(() -> servico.criarReserva(solicitante, reservaEm(salaA, FUTURO, FUTURO.plusHours(1))))
                .isInstanceOf(ReservaCriacaoException.class)
                .hasMessageContaining("bloqueado");
    }

    @Test
    @DisplayName("P2 - cancelar deve liberar o recurso para nova reserva (ValidadorSobreposicao.java:30-43)")
    void p02_cancelarDeveLiberarORecursoParaNovaReserva() {
        Usuario dono = new Usuario(1L, "dono", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ServicoCriacaoReserva criacao = new ServicoCriacaoReserva();
        ServicoGestaoReserva gestao = new ServicoGestaoReserva(criacao, new ValidadorManutencao());
        Reserva primeira = criacao.criarReserva(dono, reservaEm(salaA, FUTURO, FUTURO.plusHours(1)));

        gestao.cancelarReserva(dono, primeira);

        assertThatNoException()
                .isThrownBy(() -> criacao.criarReserva(dono, reservaEm(salaA, FUTURO, FUTURO.plusHours(1))));
    }

    @Test
    @DisplayName("P3 - apenas Solicitante pode criar reserva (ServicoCriacaoReserva.java:38)")
    void p03_apenasSolicitantePodeCriarReserva() {
        Usuario admin = new Usuario(1L, "admin", Usuario.Perfil.ADMINISTRADOR);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ServicoCriacaoReserva servico = new ServicoCriacaoReserva();

        assertThatThrownBy(() -> servico.criarReserva(admin, reservaEm(salaA, FUTURO, FUTURO.plusHours(1))))
                .isInstanceOf(ReservaCriacaoException.class)
                .hasMessageContaining("Apenas Solicitante");
    }

    @Test
    @DisplayName("P6 - ids de reserva não devem colidir entre instâncias (ServicoCriacaoReserva.java:64)")
    void p06_idNaoDeveColidirEntreInstancias() {
        Usuario s1 = new Usuario(1L, "u1", Usuario.Perfil.SOLICITANTE);
        Usuario s2 = new Usuario(2L, "u2", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Recurso salaB = new Recurso(2L, "Sala B", Recurso.TipoRecurso.SALA);

        Reserva r1 = new ServicoCriacaoReserva().criarReserva(s1, reservaEm(salaA, FUTURO, FUTURO.plusHours(1)));
        Reserva r2 = new ServicoCriacaoReserva().criarReserva(s2, reservaEm(salaB, FUTURO, FUTURO.plusHours(1)));

        assertThat(r1.getId()).isNotEqualTo(r2.getId());
    }

    @Test
    @DisplayName("P7 - não deve aprovar reserva já REJEITADA/CANCELADA (ValidadorAprovacao.java:53-64)")
    void p07_naoDeveAprovarReservaRejeitadaOuCancelada() {
        Recurso salaRestrita = new Recurso(1L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Usuario responsavel = new Usuario(1L, "resp", Usuario.Perfil.RESPONSAVEL);
        Reserva reserva = reservaEm(salaRestrita, FUTURO, FUTURO.plusHours(1));
        reserva.setEstado("REJEITADA");
        ValidadorAprovacao validador = new ValidadorAprovacao();

        assertThatThrownBy(() -> validador.aprovar(reserva, responsavel))
                .isInstanceOf(ReservaAprovacaoException.class)
                .hasMessageContaining("não pode ser aprovada");
    }

    @Test
    @DisplayName("P8 - caminho seguro de aprovação revalida disponibilidade (ValidadorAprovacao.java:115-122)")
    void p08_aprovacaoComRevalidacaoRecusaRecursoQueFicouIndisponivel() {
        Recurso salaRestrita = new Recurso(1L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Usuario responsavel = new Usuario(1L, "resp", Usuario.Perfil.RESPONSAVEL);
        Reserva reserva = reservaEm(salaRestrita, FUTURO, FUTURO.plusHours(1));
        reserva.setId(1L);
        reserva.setEstado("SOLICITADA");
        ValidadorManutencao manutencao = new ValidadorManutencao();
        manutencao.registrarManutencao(salaRestrita, FUTURO, FUTURO.plusHours(1));
        ValidadorAprovacao validador = new ValidadorAprovacao();

        assertThatThrownBy(() -> validador.aprovarComValidacaoDisponibilidade(reserva, responsavel, manutencao))
                .isInstanceOf(ReservaAprovacaoException.class)
                .hasMessageContaining("Recurso indisponível");
    }

    @Test
    @DisplayName("P9 - não deve alterar reserva CONCLUIDA (Reserva.java:135-142)")
    void p09_naoDeveAlterarReservaConcluida() {
        Reserva reserva = new Reserva();
        reserva.setEstado("CONCLUIDA");

        assertThatThrownBy(() -> reserva.alterarHorario(FUTURO, FUTURO.plusHours(1)))
                .isInstanceOf(ReservaTemporalException.class)
                .hasMessageContaining("finalizada");
    }

    @Test
    @DisplayName("P10 - checagem de dono não deve tratar dois usuários com id nulo como o mesmo (ServicoGestaoReserva.java:44-50)")
    void p10_checagemDeDonoNaoDeveColidirComIdsNulos() {
        Usuario dono = new Usuario(null, "dono", Usuario.Perfil.SOLICITANTE);
        Usuario intruso = new Usuario(null, "intruso", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        ServicoCriacaoReserva criacao = new ServicoCriacaoReserva();
        ServicoGestaoReserva gestao = new ServicoGestaoReserva(criacao, new ValidadorManutencao());
        Reserva reserva = criacao.criarReserva(dono, reservaEm(salaA, FUTURO, FUTURO.plusHours(1)));

        assertThatThrownBy(() -> gestao.cancelarReserva(intruso, reserva))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    @DisplayName("P11 - Solicitante não pode aprovar (já era correto; caso positivo de referência)")
    void p11_solicitanteNaoPodeAprovar() {
        Recurso salaRestrita = new Recurso(1L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Usuario solicitante = new Usuario(1L, "sol", Usuario.Perfil.SOLICITANTE);
        Reserva reserva = reservaEm(salaRestrita, FUTURO, FUTURO.plusHours(1));
        reserva.setEstado("SOLICITADA");
        ValidadorAprovacao validador = new ValidadorAprovacao();

        assertThatThrownBy(() -> validador.aprovar(reserva, solicitante))
                .isInstanceOf(ReservaAprovacaoException.class)
                .hasMessageContaining("Apenas Responsável pode aprovar");
    }

    @Test
    @DisplayName("P12 - caminho seguro de aprovação respeita o escopo do Responsável (ValidadorAprovacao.java:136-142)")
    void p12_aprovacaoComEscopoRecusaForaDaResponsabilidade() {
        Recurso salaA = new Recurso(10L, "Sala A", Recurso.TipoRecurso.SALA);
        salaA.setRestrito(true);
        Usuario responsavel = new Usuario(2L, "resp", Usuario.Perfil.RESPONSAVEL);
        Reserva reserva = reservaEm(salaA, FUTURO, FUTURO.plusHours(1));
        reserva.setId(1L);
        reserva.setEstado("SOLICITADA");
        ValidadorAprovacao validador = new ValidadorAprovacao();

        assertThatThrownBy(() -> validador.aprovarComEscopo(reserva, responsavel, 99L))
                .isInstanceOf(ReservaAprovacaoException.class)
                .hasMessageContaining("fora de sua responsabilidade");
    }

    @Test
    @DisplayName("P15 - cancelar deve liberar a agenda do professor (Professor.java:13, ServicoGestaoReserva.java:35-42)")
    void p15_cancelarDeveLiberarAgendaDoProfessor() {
        Usuario dono = new Usuario(1L, "dono", Usuario.Perfil.SOLICITANTE);
        Recurso salaA = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Professor professor = new Professor(1L, "Prof X");
        ServicoCriacaoReserva criacao = new ServicoCriacaoReserva();
        ServicoGestaoReserva gestao = new ServicoGestaoReserva(criacao, new ValidadorManutencao());
        Reserva reserva = criacao.criarReservaComProfessor(dono, salaA, professor, FUTURO, FUTURO.plusHours(1));

        gestao.cancelarReserva(dono, reserva);

        assertThat(professor.temConflito(FUTURO, FUTURO.plusHours(1))).isFalse();
    }
}
