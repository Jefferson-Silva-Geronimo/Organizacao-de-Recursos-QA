# Avaliação Arquitetural — Ciclo 01 (ATAM)

| Item | Valor |
|---|---|
| **Produto** | Organização de Recursos (`qa-system`, versão `0.1.0-RED` — `pom.xml:9`) |
| **Método** | ATAM (Architecture Tradeoff Analysis Method — Bass, Clements, Kazman), primeiro ciclo |
| **Data** | 2026-09-25 |
| **Commit avaliado** | `f58f32c` (branch `docs/atam-ciclo-01`, idêntica a `main` no momento da avaliação) |
| **Avaliador** | Claude Code (modelo Sonnet 5), a pedido do mantenedor do repositório |
| **Natureza** | Avaliação com evidência: documentos + código + testes + execuções reais do GitHub Actions + medições e sondas descartáveis |
| **Alterações no repositório** | Nenhuma no código, nos testes, no `pom.xml` ou no CI. Único arquivo criado: este documento |

---

## 0. Resumo executivo

O `docs/arquitetura.md` descreve um **monólito modular Spring Boot** com persistência relacional, autorização contextual, auditoria consistente com o estado e concorrência garantida no ponto de persistência (§11, ADR-001 a ADR-005). O que existe no repositório é **um modelo de domínio em Java puro, em um único pacote, com estado em memória** (42 arquivos-fonte — 45 classes segundo o JaCoCo, com tipos internos —, ~2.150 linhas de produção e 163 testes). Isso é um bom laboratório de regras de negócio, mas ainda **não é a arquitetura descrita**.

**Números medidos** (commit `f58f32c`; método no Apêndice A):

| Métrica | Alvo (PRD) | Medido | Situação |
|---|---|---|---|
| Testes | — | 163 executados, 0 falhas, 0 erros, **16 ignorados** (`@Disabled`, todos por lacuna de decisão) | — |
| Cobertura de linhas (RNF-02) | ≥ 80% | **81,1%** (671/827) | Atende, com margem de 9 linhas |
| Cobertura de branches (RNF-03) | ≥ 70% | **61,8%** (278/450) | **Não atende** (faltam 37 branches) e o CI **não bloqueia** |
| Spring Boot 3.x (RNF-01) | presente | ausente em `pom.xml` | **Não atende** |
| Testcontainers / WireMock / SonarCloud / JMeter (RNF-07, 08, 13, 14) | presentes | ausentes em `pom.xml`, `ci.yml` e `src/` | **Não atendem** |
| GitHub Actions em PR (RNF-12) | executado em PR | 5 runs; PR #1 com 1 falha controlada e 3 verdes; `main` protegida | **Atende** |

**Achados principais** (detalhes nas seções 5, 8 e 11):

1. **RN-04 (uma única reserva aceita) só vale dentro de uma instância, em memória.** O mutex é local ao objeto `ServicoCriacaoReserva` (`ServicoCriacaoReserva.java:24,50-71`) e o próprio código admite que o mecanismo sobre persistência está pendente (`:17-18`). O teste nomeado "RN-04" exercita um artefato **sem chamador em produção** (`ValidadorConcorrencia`), que aceita períodos parcialmente sobrepostos (sonda P14).
2. **Bloqueio de recurso não impede reserva.** Sala bloqueada foi reservada sem erro (sonda P1); `GestaoBloqueios` só é consultado na pesquisa (`ConsultaDisponibilidade.java:33`), contrariando ADR-005 e os fluxos (`fluxos-personas.md:969, 2081`).
3. **A trilha de auditoria não é confiável.** O histórico some ou fica incompleto conforme a ordem de leitura (sondas P4-A/B), a lista devolvida pode ser esvaziada (P5), o armazenamento é estático e compartilhado e os IDs de reserva colidem entre instâncias (P6). RN-09 não está garantida.
4. **Aprovação tem duas rotas com garantias diferentes.** Uma reserva `REJEITADA` ou `CANCELADA` foi aprovada (P7) e uma aprovação ocorreu com manutenção registrada depois (P8) porque `aprovar()` não valida a transição nem revalida disponibilidade.
5. **Cancelar não libera o recurso nem a agenda do professor** (P2, P15). A decisão de domínio ("liberação de recursos", `arquitetura.md:256-267`) estava pendente e o código a resolveu implicitamente.
6. **Autorização é boa por perfil e incompleta no resto.** Sete pontos de entrada validam perfil; porém não há autenticação real, um Administrador cria reserva (P3), ids nulos passam na checagem de dono (P10) e qualquer Responsável aprova qualquer recurso restrito (P12).
7. **O CI é real e funciona**, mas verifica só "compila e testa": não há *gate* de cobertura, Sonar nem JMeter, e o relatório JaCoCo não é publicado como artefato (`ci.yml:32-44`).

**Conclusão do ciclo:** a arquitetura descrita é coerente como plano, mas **o código atual não a sustenta nos cenários de maior prioridade** (CEN-01, 03, 04, 08). Os cenários do batch CON-01 não têm nenhuma implementação e, portanto, **não são avaliáveis** neste ciclo. Este documento não aprova tecnologias, não aceita riscos e não substitui decisões de negócio pendentes.

---

## 1. Método, fontes e convenções

### 1.1 O que este ciclo é — e o que não é

- O `docs/arquitetura.md` (§21–31, §32–45) e o `docs/relatorio-validacao-arquitetura.md` já contêm uma avaliação ATAM **documental**: utility tree, cenários ATAM-01 a ATAM-27, sensibilidades, trade-offs e riscos, todos sem execução (`arquitetura.md:1836`: "Evidências executadas: nenhuma localizada"). Este ciclo **parte desse material** e o confronta com o código, os testes e o CI.
- Um ATAM completo envolve *stakeholders* e decisores de negócio para ratificar prioridades. **Neste ciclo não houve stakeholders**: as notas A/M/B de importância e dificuldade são **propostas do avaliador**, derivadas do PRD e das personas, e **precisam ser ratificadas pela equipe** no ciclo 02.
- Cenários de `arquitetura.md` **não retomados** aqui: ATAM-10 a 12, 14, 17 e 19 a 27 (qualidade de dados, candidato pior, rollback, LGPD, custo, janela do batch, movimentação). Os de batch dependem de CON-01, sem código; ATAM-17 fica para o ciclo 02.

### 1.2 Fontes consultadas

| Tipo | Fontes |
|---|---|
| Personas | `docs/personas/solicitante.md`, `responsavel.md`, `administrador.md` |
| Requisitos | `docs/prd.md` (RN-01 a RN-10, RF-01 a RF-23, RNF-01 a RNF-18), `docs/fluxos-personas.md` |
| Arquitetura | `docs/arquitetura.md` (ADR-001 a ADR-014 estão **dentro** dele, §37; não existem arquivos de ADR separados) |
| Relatórios anteriores | `docs/auditoria-final.md`, `docs/relatorio-validacao-arquitetura.md`, `docs/relatorio-correcao-auditoria.md` |
| Código | 42 arquivos-fonte (45 classes no JaCoCo) em `src/main/java/com/organizacao_de_recursos/domain/` |
| Testes | 29 classes em `src/test/java/com/organizacao_de_recursos/domain/`, `docs/testes/plano-tdd.md` |
| CI | `.github/workflows/ci.yml`, `pom.xml`, runs do GitHub Actions (`gh run list/view`), proteção da `main` (`gh api`) |

### 1.3 Convenções de evidência

| Tag | Significado |
|---|---|
| **[DOC]** | Afirmado em documento (`arquivo:linha`) |
| **[COD]** | Observado lendo o código (`arquivo:linha`) |
| **[MED]** | Medido nesta avaliação (Apêndice A) |
| **[SONDA]** | Comportamento observado executando um programa descartável contra as classes compiladas (Apêndice B; a sonda **não** foi commitada) |
| **[INF]** | Inferência do avaliador a partir das evidências acima |
| **[HIP]** | Hipótese ou valor pendente de decisão da equipe; **não é fato** |

**Caminhos abreviados.** Classes de produção ficam em `src/main/java/com/organizacao_de_recursos/domain/` e testes em `src/test/java/com/organizacao_de_recursos/domain/`; as citações usam só o nome do arquivo (por exemplo `ServicoCriacaoReserva.java:50-71`). Documentos usam o caminho a partir da raiz.

**Prioridade.** Importância (I) e Dificuldade (D) em **A**lta, **M**édia ou **B**aixa. Cenário de **maior prioridade = Importância A**, ordenado pela dificuldade.

---

## 2. Direcionadores de negócio

| ID | Direcionador | Origem | Atributos de qualidade que o sustentam |
|---|---|---|---|
| **BD-01** | Alocar salas, professores e materiais **sem conflito de horário**, inclusive na agenda do professor | `prd.md:19-29`; RN-01 a RN-03 (`prd.md:123-145`) | Consistência, corretude |
| **BD-02** | **Impedir dupla reserva** em solicitações simultâneas | `prd.md:25-29`; RN-04 (`prd.md:147-153`) | Consistência sob concorrência |
| **BD-03** | Controlar **recursos restritos** (aprovação só do Responsável) e **indisponibilidade** (manutenção/bloqueio) | `prd.md:25-29`; RN-05, RN-06 (`prd.md:155-169`) | Segurança, disponibilidade operacional |
| **BD-04** | **Rastrear e auditar** retiradas, devoluções e mudanças de estado | `prd.md:25-29`; RN-07 a RN-09 (`prd.md:171-193`), RF-19 | Auditabilidade, integridade |
| **BD-05** | Apoiar o **controle operacional por relatórios** | `prd.md:25-29`; RF-21 | Funcionalidade, desempenho |
| **BD-06** | Produzir **evidências objetivas** de qualidade, segurança, rastreabilidade e desempenho | `prd.md:19-21`; RNF-02 a RNF-14 | Testabilidade, verificabilidade |
| **BD-07** | Entregar **na semana 46**, em equipe, em **Java 21 e Spring Boot 3.x** | `prd.md:92-96`; RNF-01 (`prd.md:1014`) | Manutenibilidade, prazo |
| **BD-08** | Acesso restrito aos **três perfis oficiais**, sem exposição de informação interna em erros | RF-01, RNF-15 (`prd.md:205, 1252`); personas | Segurança, usabilidade |

