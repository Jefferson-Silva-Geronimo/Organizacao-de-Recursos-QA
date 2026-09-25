package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Testes para RN-06: Aprovação de Recursos Restritos
 * RN-06: Recursos restritos exigem aprovação; somente Responsável pode aprová-los.
 * Identifier: RN-06 | docs/prd.md:6.6
 *
 * Casos de teste mapeados:
 * - T-RN06-001: Happy Path - Recurso comum aceito direto
 * - T-RN06-002: Happy Path - Recurso restrito aguarda aprovação
 * - T-RN06-003: Happy Path - Responsável rejeita
 * - T-RN06-004: Forbidden State - Solicitante não pode aprovar
 * - T-RN06-005: Forbidden State - Administrador também não pode aprovar
 * - T-RN06-006: Conflicts - Responsável aprova recurso que ficou indisponível
 * - T-RN06-007: Conflicts - Responsável aprova duplicada (duas simultâneas)
 * - T-RN06-008: Invalid Input - Recurso inexistente marcado como restrito
 * - T-RN06-009: Boundary - Responsável autorizado para recurso A tenta aprovar recurso B [BLOQUEADO_POR_LACUNA]
 * - T-RN06-010: Forbidden State - Tentar aprovar recurso já aprovado
 */
@DisplayName("RN-06: Aprovação de Recursos Restritos")
class ReservaAprovacao_RN06_Test {

    private static final LocalDateTime DIA_08H = LocalDateTime.now().plusDays(3)
            .withHour(8).withMinute(0).withSecond(0).withNano(0);
    private static final LocalDateTime DIA_09H = DIA_08H.plusHours(1);

    private Reserva solicitarReserva(Usuario solicitante, Recurso recurso) {
        Reserva reserva = new Reserva();
        reserva.setRecurso(recurso);
        reserva.setInicio(DIA_08H);
        reserva.setFim(DIA_09H);
        return new ServicoCriacaoReserva().criarReserva(solicitante, reserva);
    }

    @Test
    @DisplayName("T-RN06-001: Happy Path - Recurso comum aceito direto")
    void recursoComumDeveSerAceitoSemAprovacao() {
        // Arrange
        Recurso salaComum = new Recurso(1L, "Sala Comum", Recurso.TipoRecurso.SALA);
        salaComum.setRestrito(false);
        Usuario solicitante = new Usuario(1L, "user1", Usuario.Perfil.SOLICITANTE);

        // Act
        Reserva reserva = solicitarReserva(solicitante, salaComum);

        // Assert - aceita direto no estado SOLICITADA, sem aguardar aprovação
        assertThat(reserva.getEstado()).isEqualTo("SOLICITADA");
        assertThat(reserva.isApprovalRequired()).isFalse();
    }

