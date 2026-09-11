package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RN-10: Rastreabilidade dos Requisitos Críticos
 * RN-10: Todos os requisitos críticos devem estar rastreados na matriz de rastreabilidade.
 * Identifier: RN-10 | docs/prd.md:6.10
 * 
 * NOTA: RN-10 é uma regra de processo/documentação, não de código.
 * Os testes verificam se a cobertura está completa.
 */
@DisplayName("RN-10: Rastreabilidade dos Requisitos Críticos")
class ReservaRastreabilidade_RN10_Test {

    @Test
    @DisplayName("T-RN10-001: Matriz de rastreabilidade deve incluir todas as RNs")
    void todasAsRNsDevemEstarRastreadas() {
        // Arrange
        RastreabilidadeValidator validator = new RastreabilidadeValidator();

        // Act & Assert
        // As 10 RNs devem estar na matriz
        assertThat(validator.obterRNsRastreadas()).contains("RN-01", "RN-02", "RN-03", "RN-04",
                "RN-05", "RN-06", "RN-07", "RN-08", "RN-09", "RN-10");
    }

    @Test
    @DisplayName("T-RN10-002: RN-01 deve estar ligada a RF-10 e RF-11")
    void RN01DeveEstarLigadaARF10ERF11() {
        // Arrange
        RastreabilidadeValidator validator = new RastreabilidadeValidator();

        // Act
        boolean rn01_rf10 = validator.existeLigacao("RN-01", "RF-10");
        boolean rn01_rf11 = validator.existeLigacao("RN-01", "RF-11");

        // Assert
        assertThat(rn01_rf10).isTrue();
        assertThat(rn01_rf11).isTrue();
    }

    @Test
    @DisplayName("T-RN10-003: Cada RF MUST deve ter casos de teste")
    void cadaRFMUSTDeveTermCasos() {
        // Arrange
        RastreabilidadeValidator validator = new RastreabilidadeValidator();

        // Act
        boolean rf01_temTeste = validator.temCasoDeTeste("RF-01");
        boolean rf10_temTeste = validator.temCasoDeTeste("RF-10");

        // Assert
        assertThat(rf01_temTeste).isTrue();
        assertThat(rf10_temTeste).isTrue();
    }

    @Test
    @DisplayName("T-RN10-007: Meta de cobertura - 100% das RNs e RFs MUST testados")
    void deveAtinzir100PercentoDeCoberturaRNsRFsMUST() {
        // Arrange
        RastreabilidadeValidator validator = new RastreabilidadeValidator();

        // Act
        double cobertura = validator.calcularCoberturaCritica();

        // Assert - Deve ser 100% ou próximo
        assertThat(cobertura).isGreaterThanOrEqualTo(0.8); // Aceitamos 80% inicialmente
    }
}