**Restrição adicional CON-01** (batch diário de treino/retreino com dados novos): vem de `arquitetura.md:8, 108-116`, **está fora da baseline do produto** (`arquitetura.md:209`) e não tem finalidade definida. Entra neste ciclo apenas como restrição herdada (CEN-15 e CEN-16).

---

## 3. Utility tree

### 3.1 Árvore (Importância, Dificuldade)

```text
Utilidade do sistema
├── Consistência
│   ├── Concorrência ............... CEN-01 Dupla reserva simultânea ............ (A, A)
│   ├── Agenda docente ............. CEN-02 Conflito só na agenda do professor .. (A, M)
│   └── Ciclo de vida .............. CEN-07 Reserva iniciada/estados válidos .... (A, M)
├── Segurança
│   ├── Autorização contextual ..... CEN-03 Acesso indevido (perfil/objeto) ..... (A, A)
│   ├── Aprovação restrita ......... CEN-06 Aprovação com revalidação ........... (A, M)
│   └── Erros seguros .............. CEN-10 Recusa sem detalhes internos ........ (M, B)
├── Auditabilidade
│   └── Estado × auditoria ......... CEN-04 Nenhuma mudança sem registro ........ (A, A)
├── Disponibilidade operacional
│   ├── Manutenção e bloqueio ...... CEN-05 Recurso indisponível ................ (A, M)
│   └── Falha externa .............. CEN-11 Canal de notificação degradado ...... (M, M)
├── Testabilidade e verificabilidade
│   ├── Persistência realista ...... CEN-08 Testes com banco containerizado ..... (A, A)
│   ├── Gate de qualidade no CI .... CEN-09 Pull request bloqueado por critério . (A, B)
│   └── Rastreabilidade ............ CEN-14 Requisito crítico sem teste ......... (A, B)
├── Desempenho
│   └── Carga ...................... CEN-13 Consultas/reservas/relatórios ........ (M, A)
├── Manutenibilidade
│   └── Política de estados ........ CEN-12 Nova transição/ator ................. (M, M)
└── Operabilidade do batch (CON-01)
    ├── Execução diária ............ CEN-15 Só dados novos ...................... (A, A)  [herdada]
    └── Jobs sobrepostos/falha ..... CEN-16 Exclusão e recuperação ............... (A, A)  [herdada]
```

### 3.2 Prioridades e justificativas

| Ordem | Cenário | I | D | Justificativa da importância | Justificativa da dificuldade (estado atual) | Origem em `arquitetura.md` |
|---|---|---|---|---|---|---|
| 1 | CEN-01 | A | A | RN-04 crítica (`prd.md:147`); BD-02 | Exige mecanismo sobre persistência que não existe | ATAM-01 (`:633`) |
| 2 | CEN-03 | A | A | RN-06 e RF-01 críticas; BD-03, BD-08 | Falta autenticação e autorização contextual por operação | ATAM-03 (`:659`) |
| 3 | CEN-04 | A | A | RN-09 crítica; BD-04 | Exige atomicidade estado+auditoria, hoje inexistente | ATAM-04 (`:672`) |
| 4 | CEN-08 | A | A | RNF-07 MUST; RN-09 exige "persistência realista" (`prd.md:193`) | Não há camada de persistência a testar | novo (RNF-07) |
| 5 | CEN-15 | A | A | Herdada (`arquitetura.md:617-620`); **não ratificada** | Sem finalidade, sem código | ATAM-06/07/18 |
| 6 | CEN-16 | A | A | Herdada; **não ratificada** | Sem finalidade, sem código | ATAM-08/09/19 |
| 7 | CEN-02 | A | M | RN-03 crítica; BD-01 | Regra existe; falta liberar/proteger a agenda | ATAM-02 (`:646`) |
| 8 | CEN-05 | A | M | RN-05 crítica; BD-03 | Manutenção existe; bloqueio e política de reservas existentes faltam | ATAM-05 (`:685`) |
| 9 | CEN-06 | A | M | RN-06 crítica; risco R4 (`arquitetura.md:224`) | Duas rotas de aprovação a unificar | novo |
| 10 | CEN-07 | A | M | RN-07 e RN-08 críticas | Máquina de estados existe; ponto único de mudança falta | ATAM-16 (parcial) |
| 11 | CEN-09 | A | B | RNF-02, 03, 12, 13 MUST | Mudança de configuração e testes | novo |
| 12 | CEN-14 | A | B | RN-10 e RNF-04 críticas | Já existe verificação automática por parsing de documentos | novo |
| 13 | CEN-13 | M | A | RNF-14 MUST, mas sem meta aprovada | Metas ausentes e sem infraestrutura de carga | ATAM-15 (`:815`) |
| 14 | CEN-11 | M | M | RF-20, RNF-08 | Canal já é abstraído; falta timeout e integração real | ATAM-13 (`:789`) |
| 15 | CEN-12 | M | M | RN-07 com lacunas | Tabela central existe; estados são `String` | ATAM-16 (`:828`) |
| 16 | CEN-10 | M | B | RF-22, RNF-15/17 | `TradutorErros` já existe | ATAM-14 (parcial) |

---

## 4. Cenários de atributo de qualidade (seis partes)

Cada cenário lista fonte, estímulo, artefato, ambiente, resposta e medida de resposta. Medidas que o PRD não define aparecem como **[HIP]**.

### CEN-01 — Dupla reserva simultânea · Consistência › Concorrência · (A, A)

| Parte | Conteúdo |
|---|---|
| Fonte | Dois Solicitantes autenticados, usuários distintos |
| Estímulo | Enviam, ao mesmo tempo, a criação de reserva para o mesmo recurso e o mesmo período |
| Artefato | Criação de reservas, verificação de sobreposição e persistência de reservas |
| Ambiente | Operação normal, persistência disponível (RNF-09 pede banco em Testcontainers) |
| Resposta | Exatamente uma solicitação é aceita; a outra é recusada por conflito, sem reserva parcial nem resíduo |
| Medida de resposta | **Exatamente 1 reserva aceita** por cenário (RN-04; RNF-09, `prd.md:1150-1165`). Número de repetições adicionais: **[HIP]** pendente da equipe |

### CEN-02 — Conflito somente na agenda do professor · Consistência › Agenda docente · (A, M)

| Parte | Conteúdo |
|---|---|
| Fonte | Solicitante |
| Estímulo | Solicita sala e material livres com um professor que já está alocado no mesmo período |
| Artefato | Criação e alteração de reserva; agenda do professor |
| Ambiente | Operação normal |
| Resposta | Reserva recusada com motivo compreensível; agenda do professor inalterada |
| Medida de resposta | **0 reservas aceitas com sobreposição na agenda do professor** (RN-03, `prd.md:139`; ATAM-02) |

### CEN-03 — Acesso indevido por perfil, propriedade ou responsabilidade · Segurança · (A, A)

| Parte | Conteúdo |
|---|---|
| Fonte | Usuário sem o perfil, sem a propriedade ou sem a responsabilidade exigidas |
| Estímulo | Invoca diretamente, no back-end, criar, alterar, cancelar, aprovar ou consultar histórico |
| Artefato | Autenticação e autorização nos serviços |
| Ambiente | Operação normal |
| Resposta | Operação recusada, sem alterar estado nem expor dados do objeto |
| Medida de resposta | **0 operações indevidas aceitas**; todos os fluxos críticos com testes positivos e negativos (RNF-15, `prd.md:1252-1267`). Quantidade exata de cenários: **[HIP]** |

### CEN-04 — Coerência entre estado e auditoria · Auditabilidade · (A, A)

| Parte | Conteúdo |
|---|---|
| Fonte | Falha de dependência ou de persistência; consulta de histórico concorrente às mudanças |
| Estímulo | Uma mudança de estado é efetivada (criação, aprovação, rejeição, cancelamento) e o histórico é consultado depois |
| Artefato | Transição de estados, registro de auditoria e consulta de histórico |
| Ambiente | Operação normal e degradação parcial |
| Resposta | Não existe mudança de estado sem auditoria correspondente; o histórico é completo e não editável pelos perfis operacionais |
| Medida de resposta | **0 mudanças de estado sem registro** (RN-09, `prd.md:187`; ATAM-04). Completude do histórico = 100% e 0 registros removíveis/alteráveis: **[INF]** derivadas de RF-19 e `administrador.md` (auditoria não apagável) |

### CEN-05 — Recurso em manutenção ou bloqueio · Disponibilidade operacional · (A, M)

| Parte | Conteúdo |
|---|---|
| Fonte | Administrador (registra) e Solicitante (reserva) |
| Estímulo | Criação, alteração ou aprovação de reserva cujo período cruza manutenção ou bloqueio de sala ou material |
| Artefato | Indisponibilidade, criação/alteração/aprovação e pesquisa |
| Ambiente | Operação normal |
| Resposta | Reserva nova recusada; pesquisa não apresenta o recurso como disponível; reservas já existentes seguem a política pendente |
| Medida de resposta | **0 reservas novas aceitas durante manutenção** (RN-05, `prd.md:155`). Para bloqueio, a persona Administrador exige indisponibilidade (`administrador.md:34`), mas a equivalência com manutenção segue pendente (`arquitetura.md:203, 210`): **[HIP]** 0 durante bloqueio |

### CEN-06 — Aprovação de recurso restrito com revalidação · Segurança › Aprovação · (A, M)

| Parte | Conteúdo |
|---|---|
| Fonte | Responsável; Solicitante e Administrador como tentativas indevidas |
| Estímulo | Aprovação após a disponibilidade mudar (manutenção, bloqueio, outra reserva, agenda docente) ou com a reserva em estado não elegível (`REJEITADA`, `CANCELADA`, já `APROVADA`) |
| Artefato | Aprovação (RF-14), revalidação e fluxo de estados |
| Ambiente | Operação normal |
| Resposta | Aprova só com perfil Responsável, estado elegível e disponibilidade revalidada; caso contrário recusa |
| Medida de resposta | **0 aprovações por perfil diferente de Responsável** (RN-06); **0 aprovações com conflito ou indisponibilidade** (`fluxos-personas.md:962-972`; `responsavel.md:95, 121`) |

