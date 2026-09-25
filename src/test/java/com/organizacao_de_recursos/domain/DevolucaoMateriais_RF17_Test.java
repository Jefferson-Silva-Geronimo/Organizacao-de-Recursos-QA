package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RF-17: Registro de devolução de materiais
 * Identifier: RF-17 | docs/prd.md:7.17 | E7: Retirada e Devolução de Materiais
 *
 * Casos de teste mapeados:
 * - T-RF17-001: [BLOQUEADO_POR_LACUNA] - exige persistência realista (banco e Testcontainers não definidos/disponíveis)
 * - T-RF17-002: Invalid Input - Devolução sem retirada correspondente é recusada
 */
@DisplayName("RF-17: Registro de devolução de materiais")
class DevolucaoMateriais_RF17_Test {

    @Test
    @DisplayName("T-RF17-002: Invalid Input - Devolução sem retirada correspondente é recusada com mensagem compreensível")
    void devolucaoSemRetiradaDeveSerRecusada() {
        // Arrange - material associado à reserva, sem nenhuma retirada registrada
        Usuario responsavel = new Usuario(2L, "responsavel", Usuario.Perfil.RESPONSAVEL);
        Recurso projetor = new Recurso(20L, "Projetor", Recurso.TipoRecurso.MATERIAL);
        Reserva reserva = new Reserva();
        reserva.setId(1702L);
        reserva.setRecurso(new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA));
        reserva.adicionarMaterial(projetor);
        MovimentacaoMateriais movimentacao = new MovimentacaoMateriais();

        // Act & Assert
        assertThatThrownBy(() -> movimentacao.registrarDevolucao(responsavel, reserva, projetor))
                .isInstanceOf(RuntimeException.class)
                .message().isNotBlank();
    }
}
