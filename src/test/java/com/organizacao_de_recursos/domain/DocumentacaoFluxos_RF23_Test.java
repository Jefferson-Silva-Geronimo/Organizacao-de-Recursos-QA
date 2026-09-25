package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RF-23: Documentação da API ou dos fluxos públicos
 * Identifier: RF-23 | docs/prd.md:7.23 | E12: Documentação da API ou Fluxos Públicos
 *
 * A documentação dos fluxos está em docs/fluxos-personas.md. Conforme RNF-18 (condição de medição:
 * comparação da documentação com a baseline implementada), os testes confrontam o que o documento
 * afirma com o comportamento do código: a matriz de estados (seção 12) e a matriz de permissões (seção 11).
 * Ficam em aberto a escolha entre documentar API ou fluxos, o formato e a publicação (FLX-22, pendências).
 *
 * Casos de teste mapeados:
 * - T-RF23-001: Happy Path - Estados e transições confirmados na documentação correspondem ao comportamento
 * - T-RF23-002: Happy Path - Autorizações documentadas por perfil correspondem ao comportamento
 */
@DisplayName("RF-23: Documentação da API ou dos fluxos públicos")
class DocumentacaoFluxos_RF23_Test {

    private static final Path FLUXOS = Path.of("docs", "fluxos-personas.md");
    private static final Set<String> ESTADOS_OFICIAIS = Set.of(
            "SOLICITADA", "APROVADA", "EM_USO", "CONCLUIDA", "REJEITADA", "CANCELADA", "NAO_COMPARECEU");
    private static final String CRASE = "`";

    private static String secao(String documento, String inicio, String fim) {
        int posicaoInicio = documento.indexOf(inicio);
        assertThat(posicaoInicio).as("seção " + inicio + " existe na documentação").isGreaterThanOrEqualTo(0);
        int posicaoFim = documento.indexOf(fim, posicaoInicio + inicio.length());
        return documento.substring(posicaoInicio, posicaoFim < 0 ? documento.length() : posicaoFim);
    }

    private static List<String[]> linhasDaTabela(String secao) {
        List<String[]> linhas = new ArrayList<>();
        for (String linha : secao.lines().toList()) {
            if (linha.startsWith("| ") && !linha.startsWith("| ---") && !linha.startsWith("|---")) {
                String[] celulas = linha.split(Pattern.quote("|"), -1);
                String[] limpas = new String[celulas.length - 2];
                for (int i = 1; i < celulas.length - 1; i++) {
                    limpas[i - 1] = celulas[i].replace(CRASE, "").trim();
                }
                linhas.add(limpas);
            }
        }
        return linhas.subList(1, linhas.size());
    }

    private static boolean aceita(Runnable operacao) {
        try {
            operacao.run();
            return true;
        } catch (RuntimeException recusa) {
            return false;
        }
    }

    private static boolean acessoNegado(Runnable operacao) {
        try {
            operacao.run();
            return false;
        } catch (AcessoNegadoException negado) {
            return true;
        } catch (ReservaAprovacaoException recusa) {
            return recusa.getMessage().startsWith("Acesso negado");
        } catch (RuntimeException regraDeNegocio) {
            return false;
        }
    }

    @Test
    @DisplayName("T-RF23-001: Happy Path - Estados e transições confirmados na documentação correspondem ao comportamento")
    void estadosDocumentadosDevemCorresponderAoComportamento() throws IOException {
        // Arrange - matriz de estados documentada (seção 12)
        String documento = Files.readString(FLUXOS, StandardCharsets.UTF_8);
        List<String[]> matriz = linhasDaTabela(secao(documento, "## 12. Matriz de Estados", "\n## 13."));
        Set<String> estadosDocumentados = new LinkedHashSet<>();
        matriz.forEach(linha -> {
            estadosDocumentados.add(linha[0]);
            if (ESTADOS_OFICIAIS.contains(linha[2])) {
                estadosDocumentados.add(linha[2]);
            }
        });
        ValidadorFluxoEstados fluxo = new ValidadorFluxoEstados();
        ValidadorApagamento apagamento = new ValidadorApagamento();

        // Act - confronta cada linha confirmada com o comportamento implementado
        List<String> divergencias = new ArrayList<>();
        int confirmadas = 0;
        for (String[] linha : matriz) {
            String definicao = linha[6];
            if (!definicao.startsWith("Confirmada") && !definicao.contains("oficiais")) {
                continue;
            }
            confirmadas++;
            Reserva reserva = new Reserva();
            reserva.setEstado(linha[0]);
            if (ESTADOS_OFICIAIS.contains(linha[2])) {
                if (!aceita(() -> fluxo.validarTransicao(reserva, linha[2]))) {
                    divergencias.add(linha[0] + " -> " + linha[2] + " documentada como confirmada, mas recusada pelo código");
                }
            } else if (linha[1].equals("Apagar") && aceita(() -> apagamento.validarApagamento(reserva))) {
                divergencias.add(linha[0] + " apagar documentado como proibido, mas aceito pelo código");
            }
        }

        // Assert
        assertThat(estadosDocumentados).containsExactlyInAnyOrderElementsOf(ESTADOS_OFICIAIS);
        assertThat(confirmadas).as("linhas confirmadas lidas da matriz").isGreaterThanOrEqualTo(3);
        assertThat(divergencias).as("divergências entre a documentação e o comportamento").isEmpty();
    }