### CEN-07 — Ciclo de vida e preservação do registro · Consistência › Estados · (A, M)

| Parte | Conteúdo |
|---|---|
| Fonte | Solicitante, Responsável ou Administrador |
| Estímulo | Tenta cancelar, alterar ou apagar reserva iniciada (`EM_USO`) ou concluída, ou provoca transição não prevista |
| Artefato | Fluxo de estados e preservação do registro |
| Ambiente | Operação normal |
| Resposta | Operação recusada; registro e histórico preservados; só transições especificadas são aceitas |
| Medida de resposta | **0 reservas iniciadas apagadas ou canceladas** (RN-08, `prd.md:179`); **0 transições inválidas aceitas** (RN-07, `prd.md:171`) |

### CEN-08 — Persistência crítica validada em banco real · Testabilidade · (A, A)

| Parte | Conteúdo |
|---|---|
| Fonte | Pipeline de CI (execução disparada por pull request) |
| Estímulo | Executa os testes de integração das áreas críticas: reservas, estados, auditoria e movimentação de materiais |
| Artefato | Camada de persistência e seus testes |
| Ambiente | GitHub Actions com banco containerizado |
| Resposta | Dados críticos são gravados e lidos de banco real; testes passam sem depender apenas de mocks |
| Medida de resposta | **Todas as áreas críticas** cobertas por teste contra banco containerizado (RNF-07, `prd.md:1116-1131`). Valor numérico adicional: **[HIP]** |

### CEN-09 — Pull request bloqueado por critério oficial · Verificabilidade › CI · (A, B)

| Parte | Conteúdo |
|---|---|
| Fonte | Desenvolvedor |
| Estímulo | Abre ou atualiza um pull request para `main` |
| Artefato | Pipeline de CI (build, testes, JaCoCo, análise estática) |
| Ambiente | GitHub Actions |
| Resposta | O pipeline executa build e testes e **impede o merge** se um critério oficial não for atendido |
| Medida de resposta | Linhas ≥ 80% (RNF-02); branches ≥ 70% (RNF-03); SonarCloud sem vulnerabilidade crítica (RNF-13); workflow executado no PR (RNF-12). Critérios adicionais de bloqueio: **[HIP]** (`prd.md:1201-1216`) |

### CEN-10 — Recusa sem detalhes internos · Segurança › Usabilidade · (M, B)

| Parte | Conteúdo |
|---|---|
| Fonte | Qualquer perfil |
| Estímulo | Operação recusada por regra ou falha interna inesperada |
| Artefato | Tratamento de erros e mensagens ao usuário |
| Ambiente | Operação normal e falha |
| Resposta | Mensagem compreensível com a causa observável, sem pilha, classe Java, credencial ou causa encadeada |
| Medida de resposta | **0 exposições de informação sensível**; erros críticos de RN-01, RN-02 e RN-05 cobertos (RNF-15, RNF-17) |

### CEN-11 — Canal de notificação degradado · Disponibilidade › Falha externa · (M, M)

| Parte | Conteúdo |
|---|---|
| Fonte | Canal de notificação (simulado ou API externa; escolha pendente, J15) |
| Estímulo | Timeout, indisponibilidade ou resposta inválida durante criação ou aprovação |
| Artefato | Notificação (RF-20) |
| Ambiente | Operação normal com dependência degradada |
| Resposta | Falha isolada e observável; reserva, estado e auditoria permanecem íntegros |
| Medida de resposta | **0 operações principais corrompidas** nos cenários WireMock (RNF-08; ATAM-13, `arquitetura.md:797`) |

### CEN-12 — Mudança na política de estados · Manutenibilidade · (M, M)

| Parte | Conteúdo |
|---|---|
| Fonte | Equipe do produto |
| Estímulo | Aprova estado inicial, novas transições, atores ou regra de liberação de recurso |
| Artefato | Política de estados e componentes que a aplicam |
| Ambiente | Evolução planejada |
| Resposta | Mudança concentrada em poucos pontos, rastreável e coberta por testes |
| Medida de resposta | **[HIP]** número de pontos de código alterados por mudança de política (`arquitetura.md:836`) |

### CEN-13 — Operação sob carga · Desempenho · (M, A)

| Parte | Conteúdo |
|---|---|
| Fonte | Usuários simultâneos |
| Estímulo | Aumento de consultas de disponibilidade, criações de reserva e relatórios |
| Artefato | Aplicação e persistência |
| Ambiente | Plano JMeter aprovado pela equipe |
| Resposta | Comportamento funcional e seguro dentro das metas aprovadas |
| Medida de resposta | **Pendente**: valor-alvo, unidade e taxa de erro "pendentes de aprovação da equipe" (RNF-14, `prd.md:1235-1250`) |

### CEN-14 — Requisito crítico sem teste ou sem ligação · Verificabilidade › Rastreabilidade · (A, B)

| Parte | Conteúdo |
|---|---|
| Fonte | Revisor de QA |
| Estímulo | Um requisito crítico é criado, alterado ou perde a ligação com teste, risco ou evidência |
| Artefato | Matriz de rastreabilidade (PRD §9), plano de testes e testes automatizados |
| Ambiente | Revisão e CI |
| Resposta | A lacuna é detectada automaticamente |
| Medida de resposta | **100% dos requisitos críticos rastreados** (RN-10, `prd.md:195`; RNF-04, `prd.md:1065`) |

### CEN-15 — Execução diária incremental (CON-01) · Operabilidade › Batch · (A, A) [herdada]

| Parte | Conteúdo |
|---|---|
| Fonte | Agendador |
| Estímulo | Chega o horário de fim do dia |
| Artefato | Job diário de treino/retreino |
| Ambiente | Operação normal |
| Resposta | Uma execução controlada e observável, usando somente dados novos |
| Medida de resposta | **[HIP]** "uma execução por ciclo aprovado"; "0 omissões e 0 reclassificações" (`arquitetura.md:705, 718`) |

### CEN-16 — Jobs sobrepostos e falha parcial (CON-01) · Confiabilidade › Batch · (A, A) [herdada]

| Parte | Conteúdo |
|---|---|
| Fonte | Agendador ou retomada |
| Estímulo | Nova execução começa durante outra ativa; ou falha após processamento parcial |
| Artefato | Coordenação de jobs, checkpoint e progresso |
| Ambiente | Operação ou recuperação |
| Resposta | Uma execução efetiva; sem perda silenciosa nem efeito duplicado |
| Medida de resposta | **[HIP]** "uma execução efetiva por intervalo"; "0 omissões/duplicações após recuperação" (`arquitetura.md:731, 744`) |

---

## 5. Abordagens e táticas encontradas nos cenários de maior prioridade

**Escopo:** todos os cenários de importância A (CEN-01 a 09, 14, 15, 16). Situação de cada abordagem: **Efetiva**, **Parcial** ou **Ausente**.

### 5.0 Panorama do código

- **Estilo real:** classes de domínio em Java puro, **um único pacote** (`...domain`), sem Spring, controllers, repositórios, DTOs ou camada de persistência [COD/MED: JaCoCo "45 classes"; `pom.xml` só declara dependências de teste].
- **Estado:** todo em memória, por instância (`HashMap`/`ArrayList`) e um mapa estático de auditoria [COD].
- **Fluxo de criação:** `ServicoCriacaoReserva` orquestra sobreposição, manutenção e agenda do professor dentro de um bloco `synchronized`, depois grava a auditoria [COD `:50-73`].
- **Código sem chamador em produção** (nenhum uso em `src/main`, confirmado por `grep`): `ValidadorConcorrencia`, `ValidadorAgendaProfessor`, `RastreabilidadeValidator`, `ServicoNotificacao`/`NotificacaoSimulada` (usados só em testes).

### 5.1 CEN-01 — Dupla reserva simultânea

| # | Abordagem / tática | Evidência | Situação |
|---|---|---|---|
| 1 | **Exclusão mútua**: um único `synchronized (secaoCritica)` envolve verificar e registrar | `ServicoCriacaoReserva.java:24, 50-71` | Efetiva **apenas dentro de uma instância/processo** |
| 2 | **Detecção de conflito antes de confirmar** (check-then-act na mesma seção crítica) | `ServicoCriacaoReserva.java:52-59`; `ValidadorSobreposicao.java:30-43` | Efetiva; custo O(n): varredura linear da lista (`ValidadorSobreposicao.java:31`) |
| 3 | Métodos do validador `synchronized` (segundo monitor) | `ValidadorSobreposicao.java:20, 30, 46, 64` | Efetiva, redundante |
| 4 | **Restrição na persistência** (constraint, lock ou isolamento; ADR-002) | Javadoc admite pendência: `ServicoCriacaoReserva.java:17-18`; nenhuma dependência de persistência em `pom.xml` | **Ausente** |
| 5 | Identidade da reserva | `reserva.setId((long) (reservas.size() + 1))` — `ServicoCriacaoReserva.java:64` | Frágil: **[SONDA P6]** duas instâncias geraram id 1 e 1 |
| 6 | Mecanismo alternativo `ValidadorConcorrencia` (mapa de `AtomicBoolean` por recurso+início+fim) | `ValidadorConcorrencia.java:12, 20-28, 30-37` | **Sem chamador em produção**; **[SONDA P14]** períodos 08–10 e 09–11 aceitos ambos; chave nunca liberada; cobertura L 9/26, B 1/18 **[MED]** |
| 7 | Teste automatizado de concorrência real, 300 rodadas com duas threads e `CountDownLatch` | `ReservaCriacao_RF10_Test.java:209-239` | Efetivo para o modelo em memória; passou nos 4 runs verdes do CI |
| 8 | Teste nomeado "RN-04" | `ReservaUnicidadeConcorrencia_RN04_Test.java:36-37, 157-170` exercita `ValidadorConcorrencia` | **Valida o artefato errado** (falsa confiança) |
| 9 | Evidência exigida por RNF-09 (Testcontainers) | Sem Testcontainers em `pom.xml`; ausência admitida em `DevolucaoMateriais_RF17_Test.java:13` | **Ausente** |

