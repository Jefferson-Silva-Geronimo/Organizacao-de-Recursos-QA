package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RF-16: Registro de retirada de materiais
 * Identifier: RF-16 | docs/prd.md:7.16 | E7: Retirada e Devolução de Materiais
 *
 * Casos de teste mapeados:
 * - T-RF16-001: [BLOQUEADO_POR_LACUNA] - exige persistência realista (banco e Testcontainers não definidos/disponíveis)
 * - T-RF16-002: Forbidden State - Solicitante não registra retirada
 */
@DisplayName("RF-16: Registro de retirada de materiais")
class RetiradaMateriais_RF16_Test {

    @Test
    @DisplayName("T-RF16-002: Forbidden State - Solicitante sem permissão não registra retirada")
    void solicitanteNaoDeveRegistrarRetirada() {
        // Arrange
        Usuario solicitante = new Usuario(1L, "solicitante", Usuario.Perfil.SOLICITANTE);
        Recurso projetor = new Recurso(20L, "Projetor", Recurso.TipoRecurso.MATERIAL);
        Reserva reserva = new Reserva();
        reserva.setId(1602L);
        reserva.setRecurso(new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA));
        reserva.adicionarMaterial(projetor);
        MovimentacaoMateriais movimentacao = new MovimentacaoMateriais();

        // Act & Assert
        assertThatThrownBy(() -> movimentacao.registrarRetirada(solicitante, reserva, projetor))
                .isInstanceOf(AcessoNegadoException.class);
    }
}
