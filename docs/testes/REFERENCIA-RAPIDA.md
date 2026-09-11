# Referência Rápida - Plano de TDD

> **Data:** 2026-09-11  
> **Status:** Preparação para TDD concluída  
> **Arquivo completo:** [`docs/testes/plano-tdd.md`](plano-tdd.md)

---

## 📋 Inventário Consolidado

### Regras de Negócio (RNs)

| ID | Título | Criticidade | Casos de Teste | Bloqueadores |
|---|---|---|---|---|
| **RN-01** | Ordem Temporal da Reserva | Crítica | 10 | Nenhum |
| **RN-02** | Não Sobreposição do Mesmo Recurso | Crítica | 10 | Nenhum (política de adjacência PENDENTE) |
| **RN-03** | Agenda do Professor | Crítica | 10 | Relação professor-usuário/recurso (Q-010) |
| **RN-04** | Unicidade Sob Concorrência | Crítica | 8 | Mecanismo técnico (Q-009) |
| **RN-05** | Indisponibilidade por Manutenção | Crítica | 10 | Política para reservas afetadas (Q-005) |
| **RN-06** | Aprovação de Recursos Restritos | Crítica | 10 | Critério de identificação (Q-003), responsabilidade (Q-004) |
| **RN-07** | Fluxo de Estados | Crítica | 10 | Estado inicial (Q-001), transições (Q-007, Q-008), NAO_COMPARECEU (Q-006) |
| **RN-08** | Reserva Iniciada Não Pode Ser Apagada | Crítica | 10 | Nenhum |
| **RN-09** | Auditoria de Mudança de Estado | Crítica | 10 | Campos de auditoria PENDENTE |
| **RN-10** | Rastreabilidade dos Requisitos Críticos | Crítica | 10 | Nenhum (validação documental) |

**Total de casos de teste planejados:** 98+

---

## 🔍 Guia de Localização

### Por Tipo de Consulta

#### "Quero ver casos de teste para RN-X"
→ Seção 2 do plano-tdd.md, subsseção "2.X.1 Casos de Teste para RN-X"

#### "Quero ver casos de teste para RF-X"
→ Seção 3 do plano-tdd.md, subsseção "3.X.1 Casos de Teste para RF-X"

#### "Preciso entender ambiguidades e lacunas"
→ Seção 4 do plano-tdd.md (todas as questões abertas Q-001 a Q-010)

#### "Qual é a origem desta regra?"
→ Cada RN/RF começa com campo "Origem:" mostrando arquivo, seção e fonte funcional

#### "Como testo concorrência (RN-04)?"
→ Seção 2.4: "RN-04: Unicidade Sob Concorrência", casos T-RN04-001 a T-RN04-008

#### "Quais casos de teste usam Testcontainers?"
→ Procure por "Camada(s): Integração" ou "Concorrência" nas tabelas de casos de teste

---

## 📊 Estatísticas

### Por Criticidade
- **Crítica:** 10 RNs (100% de RNs)
- **Alta:** Não aplicável a RNs (todos são críticos)
- **Média/Baixa:** Não aplicável a RNs

### Por Tipo de Teste
- **Happy Path (Caminho Feliz):** ~20% dos casos
- **Boundary (Valores-Limite):** ~20% dos casos
- **Invalid Input (Entradas Inválidas):** ~20% dos casos
- **Conflicts (Conflitos):** ~20% dos casos
- **Forbidden State (Estados Proibidos):** ~20% dos casos

### Por Camada de Teste
| Camada | Casos | Tecnologia |
|---|---|---|
| Unitário | ~35% | JUnit 5 |
| Integração | ~35% | Testcontainers |
| API | ~70% | HTTP Black-box |
| Concorrência | ~8 | Testcontainers + Threads |
| End-to-End | <5% | Fluxo completo |

---

## ⚠️ Bloqueadores Críticos

**Estas decisões DEVEM ser resolvidas antes de implementação:**

| ID | Questão | Impacto | Prioridade |
|---|---|---|---|
| **Q-001** | Estado inicial de reserva não restrita | RN-07, RF-10 | 🔴 CRÍTICA |
| **Q-003** | Critério de recurso restrito | RN-06, RF-14 | 🔴 CRÍTICA |
| **Q-004** | Responsabilidade de aprovação | RN-06 | 🟠 ALTA |
| **Q-006** | Fluxo de NAO_COMPARECEU | RN-07 | 🟠 ALTA |
| **Q-007** | Transição APROVADA → EM_USO | RN-07 | 🟠 ALTA |
| **Q-008** | Transição EM_USO → CONCLUIDA | RN-07 | 🟠 ALTA |
| **Q-009** | Mecanismo de concorrência (RN-04) | RN-04 | 🔴 CRÍTICA + Experimento |
| **Q-010** | Relação professor-usuário/recurso | RN-03 | 🟠 ALTA |

---

## 🔗 Rastreabilidade Completa

### RN → RF Mapeamento

| RN | RFs Relacionados |
|---|---|
| RN-01 | RF-10, RF-11 |
| RN-02 | RF-09, RF-10, RF-11, RF-13 |
| RN-03 | RF-03, RF-09, RF-10, RF-11, RF-13, RF-15 |
| RN-04 | RF-10, RF-13 |
| RN-05 | RF-02, RF-04, RF-06, RF-09, RF-10, RF-11 |
| RN-06 | RF-01, RF-10, RF-14, RF-15 |
| RN-07 | RF-10, RF-11, RF-12, RF-14, RF-18, RF-19, RF-20 |
| RN-08 | RF-12, RF-18, RF-19 |
| RN-09 | RF-19, RF-20, RF-21, + todos RFs de mudança de estado |
| RN-10 | Todos os RFs e RNs (regra de processo) |

