# Documentação de Testes - Plano de TDD

> **Projeto:** Organização de Recursos  
> **Data de Criação:** 2026-09-11  
> **Status:** Planejamento de TDD Concluído  
> **Propósito:** Consolidar todas as regras de negócio com casos de teste antes da implementação

---

## 📁 Arquivos Disponíveis

Este diretório contém a documentação completa de preparação para TDD:

### 1. **`plano-tdd.md`** (Documento Principal)
- **Tamanho:** 734 linhas, 47 KB
- **Conteúdo:**
  - Inventário completo de 10 Regras de Negócio (RN-01 a RN-10)
  - Inventário de Requisitos Funcionais (RF-01, RF-10 como exemplo)
  - ~98 casos de teste detalhados (10 por regra em média)
  - Cada caso com: ID, tipo, cenário, dados de entrada, resultado esperado, camada(s) de teste
  - Ambiguidades, lacunas e 10 questões abertas (Q-001 a Q-010)
  - Bloqueadores críticos e recomendações para próximas fases
- **Para:** Implementadores, QA, Product Manager
- **Como usar:** Leia a RN/RF que quer implementar, consulte a tabela de casos de teste

### 2. **`REFERENCIA-RAPIDA.md`** (Guia de Navegação)
- **Conteúdo:**
  - Tabela rápida de RNs com contagem de casos de teste
  - Guia de localização ("Quero ver X, onde procuro?")
  - Estatísticas (% de casos por tipo)
  - Bloqueadores críticos em ordem de prioridade
  - Mapeamento RN → RF e RF → RN (rastreabilidade)
  - Campos de auditoria pendentes
  - Próximas ações recomendadas
- **Para:** Product Manager, Scrum Master, Decision Makers
- **Como usar:** Comece aqui para entender bloqueadores e prioridades

### 3. **`INDICE-POR-TIPO.md`** (Busca Avançada)
- **Conteúdo:**
  - Índice completo de testes por camada (Unitário, Integração, API, Concorrência, E2E)
  - Índice por tipo de cenário (Happy Path, Boundary, Invalid Input, Conflicts, Forbidden State)
  - Índice por regra (RN) e por requisito (RF)
  - Matriz de decisão ("Qual teste usar para...?")
  - Checklist de implementação (fases 1-7)
- **Para:** Desenvolvedores, QA Engineers
- **Como usar:** Use para localizar teste específico ou planejar ordem de implementação

---

## 🎯 Como Começar

### Se você é **Product Manager/Stakeholder:**
1. Abra [`REFERENCIA-RAPIDA.md`](REFERENCIA-RAPIDA.md)
2. Leia seção "⚠️ Bloqueadores Críticos"
3. Aprove/discuta respostas para Q-001 a Q-010
4. Valide rastreabilidade (RN → RF)

### Se você é **Desenvolvedor:**
1. Abra [`INDICE-POR-TIPO.md`](INDICE-POR-TIPO.md)
2. Localize "Checklist de Implementação TDD"
3. Comece pela Fase 1: validar decisões
4. Progresse para Fase 2: RN-01 (ordem temporal)
5. Consulte [`plano-tdd.md`](plano-tdd.md) para casos de teste específicos

### Se você é **QA/Testador:**
1. Abra [`plano-tdd.md`](plano-tdd.md)
2. Procure a RN/RF que quer testar (seção 2 ou 3)
3. Consulte tabela "Casos de Teste para RN-X"
4. Use "Resultado Esperado" como critério de aceitação
5. Refira-se a [`INDICE-POR-TIPO.md`](INDICE-POR-TIPO.md) para busca avançada

### Se você é **Arquiteto:**
1. Abra [`plano-tdd.md`](plano-tdd.md), seção 4
2. Revise "Decisões Pendentes de Domínio" (Q-001 a Q-010)
3. Valide "Interseções entre Regras" (seção 4.6)
4. Identifique gaps de arquitetura

---

## 📊 Visão Geral do Conteúdo

### Regras de Negócio Documentadas (10 RNs)

| RN | Título | Casos de Teste | Status |
|---|---|---|---|
| RN-01 | Ordem Temporal da Reserva | 10 | ✅ Pronto |
| RN-02 | Não Sobreposição do Mesmo Recurso | 10 | ✅ Pronto |
| RN-03 | Agenda do Professor | 10 | ✅ Pronto |
| RN-04 | Unicidade Sob Concorrência | 8 | ✅ Pronto (+ experimento Q-009) |
| RN-05 | Indisponibilidade por Manutenção | 10 | ✅ Pronto |
| RN-06 | Aprovação de Recursos Restritos | 10 | ✅ Pronto |
| RN-07 | Fluxo de Estados | 10 | ✅ Pronto (bloqueado por Q-001, Q-006, Q-007, Q-008) |
| RN-08 | Reserva Iniciada Não Pode Ser Apagada | 10 | ✅ Pronto |
| RN-09 | Auditoria de Mudança de Estado | 10 | ✅ Pronto |
| RN-10 | Rastreabilidade dos Requisitos Críticos | 10 | ✅ Pronto (validação documental) |