**Leitura:** RN-04 é atendida no modelo em memória de **uma** instância. Sob instâncias múltiplas ou persistência compartilhada, a garantia desaparece; o teste que carrega o nome da regra não protege o caminho real. Consultas de disponibilidade tomam o **mesmo monitor** (`ServicoCriacaoReserva.java:106-108`), o que aproxima CEN-01 de CEN-13 (ver TO-01).

### 5.2 CEN-02 — Conflito na agenda do professor

| # | Abordagem / tática | Evidência | Situação |
|---|---|---|---|
| 1 | Agenda embutida no `Professor` com predicado de sobreposição semiaberto | `Professor.java:13, 24-31, 97-99` | Efetiva |
| 2 | Verificação e registro da agenda dentro da seção crítica da criação | `ServicoCriacaoReserva.java:57, 68-70` | Efetiva |
| 3 | Verificação na alteração, ignorando a entrada "própria" por **igualdade de período** | `ServicoCriacaoReserva.java:120-130`; `Professor.java:37-50, 53-63` | Parcial (heurística, não identidade da reserva) |
| 4 | **Liberação da agenda ao cancelar** | `ServicoGestaoReserva.java:35-42` não toca a agenda | **Ausente** — **[SONDA P15]** após cancelar, a agenda mantém 1 entrada e nova reserva do mesmo professor em outra sala e mesmo horário é recusada ("conflito professor") |
| 5 | Lista `Reserva.professores` (múltiplos docentes) | Criação usa só `getProfessor()` (`ServicoCriacaoReserva.java:57, 68`); a lista só é lida em `ValidacaoAlocacaoDocente.java:26-30` e `ValidadorAgendaProfessor.java:37-43` | **Lacuna**: docentes da lista não bloqueiam na criação **[COD]** |
| 6 | Proteção do estado do professor | `Professor.agenda` é `ArrayList` sem sincronização própria (`Professor.java:13`); protegida apenas pelo monitor de uma instância do serviço; `ValidacaoAlocacaoDocente.java:24, 40` lê sob outro monitor | Parcial |
| 7 | `ValidadorAgendaProfessor` (RN-03 isolado) | Sem chamador em produção | Código morto |
| 8 | Modelo do professor duplicado | `Recurso.TipoRecurso.PROFESSOR` (`Recurso.java:8`) e a classe `Professor` coexistem (pendência `arquitetura.md:78-80`) | Decisão pendente |

### 5.3 CEN-03 — Acesso indevido

| # | Abordagem / tática | Evidência | Situação |
|---|---|---|---|
| 1 | **Autenticar usuários** | `Usuario` sem credencial (`Usuario.java:11-15`); `validarToken` só confere prefixo `Bearer ` e tamanho (`ValidadorAutorizacao.java:36-38`); perfil e `ativo` vêm do chamador (`Usuario.java:39`) | **Ausente** |
| 2 | **Autorização por perfil na camada de serviço** (RBAC; ADR-003) | `ValidadorAutorizacao.java:40-45, 53-65`; usada em `CadastroRecursos.java:22, 31, 37, 46`, `GestaoUsuarios.java:14`, `GestaoBloqueios.java:21`, `MovimentacaoMateriais.java:22, 29`, `ValidacaoAlocacaoDocente.java:25`, `ServicoRelatorios.java:15` | **Efetiva** (7 pontos, uso consistente) |
| 3 | Permissões das operações de reserva | Solicitante só tem `CONSULTAR_DISPONIBILIDADE` (`ValidadorAutorizacao.java:60-61`); criar exige só usuário ativo (`ServicoCriacaoReserva.java:38`) | **Ausente** — **[SONDA P3]** um ADMINISTRADOR criou reserva |
| 4 | **Autorização por objeto** (propriedade) | `ServicoGestaoReserva.java:44-50` (`Objects.equals` nos ids) | Parcial — **[SONDA P10]** dois usuários com `id` nulo passam; `ServicoCriacaoReserva.alterarPeriodo` é público e sem checagem (`:116-132`) |
| 5 | Aprovação só por Responsável | Rota A por permissão: `ValidadorAutorizacao.java:19-26`. Rota B por perfil: `ValidadorAprovacao.java:53-56` (não verifica usuário ativo) | Parcial — **[SONDA P11]** Solicitante recusado; **[SONDA P12]** qualquer Responsável aprova qualquer recurso restrito (escopo só em `aprovarComEscopo`, opcional, `:136-142`) |
| 6 | **Limitar exposição em erros** | `TradutorErros.java:14-19, 37-41` | Efetiva (lista de exceções + filtro de "Exception"/"java."; B 25/42 **[MED]**) |
| 7 | Autorização na consulta de histórico (RF-19) | `ValidadorAuditoria.obterAuditorias(Long)` não recebe usuário (`ValidadorAuditoria.java:47`) | **Ausente** |

### 5.4 CEN-04 — Estado × auditoria

| # | Abordagem / tática | Evidência | Situação |
|---|---|---|---|
| 1 | **Registrar antes de efetivar** | Aprovação/rejeição: `ValidadorAprovacao.java:104-108`; cancelamento: `ServicoGestaoReserva.java:39-41`; aprovação por permissão: `ValidadorAutorizacao.java:23-25` | Parcial: ordem correta, **sem atomicidade** |
| 2 | Criação | Estado em `ServicoCriacaoReserva.java:61`; auditoria **depois** e fora da seção crítica em `:72-73` | **Ordem inversa**: falha entre as duas linhas deixa estado sem auditoria **[INF]** |
| 3 | Armazenamento da auditoria | `auditoriasPendentes` estático (`ValidadorAuditoria.java:11, 13-17`) + mapa por instância `HashMap` sem sincronização (`:10, 30, 40, 52, 56`) | Frágil |
| 4 | Semântica de leitura | `obterAuditorias` só "drena" pendentes se a chave ainda não existe (`ValidadorAuditoria.java:47-57`) | **Defeito** — **[SONDA P4-A]** após ler o histórico entre criação e cancelamento, o cancelamento não aparece (1 de 2); um segundo validador vê só 1. **[SONDA P4-B]** sem leitura intermediária: 2; segundo validador: 0 |
| 5 | Imutabilidade | `editarAuditoria`/`apagarAuditoria` só lançam exceção (`:62-71`); `obterAuditorias` devolve a **lista interna** (`:56`); `Auditoria.setDescricao` público (`Auditoria.java:64`) | **Fachada** — **[SONDA P5-C]** `clear()` na lista devolvida zerou o histórico |
| 6 | Isolamento entre reservas | ids `size()+1` por instância (`ServicoCriacaoReserva.java:64`) + mapa estático por id | **Defeito** — **[SONDA P6]** auditoria da reserva #1 continha registros de `u1` e `u2` (duas reservas distintas) |
| 7 | Ponto único de mudança de estado | `Reserva.setEstado` público (`Reserva.java:93`); 6 escritores em produção (`ServicoCriacaoReserva.java:61`, `ServicoGestaoReserva.java:41`, `ValidadorApagamento.java:38`, `ValidadorAprovacao.java:62, 101`, `ValidadorAutorizacao.java:25`) | **Ausente**; `ValidadorApagamento.forcarCancelamento` (`:36-39`) muda o estado **sem auditoria e sem validar a transição** |
| 8 | Auditoria de alteração de período | Admitida como não implementada (`ServicoGestaoReserva.java:13-14`) | Decisão pendente registrada |
| 9 | Persistência da auditoria | Nenhuma | **Ausente** (RN-09 exige "persistência realista", `prd.md:193`) |

### 5.5 CEN-05 — Manutenção e bloqueio

| # | Abordagem / tática | Evidência | Situação |
|---|---|---|---|
| 1 | **Validar pré-condição antes de reservar** (manutenção de sala e materiais) | `ServicoCriacaoReserva.java:56, 140-151`; `ValidadorManutencao.java:68-79` | Efetiva na criação |
| 2 | Revalidação na alteração | `ServicoGestaoReserva.java:52-59` **fora** da seção crítica (`:30` antes de `:31`) | Parcial (janela entre validar e aplicar) **[INF]** |
| 3 | **Bloqueio na criação/alteração** | `GestaoBloqueios` só é usado em `ConsultaDisponibilidade.java:33`; ausente de `ServicoCriacaoReserva` | **Ausente** — **[SONDA P1]** reserva criada em sala bloqueada (`estaDisponivel = false`, mesmo assim `SOLICITADA`) |
| 4 | Revalidação na aprovação | Opt-in: `aprovarComValidacaoDisponibilidade` (`ValidadorAprovacao.java:115-122`) só checa manutenção | Parcial — **[SONDA P8]** `aprovar()` direto aprovou com manutenção registrada depois |
| 5 | Segurança de threads do cadastro de manutenção | `ValidadorManutencao.manutencoesPorRecurso` é `HashMap` (`:14`); `registrarManutencao` (`:19-24`) não usa a seção crítica de reservas | Frágil **[INF]** |
| 6 | Política para reservas já existentes | Nenhum código; pendente (`administrador.md` "Erros e Impedimentos"; `arquitetura.md:266`) | Decisão pendente |

### 5.6 CEN-06 — Aprovação com revalidação

| # | Abordagem / tática | Evidência | Situação |
|---|---|---|---|
| 1 | Rota A (`ValidadorAutorizacao.aprovarSolicitacao`) valida perfil **e** transição (`ValidadorFluxoEstados.validarTransicao`) e audita | `ValidadorAutorizacao.java:19-26` | Efetiva; **não** revalida disponibilidade |
| 2 | Rota B (`ValidadorAprovacao.aprovar`) valida só perfil, reaprovação e conflito com aprovadas **da própria instância** | `ValidadorAprovacao.java:13, 53-64, 70-82, 144-148` | Parcial — **[SONDA P7]** `REJEITADA → APROVADA` e `CANCELADA → APROVADA` aceitas |
| 3 | Separação de funções (Solicitante não aprova) | `ValidadorAprovacao.java:54-56`; `ValidadorAutorizacao.java:20` | Efetiva (P11) |
| 4 | Escopo de responsabilidade do Responsável | `aprovarComEscopo` recebe o id permitido por parâmetro (`ValidadorAprovacao.java:136-142`); teste correspondente `@Disabled` (`ReservaAprovacao_RN06_Test.java:210`) | **Ausente** (decisão Q-004 pendente) |
| 5 | Aprovação concorrente | `aprovarConcorrente` compara referência e não faz mais nada (`ValidadorAprovacao.java:124-128`) | Stub |

