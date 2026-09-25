package com.organizacao_de_recursos.domain;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Testes para RN-10: Rastreabilidade dos Requisitos Críticos
 * RN-10: Todos os requisitos críticos devem estar rastreados na matriz de rastreabilidade.
 * Identifier: RN-10 | docs/prd.md:6.10
 *
 * Camada "Documental" (plano, seção 2.10): as verificações leem os documentos do repositório
 * (docs/prd.md seção 9 - Matriz de Rastreabilidade Inicial; docs/arquitetura.md seção 38;
 * docs/testes/plano-tdd.md; docs/relatorio-validacao-arquitetura.md) e os testes em src/test/java.
 *
 * Casos de teste mapeados:
 * - T-RN10-001: Happy Path - Matriz de rastreabilidade completa
 * - T-RN10-002: Happy Path - RN-01 rastreada a RF-10 e RF-11
 * - T-RN10-003: Happy Path - Cada RF MUST possui pelo menos um teste
 * - T-RN10-004: Invalid Input - Requisito não rastreado
 * - T-RN10-005: Conflicts - Orfandade (teste sem requisito)
 * - T-RN10-006: Boundary - Requisito rastreado a múltiplos requisitos
 * - T-RN10-007: Happy Path - Meta de cobertura RNs críticas
 * - T-RN10-008: Invalid Input - Requisito crítico não testado
 * - T-RN10-009: Happy Path - Divergências e aceites registrados
 * - T-RN10-010: Boundary - Matriz atualizada após mudança [BLOQUEADO_POR_LACUNA]
 */
@DisplayName("RN-10: Rastreabilidade dos Requisitos Críticos")
class ReservaRastreabilidade_RN10_Test {

    private static final Path PRD = Path.of("docs", "prd.md");
    private static final Path ARQUITETURA = Path.of("docs", "arquitetura.md");
    private static final Path PLANO = Path.of("docs", "testes", "plano-tdd.md");
    private static final Path RELATORIO_VALIDACAO = Path.of("docs", "relatorio-validacao-arquitetura.md");
    private static final Path TESTES = Path.of("src", "test", "java");

    private static final Pattern ID_DE_TESTE = Pattern.compile("T-R[NF]\\d{2}-\\d{3}");

    private static final Set<String> STATUS_SEM_TESTE_PERMITIDO = Set.of(
            "DUPLICADO", "BLOQUEADO_POR_LACUNA", "SEM_COMPORTAMENTO_VERIFICAVEL", "FORA_DO_ESCOPO_DO_REQUISITO");

    /** Linha da matriz RF -> caso -> teste do plano (seção 9): classe de teste, status e testes que cobrem um caso duplicado. */
    private record LinhaRastreio(String classe, String status, List<String> coberturaEmTeste) {
    }

    /** Linha da matriz do PRD (seção 9): RF ou RNF com regra relacionada, prioridade e evidência. */
    private record LinhaMatriz(String id, Set<String> regras, String prioridade, String evidencia) {
    }

    @Test
    @DisplayName("T-RN10-001: Happy Path - Matriz de rastreabilidade deve incluir todas as RNs")
    void todasAsRNsDevemEstarRastreadas() throws IOException {
        // Arrange
        List<LinhaMatriz> matrizPrd = lerMatrizDoPrd();
        String matrizArquitetura = secao(ler(ARQUITETURA), "## 38.", "## 39.");

        // Act
        Set<String> rnsNaMatrizDoPrd = new LinkedHashSet<>();
        matrizPrd.forEach(linha -> rnsNaMatrizDoPrd.addAll(linha.regras()));
        List<String> rnsEsperadas = idsSequenciais("RN-", 1, 10);
        List<String> rfsEsperadas = idsSequenciais("RF-", 1, 23);
        List<String> rfsNaMatriz = matrizPrd.stream().map(LinhaMatriz::id).filter(id -> id.startsWith("RF-")).toList();

        // Assert - as 10 RNs e os 23 RFs MUST estão mapeados
        assertThat(rnsNaMatrizDoPrd).containsAll(rnsEsperadas);
        assertThat(rfsNaMatriz).containsAll(rfsEsperadas);
        rnsEsperadas.forEach(rn -> assertThat(matrizArquitetura).as("arquitetura.md §38 cita " + rn).contains(rn));
    }