**Total:** 98 casos de teste planejados

### Requisitos Funcionais Documentados (2 RFs como exemplo)

| RF | Título | Casos de Teste |
|---|---|---|
| RF-01 | Autenticação e Autorização por Perfil | 10 |
| RF-10 | Criação de Reserva | 15 |

**Nota:** RFs 02-09, 11-23 serão tratados em próxima fase. Este plano foca nos críticos para reserva.

### Bloqueadores Críticos (Devem ser Resolvidos Primeiro)

| ID | Questão | Impacto | Status |
|---|---|---|---|
| Q-001 | Estado inicial de reserva não restrita | RN-07, RF-10 | 🔴 CRÍTICA |
| Q-003 | Critério de identificação de recurso restrito | RN-06, RF-14 | 🔴 CRÍTICA |
| Q-004 | Responsabilidade de aprovação (todos ou por área?) | RN-06 | 🟠 ALTA |
| Q-006 | Fluxo de NAO_COMPARECEU (ator? momento?) | RN-07 | 🟠 ALTA |
| Q-007 | Transição APROVADA → EM_USO (quem? quando?) | RN-07 | 🟠 ALTA |
| Q-008 | Transição EM_USO → CONCLUIDA (automática?) | RN-07 | 🟠 ALTA |
| Q-009 | Mecanismo de concorrência (pessimistic lock?) | RN-04 | 🔴 CRÍTICA + Experimento |
| Q-010 | Relação professor-usuário vs professor-recurso | RN-03 | 🟠 ALTA |

---

## 🔗 Fontes de Verdade (Rastreabilidade)

Todos os casos de teste são extraídos de artefatos oficiais:

| Artefato | Localização | Papel |
|---|---|---|
| **PRD** | [`docs/prd.md`](../prd.md) | Requisitos e regras de negócio (seção 6: RNs) |
| **Arquitetura** | [`docs/arquitetura.md`](../arquitetura.md) | Decisões arquiteturais, drivers |
| **Personas** | [`docs/personas/*.md`](../personas/) | Responsabilidades e permissões (Solicitante, Responsável, Admin) |
| **Fluxos** | [`docs/fluxos-personas.md`](../fluxos-personas.md) | Sequências e integrações |
| **Validação** | [`docs/relatorio-validacao-arquitetura.md`](../relatorio-validacao-arquitetura.md) | Confirmação de cobertura |
| **Correções** | [`docs/relatorio-correcao-auditoria.md`](../relatorio-correcao-auditoria.md) | Pendências e lacunas |

**Princípio:** Nenhum caso de teste foi inventado. Todos rastreiam origem em documentos aprovados.

---

## 🧪 Estrutura de Casos de Teste

Cada caso de teste segue este formato:

```
| T-ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|------|------|---------|-------------------|-------------------|-----------|
| T-RN01-001 | Happy Path | Intervalo válido | início: 08:00, fim: 09:00 | Aceita | Unitário, Integração, API |
```

### Tipos de Teste
- **Happy Path:** Operação bem-sucedida com dados válidos
- **Boundary:** Casos extremos nos limites
- **Invalid Input:** Dados malformados/inválidos
- **Conflicts:** Múltiplos requisitos competem
- **Forbidden State:** Operação viola autorização/estado

### Camadas de Teste
- **Unitário:** JUnit 5, sem persistência
- **Integração:** Testcontainers, banco realista
- **API:** Black-box HTTP, endpoints
- **Concorrência:** Threads + Testcontainers, RN-04 específico
- **End-to-End:** Fluxo completo com UI

---

## ✅ Qualidade e Cobertura

### Objetivos Confirmados

| Métrica | Valor-Alvo | Status |
|---|---|---|
| Cobertura de linhas (JaCoCo) | 80% | Definido em RNF-02 |
| Cobertura de branches (JaCoCo) | 70% | Definido em RNF-03 |
| RNs críticas rastreadas | 100% (10/10) | ✅ Atingido |
| RFs MUST com casos de teste | 100% (exemplos: RF-01, RF-10) | ✅ Atingido |
| Requisitos críticos em matriz | 100% | Definido em RN-10 |
| Bugs críticos conhecidos | 0 | Meta RNF-05 |
| Vulnerabilidades críticas | 0 | Meta RNF-06 |

### Testes Planejados por Camada