### 5.7 CEN-07 — Ciclo de vida e preservação do registro

| # | Abordagem / tática | Evidência | Situação |
|---|---|---|---|
| 1 | **Máquina de estados orientada a tabela** | `ValidadorFluxoEstados.java:14-55, 64-86` | Efetiva (cobertura L 43/43, B 12/14 **[MED]**) |
| 2 | Bloqueio de cancelamento/apagamento de `EM_USO` e `CONCLUIDA` | `ValidadorApagamento.java:15-23`; usado em `ServicoGestaoReserva.java:37-38` | Efetiva |
| 3 | Operação "apagar" | Não existe método para remover reserva; `apagarPorId` só valida `id > 0` (`ValidadorApagamento.java:48-52`) | RN-08 protegida por ausência de operação **[INF]** |
| 4 | Alteração em estado terminal | `Reserva.alterarHorario` só bloqueia `EM_USO` (`Reserva.java:135-142`) | **Defeito** — **[SONDA P9]** reserva `CANCELADA` teve o horário alterado |
| 5 | Transições `EM_USO`, `CONCLUIDA`, `NAO_COMPARECEU` | Definidas na tabela, mas **nenhum código de produção as executa** (`grep`: só leituras em `ValidadorApagamento` e `Reserva.java:136`) | Lacuna de atores (`arquitetura.md:256-267`) |
| 6 | Liberação de recurso ao cancelar | `ValidadorSobreposicao.java:30-43` não filtra por estado | Não libera — **[SONDA P2]** nova reserva no mesmo período recusada após cancelar |
| 7 | Estado como `String` | `Reserva.java:15` | Frágil (sem verificação de tipo) |

### 5.8 CEN-08 — Persistência realista

| # | Abordagem / tática | Evidência | Situação |
|---|---|---|---|
| 1 | Camada de persistência | Todos os repositórios são coleções em memória: `ServicoCriacaoReserva.java:21`, `ValidadorSobreposicao.java:15`, `ValidadorAuditoria.java:10-11`, `ValidadorManutencao.java:14`, `GestaoBloqueios.java:17`, `CadastroRecursos.java:17-18`, `MovimentacaoMateriais.java:17-18` (javadoc `:12-14` admite que RNF-07 não é atendida) | **Ausente** |
| 2 | Testes contra banco containerizado | Sem Testcontainers em `pom.xml`, `ci.yml` ou `src/` | **Ausente** |
| 3 | Testes de RF-16/RF-17 | Bloqueados por "exige persistência realista" (`RetiradaMateriais_RF16_Test.java:13`, `DevolucaoMateriais_RF17_Test.java:13`) | Bloqueados |
| 4 | Viabilidade no CI | O runner `ubuntu-latest` (`ci.yml:19`) | **[INF]** adotar Testcontainers é compatível com o CI atual; não verificado |

### 5.9 CEN-09 — Gate de qualidade no CI

| # | Abordagem / tática | Evidência | Situação |
|---|---|---|---|
| 1 | Workflow em pull request e em `push` na `main`, Java 21, cache Maven | `ci.yml:3-7, 25-30` | Efetiva |
| 2 | `mvn -B verify` | `ci.yml:32-33` | Efetiva para "compila e testa" |
| 3 | JaCoCo: só `prepare-agent` e `report` | `pom.xml:88-107`; **sem** goal `check` | **Sem gate** — **[MED]** branches 61,8% e o CI passa (run 36158506674) |
| 4 | Publicação do relatório | `ci.yml:35-44` publica só `surefire`/`failsafe`; JaCoCo não é publicado | Parcial |
| 5 | SonarCloud / JMeter | Ausentes de `pom.xml` e `ci.yml` | **Ausente** |
| 6 | Proteção da `main` | `gh api .../branches/main/protection`: check `Build and test` obrigatório, `strict: true`, `enforce_admins: true`, sem force-push, sem exclusão, 0 aprovações exigidas **[MED]** | Efetiva |
| 7 | Prova de que o gate detecta falha | Run `36157529745` **falhou** com 1 falha controlada e o `revert` voltou a verde (`36157791758`) **[MED]** | Efetiva |

### 5.10 CEN-14 — Rastreabilidade

| # | Abordagem / tática | Evidência | Situação |
|---|---|---|---|
| 1 | **Verificação automatizada por parsing**: os testes leem a matriz do PRD §9, o plano de testes e os IDs `T-…` dos testes | `ReservaRastreabilidade_RN10_Test.java:346, 370` (e `:72, 92, 107, 181, 225, 244, 265`) | Efetiva para rastreabilidade **documento ↔ teste** |
| 2 | Acoplamento à árvore de arquivos | Falhou 11 testes ao rodar sem `docs/` (descoberto na medição) **[MED]** | Aceitável; dependência de diretório de trabalho |
| 3 | `RastreabilidadeValidator` | `calcularCoberturaCritica()` devolve `1.0` fixo (`RastreabilidadeValidator.java:24-26`); `existeLigacao` só para RN-01 (`:16-18`); **0/11 linhas cobertas** **[MED]** | Stub perigoso: falsa garantia se for adotado |
| 4 | Ligação requisito → **código** | Nenhuma | Ausente (matriz cobre requisito → teste) |

### 5.11 CEN-15 e CEN-16 — Batch CON-01

Não há scheduler, checkpoint, job, extração, treinamento ou artefato em `src/main`, `pom.xml` ou `ci.yml` [MED: `grep`]. **Nenhuma abordagem ou tática foi encontrada; os cenários não são avaliáveis por evidência neste ciclo.** O `arquitetura.md` trata o batch como "candidato" (§14.4, §15.2, §16, ADR-006 a ADR-014) e bloqueia a implementação até a decisão de finalidade (ADR-011, `arquitetura.md:1373-1385`; §49.1, `:1817-1819`). Isso é coerente, mas significa que dois cenários herdados de importância A permanecem **sem nenhuma evidência**.

---

## 6. Pontos de sensibilidade

Propriedades de componentes cujo ajuste altera de forma decisiva a resposta de um cenário.

| ID | Ponto de sensibilidade | Evidência | Cenários | Direcionadores |
|---|---|---|---|---|
| SP-01 | **Escopo do mutex**: um monitor por instância de `ServicoCriacaoReserva`, local à JVM | `ServicoCriacaoReserva.java:24, 50` | CEN-01, 02 | BD-01, BD-02 |
| SP-02 | **Fronteira do check-then-act**: tudo que valida e muda estado precisa estar na mesma seção crítica. A aprovação usa outro monitor (`synchronized` em `ValidadorAprovacao.java:53`) e sua própria lista (`:13`) | `ValidadorAprovacao.java:13, 53, 70-82` | CEN-01, 06 | BD-02, BD-03 |
| SP-03 | **Identidade da reserva** (`size()+1`) alimenta a sobreposição (`ValidadorSobreposicao.java:66`) e a auditoria (mapa por id) | `ServicoCriacaoReserva.java:64` | CEN-01, 04 | BD-02, BD-04 |
| SP-04 | **Ordem "auditar → mudar" versus "mudar → auditar"** | `ServicoCriacaoReserva.java:61, 72` × `ServicoGestaoReserva.java:39-41` | CEN-04 | BD-04 |
| SP-05 | **Semântica de leitura do armazenamento de auditoria** (drenar ao ler) | `ValidadorAuditoria.java:47-57` | CEN-04 | BD-04 |
| SP-06 | **Predicado de ocupação**: quais estados ocupam recurso e agenda | `ValidadorSobreposicao.java:30-43` (sem filtro de estado) | CEN-01, 02, 07 | BD-01 |
| SP-07 | **Mapa perfil → permissão** e ausência de permissões de reserva | `ValidadorAutorizacao.java:53-65` | CEN-03 | BD-03, BD-08 |
| SP-08 | **Origem da identidade do chamador** (objeto `Usuario` entregue pelo chamador; `validarToken` só sintático) | `ValidadorAutorizacao.java:36-38`; `Usuario.java:39` | CEN-03 | BD-08 |
| SP-09 | **Predicado de sobreposição semiaberto replicado em 5 lugares** (adjacência aceita de fato) | `ValidadorSobreposicao.java:60-62, 102-104`; `Professor.java:97-99`; `ValidadorManutencao.java:107-109`; `GestaoBloqueios.java:33`; `ValidadorAprovacao.java:77-78` | CEN-02, 05, 12 | BD-01 |
| SP-10 | **Relógio do sistema** (`LocalDateTime.now()`) dentro da regra RN-01; testes com datas fixas já passadas | `Reserva.java:41`; `ReservaSobreposicao_RN02_Test.java:33-38`; incidente registrado em `docs/testes/plano-tdd.md:1146` | CEN-12, 09 | BD-06 |
| SP-11 | **Momento e caminho da revalidação na aprovação** (opt-in) | `ValidadorAprovacao.java:53` × `:115` | CEN-05, 06 | BD-03 |
| SP-12 | **Representação do professor** (`Professor` com agenda embutida × `Recurso` do tipo `PROFESSOR`) | `Professor.java:13`; `Recurso.java:8` | CEN-02 | BD-01 |
| SP-13 | **Chamada síncrona e sem limite de tempo ao canal de notificação** | `ServicoNotificacao.java:36` | CEN-11, 13 | BD-03 |
| SP-14 | **Limiar de bloqueio do CI** (ausência de `jacoco:check`) | `pom.xml:88-107`; `ci.yml:33` | CEN-09 | BD-06 |

---

## 7. Pontos de trade-off

Sensibilidades que afetam **mais de um atributo** de qualidade em sentidos opostos.

