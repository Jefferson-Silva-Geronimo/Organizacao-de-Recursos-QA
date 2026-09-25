package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RF-21: Relatórios operacionais
 * Identifier: RF-21 | docs/prd.md:7.21 | E10: Relatórios
 *
 * Casos de teste mapeados:
 * - T-RF21-001: [BLOQUEADO_POR_LACUNA] - fórmulas dos indicadores pendentes (arquitetura §38 e §12, item 10)
 * - T-RF21-002: Forbidden State - Solicitante não consulta relatórios operacionais
 */
@DisplayName("RF-21: Relatórios operacionais")
class RelatoriosOperacionais_RF21_Test {

    @Test
    @DisplayName("T-RF21-002: Forbidden State - Solicitante sem permissão não consulta relatórios operacionais")
    void solicitanteNaoDeveConsultarRelatorios() {
        // Arrange
        Usuario solicitante = new Usuario(1L, "solicitante", Usuario.Perfil.SOLICITANTE);
        ServicoRelatorios relatorios = new ServicoRelatorios();

        // Act & Assert
        assertThatThrownBy(() -> relatorios.consultarRelatorio(solicitante))
                .isInstanceOf(AcessoNegadoException.class);
    }
}
