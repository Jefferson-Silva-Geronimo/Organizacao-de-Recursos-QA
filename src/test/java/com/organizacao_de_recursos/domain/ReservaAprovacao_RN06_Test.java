package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

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
 * - T-RN06-009: Boundary - Responsável autorizado para recurso A tenta aprovar recurso B
 * - T-RN06-010: Forbidden State - Tentar aprovar recurso já aprovado
 */
@DisplayName("RN-06: Aprovação de Recursos Restritos")
class ReservaAprovacao_RN06_Test {

    @Test
    @DisplayName("T-RN06-001: Happy Path - Recurso comum aceito direto")
    void recursoComumDeveSerAceitoSemAprovacao() {
        // Arrange
        Recurso salaComum = new Recurso(1L, "Sala Comum", Recurso.TipoRecurso.SALA);
        salaComum.setRestrito(false);
        Usuario solicitante = new Usuario(1L, "user1", Usuario.Perfil.SOLICITANTE);
        
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaComum);
        
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.validarAprovacao(reserva, solicitante));
    }

    @Test
    @DisplayName("T-RN06-002: Happy Path - Recurso restrito aguarda aprovação")
    void recursoRestritoDeveAguardarAprovacao() {
        // Arrange
        Recurso salaRestrita = new Recurso(2L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Usuario solicitante = new Usuario(1L, "user1", Usuario.Perfil.SOLICITANTE);
        
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaRestrita);
        
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act & Assert
        String statusAprovacao = validador.determinarStatusAprovacao(reserva, solicitante);
        assertThat(statusAprovacao).isEqualTo("AGUARDANDO_APROVACAO");
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

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.rejeitarComMotivo(reserva, responsavel, "Horário reservado para evento institucional"));
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
        
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act & Assert
        assertThatThrownBy(() -> validador.aprovar(reserva, solicitante))
                .isInstanceOf(ReservaAprovacaoException.class)
                .hasMessageContaining("Apenas Responsável pode aprovar");
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
        
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act & Assert
        assertThatThrownBy(() -> validador.aprovar(reserva, admin))
                .isInstanceOf(ReservaAprovacaoException.class)
                .hasMessageContaining("Apenas Responsável pode aprovar");
    }

    @Test
    @DisplayName("T-RN06-006: Conflicts - Responsável aprova recurso que ficou indisponível")
    void deveRecusarAprovacaoDeRecursoQueFicouIndisponivel() {
        // Arrange
        Recurso salaRestrita = new Recurso(2L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Usuario responsavel = new Usuario(2L, "user2", Usuario.Perfil.RESPONSAVEL);
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaRestrita);
        
        ValidadorManutencao validadorManutencao = new ValidadorManutencao();
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act & Assert
        assertThatThrownBy(() -> validador.aprovarComValidacaoDisponibilidade(reserva, responsavel, validadorManutencao))
                .isInstanceOf(ReservaAprovacaoException.class)
                .hasMessageContaining("Recurso indisponível no período");
    }

    @Test
    @DisplayName("T-RN06-007: Conflicts - Responsável aprova duplicada (duas simultâneas)")
    void deveGarantirApenasUmaAprovadaSobAprovacaoConcorrente() {
        // Arrange
        Recurso salaRestrita = new Recurso(2L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        Usuario responsavel = new Usuario(2L, "user2", Usuario.Perfil.RESPONSAVEL);
        Reserva r1 = new Reserva();
        r1.setId(1L);
        r1.setRecurso(salaRestrita);
        Reserva r2 = new Reserva();
        r2.setId(2L);
        r2.setRecurso(salaRestrita);
        
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act & Assert
        assertThatThrownBy(() -> validador.aprovarConcorrente(r1, r2, responsavel))
                .isInstanceOf(ReservaAprovacaoException.class);
    }

    @Test
    @DisplayName("T-RN06-008: Invalid Input - Recurso inexistente marcado como restrito")
    void deveRecusarAprovacaoDeRecursoInexistente() {
        // Arrange
        Reserva reserva = new Reserva();
        reserva.setRecurso(null);
        
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act & Assert
        assertThatThrownBy(() -> validador.validarRecursoExistente(reserva))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Recurso não encontrado");
    }

    @Test
    @DisplayName("T-RN06-009: Boundary - Responsável autorizado para recurso A tenta aprovar recurso B")
    void deveRecusarAprovacaoDeRecursoForaDaResponsabilidade() {
        // Arrange
        Recurso salaB = new Recurso(2L, "Sala B (Outro Depto)", Recurso.TipoRecurso.SALA);
        salaB.setRestrito(true);
        Usuario responsavel = new Usuario(2L, "user2", Usuario.Perfil.RESPONSAVEL);
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaB);
        
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act & Assert - Responsável autorizado apenas para recurso 1L tenta aprovar recurso 2L
        assertThatThrownBy(() -> validador.aprovarComEscopo(reserva, responsavel, 1L))
                .isInstanceOf(ReservaAprovacaoException.class)
                .hasMessageContaining("Recurso fora de sua responsabilidade");
    }

    @Test
    @DisplayName("T-RN06-010: Forbidden State - Tentar aprovar recurso já aprovado")
    void deveRecusarReaprovacaoDeReservaJaAprovada() {
        // Arrange
        Recurso salaRestrita = new Recurso(2L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaRestrita);
        reserva.setEstado("APROVADA");
        
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act & Assert
        assertThatThrownBy(() -> validador.validarReaprovacao(reserva))
                .isInstanceOf(ReservaAprovacaoException.class)
                .hasMessageContaining("Solicitação já foi aprovada");
    }
}