| ID | Trade-off | Ganho | Custo | Evidência | Cenários |
|---|---|---|---|---|---|
| TO-01 | **Monitor único para todas as reservas** | Consistência (RN-04) simples de provar | Serializa reservas de recursos independentes; consultas de disponibilidade competem pelo mesmo monitor; varredura O(n) sob o lock | `ServicoCriacaoReserva.java:50, 106-108`; `ValidadorSobreposicao.java:31` | CEN-01 × CEN-13 |
| TO-02 | **Estado em memória por instância** | Testes rápidos (build completo com 163 testes em ~22 s no CI **[MED]**) e domínio simples | Sem durabilidade, sem auditoria persistente, sem verificação de RNF-07; conflito entre instâncias | ver 5.8 | CEN-08, 04, 01 × testabilidade rápida |
| TO-03 | **Domínio em Java puro, sem framework** | Independência tecnológica e testabilidade | Descumpre RNF-01 (Spring Boot 3.x); nenhuma facilidade de transação, segurança ou HTTP | `pom.xml`; `arquitetura.md:92, 237-254` | Manutenibilidade × BD-07 |
| TO-04 | **Auditoria estática global** (sem injeção) | Baixo acoplamento entre serviços | Estado global, colisão de ids, leitura destrutiva, difícil de isolar em testes | `ValidadorAuditoria.java:11-17` | CEN-04 × manutenibilidade |
| TO-05 | **Revalidação de aprovação opt-in** | API flexível | Aprovações sem revalidar disponibilidade (RN-06) | `ValidadorAprovacao.java:53, 115` | CEN-06 × usabilidade da API |
| TO-06 | **Notificação síncrona com captura total de `RuntimeException`** | Isola a falha do canal (BD-03) | Sem timeout; sem log da causa; bloqueia o chamador | `ServicoNotificacao.java:35-40` | CEN-11 × CEN-13 |
| TO-07 | **Mensagens por lista de exceções** | Nenhum vazamento de detalhes internos | Mensagem genérica para qualquer exceção não catalogada | `TradutorErros.java:14-35` | CEN-10 |

---

## 8. Riscos

Severidade proposta pelo avaliador (A/M/B). Nenhum risco é considerado aceito (`arquitetura.md:1541`).

| ID | Risco | Evidência | Cenários | Direcionadores | Sev. |
|---|---|---|---|---|---|
| **R-01** | RN-04 só é garantida em memória, por instância; sem garantia sobre persistência compartilhada | `ServicoCriacaoReserva.java:17-18, 24, 50, 64`; SONDA P6 | CEN-01, 08 | BD-02 | A |
| **R-02** | O teste de RN-04 valida código sem uso em produção e que aceita sobreposição parcial: **falsa confiança** | `ReservaUnicidadeConcorrencia_RN04_Test.java:36-37`; `ValidadorConcorrencia.java:24-37`; SONDA P14 | CEN-01 | BD-02, BD-06 | A |
| **R-03** | **Bloqueio não impede reserva**; `alterarPeriodo` público sem checagem de bloqueio, manutenção ou dono | `ServicoCriacaoReserva.java:52-59, 116-132`; SONDA P1 | CEN-05, 03 | BD-03 | A |
| **R-04** | Auditoria perde, oculta ou mistura registros; lista mutável; ids colidem; nada persiste | `ValidadorAuditoria.java:10-17, 47-71`; SONDAS P4, P5, P6 | CEN-04 | BD-04 | A |
| **R-05** | Duas rotas de aprovação: `aprovar()` aceita transições inválidas e não revalida | `ValidadorAprovacao.java:53-64, 115-122`; SONDAS P7, P8, P12 | CEN-06, 07 | BD-03 | A |
| **R-06** | Segurança incompleta: sem autenticação real, criação sem checagem de perfil, ids nulos passam, histórico sem autorização, escopo do Responsável inexistente | `ValidadorAutorizacao.java:36-38, 60-61`; `ServicoCriacaoReserva.java:38`; `ServicoGestaoReserva.java:44-50`; SONDAS P3, P10, P12 | CEN-03 | BD-08, BD-03 | A |
| **R-07** | **Cancelar não libera** recurso nem agenda do professor (decisão de domínio resolvida implicitamente) | `ValidadorSobreposicao.java:30-43`; `ServicoGestaoReserva.java:35-42`; SONDAS P2, P15 | CEN-02, 07 | BD-01 | A |
| **R-08** | Reserva `CANCELADA` pode ter o horário alterado | `Reserva.java:135-142`; SONDA P9 | CEN-07 | BD-04 | M |
| **R-09** | Sem ponto único de mudança de estado: `String` + `setEstado` público + 6 escritores + `forcarCancelamento` sem auditoria | `Reserva.java:15, 93`; `ValidadorApagamento.java:36-39` | CEN-04, 07, 12 | BD-04 | A |
| **R-10** | Não há persistência; RNF-07 e as evidências de RN-09 não são possíveis; RF-16/17 têm testes bloqueados | ver 5.8 | CEN-08 | BD-04, BD-06 | A |
| **R-11** | Plataforma prometida ausente: sem Spring Boot 3.x (RNF-01), sem controllers/DTOs/repositórios; ADR-001 inverificável (pacote único) | `pom.xml`; `arquitetura.md:237-254` | CEN-08, 12, 13 | BD-07 | A |
| **R-12** | Critérios oficiais não impostos: CI verde com branches 61,8% < 70%; sem Sonar e sem JMeter | `ci.yml:32-44`; `pom.xml:88-107`; **[MED]** | CEN-09, 13 | BD-06 | A |
| **R-13** | Estruturas mutáveis sem sincronização fora da seção crítica (`ValidadorManutencao`, `Professor`, mapa de auditoria por instância) | `ValidadorManutencao.java:14, 19-24`; `Professor.java:13`; `ValidadorAuditoria.java:10` | CEN-01, 02, 05 | BD-01, BD-02 | M |
| **R-14** | **Decisões de domínio tomadas no código sem ADR nem aprovação**: estado inicial `SOLICITADA` para recurso comum e ocupação imediata; adjacência aceita; "Data no passado" (não consta de RN-01, `prd.md:123-129`; origem em `plano-tdd.md:96`); cancelamento não libera | `ServicoCriacaoReserva.java:61-67`; `Reserva.java:41-43`; SONDAS P2, P13 | CEN-07, 12 | BD-01, BD-03 | M |
| **R-15** | Relógio acoplado à regra RN-01 e testes com datas fixas já passadas; incidente anterior de 8 erros | `Reserva.java:41`; `ReservaSobreposicao_RN02_Test.java:33-38`; `plano-tdd.md:1146` | CEN-12, 09 | BD-06 | M |
| **R-16** | RF-20 não está ligado a nenhum fluxo; chamada síncrona sem timeout; falha sem log | `ServicoNotificacao.java:35-40` (sem chamadores em `src/main`) | CEN-11 | BD-03 | M |
| **R-17** | Cobertura funcional parcial: relatórios só com autorização, pesquisa sem filtros, sem camada de entrada (E10, E11, E12) | `ServicoRelatorios.java:8-9, 14-16`; `ConsultaDisponibilidade.java:10` | CEN-13 | BD-05, BD-08 | M |
| **R-18** | CON-01 sem finalidade e sem nenhuma implementação: dois cenários herdados de importância A sem evidência | `arquitetura.md:116, 209`; ver 5.11 | CEN-15, 16 | (restrição) | A |
| **R-19** | `RastreabilidadeValidator` devolve 100% fixo; se for adotado, gera falsa garantia de RNF-04 | `RastreabilidadeValidator.java:24-26` | CEN-14 | BD-06 | B |
| **R-20** | Regra de sobreposição replicada em 5 pontos; mudança de política (adjacência, Q-002) exige alterar todos | ver SP-09 | CEN-12 | BD-01 | M |

---

## 9. Não riscos

Decisões que, à luz das evidências, sustentam os cenários (com a condição que as limita).

| ID | Decisão | Evidência | Condição |
|---|---|---|---|
| NR-01 | **Domínio isolado de tecnologia** (sem framework) | Build completo com 163 testes em ~22 s no CI; nenhuma dependência de infraestrutura **[MED]** | Vale enquanto a persistência e o framework forem introduzidos por portas, sem contaminar o domínio |
| NR-02 | **RBAC por perfil na camada de serviço** em 7 pontos de entrada | `ValidadorAutorizacao.java:40-45`, chamadas listadas em 5.3 | Precisa cobrir também criação, histórico e escopo do Responsável (R-06) |
| NR-03 | **Predicado de sobreposição semiaberto** correto e consistente (`início < fim ∧ fim > início`) | `ValidadorSobreposicao.java:60-62` e réplicas (SP-09) | Adjacência é decisão de negócio pendente (Q-002); centralizar o predicado |
| NR-04 | **Máquina de estados em tabela** com transições e mensagens específicas | `ValidadorFluxoEstados.java:14-86` (L 43/43) | Tornar o tipo do estado forte e único ponto de mudança (R-09) |
| NR-05 | **Abstração `CanalNotificacao`** com implementação simulada e isolamento de falha do canal | `CanalNotificacao.java:8-11`; `ServicoNotificacao.java:35-40` | Adicionar timeout, log da causa e ligação ao fluxo (R-16) |
| NR-06 | **Tradução segura de erros** | `TradutorErros.java:14-41` | Ampliar testes de ramos (25/42) |
| NR-07 | **CI em pull request com proteção da `main` e prova de detecção de falha** | `ci.yml:3-7`; proteção **[MED]**; runs `36157319937`, `36157529745`, `36157791758`, `36157922656`, `36158506674` | Falta impor critérios de cobertura e análise estática (R-12) |
| NR-08 | **Rastreabilidade documento ↔ teste automatizada** | `ReservaRastreabilidade_RN10_Test.java:346, 370` | Não cobre requisito → código |
| NR-09 | **Testes bloqueados registram decisões pendentes de forma explícita** (`@Disabled("BLOQUEADO_POR_LACUNA…")`, 16 casos) | Apêndice C | Cada bloqueio deve virar decisão ou story |

---

## 10. Temas de risco × direcionadores