    @Test
    @DisplayName("T-RF23-002: Happy Path - Autorizações documentadas por perfil correspondem ao comportamento")
    void autorizacoesDocumentadasDevemCorresponderAoComportamento() throws IOException {
        // Arrange - matriz de permissões documentada (seção 11) e operações reais dos componentes
        String documento = Files.readString(FLUXOS, StandardCharsets.UTF_8);
        List<String[]> matriz = linhasDaTabela(secao(documento, "## 11. Matriz de Permissões", "\n## 12."));
        LocalDateTime inicio = LocalDateTime.now().plusDays(11).withHour(8).withMinute(0).withSecond(0).withNano(0);
        Recurso sala = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Recurso salaRestrita = new Recurso(2L, "Sala Restrita", Recurso.TipoRecurso.SALA);
        salaRestrita.setRestrito(true);
        Recurso material = new Recurso(3L, "Projetor", Recurso.TipoRecurso.MATERIAL);
        Usuario alvo = new Usuario(9L, "alvo", Usuario.Perfil.SOLICITANTE);
        Map<String, Function<Usuario, Runnable>> operacoes = new LinkedHashMap<>();
        operacoes.put("Consultar recursos", u -> () -> new CadastroRecursos().consultarRecursos(u, Recurso.TipoRecurso.SALA));
        operacoes.put("Gerenciar salas", u -> () -> new CadastroRecursos().cadastrarRecurso(u, sala));
        operacoes.put("Gerenciar professores", u -> () -> new CadastroRecursos().cadastrarProfessor(u, new Professor(7L, "Prof Carlos")));
        operacoes.put("Gerenciar materiais", u -> () -> new CadastroRecursos().cadastrarRecurso(u, material));
        operacoes.put("Gerenciar usuários", u -> () -> new GestaoUsuarios().definirPerfil(u, alvo, Usuario.Perfil.RESPONSAVEL));
        operacoes.put("Gerenciar bloqueios", u -> () -> new GestaoBloqueios().registrarBloqueio(u, sala, inicio, inicio.plusHours(1)));
        operacoes.put("Aprovar recurso restrito", u -> () -> {
            Reserva pendente = new Reserva();
            pendente.setRecurso(salaRestrita);
            pendente.setEstado("SOLICITADA");
            new ValidadorAprovacao().aprovar(pendente, u);
        });
        operacoes.put("Validar professor", u -> () -> {
            Reserva alocacao = new Reserva();
            alocacao.setProfessor(new Professor(7L, "Prof Carlos"));
            alocacao.setInicio(inicio);
            alocacao.setFim(inicio.plusHours(1));
            new ValidacaoAlocacaoDocente().validar(u, alocacao);
        });
        operacoes.put("Registrar retirada", u -> () -> {
            Reserva reserva = new Reserva();
            reserva.adicionarMaterial(material);
            new MovimentacaoMateriais().registrarRetirada(u, reserva, material);
        });
        operacoes.put("Registrar devolução", u -> () -> {
            Reserva reserva = new Reserva();
            reserva.adicionarMaterial(material);
            new MovimentacaoMateriais().registrarDevolucao(u, reserva, material);
        });
        operacoes.put("Consultar relatórios", u -> () -> new ServicoRelatorios().consultarRelatorio(u));
        Usuario.Perfil[] perfis = {Usuario.Perfil.SOLICITANTE, Usuario.Perfil.RESPONSAVEL, Usuario.Perfil.ADMINISTRADOR};

        // Act - confronta cada célula PERMITIDO ou NEGADO com a autorização real
        List<String> divergencias = new ArrayList<>();
        int comparacoes = 0;
        Set<String> operacoesEncontradas = new LinkedHashSet<>();
        for (String[] linha : matriz) {
            Function<Usuario, Runnable> operacao = operacoes.get(linha[0]);
            if (operacao == null) {
                continue;
            }
            operacoesEncontradas.add(linha[0]);
            for (int indice = 0; indice < perfis.length; indice++) {
                String documentado = linha[indice + 1];
                if (!documentado.equals("PERMITIDO") && !documentado.equals("NEGADO")) {
                    continue;
                }
                comparacoes++;
                boolean negado = acessoNegado(operacao.apply(new Usuario(1L, "u", perfis[indice])));
                if (negado != documentado.equals("NEGADO")) {
                    divergencias.add(linha[0] + " para " + perfis[indice] + ": documentado " + documentado
                            + ", comportamento " + (negado ? "NEGADO" : "PERMITIDO"));
                }
            }
        }

        // Assert
        assertThat(operacoesEncontradas).as("operações da matriz confrontadas")
                .containsExactlyInAnyOrderElementsOf(operacoes.keySet());
        assertThat(comparacoes).as("células PERMITIDO/NEGADO confrontadas").isGreaterThanOrEqualTo(30);
        assertThat(divergencias).as("divergências entre a documentação e o comportamento").isEmpty();
    }
}
