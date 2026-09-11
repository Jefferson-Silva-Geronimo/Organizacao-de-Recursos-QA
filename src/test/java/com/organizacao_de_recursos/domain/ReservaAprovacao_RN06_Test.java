package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RN-06: Aprovação de Recursos Restritos
 * RN-06: Recursos restritos exigem aprovação; somente Responsável pode aprová-los.
 * Identifier: RN-06 | docs/prd.md:6.6
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
    void recursoRestritoDeve AguardarAprovacao() {
        // Arrange
        Recurso salaRestrita = new Recurso(2L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Usuario solicitante = new Usuario(1L, "user1", Usuario.Perfil.SOLICITANTE);
        
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaRestrita);
        
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act & Assert - Não lança exceção, apenas indica que precisa aprovação
        String statusAprovacao = validador.determinarStatusAprovacao(reserva, solicitante);
        assertThat(statusAprovacao).isEqualTo("AGUARDANDO_APROVACAO");
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
    @DisplayName("T-RN06-002b: Responsável aprova recurso restrito")
    void responsavelPodeAprovarRecursoRestrito() {
        // Arrange
        Recurso salaRestrita = new Recurso(2L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Usuario responsavel = new Usuario(2L, "user2", Usuario.Perfil.RESPONSAVEL);
        
        Reserva reserva = new Reserva();
        reserva.setRecurso(salaRestrita);
        
        ValidadorAprovacao validador = new ValidadorAprovacao();

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validador.aprovar(reserva, responsavel));
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
}