    @Test
    @DisplayName("T-RN10-002: Happy Path - RN-01 deve estar ligada a RF-10 e RF-11")
    void RN01DeveEstarLigadaARF10ERF11() throws IOException {
        // Arrange
        List<LinhaMatriz> matriz = lerMatrizDoPrd();

        // Act
        boolean rn01_rf10 = regrasDe(matriz, "RF-10").contains("RN-01");
        boolean rn01_rf11 = regrasDe(matriz, "RF-11").contains("RN-01");

        // Assert
        assertThat(rn01_rf10).as("RF-10 liga RN-01").isTrue();
        assertThat(rn01_rf11).as("RF-11 liga RN-01").isTrue();
    }

    @Test
    @DisplayName("T-RN10-003: Happy Path - Cada RF MUST deve ter casos de teste")
    void cadaRFMUSTDeveTerCasos() throws IOException {
        // Arrange - RFs MUST (PRD §9), casos do plano, testes JUnit reais e matriz de rastreabilidade do plano (§9)
        List<LinhaMatriz> matrizPrd = lerMatrizDoPrd();
        String plano = ler(PLANO);
        Set<String> casosDoPlano = new LinkedHashSet<>();
        Matcher definicoes = Pattern.compile(
                "(?m)^\\| \\*\\*(T-RF\\d{2}-\\d{3})\\*\\* \\| (?:Happy Path|Boundary|Invalid Input|Conflicts|Forbidden State) \\|")
                .matcher(plano);
        while (definicoes.find()) {
            casosDoPlano.add(definicoes.group(1));
        }
        Map<String, String> classeDoTeste = idsDeTesteNoCodigo();
        Map<String, LinhaRastreio> rastreio = lerRastreioDoPlano(plano);

        // Act
        List<String> rfsSemCaso = new ArrayList<>();
        List<String> rfsSemTesteAutomatizado = new ArrayList<>();
        List<String> casosValidosSemTeste = new ArrayList<>();
        List<String> idsOrfaos = new ArrayList<>();
        List<String> rastreabilidadeQuebrada = new ArrayList<>();
        for (LinhaMatriz rf : matrizPrd) {
            if (!rf.id().startsWith("RF-") || !"MUST".equals(rf.prioridade())) {
                continue;
            }
            String prefixo = "T-RF" + rf.id().substring(3) + "-";
            List<String> casos = casosDoPlano.stream().filter(caso -> caso.startsWith(prefixo)).toList();
            if (casos.isEmpty()) {
                rfsSemCaso.add(rf.id());
            }
            boolean algumCasoComTeste = casos.stream().anyMatch(caso -> {
                if (classeDoTeste.containsKey(caso)) {
                    return true;
                }
                LinhaRastreio linha = rastreio.get(caso);
                return linha != null && "DUPLICADO".equals(linha.status())
                        && linha.coberturaEmTeste().stream().anyMatch(classeDoTeste::containsKey);
            });
            if (!algumCasoComTeste) {
                rfsSemTesteAutomatizado.add(rf.id());
            }
        }
        for (String caso : casosDoPlano) {
            LinhaRastreio linha = rastreio.get(caso);
            boolean semTestePermitido = linha != null && STATUS_SEM_TESTE_PERMITIDO.contains(linha.status());
            if (!semTestePermitido && !classeDoTeste.containsKey(caso)) {
                casosValidosSemTeste.add(caso);
            }
            if (linha != null && classeDoTeste.containsKey(caso) && !linha.classe().equals(classeDoTeste.get(caso))) {
                rastreabilidadeQuebrada.add(caso + " (plano: " + linha.classe() + ", código: " + classeDoTeste.get(caso) + ")");
            }
        }
        classeDoTeste.keySet().stream()
                .filter(id -> id.startsWith("T-RF") && !casosDoPlano.contains(id))
                .forEach(idsOrfaos::add);
        rastreio.keySet().stream().filter(id -> !casosDoPlano.contains(id)).forEach(idsOrfaos::add);

        // Assert
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(rfsSemCaso).as("RFs MUST sem caso no plano").isEmpty();
            soft.assertThat(rfsSemTesteAutomatizado)
                    .as("RFs MUST sem nenhum teste JUnit real (nem caso duplicado coberto por teste)").isEmpty();
            soft.assertThat(casosValidosSemTeste).as("casos válidos sem teste JUnit").isEmpty();
            soft.assertThat(idsOrfaos).as("identificadores órfãos (teste ou matriz sem caso no plano)").isEmpty();
            soft.assertThat(rastreabilidadeQuebrada).as("RF -> caso -> classe divergente entre plano e código").isEmpty();
        });
    }

    @Test
    @DisplayName("T-RN10-004: Invalid Input - Requisito não rastreado identificado como lacuna")
    void requisitoNaoRastreadoDeveSerIdentificadoComoLacuna() throws IOException {
        // Arrange
        Set<String> rfsDeclarados = new LinkedHashSet<>();
        Matcher cabecalhos = Pattern.compile("(?m)^### (RF-\\d{2}):").matcher(ler(PRD));
        while (cabecalhos.find()) {
            rfsDeclarados.add(cabecalhos.group(1));
        }
        List<String> rfsNaMatriz = lerMatrizDoPrd().stream().map(LinhaMatriz::id).toList();

        // Act - RF declarado na seção 7 do PRD sem linha na matriz da seção 9
        List<String> naoRastreados = rfsDeclarados.stream().filter(rf -> !rfsNaMatriz.contains(rf)).toList();

        // Assert
        assertThat(rfsDeclarados).isNotEmpty();
        assertThat(naoRastreados).as("requisitos declarados sem correspondência na matriz").isEmpty();
    }

    @Test
    @DisplayName("T-RN10-005: Conflicts - Orfandade (teste sem requisito identificado como erro)")
    void testeOrfaoDeveSerIdentificadoComoErro() throws IOException {
        // Arrange - ids dos casos definidos no plano e ids citados nos testes JUnit
        Set<String> idsDoPlano = new LinkedHashSet<>();
        Matcher definicoes = Pattern.compile("\\*\\*(T-R[NF]\\d{2}-\\d{3})\\*\\*").matcher(ler(PLANO));
        while (definicoes.find()) {
            idsDoPlano.add(definicoes.group(1));
        }
        Set<String> idsNosTestes = new LinkedHashSet<>();
        try (Stream<Path> arquivos = Files.walk(TESTES)) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith("_Test.java")).toList()) {
                Matcher displayNames = Pattern.compile("@DisplayName\\(\"([^\"]+)\"\\)").matcher(ler(arquivo));
                while (displayNames.find()) {
                    Matcher ids = ID_DE_TESTE.matcher(displayNames.group(1));
                    while (ids.find()) {
                        idsNosTestes.add(ids.group());
                    }
                }
            }
        }

        // Act
        List<String> orfaos = idsNosTestes.stream().filter(id -> !idsDoPlano.contains(id)).toList();

        // Assert
        assertThat(idsNosTestes).isNotEmpty();
        assertThat(orfaos).as("testes sem caso/requisito de origem no plano").isEmpty();
    }

    @Test
    @DisplayName("T-RN10-006: Boundary - Requisito rastreado a múltiplos requisitos sem ciclo")
    void ligacoesMultiplasNaoDevemConterCiclos() throws IOException {
        // Arrange
        List<LinhaMatriz> matriz = lerMatrizDoPrd();

        // Act
        Set<String> regrasRf10 = regrasDe(matriz, "RF-10");
        Set<String> regrasRf13 = regrasDe(matriz, "RF-13");
        boolean somenteRegrasNaColunaDeRegras = matriz.stream()
                .flatMap(linha -> linha.regras().stream())
                .allMatch(regra -> regra.startsWith("RN-"));

        // Assert - RN-04 ligada a RF-10 e a RF-13; a coluna aponta só para RNs, então o grafo RF -> RN não tem ciclo
        assertThat(regrasRf10).contains("RN-04");
        assertThat(regrasRf13).contains("RN-04");
        assertThat(somenteRegrasNaColunaDeRegras).isTrue();
    }

    @Test
    @DisplayName("T-RN10-007: Happy Path - Meta de cobertura - 100% das RNs e RFs MUST mapeados")
    void deveAtinzir100PercentoDeCoberturaRNsRFsMUST() throws IOException {
        // Arrange
        List<LinhaMatriz> matriz = lerMatrizDoPrd();
        List<LinhaMatriz> rfsMust = matriz.stream()
                .filter(linha -> linha.id().startsWith("RF-") && "MUST".equals(linha.prioridade()))
                .toList();
        Set<String> rnsMapeadas = new LinkedHashSet<>();
        rfsMust.forEach(linha -> rnsMapeadas.addAll(linha.regras()));
        matriz.stream().filter(linha -> linha.id().startsWith("RNF-")).forEach(linha -> rnsMapeadas.addAll(linha.regras()));

        // Act
        long rfsSemRegra = rfsMust.stream().filter(linha -> linha.regras().isEmpty()).count();

        // Assert - 23 RFs MUST, cada um ligado a ao menos uma RN, e as 10 RNs cobertas
        assertThat(rfsMust).hasSize(23);
        assertThat(rfsSemRegra).isZero();
        assertThat(rnsMapeadas).containsAll(idsSequenciais("RN-", 1, 10));
    }

    @Test
    @DisplayName("T-RN10-008: Invalid Input - Requisito crítico não testado deve ser escalado")
    void requisitoCriticoSemTesteDeveSerEscalado() throws IOException {
        // Arrange
        List<LinhaMatriz> matriz = lerMatrizDoPrd();

        // Act - RF MUST sem evidência de teste esperada na matriz
        List<String> semEvidencia = matriz.stream()
                .filter(linha -> linha.id().startsWith("RF-") && "MUST".equals(linha.prioridade()))
                .filter(linha -> linha.evidencia().isBlank())
                .map(LinhaMatriz::id)
                .toList();

        // Assert
        assertThat(semEvidencia).as("RFs MUST sem evidência de teste na matriz").isEmpty();
    }

    @Test
    @DisplayName("T-RN10-009: Happy Path - Divergências e aceites registrados")
    void divergenciasEAceitesDevemEstarRegistrados() throws IOException {
        // Arrange
        String relatorio = ler(RELATORIO_VALIDACAO);

        // Act
        String secaoDivergencias = secao(relatorio, "## 16. Validação das divergências", "\n## 17.");

        // Assert
        assertThat(secaoDivergencias).contains("Foram preservadas divergências sobre");
        assertThat(secaoDivergencias).contains("as divergências não foram ocultadas");
    }

    @Test
    @Disabled("BLOQUEADO_POR_LACUNA: 'matriz atualizada em paralelo à implementação (TDD)' é regra de processo sem entrada/estímulo verificável definidos no plano")
    @DisplayName("T-RN10-010: Boundary - Matriz atualizada após mudança")
    void matrizDeveSerAtualizadaAposMudanca() {
        fail("Caso bloqueado: critério verificável de atualização da matriz após mudança indefinido no plano");
    }

    /** Mapa id do caso -> classe de teste, lido dos @DisplayName em src/test/java (casos T-RNnn e T-RFnn). */
    private static Map<String, String> idsDeTesteNoCodigo() throws IOException {
        Map<String, String> ids = new LinkedHashMap<>();
        try (Stream<Path> arquivos = Files.walk(TESTES)) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith("_Test.java")).toList()) {
                String classe = arquivo.getFileName().toString().replace(".java", "");
                Matcher displayNames = Pattern.compile("@DisplayName\\(\"([^\"]+)\"\\)").matcher(ler(arquivo));
                while (displayNames.find()) {
                    Matcher achados = ID_DE_TESTE.matcher(displayNames.group(1));
                    while (achados.find()) {
                        ids.putIfAbsent(achados.group(), classe);
                    }
                }
            }
        }
        return ids;
    }

    /**
     * Lê a matriz da seção "## 9." do plano, com colunas:
     * RF | caso | origem | comportamento | classe | método | nível | status | motivo | evidência | lacuna ou duplicidade.
     */
    private static Map<String, LinhaRastreio> lerRastreioDoPlano(String plano) {
        int inicio = plano.indexOf("\n## 9.");
        assertThat(inicio).as("seção 9 (matriz T-RFnn) existe no plano").isGreaterThanOrEqualTo(0);
        Map<String, LinhaRastreio> linhas = new LinkedHashMap<>();
        Pattern inicioDeLinha = Pattern.compile("^\\| RF-\\d{2} \\| \\*\\*(T-RF\\d{2}-\\d{3})\\*\\* \\|");
        for (String linha : plano.substring(inicio).split("\\R")) {
            Matcher caso = inicioDeLinha.matcher(linha);
            if (!caso.find()) {
                continue;
            }
            String[] colunas = linha.split("\\|", -1);
            List<String> cobertura = new ArrayList<>();
            Matcher ids = ID_DE_TESTE.matcher(colunas[11]);
            while (ids.find()) {
                cobertura.add(ids.group());
            }
            linhas.put(caso.group(1),
                    new LinhaRastreio(colunas[5].replace("`", "").trim(), colunas[8].replace("*", "").trim(), cobertura));
        }
        return linhas;
    }

    private static String ler(Path arquivo) throws IOException {
        return Files.readString(arquivo, StandardCharsets.UTF_8);
    }

    private static String secao(String documento, String inicio, String fim) {
        int posicaoInicio = documento.indexOf(inicio);
        assertThat(posicaoInicio).as("seção '" + inicio + "' existe no documento").isGreaterThanOrEqualTo(0);
        int posicaoFim = documento.indexOf(fim, posicaoInicio + inicio.length());
        return documento.substring(posicaoInicio, posicaoFim < 0 ? documento.length() : posicaoFim);
    }

    private static List<String> idsSequenciais(String prefixo, int de, int ate) {
        List<String> ids = new ArrayList<>();
        for (int numero = de; numero <= ate; numero++) {
            ids.add(String.format("%s%02d", prefixo, numero));
        }
        return ids;
    }

    private static Set<String> regrasDe(List<LinhaMatriz> matriz, String id) {
        return matriz.stream().filter(linha -> linha.id().equals(id)).findFirst()
                .map(LinhaMatriz::regras).orElse(Set.of());
    }

    /** Lê as linhas RF/RNF da seção "## 9. Matriz de Rastreabilidade Inicial" do PRD. */
    private static List<LinhaMatriz> lerMatrizDoPrd() throws IOException {
        String secao = secao(ler(PRD), "## 9. Matriz de Rastreabilidade Inicial", "\n## 10.");
        List<LinhaMatriz> linhas = new ArrayList<>();
        for (String linha : secao.split("\\R")) {
            if (!linha.startsWith("| RF-") && !linha.startsWith("| RNF-")) {
                continue;
            }
            // colunas: 1=ID, 8=Regra relacionada, 9=Prioridade, 10=Evidência esperada
            String[] colunas = linha.split("\\|", -1);
            Set<String> regras = new LinkedHashSet<>();
            for (String regra : colunas[8].split(";")) {
                if (!regra.isBlank()) {
                    regras.add(regra.trim());
                }
            }
            linhas.add(new LinhaMatriz(colunas[1].trim(), regras, colunas[9].trim(), colunas[10].trim()));
        }
        assertThat(linhas).as("linhas RF/RNF lidas da matriz do PRD").isNotEmpty();
        return linhas;
    }
}