| Tema | Riscos | Direcionadores ameaçados | Cenários |
|---|---|---|---|
| **RT-1. Integridade sustentada por mecanismos de processo, não de persistência** | R-01, R-02, R-10, R-13 | BD-01, BD-02, BD-04 | CEN-01, 02, 08 |
| **RT-2. Regras críticas com várias rotas e sem ponto único de aplicação** (estado, aprovação, disponibilidade, sobreposição) | R-03, R-05, R-08, R-09, R-20 | BD-01, BD-03, BD-04 | CEN-05, 06, 07, 12 |
| **RT-3. Trilha de auditoria não confiável** | R-04, R-09, R-19 | BD-04, BD-06 | CEN-04, 14 |
| **RT-4. Segurança sem autenticação e sem autorização contextual completa** | R-06, R-03, R-05 | BD-08, BD-03 | CEN-03, 06 |
| **RT-5. Distância entre a plataforma e as evidências prometidas** (Spring, Testcontainers, Sonar, JMeter, gates) | R-10, R-11, R-12, R-17 | BD-06, BD-07, BD-05 | CEN-08, 09, 13 |
| **RT-6. Decisões de domínio pendentes ou tomadas implicitamente** | R-07, R-14, R-15 | BD-01, BD-03 | CEN-02, 07, 12 |
| **RT-7. Restrição CON-01 sem base** | R-18 | (restrição adicional) | CEN-15, 16 |

---

## 11. O que `arquitetura.md` promete × o que o código faz

Tipos: **Gap** (promessa não implementada), **Decisão implícita** (código decidiu o que o documento deixou pendente), **Doc defasado** (o documento não reflete o estado atual), **Fonte divergente** (código ou plano além do PRD).

| ID | Promessa (documento) | Realidade (evidência) | Tipo | Impacto |
|---|---|---|---|---|
| DIV-01 | Java 21 **e Spring Boot 3.x** (RNF-01; `arquitetura.md:92`) | Java 21 sim (`pom.xml:14-15`, `ci.yml:29`); **Spring ausente** | Gap | R-11 |
| DIV-02 | **Monólito modular** com módulos `identity`, `resources`, `availability`, `reservations`, `approvals`, `inventory`, `audit`, `notifications`, `reports`, `shared`; controllers, services, repositories, DTOs (`arquitetura.md:237-254`; ADR-001 `:1233-1245`) | **Pacote único** com 42 arquivos-fonte; sem controllers, repositórios ou DTOs; "inspeção de dependências modulares" (evidência do ADR-001) impossível | Gap | R-11, RT-2 |
| DIV-03 | Persistência relacional no C4 (`arquitetura.md:332-356`); "Tenta persistir com controle de concorrência" (`:432`) | Nenhuma persistência; tudo em memória | Gap | R-01, R-10 |
| DIV-04 | ADR-002 **não escolhe** mecanismo antes de experimento com banco (`arquitetura.md:1247-1259`) | O código **já implementa** exclusão mútua local (`ServicoCriacaoReserva.java:24, 50`) e um segundo mecanismo morto (`ValidadorConcorrencia`); nenhum ADR os registra | Decisão implícita | R-01, R-02 |
| DIV-05 | ADR-003: validar **perfil, operação e objeto** na camada de serviço (`arquitetura.md:1261-1273`); sequência "Valida perfil e propriedade" (`:426`) | Perfil e operação: ok em 7 pontos. Objeto: só em `ServicoGestaoReserva`. Criação sem perfil (P3); duas rotas de aprovação; histórico sem autorização; sem autenticação | Gap parcial | R-06 |
| DIV-06 | ADR-004: consistência **observável** entre estado e auditoria (`arquitetura.md:1275-1287`) | Sem atomicidade; ordem inversa na criação; leitura destrutiva; lista mutável; `forcarCancelamento` sem auditoria | Gap | R-04, R-09 |
| DIV-07 | ADR-005: revalidar **sala, material, professor, manutenção e bloqueio** na confirmação (`arquitetura.md:1289-1301`; `fluxos-personas.md:969, 2081`) | Revalida sala, material, professor e manutenção na **criação**; **não** revalida bloqueio (P1); na aprovação é opt-in e só manutenção (P8) | Gap | R-03, R-05 |
| DIV-08 | Estado inicial da reserva não restrita e ocupação por `SOLICITADA` são **pendentes** (`arquitetura.md:256-267`; `fluxos-personas.md:832, 869`) | Todo recurso comum nasce `SOLICITADA` com `approvalRequired=false` e ocupa imediatamente (`ServicoCriacaoReserva.java:61-67`; P13) | Decisão implícita | R-14 |
| DIV-09 | "Liberação de recursos" pendente (`arquitetura.md:256-267`) | Cancelar **não** libera recurso nem agenda (P2, P15) | Decisão implícita | R-07 |
| DIV-10 | Bloqueio × manutenção: diferença pendente (`arquitetura.md:203, 210`) | Bloqueio só afeta a pesquisa (`ConsultaDisponibilidade.java:33`); manutenção afeta criação | Decisão implícita | R-03 |
| DIV-11 | RN-01: "término posterior ao início" (`prd.md:123-129`) | Também rejeita "Data no passado" (`Reserva.java:41-43`), regra que vem de `plano-tdd.md:96`, não do PRD | Fonte divergente | R-14 |
| DIV-12 | Matriz §38: RNF-12 "EVIDÊNCIA NÃO EXECUTADA" (`arquitetura.md:1454`); "Evidências executadas: nenhuma" (`:1836`) | Existe workflow e 5 runs reais; `main` protegida | Doc defasado | Atualizar o documento |
| DIV-13 | Matriz §38: gates JaCoCo "≥ 80% linhas e ≥ 70% branches" (`arquitetura.md:1448`) | Sem gate; medido: 81,1% linhas e 61,8% branches; CI verde | Gap | R-12 |
| DIV-14 | RN-10/RNF-04 "COBERTA DOCUMENTALMENTE" (`arquitetura.md:1447`) | Coberta por teste de parsing (NR-08), mas existe `RastreabilidadeValidator` com 100% fixo | Parcial | R-19 |
| DIV-15 | RN-04/RNF-09: teste concorrente com banco em Testcontainers (`prd.md:147-153, 1150-1165`) | Teste concorrente existe (em memória) e outro sobre código morto; nenhum com banco | Gap | R-01, R-02 |
| DIV-16 | Interfaces confirmadas: histórico (RF-19), relatórios (RF-21), pesquisa com filtros (`arquitetura.md:283-295`) | Relatórios só autorizam; pesquisa sem filtros (`ConsultaDisponibilidade.java:10`); histórico sem controle de acesso | Gap | R-17 |
| DIV-17 | Componente Notificação e falha externa segura (`arquitetura.md:373`, ATAM-13) | Serviço com isolamento de falha, **sem chamadores** e sem timeout | Gap parcial | R-16 |
| DIV-18 | Batch CON-01 detalhado (`arquitetura.md:382-408, 442-471`, ADR-006 a ADR-014) | Sem qualquer artefato no repositório | Gap (declarado como candidato) | R-18 |
| DIV-19 | Professor como usuário **e** recurso, relação pendente (`arquitetura.md:78-80`) | Duas representações coexistem (`Recurso.java:8`; `Professor.java`); a lista `Reserva.professores` não bloqueia na criação | Decisão implícita | R-07, SP-12 |

---

## 12. Ações priorizadas para o próximo ciclo

Prioridade: **P1** bloqueia cenários de importância A com risco A; **P2** reduz risco A/M com custo baixo; **P3** prepara o ciclo 02. Responsáveis seguem as papéis do projeto (`.claude/rules/agent-authority.md`); cada ação deve virar *story* (Art. III da Constituição). Critérios de saída são verificáveis.

### P1 — Decisões e correções que destravam a integridade

| ID | Ação | Trata | Responsável | Critério de saída |
|---|---|---|---|---|
| A-01 | **Decidir e registrar (ADR) o mecanismo de RN-04 e a persistência**; implementar o mínimo para rodar RNF-07/09 com Testcontainers | R-01, R-02, R-10; CEN-01, 08 | @architect decide, @data-engineer projeta, @dev implementa | Teste concorrente contra banco containerizado aceita exatamente 1 reserva; ADR-002 vira "aceito" |
| A-02 | **Redirecionar o teste "RN-04" para o caminho real** (`ServicoCriacaoReserva`) ou remover `ValidadorConcorrencia`; cobrir sobreposição parcial | R-02 | @qa, @dev | Nenhum teste de RN-04 exercita código sem chamador; caso 08–10 × 09–11 recusado |
| A-03 | **Aplicar bloqueio na criação, alteração e aprovação** (mesma seção crítica) | R-03; CEN-05 | @dev | Sonda P1 vira teste e falha antes da correção; passa depois |
| A-04 | **Corrigir a trilha de auditoria**: armazenamento único, thread-safe, sem leitura destrutiva, lista imutável, id de reserva único, auditoria na mesma unidade de trabalho do estado | R-04, R-09; CEN-04 | @architect (ADR-004), @dev | Sondas P4, P5, P6 viram testes verdes; nenhum `setEstado` público fora da transição auditada |
| A-05 | **Unificar aprovação em uma rota**: validar transição, perfil, usuário ativo e revalidar disponibilidade (sala, material, professor, manutenção, bloqueio) | R-05; CEN-06 | @dev | Sondas P7, P8 viram testes verdes; `aprovarConcorrente` removido ou implementado |
| A-06 | **Completar autorização**: permissões para criar/alterar/cancelar reserva, checagem de dono robusta a `id` nulo, autorização na consulta de histórico | R-06; CEN-03 | @dev, @qa | Sondas P3, P10 viram testes verdes; teste negativo por operação e perfil |

### P1 — Decisões de negócio (bloqueiam código e testes)