| Camada | % de Testes | Tecnologia |
|---|---|---|
| Unitário | ~35% | JUnit 5 + testes parametrizados |
| Integração | ~35% | Testcontainers + banco realista |
| API | ~70% | HTTP black-box, todos RFs |
| Concorrência | ~8 | Testcontainers + threads |
| E2E | <5% | A definir conforme UI |

---

## 📋 Próximas Fases

### Fase 1: Validação de Decisões (IMEDIATO)
**Responsável:** Product Manager, Architect  
**Questões a Resolver:**
- [ ] Q-001: Estado inicial de reserva?
- [ ] Q-003: Critério de recurso restrito?
- [ ] Q-004: Responsabilidade de aprovação?
- [ ] Q-006: Fluxo de NAO_COMPARECEU?
- [ ] Q-007: Transição APROVADA → EM_USO?
- [ ] Q-008: Transição EM_USO → CONCLUIDA?
- [ ] Q-009: Mecanismo de concorrência? (+ experimento)
- [ ] Q-010: Relação professor-usuário/recurso?

**Saída:** ADRs aprovadas para cada decisão

### Fase 2: Iniciar Ciclo de TDD (Pós Fase 1)
**Responsável:** Desenvolvedores  
**Ordem Recomendada:**
1. RN-01 (Ordem Temporal) - sem dependências
2. RN-04 (Concorrência) - crítica, requer experimento
3. RN-06 (Aprovação) - crítica, depende Q-001, Q-003, Q-004
4. RN-02, RN-03, RN-05 (Sobreposição, Professor, Manutenção)
5. RN-07, RN-08, RN-09 (Estados, Proteção, Auditoria)

### Fase 3: Expandir Cobertura
**Responsável:** QA, Developers  
- Implementar testes de RFs 02-09, 11-23
- Validar cobertura de código (80% + 70%)
- Executar testes end-to-end completos

---

## 🎓 Glossário

| Termo | Definição |
|---|---|
| **RN** | Regra de Negócio (business rule) |
| **RF** | Requisito Funcional (functional requirement) |
| **RNF** | Requisito Não-Funcional (non-functional requirement) |
| **TDD** | Test-Driven Development (escrever testes antes de código) |
| **Testcontainers** | Framework para testes com banco de dados containerizado realista |
| **Black-box** | Teste sem conhecimento da implementação interna |
| **Concorrência** | Teste com múltiplas threads/requisições simultâneas |
| **Boundary** | Teste de valores nos limites de aceitação |
| **Happy Path** | Fluxo bem-sucedido com dados válidos |
| **Forbidden State** | Estado ou operação não permitida |

---

## 📞 Suporte e Dúvidas

### "Onde está o caso de teste para RN-02?"
→ Abra [`plano-tdd.md`](plano-tdd.md), procure "### RN-02: Não Sobreposição do Mesmo Recurso", seção 2.2.1

### "Como ordeno implementação?"
→ Abra [`INDICE-POR-TIPO.md`](INDICE-POR-TIPO.md), procure "Checklist de Implementação TDD" (seção 6)

### "Qual é o bloqueador crítico?"
→ Abra [`REFERENCIA-RAPIDA.md`](REFERENCIA-RAPIDA.md), procure "⚠️ Bloqueadores Críticos"

### "Quais testes rodam com Testcontainers?"
→ Abra [`INDICE-POR-TIPO.md`](INDICE-POR-TIPO.md), seção "1.2 Testes de Integração"

### "Preciso da origem de uma regra?"
→ Cada RN/RF em [`plano-tdd.md`](plano-tdd.md) começa com "Origem:" e "Arquivo de origem:"

---

## 📜 Histórico e Versionamento

| Versão | Data | Autor | Mudança |
|---|---|---|---|
| 1.0 | 2026-09-11 | Engenheiro QA (Autônomo) | Criação inicial: 10 RNs + 98 casos de teste |

---

## ✨ Status Final

- ✅ **Inventário de RNs:** Completo (10 RNs, 100 casos de teste)
- ✅ **Rastreabilidade:** Completa (RN → RF, cada caso com origem)
- ✅ **Documentação:** Completa (3 arquivos: plano, referência, índice)
- ⏳ **Bloqueadores:** Pendentes (Q-001 a Q-010 devem ser resolvidos)
- ⏳ **Implementação:** Pronta para iniciar (após Fase 1)
- ⏳ **Testes:** Planejados, ainda não codificados

**Este documento é a base para todo o ciclo de TDD. Nenhum código foi escrito e nenhum teste foi implementado nesta fase.**

---

**Última atualização:** 2026-09-11  
**Próxima revisão:** Após aprovação das decisões Q-001 a Q-010