    @Test
    @DisplayName("T-RN06-002: Happy Path - Recurso restrito aguarda aprovação")
    void recursoRestritoDeveAguardarAprovacao() {
        // Arrange
        Recurso salaRestrita = new Recurso(2L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Usuario solicitante = new Usuario(1L, "user1", Usuario.Perfil.SOLICITANTE);
        Usuario responsavel = new Usuario(2L, "user2", Usuario.Perfil.RESPONSAVEL);
        Reserva reserva = solicitarReserva(solicitante, salaRestrita);
        ValidadorAprovacao validador = new ValidadorAprovacao();
        String estadoAntesDaAprovacao = reserva.getEstado();

        // Act
        validador.aprovar(reserva, responsavel);

        // Assert - SOLICITADA -> APROVADA somente após a aprovação do Responsável
        assertThat(estadoAntesDaAprovacao).isEqualTo("SOLICITADA");
        assertThat(reserva.isApprovalRequired()).isTrue();
        assertThat(reserva.getEstado()).isEqualTo("APROVADA");
    }

    @Test
    @DisplayName("T-RN06-003: Happy Path - Recurso restrito: Responsável rejeita")
    void responsavelRejeitaRecursoRestritoComMotivo() {
        // Arrange
        Recurso salaRestrita = new Recurso(2L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Usuario responsavel = new Usuario(2L, "user2", Usuario.Perfil.RESPONSAVEL);
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaRestrita);
        reserva.setEstado("SOLICITADA");
        ValidadorAprovacao validador = new ValidadorAprovacao();
        String motivo = "Horário reservado para evento institucional";

        // Act
        validador.rejeitarComMotivo(reserva, responsavel, motivo);

        // Assert - SOLICITADA -> REJEITADA com o motivo informado
        assertThat(reserva.getEstado()).isEqualTo("REJEITADA");
        assertThat(reserva.getMotivoRejeicao()).isEqualTo(motivo);
    }

    @Test
    @DisplayName("T-RN06-004: Forbidden State - Solicitante não pode aprovar")
    void solicitanteNaoPodeAprovarRecursoRestrito() {
        // Arrange
        Recurso salaRestrita = new Recurso(2L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Usuario solicitante = new Usuario(1L, "user1", Usuario.Perfil.SOLICITANTE);
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaRestrita);
        reserva.setEstado("SOLICITADA");
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act & Assert
        assertThatThrownBy(() -> validador.aprovar(reserva, solicitante))
                .isInstanceOf(ReservaAprovacaoException.class)
                .hasMessageContaining("Acesso negado. Apenas Responsável pode aprovar");
        assertThat(reserva.getEstado()).isEqualTo("SOLICITADA");
    }

    @Test
    @DisplayName("T-RN06-005: Forbidden State - Administrador também não pode aprovar")
    void administradorNaoPodeAprovarRecursoRestrito() {
        // Arrange
        Recurso salaRestrita = new Recurso(2L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Usuario admin = new Usuario(3L, "admin", Usuario.Perfil.ADMINISTRADOR);
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaRestrita);
        reserva.setEstado("SOLICITADA");
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act & Assert
        assertThatThrownBy(() -> validador.aprovar(reserva, admin))
                .isInstanceOf(ReservaAprovacaoException.class)
                .hasMessageContaining("Acesso negado. Apenas Responsável pode aprovar");
        assertThat(reserva.getEstado()).isEqualTo("SOLICITADA");
    }

    @Test
    @DisplayName("T-RN06-006: Conflicts - Responsável aprova recurso que ficou indisponível")
    void deveRecusarAprovacaoDeRecursoQueFicouIndisponivel() {
        // Arrange - sala restrita solicitada 08:00-09:00; depois o Admin registra manutenção no período
        Recurso salaRestrita = new Recurso(2L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Usuario responsavel = new Usuario(2L, "user2", Usuario.Perfil.RESPONSAVEL);
        Reserva reserva = reservaSolicitada(1L, salaRestrita);
        ValidadorManutencao validadorManutencao = new ValidadorManutencao();
        validadorManutencao.registrarManutencao(salaRestrita, DIA_08H, DIA_09H);
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act & Assert
        assertThatThrownBy(() -> validador.aprovarComValidacaoDisponibilidade(reserva, responsavel, validadorManutencao))
                .isInstanceOf(ReservaAprovacaoException.class)
                .hasMessageContaining("Recurso indisponível no período");
        assertThat(reserva.getEstado()).isEqualTo("SOLICITADA");
    }
    @Test
    @DisplayName("T-RN06-007: Conflicts - Responsável aprova duplicada (duas simultâneas)")
    void deveGarantirApenasUmaAprovadaSobAprovacaoConcorrente() {
        // Arrange - duas solicitações SOLICITADA da mesma sala restrita no mesmo período
        Recurso salaRestrita = new Recurso(2L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Usuario responsavel = new Usuario(2L, "user2", Usuario.Perfil.RESPONSAVEL);
        Reserva r1 = reservaSolicitada(1L, salaRestrita);
        Reserva r2 = reservaSolicitada(2L, salaRestrita);
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act - o Responsável tenta aprovar ambas
        for (Reserva reserva : List.of(r1, r2)) {
            try {
                validador.aprovar(reserva, responsavel);
            } catch (ReservaAprovacaoException recusada) {
                // aprovação recusada é um desfecho válido para a segunda solicitação
            }
        }

        // Assert - exatamente 1 aprovada
        long aprovadas = List.of(r1, r2).stream().filter(r -> "APROVADA".equals(r.getEstado())).count();
        assertThat(aprovadas).isEqualTo(1);
    }

    private Reserva reservaSolicitada(Long id, Recurso recurso) {
        Reserva reserva = new Reserva();
        reserva.setId(id);
        reserva.setRecurso(recurso);
        reserva.setInicio(DIA_08H);
        reserva.setFim(DIA_09H);
        reserva.setEstado("SOLICITADA");
        return reserva;
    }

    @Test
    @DisplayName("T-RN06-008: Invalid Input - Recurso inexistente marcado como restrito")
    void deveRecusarAprovacaoDeRecursoInexistente() {
        // Arrange - recurso inexistente (nenhum recurso associado à reserva)
        Usuario responsavel = new Usuario(2L, "user2", Usuario.Perfil.RESPONSAVEL);
        Reserva reserva = new Reserva();
        reserva.setRecurso(null);
        reserva.setEstado("SOLICITADA");
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act & Assert
        assertThatThrownBy(() -> validador.aprovar(reserva, responsavel))
                .hasMessageContaining("Recurso não encontrado");
    }

    @Test
    @Disabled("BLOQUEADO_POR_LACUNA: 'Recusada (se implementado) OU aceita (PENDENTE de decisão)' - responsabilidade do Responsável por recurso (Q-004)")
    @DisplayName("T-RN06-009: Boundary - Responsável autorizado para recurso A tenta aprovar recurso B")
    void deveRecusarAprovacaoDeRecursoForaDaResponsabilidade() {
        fail("Caso bloqueado: divisão de responsabilidade do Responsável indefinida no plano (Q-004)");
    }

    @Test
    @DisplayName("T-RN06-010: Forbidden State - Tentar aprovar recurso já aprovado")
    void deveRecusarReaprovacaoDeReservaJaAprovada() {
        // Arrange
        Recurso salaRestrita = new Recurso(2L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Usuario responsavel = new Usuario(2L, "user2", Usuario.Perfil.RESPONSAVEL);
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaRestrita);
        reserva.setEstado("APROVADA");
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act & Assert
        assertThatThrownBy(() -> validador.aprovar(reserva, responsavel))
                .isInstanceOf(ReservaAprovacaoException.class)
                .hasMessageContaining("Solicitação já foi aprovada");
    }
}