### RF → RN Mapeamento

| RF | RNs Relacionados |
|---|---|
| RF-01 | RN-06 |
| RF-10 | RN-01, RN-02, RN-03, RN-04, RN-05, RN-06, RN-07, RN-09 |

---

## 📝 Campos de Auditoria (RN-09) - Pendentes

Estes campos DEVEM ser definidos antes de implementar auditoria:

- [ ] Precisão de timestamp (segundo, ms, ns?)
- [ ] Timezone (UTC obrigatório?)
- [ ] Ator (ID, email, nome?)
- [ ] Registro de IP/Sessão?
- [ ] Motivo/Descrição (obrigatório ou opcional?)
- [ ] HTTP Method?
- [ ] Política de imutabilidade
- [ ] Retenção de dados (duração, LGPD?)

---

## 🎯 Próximas Ações Recomendadas

### Fase 1: Validação de Decisões (IMEDIATO)
```
1. [ ] Resolver Q-001: Estado inicial de reserva
2. [ ] Resolver Q-003: Critério de recurso restrito
3. [ ] Resolver Q-009: Mecanismo de concorrência (+ experimento)
4. [ ] Resolver Q-004, Q-006, Q-007, Q-008, Q-010
```

### Fase 2: Iniciar TDD (Após Fase 1)
```
1. [ ] Implementar testes de RN-01 (ordem temporal)
2. [ ] Implementar testes de RN-04 (concorrência)
3. [ ] Implementar testes de RN-06 (aprovação)
4. [ ] Construir código de produção conforme testes
5. [ ] Validar cobertura de código (80% linhas, 70% branches)
```

### Fase 3: Expandir Cobertura
```
1. [ ] RN-02, RN-03 (sobreposição)
2. [ ] RN-05 (manutenção)
3. [ ] RN-07, RN-08, RN-09 (fluxo de estados e auditoria)
```

---

## 🔗 Referências Cruzadas

### Arquivos Relacionados
- **PRD:** [`docs/prd.md`](../../prd.md) - Requisitos e regras de negócio oficiais
- **Arquitetura:** [`docs/arquitetura.md`](../../arquitetura.md) - Decisões arquiteturais
- **Personas:** [`docs/personas/`](../../personas/) - Solicitante, Responsável, Administrador
- **Fluxos:** [`docs/fluxos-personas.md`](../../fluxos-personas.md) - Sequências e integrações
- **Validação:** [`docs/relatorio-validacao-arquitetura.md`](../../relatorio-validacao-arquitetura.md)
- **Correções:** [`docs/relatorio-correcao-auditoria.md`](../../relatorio-correcao-auditoria.md)

### Documentação de Teste
- **Plano Completo:** [`docs/testes/plano-tdd.md`](plano-tdd.md) ← VOCÊ ESTÁ AQUI
- **Referência Rápida:** [`docs/testes/REFERENCIA-RAPIDA.md`](REFERENCIA-RAPIDA.md)

---

## 🎓 Como Usar Este Documento

### Para QA/Testador
1. Abra [`plano-tdd.md`](plano-tdd.md)
2. Localize a RN/RF que deseja testar
3. Consulte "Casos de Teste para RN-X" (seção 2.X.1 ou 3.X.1)
4. Use a tabela de casos como checklist de testes

### Para Desenvolvedor
1. Abra [`plano-tdd.md`](plano-tdd.md), seção 2 (RN) ou 3 (RF)
2. Leia "Critério de Aceitação Oficial" para entender o comportamento esperado
3. Implemente testes unitários seguindo casos de teste
4. Use "Resultado Esperado" como assertion

### Para Product Manager/Stakeholder
1. Abra esta referência rápida (REFERENCIA-RAPIDA.md)
2. Consulte "Bloqueadores Críticos" (seção de ⚠️) para decisões necessárias
3. Aprove/discuta respostas para Q-001 a Q-010
4. Valide rastreabilidade (seção "🔗 Rastreabilidade Completa")

### Para Arquiteto
1. Abra [`plano-tdd.md`](plano-tdd.md)
2. Revise "Decisões pendentes de domínio" (seção 4.1)
3. Valide interseções entre regras (seção 4.6)
4. Identifique gaps que impactam design (seções 4.2 a 4.7)

---

## 📞 Suporte

**Dúvida sobre um caso de teste?**
→ Consulte a tabela T-RN0X ou T-RF0X correspondente, coluna "Resultado Esperado"

**Precisa da origem de uma regra?**
→ Cada RN/RF começa com campo "Origem:" e "Arquivo de origem:"

**Quer entender bloqueadores?**
→ Vá para seção 4.1 "Decisões Pendentes de Domínio"

**Qual teste executa contra banco?**
→ Procure por "Integração" na coluna "Camada(s)" ou busque "Testcontainers"

---

**Documento atualizado:** 2026-09-11  
**Versão:** 1.0  
**Status:** Pronto para aprovação de decisões (Q-001 a Q-010)