| ID | Decisão | Bloqueia | Responsável | Critério de saída |
|---|---|---|---|---|
| D-01 | Estado inicial da reserva não restrita e se `SOLICITADA` ocupa | DIV-08; CEN-01, 12; `@Disabled` Q-003 | @po / equipe | Fontes (PRD e fluxos) atualizadas |
| D-02 | Cancelar libera recurso e agenda? Quais estados ocupam? | R-07; DIV-09 | @po / equipe | Regra escrita e coberta por teste |
| D-03 | Bloqueio × manutenção; política para reservas existentes | R-03; DIV-10 | @po / equipe | Regra escrita e testada |
| D-04 | Escopo de responsabilidade do Responsável (Q-004) | R-06; CEN-06 | @po / equipe | `aprovarComEscopo` deixa de ser opcional |
| D-05 | Adjacência (Q-002), duração mínima, regra "data no passado" | R-14, R-20; 6 testes `@Disabled` | @po / equipe | Predicado centralizado; testes reabilitados |
| D-06 | **Spring Boot 3.x**: adotar ou registrar desvio formal de RNF-01 | R-11; DIV-01, DIV-02 | @architect, @pm | ADR aceito; `pom.xml` coerente com a decisão |

### P2 — Baixo custo, alto retorno

| ID | Ação | Trata | Responsável | Critério de saída |
|---|---|---|---|---|
| A-07 | **Impor o gate de cobertura**: `jacoco:check` (linhas ≥ 80%, branches ≥ 70%) e publicar o relatório JaCoCo como artefato | R-12; CEN-09 | @devops, @qa | PR com cobertura abaixo do alvo falha; hoje faltam **37 branches** para 70% **[MED]** |
| A-08 | **Fechar o ramo de branches**: priorizar `ValidadorAprovacao` (B 21/54), `ValidadorAgendaProfessor` (18/30), `Professor` (9/20), `TradutorErros` (25/42) | R-12 | @qa | Branches ≥ 70% medidos no CI |
| A-09 | **Transformar as sondas P1–P15 em testes de regressão** (Apêndice B) | R-01 a R-09 | @qa | Cada sonda vira teste nomeado e rastreado à matriz |
| A-10 | **Ponto único de mudança de estado** e tipo forte para o estado (enum) | R-09; CEN-07, 12 | @dev | Sem `String` de estado e sem `setEstado` público |
| A-11 | Corrigir alteração em estado terminal e liberar agenda do professor ao cancelar (após D-02) | R-07, R-08 | @dev | Sondas P9, P15 viram testes |
| A-12 | Injetar `Clock` na regra RN-01 e remover datas fixas dos testes | R-15 | @dev | Nenhum teste depende da data do dia |
| A-13 | Tratar `Reserva.professores` na criação | DIV-19; CEN-02 | @dev | Docente da lista bloqueia como o principal |
| A-14 | Remover ou marcar como não utilizável `RastreabilidadeValidator` | R-19 | @dev | Sem retorno fixo `1.0` |

### P3 — Preparação do ciclo 02

| ID | Ação | Trata | Responsável | Critério de saída |
|---|---|---|---|---|
| A-15 | **Ratificar com a equipe** as notas A/M/B e as medidas [HIP] deste documento | Limitação 1.1 | @po, @architect | Utility tree do ciclo 02 aprovada |
| A-16 | **Atualizar `arquitetura.md`**: §38 (RNF-12), §31/§45 (evidências executadas), ADRs para decisões já tomadas no código, e este relatório como referência | DIV-04, DIV-12 | @architect | Nenhuma divergência DIV-04/DIV-12 aberta |
| A-17 | Definir metas de RNF-14 e adicionar SonarCloud (RNF-13) e plano JMeter | R-12; CEN-13 | @pm, @devops | Metas aprovadas; Sonar no CI |
| A-18 | Ligar notificação ao fluxo com timeout e log; escolher simulada ou WireMock (J15) | R-16; CEN-11 | @architect, @dev | Teste de canal indisponível com operação principal íntegra |
| A-19 | Decidir a finalidade de CON-01 **antes** de qualquer implementação; só então retomar CEN-15/16 | R-18 | @pm, @analyst | Finalidade aprovada ou CON-01 retirada do escopo |
| A-20 | Retomar ATAM-17 (movimentação) e ATAM-14 (vazamento) no ciclo 02 | Escopo 1.1 | @architect | Cenários incluídos e avaliados |

---

## Apêndice A — Como as medições foram obtidas

Nada foi executado dentro do repositório: `pom.xml`, `src/` e `docs/` foram **copiados** para o diretório temporário da sessão e o build rodou lá.

| Medição | Comando / origem | Resultado |
|---|---|---|
| Testes e cobertura | Na cópia: `mvn -B -o verify`, depois soma de `target/site/jacoco/jacoco.csv` | 163 testes, 0 falhas, 0 erros, 16 ignorados; linhas 671/827 (81,1%); branches 278/450 (61,8%) |
| Efeito de `docs/` | A 1ª execução sem `docs/` falhou em 11 testes de RN-10 (`NoSuchFile docs\prd.md`) | Testes de rastreabilidade dependem de `docs/` no diretório de trabalho |
| Testes no CI | `gh run view 36158506674 --log` | `Tests run: 163, Failures: 0, Errors: 0, Skipped: 16`; `BUILD SUCCESS`; 21,9 s; JaCoCo "Analyzed bundle … with 45 classes" (percentuais **não** aparecem no log) |
| Runs do CI | `gh run list --json …` | `36157319937` success (`e25b8c3`), `36157529745` **failure** (`1afeb12`, falha controlada), `36157791758` success (`0af6f81`), `36157922656` success (`e142d5a`), `36158506674` success em `main` (`f58f32c`) |
| Proteção da `main` | `gh api repos/…/branches/main/protection` | Check `Build and test`, `strict: true`, `enforce_admins: true`, sem force-push, sem exclusão, 0 aprovações exigidas |
| Ausências | `grep` por `spring`, `testcontainers`, `wiremock`, `jmeter`, `sonar` em `pom.xml`, `src/`, `.github/` | Sem ocorrências além de comentários em testes |
| Código sem chamador | `grep` por uso em `src/main` | `ValidadorConcorrencia`, `ValidadorAgendaProfessor`, `RastreabilidadeValidator`, `ServicoNotificacao` e seus canais não são usados por nenhuma outra classe de produção |

**Limite:** a cobertura foi medida localmente (mesmo commit e JDK 21.0.6); o CI executa o mesmo `jacoco:report`, mas não publica os percentuais.

## Apêndice B — Sondas executadas

Programas Java descartáveis, compilados contra as classes da cópia e **não commitados**. Cada sonda chama a API pública de produção e registra o resultado. Sondas de auditoria rodaram em JVMs separadas porque o armazenamento de auditoria é estático.

| ID | Procedimento | Resultado observado |
|---|---|---|
| P1 | Administrador bloqueia sala 08–10; Solicitante cria reserva 08–09 | **Criada** (`SOLICITADA`), embora `GestaoBloqueios.estaDisponivel` = `false` |
| P2 | Cria reserva, cancela, outra pessoa reserva o mesmo período | Estado `CANCELADA`; nova reserva **recusada** ("conflito em sala") |
| P3 | Usuário `ADMINISTRADOR` chama `criarReserva` | **Criada** |
| P4-A | Cria; lê auditorias (1); cancela; lê de novo | **1** (esperado 2); um validador novo vê **1** |
| P4-B | Cria; cancela; lê | 2; um segundo validador vê **0** |
| P5-C | `obterAuditorias(id).clear()` | Nova leitura devolve **0** |
| P6 | Duas instâncias de `ServicoCriacaoReserva`, uma reserva em cada | Ids **1 e 1**; auditoria da #1 contém usuários `u1` e `u2` |
| P7 | Rejeita e depois `aprovar()`; depois `aprovar()` sobre reserva `CANCELADA` | Ambas ficam **`APROVADA`** |
| P8 | Cria reserva restrita; registra manutenção; `aprovar()` | **`APROVADA`** |
| P9 | Cria, cancela e chama `alterarReserva` | Estado `CANCELADA`, horário **alterado** |
| P10 | Dois usuários com `id = null`; o "intruso" cancela a reserva do "dono" | **Cancelada** |
| P11 | Solicitante chama `ValidadorAprovacao.aprovar` | Recusado ("Apenas Responsável pode aprovar") |
| P12 | Responsável qualquer aprova recurso restrito | **`APROVADA`** |
| P13 | Reserva de recurso comum | `SOLICITADA`, `approvalRequired = false` |
| P14 | `ValidadorConcorrencia` com períodos 08–10 e 09–11 | **Ambos aceitos** |
| P15 | Reserva com professor, cancela, nova reserva com o mesmo professor em outra sala e mesmo horário | Agenda mantém 1 entrada; nova reserva **recusada** ("conflito professor") |

## Apêndice C — Os 16 testes ignorados como mapa de decisões pendentes

Todos com `@Disabled("BLOQUEADO_POR_LACUNA: …")`.

| Decisão pendente | Testes |
|---|---|
| Adjacência de períodos (Q-002) | `ReservaAgendaProfessor_RN03_Test.java:133`, `ReservaManutencao_RN05_Test.java:202`, `ReservaSobreposicao_RN02_Test.java:146`, `ReservaUnicidadeConcorrencia_RN04_Test.java:118` |
| Duração mínima da reserva (§4.3) | `ReservaCriacao_RF10_Test.java:255`, `ReservaTemporal_RN01_Test.java:66` |
| Critério de recurso restrito (Q-003) | `ReservaUnicidadeConcorrencia_RN04_Test.java:125` |
| Responsabilidade do Responsável por recurso (Q-004) | `ReservaAprovacao_RN06_Test.java:210` |
| Origem, ator e condição de `NAO_COMPARECEU` (Q-006) | `ReservaFluxoEstados_RN07_Test.java:179` |
| Política de timestamp e ordem da auditoria (§4.2) | `ReservaAuditoria_RN09_Test.java:177`, `ReservaUnicidadeConcorrencia_RN04_Test.java:150` |
| Auditoria de tentativa recusada | `ReservaAuditoria_RN09_Test.java:170` |
| Perfil prioritário com múltiplos perfis | `ReservaAutenticacaoAutorizacao_RF01_Test.java:136` |
| Agenda com formato inválido; recusa sem política definida | `ReservaAgendaProfessor_RN03_Test.java:152, 158` |
| Regra de processo sem estímulo verificável (matriz atualizada após mudança) | `ReservaRastreabilidade_RN10_Test.java:293` |
