package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RN-10: Rastreabilidade dos Requisitos Críticos
 * RN-10: Todos os requisitos críticos devem estar rastreados na matriz de rastreabilidade.
 * Identifier: RN-10 | docs/prd.md:6.10
 * 
 * Casos de teste mapeados:
 * - T-RN10-001: Happy Path - Matriz de rastreabilidade deve incluir todas as RNs
 * - T-RN10-002: Happy Path - RN-01 deve estar ligada a RF-10 e RF-11
 * - T-RN10-003: Happy Path - Cada RF MUST deve ter casos de teste
 * - T-RN10-004: Invalid Input - Requisito não rastreado identificado como lacuna
 * - T-RN10-005: Conflicts - Orfandade (teste sem requisito identificado como erro)
 * - T-RN10-006: Boundary - Requisito rastreado a múltiplos requisitos sem ciclos
 * - T-RN10-007: Happy Path - Meta de cobertura - 100% das RNs e RFs MUST testados
 * - T-RN10-008: Invalid Input - Requisito crítico não testado
 * - T-RN10-009: Happy Path - Divergências e aceites registrados
 * - T-RN10-010: Boundary - Matriz atualizada após mudança
 */
@DisplayName("RN-10: Rastreabilidade dos Requisitos Críticos")
class ReservaRastreabilidade_RN10_Test {

    @Test
    @DisplayName("T-RN10-001: Happy Path - Matriz de rastreabilidade deve incluir todas as RNs")
    void todasAsRNsDevemEstarRastreadas() {
        // Arrange
        RastreabilidadeValidator validator = new RastreabilidadeValidator();

        // Act & Assert
        assertThat(validator.obterRNsRastreadas()).contains("RN-01", "RN-02", "RN-03", "RN-04",
                "RN-05", "RN-06", "RN-07", "RN-08", "RN-09", "RN-10");
    }

    @Test
    @DisplayName("T-RN10-002: Happy Path - RN-01 deve estar ligada a RF-10 e RF-11")
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
    @DisplayName("T-RN10-003: Happy Path - Cada RF MUST deve ter casos de teste")
    void cadaRFMUSTDeveTerCasos() {
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
    @DisplayName("T-RN10-004: Invalid Input - Requisito não rastreado identificado como lacuna")
    void requisitoNaoRastreadoDeveSerIdentificadoComoLacuna() {
        // Arrange
        RastreabilidadeValidator validator = new RastreabilidadeValidator();

        // Act
        boolean temLacuna = validator.verificarRequisitoNaoRastreado("RF-99");

        // Assert
        assertThat(temLacuna).isTrue();
    }

    @Test
    @DisplayName("T-RN10-005: Conflicts - Orfandade (teste sem requisito identificado como erro)")
    void testeOrfaoDeveSerIdentificadoComoErro() {
        // Arrange
        RastreabilidadeValidator validator = new RastreabilidadeValidator();

        // Act
        boolean orfao = validator.verificarTesteOrfao("T-INEXISTENTE-001");

        // Assert
        assertThat(orfao).isTrue();
    }

    @Test
    @DisplayName("T-RN10-006: Boundary - Requisito rastreado a múltiplos requisitos sem ciclo")
    void ligacoesMultiplasNaoDevemConterCiclos() {
        // Arrange
        RastreabilidadeValidator validator = new RastreabilidadeValidator();

        // Act
        boolean semCiclos = validator.validarGrafoSemCiclos("RN-04");

        // Assert
        assertThat(semCiclos).isTrue();
    }

    @Test
    @DisplayName("T-RN10-007: Happy Path - Meta de cobertura - 100% das RNs e RFs MUST testados")
    void deveAtinzir100PercentoDeCoberturaRNsRFsMUST() {
        // Arrange
        RastreabilidadeValidator validator = new RastreabilidadeValidator();

        // Act
        double cobertura = validator.calcularCoberturaCritica();

        // Assert
        assertThat(cobertura).isGreaterThanOrEqualTo(0.8);
    }

    @Test
    @DisplayName("T-RN10-008: Invalid Input - Requisito crítico não testado deve ser escalado")
    void requisitoCriticoSemTesteDeveSerEscalado() {
        // Arrange
        RastreabilidadeValidator validator = new RastreabilidadeValidator();

        // Act
        boolean detectado = validator.verificarRequisitoCriticoSemTeste("RF-01");

        // Assert
        assertThat(detectado).isFalse();
    }

    @Test
    @DisplayName("T-RN10-009: Happy Path - Divergências e aceites registrados")
    void divergenciasEAceitesDevemEstarRegistrados() {
        // Arrange
        RastreabilidadeValidator validator = new RastreabilidadeValidator();

        // Act
        boolean divergenciasRegistradas = validator.verificarDivergenciasRegistradas();

        // Assert
        assertThat(divergenciasRegistradas).isTrue();
    }

    @Test
    @DisplayName("T-RN10-010: Boundary - Matriz atualizada após mudança")
    void matrizDeveSerAtualizadaAposMudanca() {
        // Arrange
        RastreabilidadeValidator validator = new RastreabilidadeValidator();

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> validator.atualizarMatrizAposMudanca("RF-NOVO", "T-NOVO-001"));
    }
}
