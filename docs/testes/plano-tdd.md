# Plano de TDD - Organização de Recursos

> **Documento:** Plano de Test-Driven Development (TDD)  
> **Projeto:** Organização de Recursos  
> **Data:** 2026-09-11  
> **Status:** Novo ciclo RED dos casos T-RFnn em 2026-09-25 (ver seção 9). Fase GREEN anterior: seção 8. Fase RED: seção 7. Estado anterior (2026-09-11): Preparação para ciclo de TDD  
> **Propósito:** Inventário consolidado de regras de negócio com casos de teste e rastreabilidade  
> **Observação (estado anterior, 2026-09-11):** Neste estágio, nenhum teste foi implementado. Este documento descreve o plano de testes antes da codificação.  
> **Observação (estado atual, 2026-09-25):** existe um teste JUnit 5 para cada um dos 123 casos deste plano; a matriz regra → caso → teste → status está na seção 7.

---

## 1. Escopo e Metodologia

### 1.1 Objetivo

Consolidar todas as regras de negócio (RNs), requisitos funcionais (RFs) e não-funcionais (RNFs) extraídas das fontes oficiais com:
- Identificador único de origem
- Casos de teste detalhados (caminho feliz, valores-limite, entradas inválidas, conflitos, estados proibidos)
- Resultado esperado para cada caso
- Rastreabilidade completa para auditoria

### 1.2 Fontes de Verdade

| Artefato | Localização | Papel |
|---|---|---|
| **PRD** | `docs/prd.md` | Requisitos, regras de negócio, métricas |
| **Arquitetura** | `docs/arquitetura.md` | Decisões, drivers, atributos de qualidade |
| **Personas** | `docs/personas/*.md` | Responsabilidades, permissões, restrições |
| **Fluxos** | `docs/fluxos-personas.md` | Sequências e integrações |
| **Validação Arquitetural** | `docs/relatorio-validacao-arquitetura.md` | Confirmação de cobertura |
| **Correção Auditória** | `docs/relatorio-correcao-auditoria.md` | Pendências e lacunas |

### 1.3 Categorias de Teste

Os casos de teste são organizados por tipo:

| Tipo | Descrição | Exemplos |
|---|---|---|
| **Caminho Feliz (Happy Path)** | Fluxo normal onde operação é bem-sucedida com dados válidos | Reserva criada sem conflito, aprovação processada |
| **Valores-Limite (Boundary)** | Casos extremos que testam limites (início=término, intervalo mínimo/máximo) | Reserva com duração zero, horários iguais |
| **Entradas Inválidas (Invalid Input)** | Dados que violam regras de formato ou tipo | Data passada, horário negativo, recurso inexistente |
| **Conflitos (Conflicts)** | Situações onde múltiplas reservas competem ou violam regras | Sobreposição de sala, dupla reserva simultânea |
| **Estados Proibidos (Forbidden States)** | Transições ou operações não permitidas | Cancelar reserva iniciada, apagar com auditoria |

### 1.4 Camadas de Teste

Cada regra será validada em múltiplas camadas:

| Camada | Tipo | Tecnologia | Escopo |
|---|---|---|---|
| **Unitário** | JUnit 5 + testes parametrizados | Função isolada, validação de domínio | Sem persistência |
| **Integração** | Testcontainers + banco realista | Persistência, transações, constraints DB | Com DB containerizado |
| **API** | Black-box HTTP + endpoints | Autorização, autorização por perfil | Sem UI |
| **Concorrência** | Testcontainers + threads | Dupla reserva simultânea, race conditions | Múltiplas solicitações |
| **End-to-End** | Fluxo completo (frontend/backend) | Jornada completa da persona | UI responsiva |

---

## 2. Inventário de Regras de Negócio

### RN-01: Ordem Temporal da Reserva

**ID completo:** `RN-01 | docs/prd.md:6.1`

**Descrição:** O término da reserva deve ser posterior ao início.

**Origem:** 
- Arquivo: `docs/prd.md`
- Seção: 6. Regras de Negócio (seção 6.1)
- Origem funcional: Regras críticas de tempo e concorrência

**Requisitos Relacionados:**
- RF-10: Criação de reserva
- RF-11: Alteração de reserva
- RF-12: Cancelamento de reserva (não-aplicável a cancelamento)

**Criticidade:** Crítica

**Critério de Aceitação Oficial:**
```gherkin
Dado que uma reserva é solicitada
Quando o término é igual ou anterior ao início
Então a operação é recusada com erro claro
```

#### 2.1.1 Casos de Teste para RN-01

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RN01-001** | Happy Path | Reserva com intervalo válido | início: 08:00, fim: 09:00 | Reserva aceita, estado SOLICITADA | Unitário, Integração, API |
| **T-RN01-002** | Happy Path | Intervalo longo | início: 08:00, fim: 18:00 | Reserva aceita | Unitário, Integração |
| **T-RN01-003** | Boundary | Intervalo mínimo (1 minuto) | início: 08:00, fim: 08:01 | Reserva aceita ou recusada conforme política (PENDENTE) | Unitário |
| **T-RN01-004** | Boundary | Intervalo zero (início = fim) | início: 08:00, fim: 08:00 | Recusada: "Duração inválida" | Unitário, API |
| **T-RN01-005** | Invalid Input | Fim anterior ao início (regressão temporal) | início: 09:00, fim: 08:00 | Recusada: "Fim anterior ao início" | Unitário, API |
| **T-RN01-006** | Invalid Input | Data passada | início: 2026-01-01 08:00 (hoje é 2026-09-11) | Recusada: "Data no passado" | Unitário |
| **T-RN01-007** | Invalid Input | Início nulo | início: null, fim: 09:00 | Recusada: "Início é obrigatório" | Unitário, API |
| **T-RN01-008** | Invalid Input | Fim nulo | início: 08:00, fim: null | Recusada: "Fim é obrigatório" | Unitário, API |
| **T-RN01-009** | Invalid Input | Formato de hora inválido | início: "08h30", fim: "09:30" | Recusada: "Formato de hora inválido" | Unitário |
| **T-RN01-010** | Forbidden State | Alteração de reserva já iniciada | início: 2026-09-11 07:00, fim: 2026-09-11 08:00 (começou) | Recusada: "Reserva já iniciada não pode ser alterada" | API, Integração |

---

### RN-02: Não Sobreposição do Mesmo Recurso

**ID completo:** `RN-02 | docs/prd.md:6.2`

**Descrição:** Reservas do mesmo recurso não podem se sobrepor.

**Origem:**
- Arquivo: `docs/prd.md`
- Seção: 6. Regras de Negócio (seção 6.2)
- Origem funcional: Regras críticas de tempo e concorrência

**Requisitos Relacionados:**
- RF-09: Pesquisa e disponibilidade
- RF-10: Criação de reserva
- RF-11: Alteração de reserva
- RF-13: Detecção de sobreposição e dupla reserva

**Criticidade:** Crítica

**Critério de Aceitação Oficial:**
```gherkin
Dado que existe uma reserva confirmada na sala A de 08:00 a 09:00
Quando nova solicitação é enviada para a mesma sala no mesmo período
Então a nova solicitação é recusada com indicação de conflito
```

#### 2.2.1 Casos de Teste para RN-02

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RN02-001** | Happy Path | Reserva em sala sem conflito | Sala A: 08:00-09:00 (nova: 10:00-11:00) | Reserva aceita | Integração, API |
| **T-RN02-002** | Happy Path | Diferentes recursos no mesmo horário | Sala A: 08:00-09:00, Sala B: 08:00-09:00 | Ambas aceitas | Integração, API |
| **T-RN02-003** | Conflicts | Sobreposição total (mesma hora) | Existente: 08:00-09:00, Nova: 08:00-09:00 | Recusada: "Recurso indisponível no período" | API, Integração |
| **T-RN02-004** | Conflicts | Sobreposição parcial (nova começa dentro) | Existente: 08:00-09:00, Nova: 08:30-09:30 | Recusada: "Conflito de horário" | API, Integração |
| **T-RN02-005** | Conflicts | Sobreposição parcial (nova termina dentro) | Existente: 08:00-09:00, Nova: 07:30-08:30 | Recusada: "Conflito de horário" | API, Integração |
| **T-RN02-006** | Conflicts | Sobreposição: nova envolve existente | Existente: 08:00-09:00, Nova: 07:00-10:00 | Recusada: "Conflito de horário" | API, Integração |
| **T-RN02-007** | Boundary | Reservas adjacentes (fim = início) | Existente: 08:00-09:00, Nova: 09:00-10:00 | Aceita (sem sobreposição) OU Recusada conforme política (PENDENTE) | Integração |
| **T-RN02-008** | Boundary | Limite exato de coincidência | Existente: 08:00-09:00, Nova: 09:00-09:00 (erro de entrada) | Recusada por RN-01 antes de verificar sobreposição | API |
| **T-RN02-009** | Conflicts | Múltiplas existentes, conflita com uma | Existentes: 08:00-09:00, 10:00-11:00; Nova: 08:30-08:45 | Recusada: "Conflita com reserva 08:00-09:00" | Integração |
| **T-RN02-010** | Forbidden State | Alterar existente para criar sobreposição | Existentes: A 08:00-09:00, B 08:00-09:00; Alterar A para 08:30-09:00 | Recusada: "Alteração causaria sobreposição" | Integração, API |

---

### RN-03: Agenda do Professor

**ID completo:** `RN-03 | docs/prd.md:6.3`

**Descrição:** A regra de sobreposição também se aplica à agenda do professor alocado.

**Origem:**
- Arquivo: `docs/prd.md`
- Seção: 6. Regras de Negócio (seção 6.3)
- Origem funcional: Regras críticas de tempo e concorrência + Personas

**Requisitos Relacionados:**
- RF-03: Gestão de professores
- RF-09: Pesquisa e disponibilidade
- RF-10: Criação de reserva
- RF-11: Alteração de reserva
- RF-13: Detecção de sobreposição
- RF-15: Validação da alocação de docentes

**Criticidade:** Crítica

**Critério de Aceitação Oficial:**
```gherkin
Dado que Professor X possui agenda: segunda 08:00-09:00 aula de Cálculo
Quando solicito reserva com Professor X na segunda 08:00-09:00
Então a reserva é recusada por conflito com agenda do professor
```

**Nota arquitetural:** Professor pode ser usuário (pessoa) E recurso (agenda de alocação). Relação ainda pendente de decisão em `docs/arquitetura.md`, seção 3.1.

#### 2.3.1 Casos de Teste para RN-03

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RN03-001** | Happy Path | Professor alocado, agenda sem conflito | Prof X agenda: Seg 08:00-09:00; Nova: Seg 10:00-11:00 | Reserva aceita | Integração, API |
| **T-RN03-002** | Happy Path | Professor sem agenda registrada | Prof Y sem agenda; Nova: Qualquer hora | Reserva aceita | Integração, API |
| **T-RN03-003** | Conflicts | Sobreposição com agenda do professor | Prof X agenda: Seg 08:00-09:00; Nova: Seg 08:30-09:00 com Prof X | Recusada: "Professor indisponível" | API, Integração |
| **T-RN03-004** | Conflicts | Múltiplos professores, um com conflito | Prof A,B,C requisitados; B tem conflito | Recusada: "Professor B indisponível" | Integração, API |
| **T-RN03-005** | Conflicts | Alterar reserva criando conflito de agenda | Reserva existente: Prof X Ter 10:00-11:00; Alterar para Seg 08:00 (conflit com Prof X agenda) | Recusada: "Alteração causaria conflito com agenda" | API, Integração |
| **T-RN03-006** | Boundary | Professor com agenda até o minuto exato de início | Prof X agenda: Seg 08:00-09:00; Nova: Seg 09:00-10:00 com Prof X | Aceita OU Recusada conforme política de adjacência (PENDENTE) | Integração |
| **T-RN03-007** | Invalid Input | Professor inexistente | Prof ID: 99999 (não existe) | Recusada: "Professor não encontrado" | API |
| **T-RN03-008** | Invalid Input | Agenda com formato inválido | Prof agenda: null, vazia ou formato inválido | Sistema trata como "sem agenda" OU recusa | Unitário |
| **T-RN03-009** | Forbidden State | Tentar sobrepor agenda de professor já alocado | Reserva confirmada com Prof X; tentar alterar sem remover Prof X | Recusada conforme política | Integração |
| **T-RN03-010** | Conflicts | Professor simultaneamente em duas reservas (data diferente, mesmo horário) | Prof X: Seg 08:00-09:00 (sala A), Ter 08:00-09:00 (sala B) | Ambas aceitas (dias diferentes) | Integração |

---

### RN-04: Unicidade Sob Concorrência

**ID completo:** `RN-04 | docs/prd.md:6.4`

**Descrição:** Duas solicitações simultâneas para o mesmo recurso e período devem produzir somente uma reserva aceita.

**Origem:**
- Arquivo: `docs/prd.md`
- Seção: 6. Regras de Negócio (seção 6.4)
- Origem funcional: Regras críticas de tempo e concorrência

**Requisitos Relacionados:**
- RF-10: Criação de reserva
- RF-13: Detecção de sobreposição e dupla reserva

**Criticidade:** Crítica

**Mecanismo técnico:** PENDENTE DE DECISÃO (conforme `docs/relatorio-validacao-arquitetura.md`, seção 4.2)

**Evidência esperada:** Teste automatizado de concorrência contra banco em Testcontainers.

#### 2.4.1 Casos de Teste para RN-04

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RN04-001** | Conflicts | Dupla simultânea: ambas no mesmo recurso e período | Request 1 (Sala A, 08:00-09:00) + Request 2 (Sala A, 08:00-09:00) simultâneas | Exatamente 1 aceita (status SOLICITADA), 1 recusada (conflito) | Concorrência |
| **T-RN04-002** | Conflicts | Tripla simultânea: três solicitações | Request 1,2,3 (Sala A, 08:00-09:00) simultâneas | Exatamente 1 aceita, 2 recusadas | Concorrência |
| **T-RN04-003** | Happy Path | Sequencial (não simultâneo) | Request 1 aceita, depois Request 2 | Request 1 aceita, Request 2 recusada por sobreposição (RN-02) | Integração, API |
| **T-RN04-004** | Conflicts | Dupla em recursos diferentes | Request 1 (Sala A, 08:00-09:00) + Request 2 (Sala B, 08:00-09:00) simultâneas | Ambas aceitas (recursos diferentes) | Concorrência |
| **T-RN04-005** | Conflicts | Dupla em períodos adjacentes | Request 1 (Sala A, 08:00-09:00) + Request 2 (Sala A, 09:00-10:00) simultâneas | Ambas aceitas OU conforme política de adjacência (PENDENTE) | Concorrência |
| **T-RN04-006** | Conflicts | Dupla: um com recurso restrito, um sem | Request 1 (Sala A + Prof restrito, 08:00-09:00) + Request 2 (Sala A, 08:00-09:00) simultâneas | Request 1 aguarda aprovação (SOLICITADA com approval_required=true), Request 2 pode ser aceita conforme status | Concorrência, Integração |
| **T-RN04-007** | Conflicts | Garantir consistência após aceitar uma | Dupla simultânea; após aceitar Request 1, verificar se Request 2 recebeu resposta de erro consistente | Base de dados reflete apenas 1 reserva confirmada; Request 2 recebeu erro claro | Concorrência, Integração |
| **T-RN04-008** | Conflicts | Sem race condition em auditoria | Dupla simultânea aceita uma; verificar se auditoria registrou corretamente qual foi aceita e qual recusada | Auditoria em RN-09 traz timestamp e sequência clara (PENDENTE: política de timestamp) | Concorrência, Integração |

---

### RN-05: Indisponibilidade por Manutenção

**ID completo:** `RN-05 | docs/prd.md:6.5`

**Descrição:** Recursos em manutenção não podem ser reservados.

**Origem:**
- Arquivo: `docs/prd.md`
- Seção: 6. Regras de Negócio (seção 6.5)
- Origem funcional: Regras críticas + Personas (Admin, Solicitante)

**Requisitos Relacionados:**
- RF-02: Gestão de salas
- RF-04: Gestão de materiais
- RF-06: Gestão de bloqueios
- RF-09: Pesquisa e disponibilidade
- RF-10: Criação de reserva
- RF-11: Alteração de reserva

**Criticidade:** Crítica

**Decisão pendente:** Política para reservas existentes afetadas por mudança de manutenção (acima em `docs/relatorio-correcao-auditoria.md`, seção 12).

#### 2.5.1 Casos de Teste para RN-05

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RN05-001** | Happy Path | Reserva em recurso sem manutenção | Sala A sem manutenção; nova: 08:00-09:00 | Aceita | API, Integração |
| **T-RN05-002** | Happy Path | Reserva fora do período de manutenção | Sala A manutenção: Seg 08:00-09:00; nova: Seg 10:00-11:00 | Aceita | Integração, API |
| **T-RN05-003** | Conflicts | Reserva em recurso em manutenção (período total) | Sala A manutenção: Seg 08:00-09:00; nova: Seg 08:30-09:00 | Recusada: "Recurso em manutenção" | API, Integração |
| **T-RN05-004** | Conflicts | Reserva em recurso com manutenção parcial | Sala A manutenção: Seg 08:00-09:00; nova: Seg 08:30-09:30 | Recusada: "Recurso indisponível no período" | Integração, API |
| **T-RN05-005** | Conflicts | Múltiplos períodos de manutenção | Sala A: Seg 08:00-09:00 e 14:00-15:00; nova: Seg 08:30 | Recusada por primeira manutenção | Integração |
| **T-RN05-006** | Conflicts | Material em manutenção | Material X manutenção: Seg 08:00-09:00; nova com Material X: Seg 08:00 | Recusada: "Material indisponível" | Integração, API |
| **T-RN05-007** | Forbidden State | Alterar reserva introduzindo manutenção | Existente: Sala A Seg 10:00-11:00; Admin registra manutenção Seg 10:00-11:00; tentar alterar duramente | Recusada: "Sala em manutenção neste período" | API, Integração |
| **T-RN05-008** | Conflicts | Pesquisa de disponibilidade exclui manutenção | Solicitante consulta Sala A Seg 08:00-09:00 (com manutenção) | Sala A retorna como indisponível ou não aparece nos resultados | API, Integração |
| **T-RN05-009** | Invalid Input | Bloqueio/manutenção com período inválido | manutenção: início 09:00, fim 08:00 | Recusada conforme RN-01 | API |
| **T-RN05-010** | Boundary | Manutenção adjacente à reserva | Manutenção: Seg 09:00-10:00; reserva: Seg 08:00-09:00 | Aceita OU Recusada conforme política de adjacência (PENDENTE) | Integração |

---

### RN-06: Aprovação de Recursos Restritos

**ID completo:** `RN-06 | docs/prd.md:6.6`

**Descrição:** Recursos restritos exigem aprovação; somente o perfil Responsável pode aprová-los.

**Origem:**
- Arquivo: `docs/prd.md`
- Seção: 6. Regras de Negócio (seção 6.6)
- Origem funcional: Regras críticas + Personas (Responsável)

**Requisitos Relacionados:**
- RF-01: Autenticação e autorização por perfil
- RF-10: Criação de reserva
- RF-14: Aprovação de solicitações especiais
- RF-15: Validação da alocação de docentes

**Criticidade:** Crítica

**Decisão pendente:** Critério de identificação de recurso restrito (não definido em fontes oficiais).

#### 2.6.1 Casos de Teste para RN-06

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RN06-001** | Happy Path | Recurso comum: Solicitante cria e aceita | Sala comum, Solicitante cria | Aceita direto, estado SOLICITADA (não aguarda aprovação) | API, Integração |
| **T-RN06-002** | Happy Path | Recurso restrito: Solicitante cria, Responsável aprova | Sala restrita, Solicitante cria; Responsável aprova | Estado SOLICITADA → APROVADA após aprovação Responsável | API, Integração |
| **T-RN06-003** | Happy Path | Recurso restrito: Responsável rejeita | Sala restrita pendente, Responsável rejeita | Estado SOLICITADA → REJEITADA com motivo | API, Integração |
| **T-RN06-004** | Forbidden State | Solicitante tenta aprovar (não tem permissão) | Sala restrita pendente, Solicitante tenta aprovar | Recusada: "Acesso negado. Apenas Responsável pode aprovar" | API |
| **T-RN06-005** | Forbidden State | Administrador tenta aprovar (não tem permissão) | Sala restrita pendente, Admin tenta aprovar | Recusada: "Acesso negado. Apenas Responsável pode aprovar" | API |
| **T-RN06-006** | Conflicts | Responsável aprova recurso que ficou indisponível | Sala restrita solicitada 08:00-09:00; Admin registra manutenção; Responsável aprova | Recusada: "Recurso indisponível no período" OU aceita com status de alerta (PENDENTE) | Integração, API |
| **T-RN06-007** | Conflicts | Responsável aprova duplicada (duas solicitações simultâneas) | Dupla simultânea de sala restrita; Responsável aprova ambas | Exatamente 1 aprovada conforme RN-04 + RN-06 integrados | Concorrência, Integração |
| **T-RN06-008** | Invalid Input | Recurso inexistente marcado como restrito | Sala ID 99999 (não existe), tentativa de aprovar | Recusada: "Recurso não encontrado" | API |
| **T-RN06-009** | Boundary | Responsável autorizado para recurso A, tenta aprovar recurso B de outro responsável | Responsável A tem autorização para Sala A, tenta aprovar Sala B (não sua responsabilidade) | Recusada: "Recurso fora de sua responsabilidade" (se implementado) OU aceita (PENDENTE de decisão) | API |
| **T-RN06-010** | Forbidden State | Tentar aprovar recurso já aprovado | Sala restrita já APROVADA, Responsável tenta aprovar novamente | Recusada: "Solicitação já foi aprovada" | API |

---

### RN-07: Fluxo de Estados

**ID completo:** `RN-07 | docs/prd.md:6.7`

**Descrição:** O fluxo principal é `SOLICITADA -> APROVADA -> EM_USO -> CONCLUIDA`; estados alternativos oficiais são `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU`.

**Origem:**
- Arquivo: `docs/prd.md`
- Seção: 6. Regras de Negócio (seção 6.7)
- Origem funcional: Regras críticas de estados e auditoria

**Requisitos Relacionados:**
- RF-10: Criação de reserva
- RF-11: Alteração de reserva
- RF-12: Cancelamento de reserva
- RF-14: Aprovação de solicitações especiais
- RF-18: Gestão dos estados da reserva
- RF-19: Histórico auditável

**Criticidade:** Crítica

**Decisões pendentes:**
- Estado inicial de reserva não restrita (SOLICITADA diretamente OU APROVADA?)
- Atores e condições de cada transição
- Não especificadas: transições de APROVADA para EM_USO, de EM_USO para CONCLUIDA, e alternativas

**Diagrama de estados:**
```
SOLICITADA --[aprovação]-> APROVADA --[início]-> EM_USO --[conclusão]-> CONCLUIDA
     |                            |                   |
     +--[rejeição]-> REJEITADA     +--[cancelamento]--> CANCELADA
     |                            
     +--[cancelamento]-> CANCELADA
     
NAO_COMPARECEU: origem pendente (de qual estado?)
```

#### 2.7.1 Casos de Teste para RN-07

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RN07-001** | Happy Path | Fluxo principal completo | Nova reserva → aprovada → iniciada → concluída | SOLICITADA → APROVADA → EM_USO → CONCLUIDA | API, Integração, E2E |
| **T-RN07-002** | Happy Path | Cancelamento em SOLICITADA | Reserva em SOLICITADA, Solicitante cancela | SOLICITADA → CANCELADA com auditoria (RN-09) | API, Integração |
| **T-RN07-003** | Happy Path | Rejeição em SOLICITADA | Reserva em SOLICITADA (restrita), Responsável rejeita | SOLICITADA → REJEITADA com auditoria | API, Integração |
| **T-RN07-004** | Forbidden State | Tentar transição não especificada | SOLICITADA → EM_USO (sem passar por APROVADA) | Recusada: "Transição não permitida" | API |
| **T-RN07-005** | Forbidden State | Tentar estado inválido | Alterar estado para "PENDENTE" (não oficial) | Recusada: "Estado inválido" | API |
| **T-RN07-006** | Forbidden State | Cancelar reserva em EM_USO | Reserva em EM_USO, Solicitante tenta cancelar | Recusada conforme RN-08: "Reserva iniciada não pode ser cancelada" | API |
| **T-RN07-007** | Conflicts | Rejeitar após APROVADA (transição retrógrada) | Reserva em APROVADA, Responsável tenta rejeitar | Recusada: "Transição não permitida" OU fatura de sistema se ofertar EM_USO antes de iniciar (PENDENTE) | API |
| **T-RN07-008** | Boundary | Transição na fronteira de tempo | Reserva SOLICITADA às 23:59:59, aprovada aos 00:00:00 | Aceita; timestamp de auditoria registra transição (RN-09) | Integração, API |
| **T-RN07-009** | Invalid Input | Estado null ou vazio | Novo estado = null ou "" | Recusada: "Estado é obrigatório" | API |
| **T-RN07-010** | Forbidden State | NAO_COMPARECEU sem origem definida | Tentar marcar como NAO_COMPARECEU | Comportamento indefinido: PENDENTE DE DECISÃO (qual ator? qual origem?) | BLOQUEADO |

---

### RN-08: Reserva Iniciada Não Pode Ser Apagada

**ID completo:** `RN-08 | docs/prd.md:6.8`

**Descrição:** Reservas iniciadas não podem ser apagadas.

**Origem:**
- Arquivo: `docs/prd.md`
- Seção: 6. Regras de Negócio (seção 6.8)
- Origem funcional: Regras críticas + Personas (Admin, Solicitante)

**Requisitos Relacionados:**
- RF-12: Cancelamento de reserva
- RF-18: Gestão dos estados da reserva
- RF-19: Histórico auditável

**Criticidade:** Crítica

**Interpretação:** "Apagar" significa remover do sistema (DELETE) ou cancelar (CANCEL). Operação proibida é qualquer transição que remova registro ou cancele reserva em estado EM_USO ou posterior.

#### 2.8.1 Casos de Teste para RN-08

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RN08-001** | Happy Path | Cancelar antes de iniciar | Reserva SOLICITADA, Solicitante cancela | Transição para CANCELADA permitida (RN-07) | API, Integração |
| **T-RN08-002** | Forbidden State | Cancelar após iniciação (EM_USO) | Reserva EM_USO, Solicitante tenta cancelar | Recusada: "Reserva em uso não pode ser cancelada" | API, Integração |
| **T-RN08-003** | Forbidden State | Cancelar após conclusão (CONCLUIDA) | Reserva CONCLUIDA, Solicitante tenta cancelar | Recusada: "Operação não permitida para reserva concluída" | API, Integração |
| **T-RN08-004** | Forbidden State | Apagar registro (DELETE) de reserva iniciada | Reserva EM_USO, Admin tenta DELETE via banco ou API | Recusada: "Registros de reserva iniciada não podem ser removidos" | API, Integração |
| **T-RN08-005** | Forbidden State | Alterar de EM_USO para CANCELADA (simulação de apagamento) | Reserva EM_USO, Admin tenta força transição para CANCELADA | Recusada OU suporta transição conforme política de encerramento (PENDENTE) | API |
| **T-RN08-006** | Boundary | Alteração imediatamente após iniciação | Reserva inicia no segundo 00, tenta cancelar no segundo 01 | Recusada: "Reserva já iniciada" | Integração |
| **T-RN08-007** | Invalid Input | Tentar apagar com ID inválido | ID = null, string, ou número negativo | Recusada: "ID inválido" | API |
| **T-RN08-008** | Conflicts | Garantir auditoria de tentativa de apagamento | Tentar apagar EM_USO; verificar se auditoria registrou tentativa | Auditoria registra: usuário, timestamp, tentativa, resultado (PENDENTE: detalhe de política) | Integração |
| **T-RN08-009** | Forbidden State | Admin não pode contornar proteção | Admin tenta apagar CONCLUIDA ou EM_USO | Recusada conforme RN-08, mesmo Admin não pode contornar | API |
| **T-RN08-010** | Forbidden State | Rejeitar após iniciada | Reserva EM_USO, Responsável tenta rejeitar | Recusada: "Transição não permitida" OU "Reserva já iniciada não pode ser alterada" | API |

---

### RN-09: Auditoria de Mudança de Estado

**ID completo:** `RN-09 | docs/prd.md:6.9`

**Descrição:** Toda mudança de estado deve gerar registro de auditoria.

**Origem:**
- Arquivo: `docs/prd.md`
- Seção: 6. Regras de Negócio (seção 6.9)
- Origem funcional: Regras críticas de estados e auditoria

**Requisitos Relacionados:**
- RF-19: Histórico auditável
- RF-20: Notificação ou integração externa
- RF-21: Relatórios operacionais
- Todas as RFs que causam mudança de estado (RF-10, RF-11, RF-12, RF-14, etc.)

**Criticidade:** Crítica

**Decisões pendentes:**
- Campos obrigatórios de auditoria (timestamp, ator, mudança anterior/nova, motivo, IP, etc.)
- Imutabilidade completa da auditoria
- Política de retenção de auditoria

#### 2.9.1 Casos de Teste para RN-09

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RN09-001** | Happy Path | Criar reserva gera auditoria | Novo: SOLICITADA | Auditoria criada com: usuário=Solicitante, ação=CRIAR, estado_novo=SOLICITADA, timestamp | Integração, API |
| **T-RN09-002** | Happy Path | Mudar de SOLICITADA para APROVADA | Responsável aprova | Auditoria com: usuário=Responsável, ação=APROVAR, estado_anterior=SOLICITADA, estado_novo=APROVADA, timestamp | Integração, API |
| **T-RN09-003** | Happy Path | Consultar histórico de reserva | Reserva com 5 mudanças de estado | Histórico lista todas 5 mudanças em ordem cronológica com ator e timestamp | API, Integração |
| **T-RN09-004** | Happy Path | Múltiplas mudanças em rápida sucessão | Criar → Aprovar → Iniciar → Concluir (tudo em <1s) | Auditoria preserva ordem com timestamp granular (ms?) | Integração |
| **T-RN09-005** | Invalid Input | Operação sem autenticação (sem usuário) | Solicitação válida, mas sem token/sessão | Auditoria registra erro: "Usuário não identificado" OU recusa operação antes | API |
| **T-RN09-006** | Forbidden State | Tentar editar registro de auditoria | Auditoria criada, tentativa de UPDATE | Recusada: "Auditoria é imutável" (se implementado) OU sem permissão | Integração |
| **T-RN09-007** | Forbidden State | Tentar apagar registro de auditoria | Auditoria criada, tentativa de DELETE | Recusada: "Auditoria não pode ser removida" | Integração, API |
| **T-RN09-008** | Conflicts | Operação recusada gera auditoria? | Solicitante tenta aprovar (sem permissão, recusada) | Comportamento pendente: gera auditoria de tentativa OU não (PENDENTE) | API |
| **T-RN09-009** | Boundary | Auditoria com timestamp duvidoso | Dois eventos no mesmo ms | Sistema usa mecanismo de desempate (sequência, nanosegundos) OU cria ambiguidade (PENDENTE) | Integração |
| **T-RN09-010** | Boundary | Auditoria após RN-08 (apagamento proibido) | Tentar apagar EM_USO; verificar se auditoria foi criada | Auditoria registra tentativa de apagamento e resultado (REJEITADO) | Integração, API |

---

### RN-10: Rastreabilidade dos Requisitos Críticos

**ID completo:** `RN-10 | docs/prd.md:6.10`

**Descrição:** Todos os requisitos críticos devem estar rastreados na matriz de rastreabilidade.

**Origem:**
- Arquivo: `docs/prd.md`
- Seção: 6. Regras de Negócio (seção 6.10)
- Origem funcional: Estratégia obrigatória de qualidade

**Requisitos Relacionados:**
- RFs: todos os MUST
- RNFs: RNF-04

**Criticidade:** Crítica

**Evidência esperada:** Matriz de rastreabilidade com 100% dos requisitos críticos mapeados (conforme `docs/relatorio-validacao-arquitetura.md`, seção 4.2).

**Nota:** RN-10 é uma regra sobre o próprio processo de engenharia (não sobre o domínio de negócio). Testes de RN-10 envolvem validação documental, não testes de código.

#### 2.10.1 Casos de Teste para RN-10

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RN10-001** | Happy Path | Matriz de rastreabilidade completa | Revisar `docs/arquitetura.md` seção 50 ou similar | Todos os 10 RNs mapeados; todos os RFs MUST mapeados | Documental |
| **T-RN10-002** | Happy Path | RN-01 rastreada a RF-10 e RF-11 | Verificar matriz | Ligação presente: RN-01 → RF-10, RF-11 | Documental |
| **T-RN10-003** | Happy Path | Cada RF MUST possui pelo menos um teste | Revisar RFs 01-23 | Cada RF MUST tem casos de teste em RN-0X e T-RF-0X (ver seção 3) | Documental |
| **T-RN10-004** | Invalid Input | Requisito não rastreado | RF-X sem correspondência em matriz | Identificado como lacuna; não deve ocorrer | Documental |
| **T-RN10-005** | Conflicts | Orfandade (teste sem requisito) | Teste T-X sem origem em RN ou RF | Identificado como erro de documentação | Documental |
| **T-RN10-006** | Boundary | Requisito rastreado a múltiplos requisitos | RN-04 relacionado a RF-10 e RF-13 | Ligações múltiplas permitidas; importante não há ciclo | Documental |
| **T-RN10-007** | Happy Path | Meta de cobertura RNs críticas | 10 RNs críticas + 23 RFs MUST | 100% mapeados conforme PRD/Arquitetura | Documental |
| **T-RN10-008** | Invalid Input | Requisito crítico não testado | RF MUST sem casos de teste | Deve ser escalado; jamais deve ocorrer | Documental |
| **T-RN10-009** | Happy Path | Divergências e aceites registrados | Conflitos entre fontes identificados e resolvidos | Registrados em `docs/relatorio-validacao-arquitetura.md` | Documental |
| **T-RN10-010** | Boundary | Matriz atualizada após mudança | Adicionar novo requisito X; verificar matriz | Matriz é atualizada em paralelo à implementação (TDD) | Documental |

---

## 3. Inventário de Requisitos Funcionais (RFs)

### RF-01: Autenticação e Autorização por Perfil

**ID completo:** `RF-01 | docs/prd.md:7.1 | E1: Acesso e Perfis`

**Descrição:** O sistema deve autenticar usuários e autorizar as ações conforme os perfis Solicitante, Responsável e Administrador.

**Prioridade:** MUST

**Persona de origem:** Solicitante, Responsável, Administrador

**Regras de negócio relacionadas:** RN-06

**Critério de Aceitação Oficial:**
```gherkin
Dado que um usuário autenticado possui um dos três perfis oficiais
Quando acessa uma funcionalidade permitida ao seu perfil
Então o sistema permite a operação

Dado que um usuário autenticado tenta executar uma ação não permitida ao seu perfil
Quando solicita a operação
Então o sistema recusa a operação e apresenta uma mensagem de erro compreensível
```

**Métrica oficial:** Taxa de acesso permitido e acesso recusado corretos = 100%

#### 3.1.1 Casos de Teste para RF-01

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF01-001** | Happy Path | Solicitante autenticado consulta disponibilidade | Token válido Solicitante | Retorna recursos e disponibilidade | API, Integração |
| **T-RF01-002** | Happy Path | Responsável autenticado aprova solicitação | Token válido Responsável, solicitação pendente | Aprova e gera auditoria (RN-09) | API, Integração |
| **T-RF01-003** | Happy Path | Administrador autenticado gerencia recurso | Token válido Admin, cadastro novo recurso | Cria recurso e torna disponível | API, Integração |
| **T-RF01-004** | Forbidden State | Solicitante tenta aprovar (sem permissão) | Token Solicitante, tentativa de aprovação | Recusada: "Acesso negado" | API |
| **T-RF01-005** | Forbidden State | Responsável tenta gerenciar usuários (sem permissão) | Token Responsável, tentativa de gerenciar usuário | Recusada: "Acesso negado" | API |
| **T-RF01-006** | Invalid Input | Token inválido ou expirado | Token malformado ou expirado | Recusada: "Autenticação inválida" OU redirecionado para login | API |
| **T-RF01-007** | Invalid Input | Sem token | Requisição sem header Authorization | Recusada: "Token obrigatório" OU erro 401 | API |
| **T-RF01-008** | Forbidden State | Usuário com múltiplos perfis (não especificado) | Usuário X tem Solicitante + Responsável | Comportamento: qual perfil é usado? Prioridade? PENDENTE DE DECISÃO | API |
| **T-RF01-009** | Boundary | Transição de perfil (logout/login com novo perfil) | Login como Solicitante, logout, login como Admin | Permissions mudam conforme novo perfil | API, Integração |
| **T-RF01-010** | Forbidden State | Usuário desativado tenta acessar | Token ativo mas usuário desativado | Recusada: "Usuário inativo" OU "Acesso negado" | API, Integração |

---

### RF-10: Criação de Reserva

**ID completo:** `RF-10 | docs/prd.md:7.10 | E4: Reservas e Agenda`

**Descrição:** O sistema deve permitir ao Solicitante criar suas próprias reservas respeitando disponibilidade, conflitos, manutenção e restrições.

**Prioridade:** MUST

**Persona de origem:** Solicitante

**Regras de negócio relacionadas:** RN-01, RN-02, RN-03, RN-04, RN-05, RN-06, RN-07, RN-09

**Critério de Aceitação Oficial:**
```gherkin
Dado que um Solicitante está autenticado e seleciona sala, material, professor válidos com período
Quando cria a reserva com início e fim válidos
Então o sistema verifica conflitos em sala, material, professor e manutenção
E se recurso é restrito, a reserva entra em SOLICITADA aguardando aprovação
Se recurso não é restrito, a reserva é aceita imediatamente

Dado que há sobreposição ou manutenção
Quando a solicitação é enviada
Então o sistema recusa com motivo claro
```

**Métrica oficial:** Taxa de reservas criadas corretamente e conflitos impedidos = 100%

#### 3.10.1 Casos de Teste para RF-10

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF10-001** | Happy Path | Criar reserva simples (sala comum) | Sala A, 08:00-09:00, sem conflito | Aceita, SOLICITADA | API, Integração |
| **T-RF10-002** | Happy Path | Criar com múltiplos recursos (sala + material) | Sala A + Material X, sem conflito | Aceita, SOLICITADA | API, Integração |
| **T-RF10-003** | Happy Path | Criar com professor | Professor Y + Sala A, sem conflito na agenda | Aceita, SOLICITADA | API, Integração |
| **T-RF10-004** | Happy Path | Recurso restrito aguarda aprovação | Sala restrita A | SOLICITADA, approval_required=true | API, Integração |
| **T-RF10-005** | Conflicts | Sobreposição em sala | Sala A 08:00-09:00 existente, nova 08:30-09:00 | Recusada: conflito em sala | API |
| **T-RF10-006** | Conflicts | Sobreposição em material | Material X 08:00-09:00 existente, nova com X 08:30 | Recusada: conflito em material | API |
| **T-RF10-007** | Conflicts | Sobreposição em professor | Prof Y agenda 08:00-09:00, nova com Y 08:30 | Recusada: conflito professor (RN-03) | API, Integração |
| **T-RF10-008** | Conflicts | Recurso em manutenção | Sala A manutenção 08:00-09:00, nova 08:30 | Recusada: manutenção (RN-05) | API, Integração |
| **T-RF10-009** | Conflicts | Dupla simultânea (RN-04) | Duas requisições simultâneas mesma sala/hora | Uma aceita, uma recusada | Concorrência |
| **T-RF10-010** | Boundary | Período RN-01 válido (fim > início) | início: 08:00, fim: 08:01 (mínimo) | Aceita OU conforme política (PENDENTE) | API |
| **T-RF10-011** | Invalid Input | Fim anterior ao início (RN-01) | início: 09:00, fim: 08:00 | Recusada conforme RN-01 | API |
| **T-RF10-012** | Invalid Input | Recurso inexistente | Sala ID: 99999 | Recusada: "Recurso não encontrado" | API |
| **T-RF10-013** | Forbidden State | Solicitante cria para outro Solicitante | User A tenta criar com owner=User B | Recusada: "Não pode criar reserva para outro usuário" | API |
| **T-RF10-014** | Happy Path | Auditoria criada (RN-09) | Criar reserva válida | Auditoria registra: usuário, ação=CRIAR, estado=SOLICITADA | Integração |
| **T-RF10-015** | Happy Path | Persistência em banco | Criar + consultar imediatamente | Reserva recuperada do banco com dados corretos | Integração |

---

### 3.A Casos adicionais para os demais RFs MUST (2026-09-25)

> Os 21 RFs abaixo (RF-02 a RF-09 e RF-11 a RF-23) não tinham casos neste plano (ver §4.4 e T-RN10-003). Os casos foram criados a partir dos critérios de aceitação oficiais do PRD, sem acrescentar resultado esperado. **Ainda não possuem teste JUnit**: os testes devem nascer na próxima fase RED (ver seção 8.4).

### RF-02: Gestão de salas

**ID completo:** `RF-02 | docs/prd.md:7.2`

**Descrição:** O sistema deve permitir ao Administrador cadastrar e consultar salas.

**Prioridade:** MUST. · **Persona de origem:** Administrador. · **Regras de negócio relacionadas:** RN-05.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.2), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.2.1 Casos de Teste para RF-02

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF02-001** | Happy Path | Critério de aceitação 1 do RF-02 | Dado que o Administrador está autenticado; Quando cadastra uma sala | Então o sistema registra a sala e permite sua consulta. | testes de API e integração de cadastro e consulta |
| **T-RF02-002** | Forbidden State | Critério de aceitação 2 do RF-02 | Dado que um Solicitante não possui permissão de gestão; Quando tenta cadastrar uma sala | Então o sistema recusa a operação. | testes de API e integração de cadastro e consulta |

---

### RF-03: Gestão de professores

**ID completo:** `RF-03 | docs/prd.md:7.3`

**Descrição:** O sistema deve permitir ao Administrador cadastrar e consultar professores.

**Prioridade:** MUST. · **Persona de origem:** Administrador. · **Regras de negócio relacionadas:** RN-03.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.3), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.3.1 Casos de Teste para RF-03

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF03-001** | Happy Path | Critério de aceitação 1 do RF-03 | Dado que o Administrador está autenticado; Quando cadastra um professor | Então o sistema registra o professor e permite sua consulta. | testes de API e integração de cadastro, consulta e uso na agenda |
| **T-RF03-002** | Forbidden State | Critério de aceitação 2 do RF-03 | Dado que um Solicitante não possui permissão de gestão; Quando tenta cadastrar um professor | Então o sistema recusa a operação. | testes de API e integração de cadastro, consulta e uso na agenda |

---

### RF-04: Gestão de materiais

**ID completo:** `RF-04 | docs/prd.md:7.4`

**Descrição:** O sistema deve permitir ao Administrador cadastrar e consultar materiais.

**Prioridade:** MUST. · **Persona de origem:** Administrador. · **Regras de negócio relacionadas:** RN-05.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.4), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.4.1 Casos de Teste para RF-04

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF04-001** | Happy Path | Critério de aceitação 1 do RF-04 | Dado que o Administrador está autenticado; Quando cadastra um material | Então o sistema registra o material e permite sua consulta. | testes de API e integração de cadastro, consulta e uso em reserva |
| **T-RF04-002** | Forbidden State | Critério de aceitação 2 do RF-04 | Dado que um Solicitante não possui permissão de gestão; Quando tenta cadastrar um material | Então o sistema recusa a operação. | testes de API e integração de cadastro, consulta e uso em reserva |

---

### RF-05: Gestão de usuários

**ID completo:** `RF-05 | docs/prd.md:7.5`

**Descrição:** O sistema deve permitir ao Administrador gerenciar usuários e seus perfis oficiais.

**Prioridade:** MUST. · **Persona de origem:** Administrador. · **Regras de negócio relacionadas:** RN-06.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.5), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.5.1 Casos de Teste para RF-05

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF05-001** | Happy Path | Critério de aceitação 1 do RF-05 | Dado que o Administrador está autenticado; Quando gerencia um usuário com um perfil oficial | Então o sistema registra a alteração para uso na autorização. | testes de API e autorização para gestão de usuários |
| **T-RF05-002** | Forbidden State | Critério de aceitação 2 do RF-05 | Dado que um usuário não é Administrador; Quando tenta gerenciar usuários | Então o sistema recusa a operação. | testes de API e autorização para gestão de usuários |

---

### RF-06: Gestão de bloqueios

**ID completo:** `RF-06 | docs/prd.md:7.6`

**Descrição:** O sistema deve permitir ao Administrador gerenciar bloqueios de recursos.

**Prioridade:** MUST. · **Persona de origem:** Administrador. · **Regras de negócio relacionadas:** RN-05.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.6), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.6.1 Casos de Teste para RF-06

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF06-001** | Happy Path | Critério de aceitação 1 do RF-06 | Dado que o Administrador está autenticado e o recurso existe; Quando cria um bloqueio para o recurso | Então o sistema registra o bloqueio e o considera indisponível no período correspondente. | testes de API e integração de criação e consulta de bloqueio |
| **T-RF06-002** | Forbidden State | Critério de aceitação 2 do RF-06 | Dado que um Solicitante tenta gerenciar bloqueios; Quando solicita a operação | Então o sistema recusa a operação. | testes de API e integração de criação e consulta de bloqueio |

---

### RF-07: Gestão de manutenção

**ID completo:** `RF-07 | docs/prd.md:7.7`

**Descrição:** O sistema deve permitir ao Administrador gerenciar períodos de manutenção de recursos.

**Prioridade:** MUST. · **Persona de origem:** Administrador. · **Regras de negócio relacionadas:** RN-05.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.7), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.7.1 Casos de Teste para RF-07

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF07-001** | Happy Path | Critério de aceitação 1 do RF-07 | Dado que o Administrador está autenticado e o recurso existe; Quando registra um período de manutenção | Então o sistema bloqueia reservas do recurso nesse período. | testes de integração com banco em Testcontainers e testes de API |
| **T-RF07-002** | Conflicts | Critério de aceitação 2 do RF-07 | Dado que um Solicitante tenta reservar o recurso durante a manutenção; Quando envia a solicitação | Então o sistema recusa a reserva e informa a indisponibilidade. | testes de integração com banco em Testcontainers e testes de API |

---

### RF-08: Consulta de recursos

**ID completo:** `RF-08 | docs/prd.md:7.8`

**Descrição:** O sistema deve permitir ao Solicitante consultar salas, professores e materiais disponíveis no cadastro.

**Prioridade:** MUST. · **Persona de origem:** Solicitante. · **Regras de negócio relacionadas:** RN-05.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.8), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.8.1 Casos de Teste para RF-08

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF08-001** | Happy Path | Critério de aceitação 1 do RF-08 | Dado que o Solicitante está autenticado; Quando consulta salas, professores ou materiais | Então o sistema retorna os recursos cadastrados para consulta. | testes de API e end-to-end com consulta autorizada e não autorizada |
| **T-RF08-002** | Conflicts | Critério de aceitação 2 do RF-08 | Dado que o Solicitante consulta um recurso em manutenção; Quando verifica sua disponibilidade | Então o sistema informa que o recurso não pode ser reservado no período de manutenção. | testes de API e end-to-end com consulta autorizada e não autorizada |

---

### RF-09: Pesquisa por filtros e disponibilidade

**ID completo:** `RF-09 | docs/prd.md:7.9`

**Descrição:** O sistema deve permitir pesquisar recursos por tipo, capacidade, localização, competência e disponibilidade.

**Prioridade:** MUST. · **Persona de origem:** Solicitante. · **Regras de negócio relacionadas:** RN-02, RN-03, RN-05.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.9), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.9.1 Casos de Teste para RF-09

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF09-001** | Happy Path | Critério de aceitação 1 do RF-09 | Dado que existem recursos cadastrados com diferentes tipos, capacidades, localizações, competências e agendas; Quando o Solicitante pesquisa usando esses filtros | Então o sistema retorna os recursos compatíveis com os filtros informados. | testes parametrizados de API e end-to-end para cada filtro e para recursos indisponíveis |
| **T-RF09-002** | Conflicts | Critério de aceitação 2 do RF-09 | Dado que um recurso está reservado, bloqueado, em manutenção ou conflita com a agenda do professor no período; Quando o Solicitante pesquisa sua disponibilidade | Então o sistema não o apresenta como disponível para reserva naquele período. | testes parametrizados de API e end-to-end para cada filtro e para recursos indisponíveis |

---

### RF-11: Alteração de reservas

**ID completo:** `RF-11 | docs/prd.md:7.11`

**Descrição:** O sistema deve permitir ao Solicitante alterar suas próprias reservas quando a alteração respeitar as regras de período, conflito e manutenção.

**Prioridade:** MUST. · **Persona de origem:** Solicitante. · **Regras de negócio relacionadas:** RN-01, RN-02, RN-03, RN-05, RN-06, RN-09.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.11), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.11.1 Casos de Teste para RF-11

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF11-001** | Happy Path | Critério de aceitação 1 do RF-11 | Dado que a reserva pertence ao Solicitante, não foi iniciada e o novo período está livre; Quando o Solicitante altera a reserva | Então o sistema salva a alteração sem conflito. | testes de API, integração, autorização, validação de entrada e auditoria |
| **T-RF11-002** | Forbidden State | Critério de aceitação 2 do RF-11 | Dado que a reserva pertence a outro Solicitante, já foi iniciada, conflita ou usa recurso em manutenção; Quando o Solicitante tenta alterá-la | Então o sistema recusa a operação. | testes de API, integração, autorização, validação de entrada e auditoria |
| **T-RF11-003** | Happy Path | Critério de aceitação 3 do RF-11 | Dado que a alteração muda o estado da reserva; Quando a alteração é concluída | Então o sistema gera registro de auditoria. | testes de API, integração, autorização, validação de entrada e auditoria |

---

### RF-12: Cancelamento de reservas

**ID completo:** `RF-12 | docs/prd.md:7.12`

**Descrição:** O sistema deve permitir ao Solicitante cancelar suas próprias reservas quando o cancelamento não violar a regra de reserva iniciada.

**Prioridade:** MUST. · **Persona de origem:** Solicitante. · **Regras de negócio relacionadas:** RN-07, RN-08, RN-09.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.12), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.12.1 Casos de Teste para RF-12

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF12-001** | Happy Path | Critério de aceitação 1 do RF-12 | Dado que a reserva pertence ao Solicitante e ainda não foi iniciada; Quando o Solicitante a cancela | Então o sistema altera a reserva para `CANCELADA` e registra a mudança. | testes de API, autorização e auditoria |
| **T-RF12-002** | Forbidden State | Critério de aceitação 2 do RF-12 | Dado que a reserva já foi iniciada ou pertence a outro Solicitante; Quando o usuário tenta cancelá-la | Então o sistema recusa a operação e informa o motivo. | testes de API, autorização e auditoria |

---

### RF-13: Detecção de sobreposição e dupla reserva

**ID completo:** `RF-13 | docs/prd.md:7.13`

**Descrição:** O sistema deve detectar sobreposição de horários em sala, material e professor e impedir dupla reserva em solicitações simultâneas.

**Prioridade:** MUST. · **Persona de origem:** Solicitante e Administrador. · **Regras de negócio relacionadas:** RN-02, RN-03, RN-04.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.13), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.13.1 Casos de Teste para RF-13

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF13-001** | Conflicts | Critério de aceitação 1 do RF-13 | Dado que uma reserva existente usa a mesma sala, material ou professor em período sobreposto; Quando uma nova solicitação é enviada | Então o sistema recusa a nova reserva e informa o conflito. | testes parametrizados para limites de intervalo e teste automatizado de concorrência com Testcontainers |
| **T-RF13-002** | Conflicts | Critério de aceitação 2 do RF-13 | Dado que duas solicitações simultâneas usam o mesmo recurso e período; Quando ambas são processadas | Então somente uma resulta em reserva aceita e a outra recebe resultado de conflito. | testes parametrizados para limites de intervalo e teste automatizado de concorrência com Testcontainers |

---

### RF-14: Aprovação de solicitações especiais

**ID completo:** `RF-14 | docs/prd.md:7.14`

**Descrição:** O sistema deve permitir ao Responsável aprovar ou rejeitar solicitações especiais de recursos restritos.

**Prioridade:** MUST. · **Persona de origem:** Responsável. · **Regras de negócio relacionadas:** RN-06, RN-07, RN-09.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.14), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.14.1 Casos de Teste para RF-14

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF14-001** | Happy Path | Critério de aceitação 1 do RF-14 | Dado que há solicitação especial pendente e o usuário é Responsável; Quando aprova a solicitação | Então o sistema altera o estado para `APROVADA` e registra a mudança de estado. | testes positivos e negativos de autorização, API, integração e auditoria |
| **T-RF14-002** | Forbidden State | Critério de aceitação 2 do RF-14 | Dado que o usuário é Solicitante ou Administrador; Quando tenta aprovar recurso restrito | Então o sistema recusa a operação. | testes positivos e negativos de autorização, API, integração e auditoria |
| **T-RF14-003** | Happy Path | Critério de aceitação 3 do RF-14 | Dado que o Responsável rejeita uma solicitação especial; Quando confirma a decisão | Então o sistema altera o estado para `REJEITADA` e registra a mudança. | testes positivos e negativos de autorização, API, integração e auditoria |

---

### RF-15: Validação da alocação de docentes

**ID completo:** `RF-15 | docs/prd.md:7.15`

**Descrição:** O sistema deve permitir ao Responsável validar a alocação de docentes.

**Prioridade:** MUST. · **Persona de origem:** Responsável. · **Regras de negócio relacionadas:** RN-03, RN-07.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.15), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.15.1 Casos de Teste para RF-15

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF15-001** | Happy Path | Critério de aceitação 1 do RF-15 | Dado que o Responsável está autenticado e existe uma alocação de docente; Quando executa a validação | Então o sistema registra o resultado da validação. | testes de autorização, API e integração com agenda do professor |
| **T-RF15-002** | Conflicts | Critério de aceitação 2 do RF-15 | Dado que a alocação conflita com a agenda do professor; Quando o Responsável tenta validá-la | Então o sistema informa o conflito e não a considera validada. | testes de autorização, API e integração com agenda do professor |

---

### RF-16: Registro de retirada de materiais

**ID completo:** `RF-16 | docs/prd.md:7.16`

**Descrição:** O sistema deve permitir ao Responsável acompanhar e registrar a retirada de materiais e equipamentos.

**Prioridade:** MUST. · **Persona de origem:** Responsável. · **Regras de negócio relacionadas:** RN-09.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.16), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.16.1 Casos de Teste para RF-16

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF16-001** | Happy Path | Critério de aceitação 1 do RF-16 | Dado que o Responsável está autenticado e existe material associado a uma reserva; Quando registra a retirada | Então o sistema grava a retirada e permite seu acompanhamento. | teste de integração com persistência realista e teste de API de autorização |
| **T-RF16-002** | Forbidden State | Critério de aceitação 2 do RF-16 | Dado que o Solicitante não possui permissão para registrar retirada; Quando tenta executar a operação | Então o sistema recusa a operação. | teste de integração com persistência realista e teste de API de autorização |

---

### RF-17: Registro de devolução de materiais

**ID completo:** `RF-17 | docs/prd.md:7.17`

**Descrição:** O sistema deve permitir ao Responsável acompanhar e registrar a devolução de materiais e equipamentos.

**Prioridade:** MUST. · **Persona de origem:** Responsável. · **Regras de negócio relacionadas:** RN-09.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.17), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.17.1 Casos de Teste para RF-17

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF17-001** | Happy Path | Critério de aceitação 1 do RF-17 | Dado que existe uma retirada registrada e o Responsável está autenticado; Quando registra a devolução | Então o sistema grava a devolução e permite seu acompanhamento. | teste de integração com persistência realista e teste de API de autorização |
| **T-RF17-002** | Invalid Input | Critério de aceitação 2 do RF-17 | Dado que não existe retirada correspondente; Quando o Responsável tenta registrar a devolução | Então o sistema recusa a operação e apresenta uma mensagem de erro compreensível. | teste de integração com persistência realista e teste de API de autorização |

---

### RF-18: Gestão dos estados da reserva

**ID completo:** `RF-18 | docs/prd.md:7.18`

**Descrição:** O sistema deve representar o fluxo principal `SOLICITADA -> APROVADA -> EM_USO -> CONCLUIDA` e os estados alternativos `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU`.

**Prioridade:** MUST. · **Persona de origem:** Solicitante e Responsável. · **Regras de negócio relacionadas:** RN-06, RN-07, RN-08, RN-09.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.18), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.18.1 Casos de Teste para RF-18

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF18-001** | Happy Path | Critério de aceitação 1 do RF-18 | Dado que uma reserva foi solicitada e aprovada conforme as regras; Quando avança pelo fluxo principal | Então o sistema representa os estados oficiais na ordem especificada. | testes de API e end-to-end dos estados oficiais e de transição não especificada |
| **T-RF18-002** | Forbidden State | Critério de aceitação 2 do RF-18 | Dado que uma operação tenta apagar reserva iniciada ou criar estado não oficial; Quando é processada | Então o sistema recusa a operação. | testes de API e end-to-end dos estados oficiais e de transição não especificada |

---

### RF-19: Histórico auditável

**ID completo:** `RF-19 | docs/prd.md:7.19`

**Descrição:** O sistema deve registrar e permitir consultar o histórico auditável das mudanças de estado das reservas.

**Prioridade:** MUST. · **Persona de origem:** Solicitante, Responsável e Administrador. · **Regras de negócio relacionadas:** RN-09, RN-10.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.19), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.19.1 Casos de Teste para RF-19

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF19-001** | Happy Path | Critério de aceitação 1 do RF-19 | Dado que uma mudança de estado é realizada; Quando a mudança é concluída | Então o sistema cria um registro de auditoria consultável. | teste de integração com Testcontainers e consulta de histórico após mudanças |
| **T-RF19-002** | Happy Path | Critério de aceitação 2 do RF-19 | Dado que uma reserva possui mudanças de estado; Quando o Administrador consulta seu histórico | Então o sistema apresenta os registros auditáveis correspondentes. | teste de integração com Testcontainers e consulta de histórico após mudanças |

---

### RF-20: Notificação ou integração externa

**ID completo:** `RF-20 | docs/prd.md:7.20`

**Descrição:** O sistema deve produzir uma notificação simulada ou realizar integração com uma API externa relacionada ao fluxo de reservas.

**Prioridade:** MUST. · **Persona de origem:** Transversal. · **Regras de negócio relacionadas:** RN-06, RN-07, RN-09.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.20), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.20.1 Casos de Teste para RF-20

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF20-001** | Happy Path | Critério de aceitação 1 do RF-20 | Dado que ocorre um evento do fluxo que utiliza notificação ou integração; Quando o evento é concluído | Então o sistema produz a notificação simulada ou realiza a chamada externa definida. | teste de integração com WireMock ou evidência da notificação simulada |
| **T-RF20-002** | Invalid Input | Critério de aceitação 2 do RF-20 | Dado que a API externa não responde; Quando a integração é acionada | Então o sistema registra o resultado da falha de forma segura e observável. | teste de integração com WireMock ou evidência da notificação simulada |

---

### RF-21: Relatórios operacionais

**ID completo:** `RF-21 | docs/prd.md:7.21`

**Descrição:** O sistema deve disponibilizar relatório de utilização por recurso, carga horária alocada e conflitos evitados.

**Prioridade:** MUST. · **Persona de origem:** Administrador. · **Regras de negócio relacionadas:** RN-02, RN-03, RN-04, RN-10.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.21), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.21.1 Casos de Teste para RF-21

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF21-001** | Happy Path | Critério de aceitação 1 do RF-21 | Dado que existem reservas e registros de conflito; Quando o Administrador consulta o relatório | Então o sistema apresenta utilização por recurso, carga horária alocada e conflitos evitados. | testes de API, integração e end-to-end com dados controlados |
| **T-RF21-002** | Forbidden State | Critério de aceitação 2 do RF-21 | Dado que um Solicitante não possui permissão para consultar relatórios operacionais; Quando tenta acessá-los | Então o sistema recusa a operação. | testes de API, integração e end-to-end com dados controlados |

---

### RF-22: Interface responsiva e erros compreensíveis

**ID completo:** `RF-22 | docs/prd.md:7.22`

**Descrição:** O sistema deve apresentar interface responsiva e mensagens de erro compreensíveis nos fluxos públicos do sistema.

**Prioridade:** MUST. · **Persona de origem:** Solicitante, Responsável e Administrador. · **Regras de negócio relacionadas:** RN-01, RN-02, RN-05, RN-06, RN-08.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.22), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.22.1 Casos de Teste para RF-22

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF22-001** | Happy Path | Critério de aceitação 1 do RF-22 | Dado que uma persona acessa um fluxo público em uma viewport suportada; Quando utiliza a interface | Então os controles e mensagens permanecem utilizáveis sem sobreposição de conteúdo. | testes end-to-end em viewports definidos pela equipe e testes de validação de entradas e erros seguros |
| **T-RF22-002** | Conflicts | Critério de aceitação 2 do RF-22 | Dado que ocorre período inválido, conflito, manutenção ou falta de autorização; Quando a operação é recusada | Então o sistema apresenta mensagem compreensível e não expõe informação sensível. | testes end-to-end em viewports definidos pela equipe e testes de validação de entradas e erros seguros |

---

### RF-23: Documentação da API ou dos fluxos públicos

**ID completo:** `RF-23 | docs/prd.md:7.23`

**Descrição:** O sistema deve disponibilizar documentação da API ou dos fluxos públicos do sistema.

**Prioridade:** MUST. · **Persona de origem:** Transversal. · **Regras de negócio relacionadas:** RN-06, RN-07, RN-09.

**Origem dos casos:** critérios de aceitação de `docs/prd.md` (seção 7.23), transcritos sem acréscimo de resultado esperado. Casos adicionados em 2026-09-25 para atender a T-RN10-003 (RTM: todo RF MUST com casos de teste).

#### 3.23.1 Casos de Teste para RF-23

| ID | Tipo | Cenário | Dados de Entrada | Resultado Esperado | Camada(s) |
|---|---|---|---|---|---|
| **T-RF23-001** | Happy Path | Critério de aceitação 1 do RF-23 | Dado que os fluxos públicos ou a API estão definidos; Quando a documentação é consultada | Então ela descreve os fluxos ou contratos disponibilizados pelo sistema. | inspeção documental e verificação de que os fluxos públicos estão descritos |
| **T-RF23-002** | Happy Path | Critério de aceitação 2 do RF-23 | Dado que um fluxo público possui estados, autorização ou erros relevantes; Quando é documentado | Então essas condições observáveis estão incluídas na documentação. | inspeção documental e verificação de que os fluxos públicos estão descritos |

---

---

## 4. Ambiguidades, Lacunas e Perguntas Abertas

### 4.1 Decisões Pendentes de Domínio

| ID | Questão | Origem | Impacto | Status |
|---|---|---|---|---|
| **Q-001** | Estado inicial de reserva não restrita: SOLICITADA ou APROVADA? | `docs/relatorio-correcao-auditoria.md`, seção 12 | RN-07, RF-10 | BLOQUEADO: Aguarda decisão de negócio |
| **Q-002** | Política de adjacência: reservas 09:00-10:00 e 10:00-11:00 na mesma sala são permitidas? | Limite de RN-02, RN-03, RN-05 | RN-02, RN-03, RN-05, RN-09 | PENDENTE: Decisão de design de API |
| **Q-003** | Critério de identificação de recurso restrito | `docs/relatorio-correcao-auditoria.md`, seção 12 | RN-06, RF-14 | BLOQUEADO: Sem critério aprovado |
| **Q-004** | Responsabilidade do Responsável: aprova todos recursos restritos OU por categoria/área? | `docs/arquitetura.md`, seção 3 | RN-06, RF-14 | PENDENTE: Divisão de responsabilidade |
| **Q-005** | Política para reservas existentes afetadas por manutenção criada depois | `docs/relatorio-correcao-auditoria.md`, seção 12 | RN-05 | PENDENTE: Cancelar? Alertar? Aceitar? |
| **Q-006** | Qual ator marcar reserva como NAO_COMPARECEU? Quando? | `docs/prd.md`, RN-07; `docs/relatorio-validacao-arquitetura.md`, seção 4.2 | RN-07, RF-18 | BLOQUEADO: Fluxo não definido |
| **Q-007** | Transição APROVADA → EM_USO: quem a dispara? Sistema automaticamente? Responsável? | `docs/relatorio-validacao-arquitetura.md`, seção 4.2 | RN-07 | BLOQUEADO: Ator não definido |
| **Q-008** | Transição EM_USO → CONCLUIDA: automática ao término do horário OU manual? | `docs/relatorio-validacao-arquitetura.md`, seção 4.2 | RN-07 | BLOQUEADO: Mecanismo não definido |
| **Q-009** | Mecanismo técnico de RN-04 (exatamente 1 reserva sob concorrência): pessimistic lock, optimistic, versioning? | `docs/relatorio-validacao-arquitetura.md`, seção 4.2 | RN-04 | PENDENTE DE EXPERIMENTO |
| **Q-010** | Relação entre professor-usuário e professor-recurso: 1:1, N:N, ou sem relação? | `docs/arquitetura.md`, seção 3.1 | RN-03, RF-03, RF-15 | PENDENTE: Arquitetura de dados |

---

### 4.2 Campos de Auditoria Não Especificados

| Campo | Questão | Status |
|---|---|---|
| **Timestamp** | Precisão necessária (segundo, milissegundo, nanossegundo)? Timezone? | PENDENTE |
| **Ator** | ID de usuário, email, nome? | PENDENTE |
| **IP/Sessão** | Registrar origem da requisição? | PENDENTE |
| **Motivo/Descrição** | Campo opcional descrevendo por que a ação foi realizada? | PENDENTE |
| **Método** | HTTP method ou tipo de operação? | PENDENTE |
| **Imutabilidade** | Auditoria pode ser editada/deletada? Histórico de alterações da própria auditoria? | PENDENTE |
| **Retenção** | Quanto tempo manter auditoria? Compliance LGPD? | PENDENTE |

---

### 4.3 Métricas Pendentes

| Métrica | Valor-alvo | Status | Notas |
|---|---|---|---|
| **Duração mínima de reserva** | Não definida | PENDENTE | RN-01 não especifica mínimo; T-RN01-003 depende disso |
| **Granularidade de horário** | Minuto? Hora? Intervalo? | PENDENTE | Afeta precisão de sobreposição (RN-02, RN-03) |
| **Carga de JMeter** | Não definida | PENDENTE | RNF-14 exigido mas sem meta |
| **Percentis de resposta** | Não definidos | PENDENTE | Desempenho esperado? |
| **Acessibilidade (WCAG)** | Não definida | PENDENTE | RNF-17 não especifica versão |
| **Política de adjacência** | Permitida OU recusada? | PENDENTE | Afeta múltiplos testes de boundary |

---

### 4.4 Lacunas de Cobertura de Requisitos

| RF/RN | Lacuna | Impacto |
|---|---|---|
| **RF-02 a RF-05** | Gestão de recursos (salas, professores, materiais, usuários) | Não há testes específicos listados aqui (escopo: casos de teste para domínio crítico de reserva) |
| **RF-06 a RF-08** | Gestão de bloqueios, manutenção, equipamentos | Parcialmente coberto por RN-05 |
| **RF-09** | Pesquisa e disponibilidade | Apenas validação indireta via RN-02, RN-03, RN-05 |
| **RF-15 a RF-17** | Validação docente, retirada/devolução | Parcialmente coberto por RN-03, RN-09 |
| **RF-20** | Notificação ou integração externa | Não há regra de negócio específica; será tratado separadamente |
| **RF-21** | Relatórios operacionais | Métricas não especificadas; será tratado em plano de validação de dados |
| **RF-22** | Interface responsiva e erros compreensíveis | Testes end-to-end; não coberto por RNs |
| **RF-23** | Documentação da API | Validação documental; não coberto por testes unitários/integração |

---

### 4.5 Decisões ainda não Aprovadas (Candidatas)

| Decisão | Origem | Impacto |
|---|---|---|
| **Separação online/batch** | `docs/arquitetura.md`, ADR-006 | CON-01 ainda não definido (propósito, dados, algoritmo) |
| **Registry e versionamento de artefatos** | `docs/arquitetura.md`, ADR-013 | Não obrigatório; impacto em gates de promoção |
| **Papel técnico de batch** | `docs/arquitetura.md`, seção 3.2 | Candidato não aprovado |
| **Autenticação específica (JWT, OAuth, Basic)** | Não especificada | Implementação de RF-01 pode variar |
| **Tecnologia de cache** | Não especificada | Otimização de RN-02 (verificação de sobreposição) |

---

### 4.6 Intersecções entre Regras (Complexidade)

| Regras | Complexidade | Cenário |
|---|---|---|
| **RN-01 + RN-02 + RN-03** | Alta | Validar múltiplos conflitos simultaneamente |
| **RN-04 + RN-06 + RN-09** | Alta | Concorrência + aprovação + auditoria simultâneas |
| **RN-05 + RN-07 + RN-08** | Alta | Manutenção afetando reserva iniciada |
| **RN-02 + RN-05 + RN-09** | Alta | Mudança de manutenção causando auditoria em cadeia |

---

### 4.7 Dados de Teste Não Definidos

| Dados | Questão | Status |
|---|---|---|
| **Sala** | Capacidade obrigatória? Localização? Equipamentos? | PENDENTE |
| **Professor** | Especialidade/competência? Identificação única? | PENDENTE |
| **Material** | Quantidade? Consumível OU reutilizável? | PENDENTE |
| **Usuário** | Email único? Email obrigatório? Múltiplos perfis? | PENDENTE |
| **Período** | Fuso horário? Dia útil/fim de semana? Feriados? | PENDENTE |

---

### 4.8 Requisitos Não-Funcionais Sem Testes Listados

| RNF | Descrição | Tipo de Teste | Status |
|---|---|---|---|
| **RNF-01** | Java 21 + Spring Boot 3.x | Integração (build/deploy) | Construído em CI/CD |
| **RNF-02** | 80% cobertura de linhas (JaCoCo) | Métrica de build | Validado em CI/CD |
| **RNF-03** | 70% cobertura de branches (JaCoCo) | Métrica de build | Validado em CI/CD |
| **RNF-07** | Testcontainers para persistência | Integração | Implícito em testes T-RN-0X |
| **RNF-09** | Teste concorrência automatizado | Concorrência | T-RN04-001 a T-RN04-008 |
| **RNF-12** | GitHub Actions em PRs | CI/CD | Fora do escopo de testes de domínio |
| **RNF-13** | SonarCloud | CI/CD | Fora do escopo de testes de domínio |
| **RNF-14** | JMeter | Performance | Plano separado de testes de carga |

---

## 5. Resumo Executivo

### 5.1 Cobertura

- **Total de RNs:** 10 (RN-01 a RN-10)
- **Total de RFs com testes listados:** 2 (RF-01, RF-10; escopo deste plano é crítico de reserva)
- **Total de casos de teste planejados:** 100+ (10 por RN/RF em média)
- **Camadas de teste:** Unitário, Integração, API, Concorrência, End-to-End

### 5.2 Bloqueadores para Implementação

| Bloqueador | Impacto | Prioridade |
|---|---|---|
| **Q-001: Estado inicial de reserva** | RN-07, RF-10 | CRÍTICA |
| **Q-003: Critério de recurso restrito** | RN-06, RF-14 | CRÍTICA |
| **Q-004: Responsabilidade de aprovação** | RN-06 | ALTA |
| **Q-006: NAO_COMPARECEU workflow** | RN-07 | ALTA |
| **Q-007/Q-008: Transições de estado** | RN-07 | ALTA |
| **Q-009: Mecanismo de concorrência RN-04** | RN-04 | CRÍTICA (+ experimento) |
| **Q-010: Relação professor-usuário/recurso** | RN-03 | ALTA |

### 5.3 Recomendações para Próximas Fases

1. **Validação de decisões:** Reunião com stakeholders para aprovar Q-001, Q-003, Q-004, Q-006, Q-007, Q-008, Q-009, Q-010.
2. **Definição de métricas:** Política de adjacência, duração mínima, granularidade de horário, campos de auditoria.
3. **Experimentação:** Testar mecanismos de concorrência (RN-04) antes de decisão final.
4. **Documentação de dados:** Definir estrutura completa de recursos, usuários e períodos.
5. **Rastreabilidade:** Atualizar matriz de requisitos-decisões-componentes-evidências após cada decisão.

---

## 6. Assinatura e Aprovação

| Papel | Nome | Data | Status |
|---|---|---|---|
| **Engenheiro QA** | Autônomo (Documento Preparado) | 2026-09-11 | Plano Preparado |
| **Product Manager** | — | — | PENDENTE |
| **Architect** | — | — | PENDENTE |
| **Scrum Master** | — | — | PENDENTE |
| **Desenvolvedor (Java/Spring)** | — | — | PENDENTE |

---

**Próximas ações:**
1. Revisar e validar este plano com o time.
2. Resolver bloqueadores (seção 5.2).
3. Iniciar ciclo de TDD com casos de teste de RN-01, RN-04, RN-06 (criticidade máxima).
4. Registrar ADRs para cada decisão de domínio resolvida.
5. Atualizar este plano após cada fase de aprovação/experimento.

---

## 7. Fase RED do TDD - Matriz regra → caso → teste → status

> **Execução:** 2026-09-25 · `mvn -o test` (Java 21, JUnit 5.10.0 / Surefire 3.1.2) na raiz do repositório.
> **Resultado da suíte completa:** 123 testes executados · 91 aprovados · **13 falhas (RED)** · 0 erros · 19 ignorados (`@Disabled`) · exit code 1.
> **Evidência:** relatórios de `target/surefire-reports/` gerados nesta execução (relatórios anteriores não foram usados).
> **Estado anterior (execução exploratória no início da tarefa, antes das correções):** 123 testes · 5 falhas · 8 erros; os 8 erros eram `ReservaTemporalException: Data no passado` em `ReservaCriacao_RF10_Test` (datas fixas em 2026-09-25 08:00, já passadas), ou seja, fixture inválida e não RED.

### 7.1 Convenções

- **Inventário:** 10 regras (RN-01 a RN-10) e 2 requisitos funcionais com casos (RF-01, RF-10), num total de **123 casos** (RN-04 tem 8 casos, os demais 10; RF-10 tem 15). Todos os identificadores são os do plano, sem alteração.
- **Nível dos testes:** todos os testes são JUnit 5 em nível de domínio (Java puro). O projeto não possui API HTTP, banco, Testcontainers nem infraestrutura de E2E, e nenhuma dependência foi adicionada. Exceções: os casos de concorrência (T-RN04-001/002/004/007 e T-RF10-009) usam threads liberadas por `CountDownLatch`, e os casos da RN-10 (camada Documental) leem os documentos do repositório. Camadas API/Integração/E2E/banco citadas na coluna "Camada(s)" dos casos **não foram exercitadas** (ver divergências em 7.12).
- **Status:** `RED_FALHA_ESPERADA` (falhou por asserção sobre comportamento previsto e ainda não implementado), `REGRA_JA_IMPLEMENTADA` (passou; comportamento já existe e confere com o plano), `TESTE_INCORRETO`, `BLOQUEADO_POR_LACUNA` (plano sem informação suficiente ou com alternativas "OU ... (PENDENTE)"; o teste ficou como placeholder `@Disabled` cujo corpo falha se habilitado, sem afirmar comportamento inventado). Não houve `FALHA_DE_COMPILACAO`, `FALHA_DE_INFRAESTRUTURA` nem `SEM_TESTE`.
- Em vários REDs a recusa **existe**, mas com mensagem diferente da citada literalmente na coluna "Resultado Esperado" do plano; esses casos estão marcados como "Divergência de mensagem" no motivo.

### 7.2 Matriz completa

| Regra | Caso | Comportamento esperado | Classe de teste | Método ou @DisplayName | Status | Motivo do status | Evidência da execução atual | Observação ou lacuna |
|---|---|---|---|---|---|---|---|---|
| RN-01 | **T-RN01-001** | Reserva aceita, estado SOLICITADA | `ReservaTemporal_RN01_Test` | T-RN01-001: Happy Path - Reserva com intervalo válido (08:00 a 09:00) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em Reserva e confere com o plano. | mvn test 2026-09-25: aprovado | O estado SOLICITADA é verificado em T-RF10-001; aqui só a validação temporal. |
| RN-01 | **T-RN01-002** | Reserva aceita | `ReservaTemporal_RN01_Test` | T-RN01-002: Happy Path - Intervalo longo (08:00 a 18:00) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em Reserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-01 | **T-RN01-003** | Reserva aceita ou recusada conforme política (PENDENTE) | `ReservaTemporal_RN01_Test` | T-RN01-003: Boundary - Intervalo mínimo (1 minuto) | **BLOQUEADO_POR_LACUNA** | plano não define a duração mínima (§4.3) - resultado 'Reserva aceita ou recusada conforme política (PENDENTE)' | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RN-01 | **T-RN01-004** | Recusada: "Duração inválida" | `ReservaTemporal_RN01_Test` | T-RN01-004: Boundary - Intervalo zero (início = fim) deve ser recusado | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em Reserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-01 | **T-RN01-005** | Recusada: "Fim anterior ao início" | `ReservaTemporal_RN01_Test` | T-RN01-005: Invalid Input - Fim anterior ao início (regressão temporal) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em Reserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-01 | **T-RN01-006** | Recusada: "Data no passado" | `ReservaTemporal_RN01_Test` | T-RN01-006: Invalid Input - Data passada | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em Reserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-01 | **T-RN01-007** | Recusada: "Início é obrigatório" | `ReservaTemporal_RN01_Test` | T-RN01-007: Invalid Input - Início nulo | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em Reserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-01 | **T-RN01-008** | Recusada: "Fim é obrigatório" | `ReservaTemporal_RN01_Test` | T-RN01-008: Invalid Input - Fim nulo | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em Reserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-01 | **T-RN01-009** | Recusada: "Formato de hora inválido" | `ReservaTemporal_RN01_Test` | T-RN01-009: Invalid Input - Formato de hora inválido | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em Reserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-01 | **T-RN01-010** | Recusada: "Reserva já iniciada não pode ser alterada" | `ReservaTemporal_RN01_Test` | T-RN01-010: Forbidden State - Alteração de reserva já iniciada | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em Reserva e confere com o plano. | mvn test 2026-09-25: aprovado | Teste usa estado EM_USO como "já iniciada"; o plano descreve o caso por horário (07:00-08:00 "começou"). |
| RN-02 | **T-RN02-001** | Reserva aceita | `ReservaSobreposicao_RN02_Test` | T-RN02-001: Happy Path - Reserva em sala sem conflito | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorSobreposicao / ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-02 | **T-RN02-002** | Ambas aceitas | `ReservaSobreposicao_RN02_Test` | T-RN02-002: Happy Path - Diferentes recursos no mesmo horário | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorSobreposicao / ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-02 | **T-RN02-003** | Recusada: "Recurso indisponível no período" | `ReservaSobreposicao_RN02_Test` | T-RN02-003: Conflicts - Sobreposição total (mesma hora) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorSobreposicao / ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-02 | **T-RN02-004** | Recusada: "Conflito de horário" | `ReservaSobreposicao_RN02_Test` | T-RN02-004: Conflicts - Sobreposição parcial (nova começa dentro) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorSobreposicao / ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-02 | **T-RN02-005** | Recusada: "Conflito de horário" | `ReservaSobreposicao_RN02_Test` | T-RN02-005: Conflicts - Sobreposição parcial (nova termina dentro) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorSobreposicao / ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-02 | **T-RN02-006** | Recusada: "Conflito de horário" | `ReservaSobreposicao_RN02_Test` | T-RN02-006: Conflicts - Sobreposição: nova envolve existente | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorSobreposicao / ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-02 | **T-RN02-007** | Aceita (sem sobreposição) OU Recusada conforme política (PENDENTE) | `ReservaSobreposicao_RN02_Test` | T-RN02-007: Boundary - Reservas adjacentes (fim = início) | **BLOQUEADO_POR_LACUNA** | política de adjacência indefinida (Q-002) - resultado 'Aceita OU Recusada conforme política (PENDENTE)' | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RN-02 | **T-RN02-008** | Recusada por RN-01 antes de verificar sobreposição | `ReservaSobreposicao_RN02_Test` | T-RN02-008: Boundary - Limite exato de coincidência recusada por RN-01 | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorSobreposicao / ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado | Teste reescrito: passa por ServicoCriacaoReserva com reserva existente registrada e datas futuras (a versão anterior usava data passada e falhava por "Data no passado", sem testar a ordem RN-01 antes da sobreposição). |
| RN-02 | **T-RN02-009** | Recusada: "Conflita com reserva 08:00-09:00" | `ReservaSobreposicao_RN02_Test` | T-RN02-009: Conflicts - Múltiplas existentes, conflita com uma | **RED_FALHA_ESPERADA** | A recusa ocorre, mas com a mensagem "Conflito de horário"; o plano exige "Conflita com reserva 08:00-09:00" (identificar a reserva conflitante). Divergência de mensagem. | mvn test 2026-09-25: FALHOU - Expecting throwable message: "Conflito de horário" to contain: "Conflita com reserva 08:00-09:00" but did not. Throwable that failed the check: com.or… |  |
| RN-02 | **T-RN02-010** | Recusada: "Alteração causaria sobreposição" | `ReservaSobreposicao_RN02_Test` | T-RN02-010: Forbidden State - Alterar existente para criar sobreposição | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorSobreposicao / ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado | Pré-condição do plano (A e B ambas 08:00-09:00) já violaria RN-02; o teste usa A 07:00-08:00 e B 08:00-09:00. |
| RN-03 | **T-RN03-001** | Reserva aceita | `ReservaAgendaProfessor_RN03_Test` | T-RN03-001: Happy Path - Professor sem conflito de agenda | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAgendaProfessor e confere com o plano. | mvn test 2026-09-25: aprovado | Entrada corrigida para Seg 10:00-11:00 (a versão anterior usava Seg 10:00 a Ter 09:00). |
| RN-03 | **T-RN03-002** | Reserva aceita | `ReservaAgendaProfessor_RN03_Test` | T-RN03-002: Happy Path - Professor sem agenda registrada | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAgendaProfessor e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-03 | **T-RN03-003** | Recusada: "Professor indisponível" | `ReservaAgendaProfessor_RN03_Test` | T-RN03-003: Conflicts - Sobreposição com agenda do professor | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAgendaProfessor e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-03 | **T-RN03-004** | Recusada: "Professor B indisponível" | `ReservaAgendaProfessor_RN03_Test` | T-RN03-004: Conflicts - Múltiplos professores, um com conflito | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAgendaProfessor e confere com o plano. | mvn test 2026-09-25: aprovado | Fixture completada com o Prof C. A mensagem "Professor B" é derivada do nome "Prof B" na implementação. |
| RN-03 | **T-RN03-005** | Recusada: "Alteração causaria conflito com agenda" | `ReservaAgendaProfessor_RN03_Test` | T-RN03-005: Conflicts - Alterar reserva criando conflito de agenda | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAgendaProfessor e confere com o plano. | mvn test 2026-09-25: aprovado | Fixture ajustada: reserva existente do Prof X na Ter 10:00-11:00. |
| RN-03 | **T-RN03-006** | Aceita OU Recusada conforme política de adjacência (PENDENTE) | `ReservaAgendaProfessor_RN03_Test` | T-RN03-006: Boundary - Professor com agenda até o minuto exato de início | **BLOQUEADO_POR_LACUNA** | política de adjacência indefinida (Q-002) - resultado 'Aceita OU Recusada conforme política de adjacência (PENDENTE)' | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RN-03 | **T-RN03-007** | Recusada: "Professor não encontrado" | `ReservaAgendaProfessor_RN03_Test` | T-RN03-007: Invalid Input - Professor inexistente | **RED_FALHA_ESPERADA** | A recusa ocorre (IllegalArgumentException), mas com a mensagem "Professor não identificado"; o plano exige "Professor não encontrado". Divergência de mensagem. | mvn test 2026-09-25: FALHOU - Expecting throwable message: "Professor não identificado" to contain: "Professor não encontrado" but did not. Throwable that failed the check: java.la… |  |
| RN-03 | **T-RN03-008** | Sistema trata como "sem agenda" OU recusa | `ReservaAgendaProfessor_RN03_Test` | T-RN03-008: Invalid Input - Agenda com formato inválido | **BLOQUEADO_POR_LACUNA** | plano admite dois comportamentos incompatíveis - 'trata como sem agenda OU recusa' | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RN-03 | **T-RN03-009** | Recusada conforme política | `ReservaAgendaProfessor_RN03_Test` | T-RN03-009: Forbidden State - Tentar sobrepor agenda de professor já alocado | **BLOQUEADO_POR_LACUNA** | resultado esperado 'Recusada conforme política' sem política definida no plano | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RN-03 | **T-RN03-010** | Ambas aceitas (dias diferentes) | `ReservaAgendaProfessor_RN03_Test` | T-RN03-010: Conflicts - Professor simultaneamente em duas reservas (dias diferentes) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAgendaProfessor e confere com o plano. | mvn test 2026-09-25: aprovado | Teste reescrito: Prof X sem agenda prévia; Seg 08:00-09:00 aceita e, depois de ocupar a agenda, Ter 08:00-09:00 também é aceita. |
| RN-04 | **T-RN04-001** | Exatamente 1 aceita (status SOLICITADA), 1 recusada (conflito) | `ReservaUnicidadeConcorrencia_RN04_Test` | T-RN04-001: Conflicts - Dupla simultânea: apenas uma deve ser aceita | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorConcorrencia e confere com o plano. | mvn test 2026-09-25: aprovado | Reescrito com threads liberadas por CountDownLatch (a versão anterior era sequencial). Verifica aceita/recusada por boolean; status SOLICITADA e banco não são exercitados. ValidadorConcorrencia arbitra somente períodos idênticos. |
| RN-04 | **T-RN04-002** | Exatamente 1 aceita, 2 recusadas | `ReservaUnicidadeConcorrencia_RN04_Test` | T-RN04-002: Conflicts - Tripla simultânea: exatamente 1 aceita, 2 recusadas | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorConcorrencia e confere com o plano. | mvn test 2026-09-25: aprovado | Reescrito com 3 threads simultâneas (versão anterior sequencial). |
| RN-04 | **T-RN04-003** | Request 1 aceita, Request 2 recusada por sobreposição (RN-02) | `ReservaUnicidadeConcorrencia_RN04_Test` | T-RN04-003: Happy Path - Sequencial (não simultâneo) - segunda recusada por sobreposição | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorConcorrencia e confere com o plano. | mvn test 2026-09-25: aprovado | Verifica a recusa por RN-02 na requisição posterior. |
| RN-04 | **T-RN04-004** | Ambas aceitas (recursos diferentes) | `ReservaUnicidadeConcorrencia_RN04_Test` | T-RN04-004: Conflicts - Dupla em recursos diferentes - ambas aceitas | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorConcorrencia e confere com o plano. | mvn test 2026-09-25: aprovado | Reescrito com 2 threads simultâneas (versão anterior sequencial). |
| RN-04 | **T-RN04-005** | Ambas aceitas OU conforme política de adjacência (PENDENTE) | `ReservaUnicidadeConcorrencia_RN04_Test` | T-RN04-005: Conflicts - Dupla em períodos adjacentes simultâneas | **BLOQUEADO_POR_LACUNA** | política de adjacência indefinida (Q-002) - resultado 'Ambas aceitas OU conforme política de adjacência (PENDENTE)' | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RN-04 | **T-RN04-006** | Request 1 aguarda aprovação (SOLICITADA com approval_required=true), Request 2 pode ser aceita conforme status | `ReservaUnicidadeConcorrencia_RN04_Test` | T-RN04-006: Conflicts - Dupla: um com recurso restrito, um sem | **BLOQUEADO_POR_LACUNA** | resultado 'Request 2 pode ser aceita conforme status' e 'Prof restrito' sem critério de recurso restrito (Q-003) | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RN-04 | **T-RN04-007** | Base de dados reflete apenas 1 reserva confirmada; Request 2 recebeu erro claro | `ReservaUnicidadeConcorrencia_RN04_Test` | T-RN04-007: Conflicts - Garantir consistência após aceitar uma | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorConcorrencia e confere com o plano. | mvn test 2026-09-25: aprovado | Reescrito (a versão anterior apenas afirmava o retorno de um helper do próprio validador). "Erro claro" da requisição recusada e leitura no banco não são verificáveis: a API devolve boolean. |
| RN-04 | **T-RN04-008** | Auditoria em RN-09 traz timestamp e sequência clara (PENDENTE: política de timestamp) | `ReservaUnicidadeConcorrencia_RN04_Test` | T-RN04-008: Conflicts - Sem race condition em auditoria | **BLOQUEADO_POR_LACUNA** | 'PENDENTE: política de timestamp' (§4.2) - ordem/sequência da auditoria sob concorrência não definida | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RN-05 | **T-RN05-001** | Aceita | `ReservaManutencao_RN05_Test` | T-RN05-001: Happy Path - Reserva em recurso sem manutenção | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorManutencao e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-05 | **T-RN05-002** | Aceita | `ReservaManutencao_RN05_Test` | T-RN05-002: Happy Path - Reserva fora do período de manutenção | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorManutencao e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-05 | **T-RN05-003** | Recusada: "Recurso em manutenção" | `ReservaManutencao_RN05_Test` | T-RN05-003: Conflicts - Reserva em recurso em manutenção (período total) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorManutencao e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-05 | **T-RN05-004** | Recusada: "Recurso indisponível no período" | `ReservaManutencao_RN05_Test` | T-RN05-004: Conflicts - Reserva com manutenção parcial | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorManutencao e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-05 | **T-RN05-005** | Recusada por primeira manutenção | `ReservaManutencao_RN05_Test` | T-RN05-005: Conflicts - Múltiplos períodos de manutenção | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorManutencao e confere com o plano. | mvn test 2026-09-25: aprovado | Assere somente o tipo da exceção (o plano não define a mensagem). |
| RN-05 | **T-RN05-006** | Recusada: "Material indisponível" | `ReservaManutencao_RN05_Test` | T-RN05-006: Conflicts - Material em manutenção | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorManutencao e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-05 | **T-RN05-007** | Recusada: "Sala em manutenção neste período" | `ReservaManutencao_RN05_Test` | T-RN05-007: Forbidden State - Alterar reserva introduzindo manutenção | **BLOQUEADO_POR_LACUNA** | plano não define a alteração tentada ('tentar alterar duramente') nem a política para reservas afetadas por manutenção posterior (Q-005) | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RN-05 | **T-RN05-008** | Sala A retorna como indisponível ou não aparece nos resultados | `ReservaManutencao_RN05_Test` | T-RN05-008: Conflicts - Pesquisa de disponibilidade exclui manutenção | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorManutencao e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-05 | **T-RN05-009** | Recusada conforme RN-01 | `ReservaManutencao_RN05_Test` | T-RN05-009: Invalid Input - Bloqueio/manutenção com período inválido | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorManutencao e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-05 | **T-RN05-010** | Aceita OU Recusada conforme política de adjacência (PENDENTE) | `ReservaManutencao_RN05_Test` | T-RN05-010: Boundary - Manutenção adjacente à reserva | **BLOQUEADO_POR_LACUNA** | política de adjacência indefinida (Q-002) - resultado 'Aceita OU Recusada conforme política de adjacência (PENDENTE)' | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RN-06 | **T-RN06-001** | Aceita direto, estado SOLICITADA (não aguarda aprovação) | `ReservaAprovacao_RN06_Test` | T-RN06-001: Happy Path - Recurso comum aceito direto | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAprovacao / ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado | Reescrito via ServicoCriacaoReserva (estado SOLICITADA e approval_required=false). Depende de Q-001 (estado inicial de recurso não restrito). |
| RN-06 | **T-RN06-002** | Estado SOLICITADA → APROVADA após aprovação Responsável | `ReservaAprovacao_RN06_Test` | T-RN06-002: Happy Path - Recurso restrito aguarda aprovação | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAprovacao / ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado | Reescrito: cria via serviço (SOLICITADA, approval_required=true) e o Responsável aprova (APROVADA). O critério de recurso restrito (Q-003) segue pendente. |
| RN-06 | **T-RN06-003** | Estado SOLICITADA → REJEITADA com motivo | `ReservaAprovacao_RN06_Test` | T-RN06-003: Happy Path - Recurso restrito: Responsável rejeita | **RED_FALHA_ESPERADA** | O estado passa a REJEITADA, mas o motivo informado não é armazenado (getMotivoRejeicao() = null). Comportamento "com motivo" não implementado. | mvn test 2026-09-25: FALHOU - expected: "Horário reservado para evento institucional" but was: null |  |
| RN-06 | **T-RN06-004** | Recusada: "Acesso negado. Apenas Responsável pode aprovar" | `ReservaAprovacao_RN06_Test` | T-RN06-004: Forbidden State - Solicitante não pode aprovar | **RED_FALHA_ESPERADA** | A recusa ocorre, mas a mensagem é "Apenas Responsável pode aprovar", sem o prefixo "Acesso negado." exigido pelo plano. Divergência de mensagem. | mvn test 2026-09-25: FALHOU - Expecting throwable message: "Apenas Responsável pode aprovar" to contain: "Acesso negado. Apenas Responsável pode aprovar" but did not. Throwable tha… |  |
| RN-06 | **T-RN06-005** | Recusada: "Acesso negado. Apenas Responsável pode aprovar" | `ReservaAprovacao_RN06_Test` | T-RN06-005: Forbidden State - Administrador também não pode aprovar | **RED_FALHA_ESPERADA** | A recusa ocorre, mas a mensagem é "Apenas Responsável pode aprovar", sem o prefixo "Acesso negado." exigido pelo plano. Divergência de mensagem. | mvn test 2026-09-25: FALHOU - Expecting throwable message: "Apenas Responsável pode aprovar" to contain: "Acesso negado. Apenas Responsável pode aprovar" but did not. Throwable tha… |  |
| RN-06 | **T-RN06-006** | Recusada: "Recurso indisponível no período" OU aceita com status de alerta (PENDENTE) | `ReservaAprovacao_RN06_Test` | T-RN06-006: Conflicts - Responsável aprova recurso que ficou indisponível | **BLOQUEADO_POR_LACUNA** | resultado 'Recusada OU aceita com status de alerta (PENDENTE)' - política para recurso que ficou em manutenção (Q-005) | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RN-06 | **T-RN06-007** | Exatamente 1 aprovada conforme RN-04 + RN-06 integrados | `ReservaAprovacao_RN06_Test` | T-RN06-007: Conflicts - Responsável aprova duplicada (duas simultâneas) | **RED_FALHA_ESPERADA** | aprovar() aprova as duas solicitações do mesmo recurso/período (2 APROVADA); o plano exige exatamente 1 aprovada. | mvn test 2026-09-25: FALHOU - expected: 1L but was: 2L |  |
| RN-06 | **T-RN06-008** | Recusada: "Recurso não encontrado" | `ReservaAprovacao_RN06_Test` | T-RN06-008: Invalid Input - Recurso inexistente marcado como restrito | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAprovacao / ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado | Reescrito para tentar aprovar (aprovar) uma reserva sem recurso, e não chamar o helper de validação diretamente. |
| RN-06 | **T-RN06-009** | Recusada: "Recurso fora de sua responsabilidade" (se implementado) OU aceita (PENDENTE de decisão) | `ReservaAprovacao_RN06_Test` | T-RN06-009: Boundary - Responsável autorizado para recurso A tenta aprovar recurso B | **BLOQUEADO_POR_LACUNA** | 'Recusada (se implementado) OU aceita (PENDENTE de decisão)' - responsabilidade do Responsável por recurso (Q-004) | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RN-06 | **T-RN06-010** | Recusada: "Solicitação já foi aprovada" | `ReservaAprovacao_RN06_Test` | T-RN06-010: Forbidden State - Tentar aprovar recurso já aprovado | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAprovacao / ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado | Reescrito para o Responsável tentar aprovar (aprovar) reserva já APROVADA. |
| RN-07 | **T-RN07-001** | SOLICITADA → APROVADA → EM_USO → CONCLUIDA | `ReservaFluxoEstados_RN07_Test` | T-RN07-001: Happy Path - Fluxo completo | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorFluxoEstados e confere com o plano. | mvn test 2026-09-25: aprovado | Transições APROVADA -> EM_USO e EM_USO -> CONCLUIDA têm atores indefinidos (Q-007, Q-008); o teste valida apenas a sequência de estados. |
| RN-07 | **T-RN07-002** | SOLICITADA → CANCELADA com auditoria (RN-09) | `ReservaFluxoEstados_RN07_Test` | T-RN07-002: Happy Path - Cancelamento em SOLICITADA | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorFluxoEstados e confere com o plano. | mvn test 2026-09-25: aprovado | A auditoria (RN-09) da transição não é verificada: não há componente que una transição de estado e auditoria. |
| RN-07 | **T-RN07-003** | SOLICITADA → REJEITADA com auditoria | `ReservaFluxoEstados_RN07_Test` | T-RN07-003: Happy Path - Rejeição em SOLICITADA | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorFluxoEstados e confere com o plano. | mvn test 2026-09-25: aprovado | A auditoria (RN-09) da transição não é verificada: não há componente que una transição de estado e auditoria. |
| RN-07 | **T-RN07-004** | Recusada: "Transição não permitida" | `ReservaFluxoEstados_RN07_Test` | T-RN07-004: Forbidden State - Transição não especificada | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorFluxoEstados e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-07 | **T-RN07-005** | Recusada: "Estado inválido" | `ReservaFluxoEstados_RN07_Test` | T-RN07-005: Forbidden State - Estado inválido | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorFluxoEstados e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-07 | **T-RN07-006** | Recusada conforme RN-08: "Reserva iniciada não pode ser cancelada" | `ReservaFluxoEstados_RN07_Test` | T-RN07-006: Forbidden State - Cancelar reserva iniciada | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorFluxoEstados e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-07 | **T-RN07-007** | Recusada: "Transição não permitida" OU fatura de sistema se ofertar EM_USO antes de iniciar (PENDENTE) | `ReservaFluxoEstados_RN07_Test` | T-RN07-007: Conflicts - Rejeitar após APROVADA (transição retrógrada) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorFluxoEstados e confere com o plano. | mvn test 2026-09-25: aprovado | Cobre a alternativa "Transição não permitida" (coerente com o diagrama); a alternativa "OU ... (PENDENTE)" do plano não foi coberta. |
| RN-07 | **T-RN07-008** | Aceita; timestamp de auditoria registra transição (RN-09) | `ReservaFluxoEstados_RN07_Test` | T-RN07-008: Boundary - Transição na fronteira de tempo | **TESTE_INCORRETO** | O teste anterior só validava SOLICITADA -> APROVADA e não alcançava a fronteira 23:59:59 -> 00:00:00 nem a auditoria com timestamp. Reescrever exige injetar o instante da transição (relógio/timestamp injetável), contrato inexistente. Teste desabilitado; pendente. | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RN-07 | **T-RN07-009** | Recusada: "Estado é obrigatório" | `ReservaFluxoEstados_RN07_Test` | T-RN07-009: Invalid Input - Estado null ou vazio | **RED_FALHA_ESPERADA** | A recusa ocorre, mas a mensagem é "Estado inválido"; o plano exige "Estado é obrigatório" para null/vazio. Divergência de mensagem. | mvn test 2026-09-25: FALHOU - Expecting throwable message: "Estado inválido" to contain: "Estado é obrigatório" but did not. Throwable that failed the check: com.organizacao_de_rec… |  |
| RN-07 | **T-RN07-010** | Comportamento indefinido: PENDENTE DE DECISÃO (qual ator? qual origem?) | `ReservaFluxoEstados_RN07_Test` | T-RN07-010: Forbidden State - NAO_COMPARECEU sem origem definida | **BLOQUEADO_POR_LACUNA** | origem, ator e condição de NAO_COMPARECEU indefinidos (Q-006) | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RN-08 | **T-RN08-001** | Transição para CANCELADA permitida (RN-07) | `ReservaApagamento_RN08_Test` | T-RN08-001: Happy Path - Cancelar antes de iniciar | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorApagamento / ValidadorAuditoria e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-08 | **T-RN08-002** | Recusada: "Reserva em uso não pode ser cancelada" | `ReservaApagamento_RN08_Test` | T-RN08-002: Forbidden State - Cancelar após iniciação | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorApagamento / ValidadorAuditoria e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-08 | **T-RN08-003** | Recusada: "Operação não permitida para reserva concluída" | `ReservaApagamento_RN08_Test` | T-RN08-003: Forbidden State - Cancelar após conclusão | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorApagamento / ValidadorAuditoria e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-08 | **T-RN08-004** | Recusada: "Registros de reserva iniciada não podem ser removidos" | `ReservaApagamento_RN08_Test` | T-RN08-004: Forbidden State - Apagar registro (DELETE) de reserva iniciada | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorApagamento / ValidadorAuditoria e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-08 | **T-RN08-005** | Recusada OU suporta transição conforme política de encerramento (PENDENTE) | `ReservaApagamento_RN08_Test` | T-RN08-005: Forbidden State - Alterar de EM_USO para CANCELADA | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorApagamento / ValidadorAuditoria e confere com o plano. | mvn test 2026-09-25: aprovado | Cobre a recusa (coerente com a interpretação de RN-08); a alternativa "suporta transição conforme política (PENDENTE)" não foi coberta. |
| RN-08 | **T-RN08-006** | Recusada: "Reserva já iniciada" | `ReservaApagamento_RN08_Test` | T-RN08-006: Boundary - Alteração imediatamente após iniciação | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorApagamento / ValidadorAuditoria e confere com o plano. | mvn test 2026-09-25: aprovado | Ajustado para reserva com início e cancelamento 1 s depois. |
| RN-08 | **T-RN08-007** | Recusada: "ID inválido" | `ReservaApagamento_RN08_Test` | T-RN08-007: Invalid Input - Tentar apagar com ID inválido | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorApagamento / ValidadorAuditoria e confere com o plano. | mvn test 2026-09-25: aprovado | Cobre ID nulo e negativo; "string" não é representável em Long. |
| RN-08 | **T-RN08-008** | Auditoria registra: usuário, timestamp, tentativa, resultado (PENDENTE: detalhe de política) | `ReservaApagamento_RN08_Test` | T-RN08-008: Conflicts - Garantir auditoria de tentativa de apagamento | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorApagamento / ValidadorAuditoria e confere com o plano. | mvn test 2026-09-25: aprovado | Reescrito: verifica que a tentativa ficou registrada (usuário, timestamp); o "resultado" do registro segue pendente no plano. |
| RN-08 | **T-RN08-009** | Recusada conforme RN-08, mesmo Admin não pode contornar | `ReservaApagamento_RN08_Test` | T-RN08-009: Forbidden State - Admin não pode contornar proteção (CONCLUIDA ou EM_USO) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorApagamento / ValidadorAuditoria e confere com o plano. | mvn test 2026-09-25: aprovado | Estendido para CONCLUIDA e EM_USO. |
| RN-08 | **T-RN08-010** | Recusada: "Transição não permitida" OU "Reserva já iniciada não pode ser alterada" | `ReservaApagamento_RN08_Test` | T-RN08-010: Forbidden State - Rejeitar após iniciada | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorApagamento / ValidadorAuditoria e confere com o plano. | mvn test 2026-09-25: aprovado | Aceita as duas mensagens permitidas pelo plano. |
| RN-09 | **T-RN09-001** | Auditoria criada com: usuário=Solicitante, ação=CRIAR, estado_novo=SOLICITADA, timestamp | `ReservaAuditoria_RN09_Test` | T-RN09-001: Happy Path - Criar reserva gera auditoria | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAuditoria e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-09 | **T-RN09-002** | Auditoria com: usuário=Responsável, ação=APROVAR, estado_anterior=SOLICITADA, estado_novo=APROVADA, timestamp | `ReservaAuditoria_RN09_Test` | T-RN09-002: Happy Path - Transição de estado gera auditoria | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAuditoria e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-09 | **T-RN09-003** | Histórico lista todas 5 mudanças em ordem cronológica com ator e timestamp | `ReservaAuditoria_RN09_Test` | T-RN09-003: Happy Path - Consultar histórico de reserva | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAuditoria e confere com o plano. | mvn test 2026-09-25: aprovado | Reescrito com 5 mudanças de estado, ator, timestamp e ordem cronológica. |
| RN-09 | **T-RN09-004** | Auditoria preserva ordem com timestamp granular (ms?) | `ReservaAuditoria_RN09_Test` | T-RN09-004: Happy Path - Múltiplas mudanças em rápida sucessão | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAuditoria e confere com o plano. | mvn test 2026-09-25: aprovado | Reescrito para verificar a ordem dos estados e timestamps não decrescentes (antes só contava 4 registros). |
| RN-09 | **T-RN09-005** | Auditoria registra erro: "Usuário não identificado" OU recusa operação antes | `ReservaAuditoria_RN09_Test` | T-RN09-005: Invalid Input - Operação sem autenticação (sem usuário) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAuditoria e confere com o plano. | mvn test 2026-09-25: aprovado | Cobre a recusa ("Usuário não identificado"); a alternativa "registra erro" não foi coberta. |
| RN-09 | **T-RN09-006** | Recusada: "Auditoria é imutável" (se implementado) OU sem permissão | `ReservaAuditoria_RN09_Test` | T-RN09-006: Forbidden State - Tentar editar auditoria | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAuditoria e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-09 | **T-RN09-007** | Recusada: "Auditoria não pode ser removida" | `ReservaAuditoria_RN09_Test` | T-RN09-007: Forbidden State - Tentar apagar auditoria | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAuditoria e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RN-09 | **T-RN09-008** | Comportamento pendente: gera auditoria de tentativa OU não (PENDENTE) | `ReservaAuditoria_RN09_Test` | T-RN09-008: Conflicts - Operação recusada gera auditoria de tentativa | **BLOQUEADO_POR_LACUNA** | plano deixa pendente se a operação recusada gera auditoria de tentativa OU não (PENDENTE) | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RN-09 | **T-RN09-009** | Sistema usa mecanismo de desempate (sequência, nanosegundos) OU cria ambiguidade (PENDENTE) | `ReservaAuditoria_RN09_Test` | T-RN09-009: Boundary - Auditoria com timestamp granular preserva ordenação | **BLOQUEADO_POR_LACUNA** | 'mecanismo de desempate OU ambiguidade (PENDENTE)' - política de timestamp/precisão indefinida (§4.2) | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RN-09 | **T-RN09-010** | Auditoria registra tentativa de apagamento e resultado (REJEITADO) | `ReservaAuditoria_RN09_Test` | T-RN09-010: Boundary - Auditoria após tentativa de apagamento proibido | **RED_FALHA_ESPERADA** | A tentativa é registrada (ação APAGAR), mas o registro traz "RECUSADA"/"Operação não permitida" e não expressa o resultado REJEITADO exigido pelo plano. | mvn test 2026-09-25: FALHOU - Expecting actual: "APAGAR RECUSADA Operação não permitida" to contain: "REJEITADO" |  |
| RN-10 | **T-RN10-001** | Todos os 10 RNs mapeados; todos os RFs MUST mapeados | `ReservaRastreabilidade_RN10_Test` | T-RN10-001: Happy Path - Matriz de rastreabilidade deve incluir todas as RNs | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em os documentos do repositório (docs/prd.md, docs/arquitetura.md, docs/testes/plano-tdd.md, docs/relatorio-validacao-arquitetura.md) e confere com o plano. | mvn test 2026-09-25: aprovado | Teste documental: lê a matriz do PRD (§9) e arquitetura.md §38. Substitui a versão anterior, que validava um stub com retornos fixos. |
| RN-10 | **T-RN10-002** | Ligação presente: RN-01 → RF-10, RF-11 | `ReservaRastreabilidade_RN10_Test` | T-RN10-002: Happy Path - RN-01 deve estar ligada a RF-10 e RF-11 | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em os documentos do repositório (docs/prd.md, docs/arquitetura.md, docs/testes/plano-tdd.md, docs/relatorio-validacao-arquitetura.md) e confere com o plano. | mvn test 2026-09-25: aprovado | Teste documental (PRD §9). |
| RN-10 | **T-RN10-003** | Cada RF MUST tem casos de teste em RN-0X e T-RF-0X (ver seção 3) | `ReservaRastreabilidade_RN10_Test` | T-RN10-003: Happy Path - Cada RF MUST deve ter casos de teste | **RED_FALHA_ESPERADA** | 21 dos 23 RFs MUST não têm casos T-RFnn no plano (só RF-01 e RF-10); lacuna já reconhecida em §4.4. Falha documental. | mvn test 2026-09-25: FALHOU - [RFs MUST sem casos de teste T-RFnn no plano] Expecting empty but was: ["RF-02", "RF-03", "RF-04", "RF-05", "RF-06", "RF-07", "RF-08", "RF-09", "RF-11… |  |
| RN-10 | **T-RN10-004** | Identificado como lacuna; não deve ocorrer | `ReservaRastreabilidade_RN10_Test` | T-RN10-004: Invalid Input - Requisito não rastreado identificado como lacuna | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em os documentos do repositório (docs/prd.md, docs/arquitetura.md, docs/testes/plano-tdd.md, docs/relatorio-validacao-arquitetura.md) e confere com o plano. | mvn test 2026-09-25: aprovado | Teste documental (PRD §7 x §9). |
| RN-10 | **T-RN10-005** | Identificado como erro de documentação | `ReservaRastreabilidade_RN10_Test` | T-RN10-005: Conflicts - Orfandade (teste sem requisito identificado como erro) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em os documentos do repositório (docs/prd.md, docs/arquitetura.md, docs/testes/plano-tdd.md, docs/relatorio-validacao-arquitetura.md) e confere com o plano. | mvn test 2026-09-25: aprovado | Teste documental: cruza @DisplayName em src/test/java com os casos do plano. |
| RN-10 | **T-RN10-006** | Ligações múltiplas permitidas; importante não há ciclo | `ReservaRastreabilidade_RN10_Test` | T-RN10-006: Boundary - Requisito rastreado a múltiplos requisitos sem ciclo | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em os documentos do repositório (docs/prd.md, docs/arquitetura.md, docs/testes/plano-tdd.md, docs/relatorio-validacao-arquitetura.md) e confere com o plano. | mvn test 2026-09-25: aprovado | Teste documental (PRD §9); "sem ciclo" verificado pela coluna de regras conter apenas RNs. |
| RN-10 | **T-RN10-007** | 100% mapeados conforme PRD/Arquitetura | `ReservaRastreabilidade_RN10_Test` | T-RN10-007: Happy Path - Meta de cobertura - 100% das RNs e RFs MUST mapeados | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em os documentos do repositório (docs/prd.md, docs/arquitetura.md, docs/testes/plano-tdd.md, docs/relatorio-validacao-arquitetura.md) e confere com o plano. | mvn test 2026-09-25: aprovado | Teste documental (PRD §9). |
| RN-10 | **T-RN10-008** | Deve ser escalado; jamais deve ocorrer | `ReservaRastreabilidade_RN10_Test` | T-RN10-008: Invalid Input - Requisito crítico não testado deve ser escalado | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em os documentos do repositório (docs/prd.md, docs/arquitetura.md, docs/testes/plano-tdd.md, docs/relatorio-validacao-arquitetura.md) e confere com o plano. | mvn test 2026-09-25: aprovado | Teste documental: verifica a coluna de evidência esperada da matriz do PRD. |
| RN-10 | **T-RN10-009** | Registrados em `docs/relatorio-validacao-arquitetura.md` | `ReservaRastreabilidade_RN10_Test` | T-RN10-009: Happy Path - Divergências e aceites registrados | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em os documentos do repositório (docs/prd.md, docs/arquitetura.md, docs/testes/plano-tdd.md, docs/relatorio-validacao-arquitetura.md) e confere com o plano. | mvn test 2026-09-25: aprovado | Teste documental (relatorio-validacao-arquitetura.md §16). |
| RN-10 | **T-RN10-010** | Matriz é atualizada em paralelo à implementação (TDD) | `ReservaRastreabilidade_RN10_Test` | T-RN10-010: Boundary - Matriz atualizada após mudança | **BLOQUEADO_POR_LACUNA** | 'matriz atualizada em paralelo à implementação (TDD)' é regra de processo sem entrada/estímulo verificável definidos no plano | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RF-01 | **T-RF01-001** | Retorna recursos e disponibilidade | `ReservaAutenticacaoAutorizacao_RF01_Test` | T-RF01-001: Happy Path - Solicitante autenticado consulta disponibilidade | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAutorizacao e confere com o plano. | mvn test 2026-09-25: aprovado | Verifica somente a permissão (RF-01); o conteúdo devolvido ("recursos e disponibilidade") não tem contrato/dados no plano (consultarDisponibilidade retorna lista vazia). |
| RF-01 | **T-RF01-002** | Aprova e gera auditoria (RN-09) | `ReservaAutenticacaoAutorizacao_RF01_Test` | T-RF01-002: Happy Path - Responsável autenticado aprova solicitação | **RED_FALHA_ESPERADA** | aprovarSolicitacao() só valida a permissão: a reserva continua SOLICITADA e nenhuma auditoria é gerada (plano: aprova e gera auditoria RN-09). | mvn test 2026-09-25: FALHOU - Multiple Failures (2 failures) -- failure 1 -- [estado da reserva após aprovação] expected: "APROVADA" but was: "SOLICITADA" at ReservaAutenticacaoAut… |  |
| RF-01 | **T-RF01-003** | Cria recurso e torna disponível | `ReservaAutenticacaoAutorizacao_RF01_Test` | T-RF01-003: Happy Path - Administrador autenticado gerencia recurso | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAutorizacao e confere com o plano. | mvn test 2026-09-25: aprovado | Verifica somente a permissão (RF-01); "cria recurso e torna disponível" não tem contrato no plano. |
| RF-01 | **T-RF01-004** | Recusada: "Acesso negado" | `ReservaAutenticacaoAutorizacao_RF01_Test` | T-RF01-004: Forbidden State - Solicitante tenta aprovar (sem permissão) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAutorizacao e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RF-01 | **T-RF01-005** | Recusada: "Acesso negado" | `ReservaAutenticacaoAutorizacao_RF01_Test` | T-RF01-005: Forbidden State - Responsável tenta gerenciar usuários (sem permissão) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAutorizacao e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RF-01 | **T-RF01-006** | Recusada: "Autenticação inválida" OU redirecionado para login | `ReservaAutenticacaoAutorizacao_RF01_Test` | T-RF01-006: Invalid Input - Token inválido ou expirado | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAutorizacao e confere com o plano. | mvn test 2026-09-25: aprovado | Reescrito: a versão anterior lançava a exceção dentro do próprio teste. A mensagem "Autenticação inválida" não é verificável (validarToken devolve boolean e aceita qualquer "Bearer x"). |
| RF-01 | **T-RF01-007** | Recusada: "Token obrigatório" OU erro 401 | `ReservaAutenticacaoAutorizacao_RF01_Test` | T-RF01-007: Invalid Input - Sem token | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAutorizacao e confere com o plano. | mvn test 2026-09-25: aprovado | Reescrito: a versão anterior lançava a exceção dentro do próprio teste. A mensagem "Token obrigatório" não é verificável (validarToken devolve boolean). |
| RF-01 | **T-RF01-008** | Comportamento: qual perfil é usado? Prioridade? PENDENTE DE DECISÃO | `ReservaAutenticacaoAutorizacao_RF01_Test` | T-RF01-008: Forbidden State - Usuário com múltiplos perfis validação de prioridade | **BLOQUEADO_POR_LACUNA** | perfil prioritário para usuário com múltiplos perfis 'PENDENTE DE DECISÃO' (plano) | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RF-01 | **T-RF01-009** | Permissions mudam conforme novo perfil | `ReservaAutenticacaoAutorizacao_RF01_Test` | T-RF01-009: Boundary - Transição de perfil atualiza permissões | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAutorizacao e confere com o plano. | mvn test 2026-09-25: aprovado | DisplayName corrigido (estava "T-RN01-009 / T-RF01-009"); cenário reescrito com dois perfis do mesmo usuário. |
| RF-01 | **T-RF01-010** | Recusada: "Usuário inativo" OU "Acesso negado" | `ReservaAutenticacaoAutorizacao_RF01_Test` | T-RF01-010: Forbidden State - Usuário desativado tenta acessar | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ValidadorAutorizacao e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-001** | Aceita, SOLICITADA | `ReservaCriacao_RF10_Test` | T-RF10-001: Happy Path - Criar reserva simples (sala comum) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado | Datas relativas ao instante atual. Depende de Q-001. |
| RF-10 | **T-RF10-002** | Aceita, SOLICITADA | `ReservaCriacao_RF10_Test` | T-RF10-002: Happy Path - Criar com múltiplos recursos (sala + material) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-003** | Aceita, SOLICITADA | `ReservaCriacao_RF10_Test` | T-RF10-003: Happy Path - Criar com professor | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-004** | SOLICITADA, approval_required=true | `ReservaCriacao_RF10_Test` | T-RF10-004: Happy Path - Recurso restrito aguarda aprovação | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-005** | Recusada: conflito em sala | `ReservaCriacao_RF10_Test` | T-RF10-005: Conflicts - Sobreposição em sala deve recusar | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado | Reescrito: reserva existente criada pelo serviço (a versão anterior não registrava a existente). |
| RF-10 | **T-RF10-006** | Recusada: conflito em material | `ReservaCriacao_RF10_Test` | T-RF10-006: Conflicts - Sobreposição em material deve recusar | **RED_FALHA_ESPERADA** | ServicoCriacaoReserva só verifica sobreposição do recurso principal; o material anexado à reserva não é verificado, e a nova reserva é aceita sem lançar exceção. | mvn test 2026-09-25: FALHOU - Expecting code to raise a throwable. |  |
| RF-10 | **T-RF10-007** | Recusada: conflito professor (RN-03) | `ReservaCriacao_RF10_Test` | T-RF10-007: Conflicts - Sobreposição em professor deve recusar (RN-03) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado | Datas relativas ao instante atual. |
| RF-10 | **T-RF10-008** | Recusada: manutenção (RN-05) | `ReservaCriacao_RF10_Test` | T-RF10-008: Conflicts - Recurso em manutenção deve recusar (RN-05) | **RED_FALHA_ESPERADA** | ServicoCriacaoReserva não consulta a manutenção (RN-05): a reserva é aceita sem lançar exceção. | mvn test 2026-09-25: FALHOU - Expecting code to raise a throwable. |  |
| RF-10 | **T-RF10-009** | Uma aceita, uma recusada | `ReservaCriacao_RF10_Test` | T-RF10-009: Conflicts - Dupla simultânea uma aceita uma recusada (RN-04) | **RED_FALHA_ESPERADA** | Com duas requisições simultâneas, ambas são aceitas ([ACEITA, ACEITA]) em várias das 300 rodadas (3/3 execuções isoladas e nas execuções completas); o serviço não sincroniza a verificação de sobreposição. Race condition. | mvn test 2026-09-25: FALHOU - [rodadas em que não houve exatamente 1 aceita e 1 recusada] Expecting empty but was: ["rodada 29: [ACEITA, ACEITA]", "rodada 38: [ACEITA, ACEITA]", "r… |  |
| RF-10 | **T-RF10-010** | Aceita OU conforme política (PENDENTE) | `ReservaCriacao_RF10_Test` | T-RF10-010: Boundary - Período RN-01 válido (fim > início) | **BLOQUEADO_POR_LACUNA** | plano não define a duração mínima (§4.3) - resultado 'Aceita OU conforme política (PENDENTE)' | mvn test 2026-09-25: ignorado (@Disabled) |  |
| RF-10 | **T-RF10-011** | Recusada conforme RN-01 | `ReservaCriacao_RF10_Test` | T-RF10-011: Invalid Input - Fim anterior ao início deve recusar por RN-01 | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado | A versão anterior passava pelo motivo errado ("Data no passado"); agora exige "Fim anterior ao início". |
| RF-10 | **T-RF10-012** | Recusada: "Recurso não encontrado" | `ReservaCriacao_RF10_Test` | T-RF10-012: Invalid Input - Recurso inexistente deve recusar | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-013** | Recusada: "Não pode criar reserva para outro usuário" | `ReservaCriacao_RF10_Test` | T-RF10-013: Forbidden State - Solicitante cria para outro Solicitante | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-014** | Auditoria registra: usuário, ação=CRIAR, estado=SOLICITADA | `ReservaCriacao_RF10_Test` | T-RF10-014: Happy Path - Auditoria criada na criação (RN-09) | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado | Verifica usuário, ação CRIAR, estado SOLICITADA e timestamp. A auditoria chega via lista estática interna de ValidadorAuditoria. |
| RF-10 | **T-RF10-015** | Reserva recuperada do banco com dados corretos | `ReservaCriacao_RF10_Test` | T-RF10-015: Happy Path - Persistência em banco e consulta | **REGRA_JA_IMPLEMENTADA** | Teste passou; o comportamento já existe em ServicoCriacaoReserva e confere com o plano. | mvn test 2026-09-25: aprovado | Verifica repositório em memória do serviço; o plano prevê banco real (camada Integração), não exercitado. |

### 7.3 Contagem por status

| Status | Casos |
|---|---|
| RED_FALHA_ESPERADA | 13 |
| REGRA_JA_IMPLEMENTADA | 91 |
| BLOQUEADO_POR_LACUNA | 18 |
| TESTE_INCORRETO | 1 |
| SEM_TESTE | 0 |
| FALHA_DE_COMPILACAO / FALHA_DE_INFRAESTRUTURA | 0 / 0 |
| **Total** | **123** |

### 7.4 Casos em RED_FALHA_ESPERADA (13)

| Regra | Caso | Classe de teste | Falha confirmada (asserção) |
|---|---|---|---|
| RN-02 | T-RN02-009 | `ReservaSobreposicao_RN02_Test` | Mensagem "Conflito de horário" não contém "Conflita com reserva 08:00-09:00". |
| RN-03 | T-RN03-007 | `ReservaAgendaProfessor_RN03_Test` | Mensagem "Professor não identificado" não contém "Professor não encontrado". |
| RN-06 | T-RN06-003 | `ReservaAprovacao_RN06_Test` | Motivo da rejeição esperado "Horário reservado para evento institucional", obtido `null`. |
| RN-06 | T-RN06-004 | `ReservaAprovacao_RN06_Test` | Mensagem sem o prefixo "Acesso negado.". |
| RN-06 | T-RN06-005 | `ReservaAprovacao_RN06_Test` | Mensagem sem o prefixo "Acesso negado.". |
| RN-06 | T-RN06-007 | `ReservaAprovacao_RN06_Test` | Esperado 1 reserva APROVADA, obtidas 2. |
| RN-07 | T-RN07-009 | `ReservaFluxoEstados_RN07_Test` | Mensagem "Estado inválido" não contém "Estado é obrigatório". |
| RN-09 | T-RN09-010 | `ReservaAuditoria_RN09_Test` | Registro "APAGAR RECUSADA Operação não permitida" não expressa "REJEITADO". |
| RN-10 | T-RN10-003 | `ReservaRastreabilidade_RN10_Test` | 21 RFs MUST sem casos T-RFnn no plano (RF-02 a RF-09 e RF-11 a RF-23). |
| RF-01 | T-RF01-002 | `ReservaAutenticacaoAutorizacao_RF01_Test` | Estado esperado APROVADA, obtido SOLICITADA; auditoria vazia. |
| RF-10 | T-RF10-006 | `ReservaCriacao_RF10_Test` | "Expecting code to raise a throwable": conflito em material não detectado (só o recurso principal é verificado). |
| RF-10 | T-RF10-008 | `ReservaCriacao_RF10_Test` | "Expecting code to raise a throwable": manutenção não consultada pelo serviço. |
| RF-10 | T-RF10-009 | `ReservaCriacao_RF10_Test` | Rodadas com `[ACEITA, ACEITA]`: duas requisições simultâneas aceitas (falhou em 3/3 execuções isoladas e nas execuções completas). |

Nenhum RED decorre de erro de compilação, `NullPointerException` acidental, fixture inválida, contexto, classe ausente, conexão ou exceção de tipo diferente do esperado.

### 7.5 Casos com REGRA_JA_IMPLEMENTADA (91) - investigação

Todos os 91 testes que passaram foram revisados individualmente (corpo do teste, entrada do plano, asserção e componente exercitado). Depois das correções, nenhum passou por fixture incorreta ou por validar um stub; as asserções fracas remanescentes estão nas ressalvas abaixo e na coluna "Observação" da matriz (por exemplo, T-RN05-005 assere só o tipo da exceção):

- **Implementação que satisfaz o teste por atalho:** `ValidadorAgendaProfessor` deriva "Professor B" removendo o prefixo "Prof " do nome (T-RN03-004); `ValidadorSobreposicao` escolhe a mensagem pela igualdade exata do período (T-RN02-003/004); `ValidadorAutorizacao.validarToken` aceita qualquer "Bearer x" (T-RF01-006/007); `ValidadorConcorrencia` arbitra só períodos idênticos (T-RN04-001/002/004/007). Os comportamentos exigidos pelo plano estão atendidos; os atalhos ficam registrados sem alteração.
- **Verificação parcial do resultado esperado:** T-RN07-002/003 (auditoria da transição), T-RN04-001/007 (status SOLICITADA, "erro claro" e leitura no banco), T-RF01-001/003 (conteúdo devolvido/criação de recurso), T-RF10-015 (repositório em memória, sem banco real), T-RN08-008 e T-RN09-010 (campo "resultado" da auditoria).
- **Alternativas "OU ... (PENDENTE)" do plano não cobertas** em T-RN07-007, T-RN08-005 e T-RN09-005 (cobertas apenas as recusas coerentes com o diagrama/RN-08/mensagem citada).

### 7.6 Testes incorretos

- **Encontrados e corrigidos nesta tarefa (a versão anterior passava ou falhava por motivo alheio ao plano):**
  - `ReservaCriacao_RF10_Test`: datas fixas hoje já passadas (8 erros "Data no passado"); T-RF10-005 a 008 sem reserva/manutenção existente (o cenário nunca era alcançado); T-RF10-009 sequencial (plano exige requisições simultâneas); T-RF10-011 passava pelo motivo errado ("Data no passado").
  - `ReservaSobreposicao_RN02_Test`: T-RN02-008 (data passada, sem reserva existente e sem a ordem RN-01 antes da sobreposição) e T-RN02-009 (afirmava a mensagem da implementação, não a do plano).
  - `ReservaAgendaProfessor_RN03_Test`: T-RN03-001 (período 10:00 a Ter 09:00), T-RN03-004 (sem Prof C), T-RN03-005 e T-RN03-010 (fixtures distintas do plano), T-RN03-007 (mensagem da implementação).
  - `ReservaUnicidadeConcorrencia_RN04_Test`: T-RN04-001/002/004 sequenciais; T-RN04-007 afirmava o retorno de um helper do próprio validador; T-RN04-006/008 sem asserção real.
  - `ReservaAprovacao_RN06_Test`: T-RN06-001/002/003 sem afirmar estado/aprovação/motivo; T-RN06-006 passava só porque o período era nulo; T-RN06-007 validava um método que sempre lança exceção; T-RN06-008/010 chamavam helpers em vez da operação.
  - `ReservaAuditoria_RN09_Test`: T-RN09-003 (2 mudanças em vez de 5), T-RN09-004 (só contava), T-RN09-009 (`isNotNull`).
  - `ReservaApagamento_RN08_Test`: T-RN08-008 (não verificava a auditoria).
  - `ReservaAutenticacaoAutorizacao_RF01_Test`: T-RF01-001 (`isNotNull` sobre lista vazia fixa), T-RF01-006/007 (a exceção era lançada dentro do próprio teste), T-RF01-009 (DisplayName com id duplo "T-RN01-009 / T-RF01-009").
  - `ReservaRastreabilidade_RN10_Test`: todos os 10 testes validavam `RastreabilidadeValidator`, cujos métodos retornam constantes fixas; substituídos por testes documentais.
- **Ainda incorreto (pendente):** T-RN07-008. O teste anterior só validava SOLICITADA → APROVADA. Reescrever exige um relógio/timestamp injetável na auditoria (contrato inexistente). Teste desabilitado.

### 7.7 Falhas de compilação

Nenhuma.

### 7.8 Falhas de infraestrutura

Nenhuma.

### 7.9 Casos bloqueados por lacuna (18) - sem invenção de comportamento

| Caso | Informação ausente | Ação futura (não executada) |
|---|---|---|
| T-RN01-003 | Duração mínima da reserva (§4.3): "aceita ou recusada conforme política (PENDENTE)". | Decidir a política e reescrever o teste. |
| T-RN02-007 | Política de adjacência (Q-002). | Decidir Q-002. |
| T-RN03-006 | Política de adjacência (Q-002). | Decidir Q-002. |
| T-RN03-008 | Agenda inválida: "trata como sem agenda OU recusa". | Escolher um dos comportamentos. |
| T-RN03-009 | "Recusada conforme política" sem política de alteração de reserva com professor alocado. | Definir a política. |
| T-RN04-005 | Política de adjacência (Q-002). | Decidir Q-002. |
| T-RN04-006 | Resultado da Request 2 ("conforme status") e critério de recurso restrito (Q-003). | Decidir Q-003 e o resultado esperado. |
| T-RN04-008 | Política de timestamp/sequência da auditoria sob concorrência (§4.2). | Definir a política de timestamp. |
| T-RN05-007 | Alteração tentada ("tentar alterar duramente") e política para reservas afetadas por manutenção posterior (Q-005). | Definir a alteração e decidir Q-005. |
| T-RN05-010 | Política de adjacência (Q-002). | Decidir Q-002. |
| T-RN06-006 | "Recusada OU aceita com status de alerta (PENDENTE)" (Q-005). | Decidir Q-005. |
| T-RN06-009 | Responsabilidade do Responsável por recurso (Q-004). | Decidir Q-004. |
| T-RN07-010 | Origem, ator e condição de NAO_COMPARECEU (Q-006). | Decidir Q-006. |
| T-RN09-008 | Se a operação recusada gera auditoria de tentativa ("OU não (PENDENTE)"). | Decidir a política. |
| T-RN09-009 | Desempate/precisão do timestamp (§4.2). | Definir a política. |
| T-RN10-010 | Critério verificável de "matriz atualizada em paralelo à implementação". | Definir o critério de verificação. |
| T-RF01-008 | Perfil prioritário de usuário com múltiplos perfis ("PENDENTE DE DECISÃO"). | Decidir a política. |
| T-RF10-010 | Duração mínima da reserva (§4.3). | Decidir a política. |

### 7.10 Casos sem teste (SEM_TESTE)

**Nenhum.** Todos os 123 casos possuem um teste JUnit correspondente (104 executados com resultado e 19 placeholders desabilitados: 18 bloqueados por lacuna e 1 `TESTE_INCORRETO` pendente).

### 7.11 Assinaturas mínimas criadas em `src/main/java/` e arquivos alterados

Assinaturas mínimas (sem lógica de negócio; verificado que não havia contrato equivalente):

- `ServicoCriacaoReserva`: construtor `ServicoCriacaoReserva(ValidadorManutencao)` que só guarda a dependência (o serviço ainda não a consulta) e construtor sem argumentos explícito. Necessário para T-RF10-008 declarar a manutenção.
- `Reserva`: campo `motivoRejeicao` e `getMotivoRejeicao()` (sem setter; nenhum código o preenche). Necessário para T-RN06-003 verificar o motivo da rejeição.

Arquivos de teste alterados (`src/test/java/com/organizacao_de_recursos/domain/`): `ReservaTemporal_RN01_Test`, `ReservaSobreposicao_RN02_Test`, `ReservaAgendaProfessor_RN03_Test`, `ReservaUnicidadeConcorrencia_RN04_Test`, `ReservaManutencao_RN05_Test`, `ReservaAprovacao_RN06_Test`, `ReservaFluxoEstados_RN07_Test`, `ReservaApagamento_RN08_Test`, `ReservaAuditoria_RN09_Test`, `ReservaRastreabilidade_RN10_Test`, `ReservaAutenticacaoAutorizacao_RF01_Test`, `ReservaCriacao_RF10_Test`. Nenhum arquivo foi criado ou removido. Nenhuma regra de negócio foi implementada ou modificada.

### 7.12 Divergências entre plano e testes

1. **Camadas:** casos com "Camada(s)" API, Integração, E2E ou banco foram verificados em nível de domínio; a infraestrutura correspondente não existe no projeto. T-RF10-015 ("Persistência em banco") verifica só o repositório em memória do serviço.
2. **Mensagens:** em 6 REDs (T-RN02-009, T-RN03-007, T-RN06-004, T-RN06-005, T-RN07-009, T-RN09-010) a recusa/registro existe, mas o texto da implementação difere do citado literalmente no plano.
3. **Pré-condição inconsistente:** T-RN02-010 descreve A e B ambas 08:00-09:00 (violaria RN-02); o teste usa A 07:00-08:00 e B 08:00-09:00.
4. **"Já iniciada":** T-RN01-010 é descrito por horário; o teste usa o estado EM_USO.
5. **Q-001 (estado inicial de recurso não restrito), marcada BLOQUEADO em §4.1:** os casos T-RF10-001 a 004, T-RN06-001/002 e T-RN07-001 afirmam explicitamente SOLICITADA no resultado esperado e foram testados como escritos. Se Q-001 decidir APROVADA, esses testes mudam.
6. **Contagem do plano:** §5.1 cita "100+ casos"; a contagem real é 123 (RN-04 tem 8 casos).
7. **Código de produção preexistente com lógica e retornos fixos:** o repositório já contém implementações (validadores) e classes marcadas "Assinatura mínima sem lógica (Fase RED TDD)" que têm lógica (`ServicoCriacaoReserva`, `ValidadorAutorizacao`) ou retornos fixos (`RastreabilidadeValidator`, agora sem uso nos testes; `ValidadorConcorrencia.processarDuplaPeriodosAdjacentes`/`processarReservaComRestricao`; `ValidadorAprovacao.aprovarConcorrente`; `ValidadorAutorizacao.consultarDisponibilidade`). Nada disso foi alterado ou removido, por isso boa parte dos casos aparece como REGRA_JA_IMPLEMENTADA em vez de RED.

### 7.13 Gate final do QA

| Verificação | Resultado |
|---|---|
| Todas as regras inventariadas (RN-01 a RN-10, RF-01, RF-10) | Sim |
| Todos os casos presentes na matriz (123) e ids conferidos com o plano | Sim (nenhum teste sem caso no plano, nenhum caso sem teste) |
| Cada caso com teste ou marcado SEM_TESTE | Sim (0 SEM_TESTE) |
| Testes em JUnit 5 com Arrange-Act-Assert reconhecível | Sim (nos 19 placeholders desabilitados o corpo é só `fail(...)`) |
| Asserções comportamentais | Sim, com as ressalvas de 7.5 |
| Cada RED confirmado pelo motivo funcional esperado | Sim (7.4) |
| Cada teste que passou foi investigado | Sim (7.5) |
| Nenhuma regra de negócio criada ou modificada | Sim (só as 2 assinaturas de 7.11) |
| Nenhuma regra ausente inventada | Sim (18 casos bloqueados por lacuna) |
| Plano atualizado | Sim (esta seção) |
| Evidência da execução atual; relatórios antigos não usados como prova | Sim |
| **Conclusão** | **PARCIAL.** Cobertura completa (nenhum SEM_TESTE), mas 18 casos bloqueados por lacuna do plano e 1 `TESTE_INCORRETO` (T-RN07-008) impedem declarar conclusão total sem inventar comportamento. |

---

## 8. Fase GREEN do TDD - Matriz atualizada

> A seção 7 (fase RED) foi preservada como histórico. Esta seção registra a fase GREEN.
> **Execução:** 2026-09-25 · `mvn -B -o verify` (não há Maven Wrapper no projeto; Maven funcional) · BUILD SUCCESS.
> **Resultado:** 123 testes · 107 aprovados · 0 falhas · 0 erros · 16 ignorados (`@Disabled`, todos bloqueados por lacuna real). JaCoCo (informativo, sem gate configurado): 84,0% de linhas e 67,3% de branches.
> **Confirmação dos REDs (antes de implementar):** execução atual reproduziu exatamente os 13 REDs da fase RED (T-RN02-009, T-RN03-007, T-RN06-003/004/005/007, T-RN07-009, T-RN09-010, T-RN10-003, T-RF01-002, T-RF10-006/008/009); 123 testes, 13 falhas, 19 ignorados.
> **Ciclo:** cada correção foi seguida do teste específico, da classe da regra e da suíte completa. Sem regressões: os 91 casos aprovados na fase RED continuam aprovados.

### 8.1 Contagem por status atual

| Status atual | Casos |
|---|---|
| GREEN (RED convertido em aprovado) | 13 |
| REGRA_JA_IMPLEMENTADA | 94 (91 da fase RED + T-RN05-007, T-RN06-006 e T-RN07-008, ver 8.5) |
| BLOQUEADO_POR_LACUNA | 16 |
| TESTE_INCORRETO / FALHA_DE_COMPILACAO / FALHA_DE_INFRAESTRUTURA | 0 / 0 / 0 |
| **Total (casos com teste)** | **123** |

Além dos 123 casos, a seção 3.A adicionou 44 casos T-RFnn (21 RFs) **sem teste ainda** (8.4).

### 8.2 Matriz regra → caso → teste → status (123 casos)

| Regra | Caso | Teste | Status anterior | Status atual | Comportamento implementado | Fonte utilizada | Componente alterado | Evidência da execução | Observações |
|---|---|---|---|---|---|---|---|---|---|
| RF-01 | **T-RF01-001** | `ReservaAutenticacaoAutorizacao_RF01_Test#solicitanteAutenticadoDeveConsultarDisponibilidade` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-01 | **T-RF01-002** | `ReservaAutenticacaoAutorizacao_RF01_Test#responsavelAutenticadoDeveAprovarSolicitacao` | RED_FALHA_ESPERADA | **GREEN** | aprovarSolicitacao valida a transição SOLICITADA -> APROVADA, registra a auditoria (usuário, ação APROVAR, estado anterior/novo, timestamp) e atualiza o estado. | plano T-RF01-002; PRD RF-01, RF-14, RN-07, RN-09; ADR-003 e ADR-004; enunciado ("toda mudança de estado deve gerar auditoria") | ValidadorAutorizacao | mvn -B verify 2026-09-25: aprovado | A auditoria é registrada antes da mudança de estado; o mecanismo transacional definitivo (ADR-004) segue pendente. |
| RF-01 | **T-RF01-003** | `ReservaAutenticacaoAutorizacao_RF01_Test#administradorAutenticadoDeveGerenciarRecurso` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-01 | **T-RF01-004** | `ReservaAutenticacaoAutorizacao_RF01_Test#solicitanteNaoDevePoderAprovar` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-01 | **T-RF01-005** | `ReservaAutenticacaoAutorizacao_RF01_Test#responsavelNaoDevePoderGerenciarUsuarios` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-01 | **T-RF01-006** | `ReservaAutenticacaoAutorizacao_RF01_Test#tokenInvalidoOuExpiradoDeveSerRecusado` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-01 | **T-RF01-007** | `ReservaAutenticacaoAutorizacao_RF01_Test#requisicaoSemTokenDeveSerRecusada` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-01 | **T-RF01-008** | `ReservaAutenticacaoAutorizacao_RF01_Test#usuarioMultiplosPerfisDeveSerValidadoConformePolitica` | BLOQUEADO_POR_LACUNA | **BLOQUEADO_POR_LACUNA** | — | plano; enunciado consultado (não resolve a lacuna) | — | mvn -B verify 2026-09-25: ignorado (@Disabled) | Bloqueio mantido; @Disabled preservado. |
| RF-01 | **T-RF01-009** | `ReservaAutenticacaoAutorizacao_RF01_Test#transicaoDePerfilDeveAtualizarPermissoes` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-01 | **T-RF01-010** | `ReservaAutenticacaoAutorizacao_RF01_Test#usuarioDesativadoDeveSerBloqueado` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-001** | `ReservaCriacao_RF10_Test#deveCriarReservaSimplesEmSalaComum` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-002** | `ReservaCriacao_RF10_Test#deveCriarReservaComMultiplosRecursos` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-003** | `ReservaCriacao_RF10_Test#deveCriarReservaComProfessor` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-004** | `ReservaCriacao_RF10_Test#deveCriarReservaRestritaAguardandoAprovacao` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-005** | `ReservaCriacao_RF10_Test#deveRecusarCriacaoComSobreposicaoEmSala` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-006** | `ReservaCriacao_RF10_Test#deveRecusarCriacaoComSobreposicaoEmMaterial` | RED_FALHA_ESPERADA | **GREEN** | A verificação de sobreposição considera sala e materiais anexados à reserva; conflito em material recusa com "conflito em material". | plano T-RF10-006; PRD RF-10 e RN-02; ADR-005 (revalida sala, material, professor); enunciado (sala, material ou professor) | ValidadorSobreposicao; ServicoCriacaoReserva; ReservaSobreposicaoException | mvn -B verify 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-007** | `ReservaCriacao_RF10_Test#deveRecusarCriacaoComSobreposicaoEmProfessor` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-008** | `ReservaCriacao_RF10_Test#deveRecusarCriacaoEmRecursoEmManutencao` | RED_FALHA_ESPERADA | **GREEN** | A criação consulta os períodos de manutenção de sala e materiais e recusa com "Recurso em manutenção no período solicitado: <nome>". | plano T-RF10-008; PRD RF-10, RN-05; ADR-005 (revalida manutenção); enunciado ("recursos em manutenção não podem ser reservados") | ServicoCriacaoReserva | mvn -B verify 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-009** | `ReservaCriacao_RF10_Test#deveGerenciarDuplaSimultaneaNaCriacao` | RED_FALHA_ESPERADA | **GREEN** | Verificação de conflito e registro da reserva executam em seção crítica única; o serviço também registra a reserva na agenda do professor alocado (RN-03). 300 rodadas com 2 requisições simultâneas: exatamente 1 aceita e 1 recusada. | plano T-RF10-009 e T-RN04-*; PRD RN-04, RF-10 crit. 3, RF-13; enunciado ("uma única reserva aceita"); ADR-002 | ServicoCriacaoReserva; ValidadorSobreposicao (métodos synchronized) | mvn -B verify 2026-09-25: aprovado | Mecanismo local ao processo. Conflito registrado com ADR-002 (mecanismo sobre banco pendente de experimento com Testcontainers): ver 8.6. |
| RF-10 | **T-RF10-010** | `ReservaCriacao_RF10_Test#deveAceitarCriacaoComPeriodoValidoMinimo` | BLOQUEADO_POR_LACUNA | **BLOQUEADO_POR_LACUNA** | — | plano; enunciado consultado (não resolve a lacuna) | — | mvn -B verify 2026-09-25: ignorado (@Disabled) | Bloqueio mantido; @Disabled preservado. |
| RF-10 | **T-RF10-011** | `ReservaCriacao_RF10_Test#deveRecusarCriacaoComFimAnteriorAoInicio` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-012** | `ReservaCriacao_RF10_Test#deveRecusarCriacaoComRecursoInexistente` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-013** | `ReservaCriacao_RF10_Test#solicitanteNaoDevePoderCriarParaOutro` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-014** | `ReservaCriacao_RF10_Test#criacaoDeReservaDeveRegistrarAuditoria` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RF-10 | **T-RF10-015** | `ReservaCriacao_RF10_Test#deveRecuperarReservaCriadaDoRepositorio` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-01 | **T-RN01-001** | `ReservaTemporal_RN01_Test#deveAceitarReservaComIntervaloValido` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-01 | **T-RN01-002** | `ReservaTemporal_RN01_Test#deveAceitarIntervaloLongo` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-01 | **T-RN01-003** | `ReservaTemporal_RN01_Test#deveAceitarIntervaloMinimo` | BLOQUEADO_POR_LACUNA | **BLOQUEADO_POR_LACUNA** | — | plano; enunciado consultado (não resolve a lacuna) | — | mvn -B verify 2026-09-25: ignorado (@Disabled) | Bloqueio mantido; @Disabled preservado. |
| RN-01 | **T-RN01-004** | `ReservaTemporal_RN01_Test#deveRecusarIntervaloZero` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-01 | **T-RN01-005** | `ReservaTemporal_RN01_Test#deveRecusarFimAnteriorAoInicio` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-01 | **T-RN01-006** | `ReservaTemporal_RN01_Test#deveRecusarDataPassada` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-01 | **T-RN01-007** | `ReservaTemporal_RN01_Test#deveRecusarInicioNulo` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-01 | **T-RN01-008** | `ReservaTemporal_RN01_Test#deveRecusarFimNulo` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-01 | **T-RN01-009** | `ReservaTemporal_RN01_Test#deveRecusarFormatoDeHoraInvalido` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-01 | **T-RN01-010** | `ReservaTemporal_RN01_Test#deveRecusarAlteracaoDeReservaJaIniciada` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-02 | **T-RN02-001** | `ReservaSobreposicao_RN02_Test#deveAceitarReservaEmSalaSemConflito` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-02 | **T-RN02-002** | `ReservaSobreposicao_RN02_Test#deveAceitarMesmoHorariosEmRecursosDiferentes` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-02 | **T-RN02-003** | `ReservaSobreposicao_RN02_Test#deveRecusarSobreposicaoTotal` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-02 | **T-RN02-004** | `ReservaSobreposicao_RN02_Test#deveRecusarSobreposicaoParcialNovaDentroDaExistente` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-02 | **T-RN02-005** | `ReservaSobreposicao_RN02_Test#deveRecusarSobreposicaoParcialNovaTerminaDentro` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-02 | **T-RN02-006** | `ReservaSobreposicao_RN02_Test#deveRecusarQuandoNovaEnvolveExistente` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-02 | **T-RN02-007** | `ReservaSobreposicao_RN02_Test#deveAceitarReservasAdjacentesSemSobreposicao` | BLOQUEADO_POR_LACUNA | **BLOQUEADO_POR_LACUNA** | — | plano; enunciado consultado (não resolve a lacuna) | — | mvn -B verify 2026-09-25: ignorado (@Disabled) | Bloqueio mantido; @Disabled preservado. |
| RN-02 | **T-RN02-008** | `ReservaSobreposicao_RN02_Test#deveRecusarPorRN01AntesDeVerificarSobreposicao` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-02 | **T-RN02-009** | `ReservaSobreposicao_RN02_Test#deveRecusarQuandoConflitaComUmaDasMultiplasExistentes` | RED_FALHA_ESPERADA | **GREEN** | A recusa por sobreposição identifica a reserva conflitante: "<Conflito de horário / Recurso indisponível no período>. Conflita com reserva HH:mm-HH:mm". Os prefixos literais de T-RN02-003 a 006 foram preservados. | plano T-RN02-003..006 e T-RN02-009 (mensagens literais) | ValidadorSobreposicao; ReservaSobreposicaoException (campo recursoEmConflito) | mvn -B verify 2026-09-25: aprovado | A mensagem composta atende simultaneamente às duas formas literais do plano; o texto após o prefixo é o do plano. |
| RN-02 | **T-RN02-010** | `ReservaSobreposicao_RN02_Test#deveRecusarAlteracaoQueCausariaSobreposicao` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-03 | **T-RN03-001** | `ReservaAgendaProfessor_RN03_Test#deveAceitarReservaComProfessorSemConflito` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-03 | **T-RN03-002** | `ReservaAgendaProfessor_RN03_Test#deveAceitarReservaComProfessorSemAgenda` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-03 | **T-RN03-003** | `ReservaAgendaProfessor_RN03_Test#deveRecusarReservaComConflitoDeProfessor` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-03 | **T-RN03-004** | `ReservaAgendaProfessor_RN03_Test#deveRecusarQuandoUmDeProfessoresTemConflito` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-03 | **T-RN03-005** | `ReservaAgendaProfessor_RN03_Test#deveRecusarAlteracaoCriandoConflitoDeAgenda` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-03 | **T-RN03-006** | `ReservaAgendaProfessor_RN03_Test#deveValidarAdjacenciaDeAgenda` | BLOQUEADO_POR_LACUNA | **BLOQUEADO_POR_LACUNA** | — | plano; enunciado consultado (não resolve a lacuna) | — | mvn -B verify 2026-09-25: ignorado (@Disabled) | Bloqueio mantido; @Disabled preservado. |
| RN-03 | **T-RN03-007** | `ReservaAgendaProfessor_RN03_Test#deveRecusarProfessorInexistente` | RED_FALHA_ESPERADA | **GREEN** | Reserva sem professor válido é recusada com "Professor não encontrado". | plano T-RN03-007 (mensagem literal) | ValidadorAgendaProfessor | mvn -B verify 2026-09-25: aprovado |  |
| RN-03 | **T-RN03-008** | `ReservaAgendaProfessor_RN03_Test#deveRecusarAgendaComFormatoInvalido` | BLOQUEADO_POR_LACUNA | **BLOQUEADO_POR_LACUNA** | — | plano; enunciado consultado (não resolve a lacuna) | — | mvn -B verify 2026-09-25: ignorado (@Disabled) | Bloqueio mantido; @Disabled preservado. |
| RN-03 | **T-RN03-009** | `ReservaAgendaProfessor_RN03_Test#deveRecusarSobreporAgendaDeProfessorJaAlocado` | BLOQUEADO_POR_LACUNA | **BLOQUEADO_POR_LACUNA** | — | plano; enunciado consultado (não resolve a lacuna) | — | mvn -B verify 2026-09-25: ignorado (@Disabled) | Bloqueio mantido; @Disabled preservado. |
| RN-03 | **T-RN03-010** | `ReservaAgendaProfessor_RN03_Test#deveAceitarMesmoProfessorEmDiasDiferentes` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-04 | **T-RN04-001** | `ReservaUnicidadeConcorrencia_RN04_Test#duasSolicitacoesSimultaneasDevemResultarEmApenasUmaAceita` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-04 | **T-RN04-002** | `ReservaUnicidadeConcorrencia_RN04_Test#triplaSimultaneaDeveAceitarApenasUma` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-04 | **T-RN04-003** | `ReservaUnicidadeConcorrencia_RN04_Test#solicitacoesSequenciaisDevemAplicarRN02` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-04 | **T-RN04-004** | `ReservaUnicidadeConcorrencia_RN04_Test#duplicaEmRecursosDiferentesDevemSerAmbosAceitos` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-04 | **T-RN04-005** | `ReservaUnicidadeConcorrencia_RN04_Test#duplaEmPeriodosAdjacentesSimultaneasDevemSerAceitas` | BLOQUEADO_POR_LACUNA | **BLOQUEADO_POR_LACUNA** | — | plano; enunciado consultado (não resolve a lacuna) | — | mvn -B verify 2026-09-25: ignorado (@Disabled) | Bloqueio mantido; @Disabled preservado. |
| RN-04 | **T-RN04-006** | `ReservaUnicidadeConcorrencia_RN04_Test#duplaComRecursoRestritoESemRestricao` | BLOQUEADO_POR_LACUNA | **BLOQUEADO_POR_LACUNA** | — | plano; enunciado consultado (não resolve a lacuna) | — | mvn -B verify 2026-09-25: ignorado (@Disabled) | Bloqueio mantido; @Disabled preservado. |
| RN-04 | **T-RN04-007** | `ReservaUnicidadeConcorrencia_RN04_Test#deveGarantirConsistenciaAposAceitarUma` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-04 | **T-RN04-008** | `ReservaUnicidadeConcorrencia_RN04_Test#naoDeveHaverRaceConditionEmAuditoria` | BLOQUEADO_POR_LACUNA | **BLOQUEADO_POR_LACUNA** | — | plano; enunciado consultado (não resolve a lacuna) | — | mvn -B verify 2026-09-25: ignorado (@Disabled) | Bloqueio mantido; @Disabled preservado. |
| RN-05 | **T-RN05-001** | `ReservaManutencao_RN05_Test#deveAceitarReservaEmRecursoSemManutencao` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-05 | **T-RN05-002** | `ReservaManutencao_RN05_Test#deveAceitarReservaForaDoPeriodoDeManutencao` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-05 | **T-RN05-003** | `ReservaManutencao_RN05_Test#deveRecusarReservaEmRecursoEmManutencao` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-05 | **T-RN05-004** | `ReservaManutencao_RN05_Test#deveRecusarReservaComManutencaoParcial` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-05 | **T-RN05-005** | `ReservaManutencao_RN05_Test#deveRecusarPorPrimeiraManutencaoEmMultiplas` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-05 | **T-RN05-006** | `ReservaManutencao_RN05_Test#deveRecusarMaterialEmManutencao` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-05 | **T-RN05-007** | `ReservaManutencao_RN05_Test#deveRecusarAlterarReservaIntroduzindoManutencao` | BLOQUEADO_POR_LACUNA | **REGRA_JA_IMPLEMENTADA** | Nenhuma alteração de produção: validarAlteracaoManutencao já recusa alteração que mantém a reserva no período de manutenção ("Sala em manutenção neste período"). | PRD RF-11 critério 2 ("usa recurso em manutenção -> recusa"); enunciado ("recursos em manutenção não podem ser reservados") | — (ValidadorManutencao existente) | mvn -B verify 2026-09-25: aprovado | Desbloqueado: a lacuna era o destino da alteração ("alterar duramente", provável "durante"); o teste altera para um período ainda dentro da manutenção. A política para reservas já existentes afetadas por manutenção posterior (Q-005) permanece pendente e não foi implementada. |
| RN-05 | **T-RN05-008** | `ReservaManutencao_RN05_Test#pesquisaDeDisponibilidadeDeveExcluirRecursoEmManutencao` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-05 | **T-RN05-009** | `ReservaManutencao_RN05_Test#deveRecusarManutencaoComPeriodoInvalido` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-05 | **T-RN05-010** | `ReservaManutencao_RN05_Test#deveAceitarManutencaoAdjacenteAReserva` | BLOQUEADO_POR_LACUNA | **BLOQUEADO_POR_LACUNA** | — | plano; enunciado consultado (não resolve a lacuna) | — | mvn -B verify 2026-09-25: ignorado (@Disabled) | Bloqueio mantido; @Disabled preservado. |
| RN-06 | **T-RN06-001** | `ReservaAprovacao_RN06_Test#recursoComumDeveSerAceitoSemAprovacao` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-06 | **T-RN06-002** | `ReservaAprovacao_RN06_Test#recursoRestritoDeveAguardarAprovacao` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-06 | **T-RN06-003** | `ReservaAprovacao_RN06_Test#responsavelRejeitaRecursoRestritoComMotivo` | RED_FALHA_ESPERADA | **GREEN** | rejeitarComMotivo passa a armazenar o motivo informado (SOLICITADA -> REJEITADA com motivo). | plano T-RN06-003; PRD RF-14 (rejeição registra a mudança); RN-07 | ValidadorAprovacao; Reserva (setMotivoRejeicao) | mvn -B verify 2026-09-25: aprovado | Não foi criada regra de motivo obrigatório (não definida). |
| RN-06 | **T-RN06-004** | `ReservaAprovacao_RN06_Test#solicitanteNaoPodeAprovarRecursoRestrito` | RED_FALHA_ESPERADA | **GREEN** | Mensagem de recusa: "Acesso negado. Apenas Responsável pode aprovar". | plano T-RN06-004 (mensagem literal); PRD RN-06 | ValidadorAprovacao | mvn -B verify 2026-09-25: aprovado |  |
| RN-06 | **T-RN06-005** | `ReservaAprovacao_RN06_Test#administradorNaoPodeAprovarRecursoRestrito` | RED_FALHA_ESPERADA | **GREEN** | Mensagem de recusa: "Acesso negado. Apenas Responsável pode aprovar". | plano T-RN06-005 (mensagem literal); PRD RN-06 | ValidadorAprovacao | mvn -B verify 2026-09-25: aprovado |  |
| RN-06 | **T-RN06-006** | `ReservaAprovacao_RN06_Test#deveRecusarAprovacaoDeRecursoQueFicouIndisponivel` | BLOQUEADO_POR_LACUNA | **REGRA_JA_IMPLEMENTADA** | Nenhuma alteração de produção: aprovarComValidacaoDisponibilidade já recusa a aprovação de recurso em manutenção com "Recurso indisponível no período" e mantém a reserva SOLICITADA. | docs/arquitetura.md §38 (RF-14, RF-15: "autorização e revalidação antes da decisão") e ADR-005; PRD RF-14; enunciado ("recursos em manutenção não podem ser reservados") | — (ValidadorAprovacao existente) | mvn -B verify 2026-09-25: aprovado | Desbloqueado: a documentação existente escolhe a revalidação na decisão; a alternativa "aceita com status de alerta" não tem origem. |
| RN-06 | **T-RN06-007** | `ReservaAprovacao_RN06_Test#deveGarantirApenasUmaAprovadaSobAprovacaoConcorrente` | RED_FALHA_ESPERADA | **GREEN** | Aprovar uma reserva do mesmo recurso com período sobreposto a uma já aprovada é recusado ("Recurso indisponível no período: já existe reserva aprovada"): exatamente 1 aprovada. | plano T-RN06-007; PRD RN-02, RN-04, RN-06; enunciado ("somente uma reserva aceita") | ValidadorAprovacao (registro das reservas aprovadas, método synchronized) | mvn -B verify 2026-09-25: aprovado | Estado das aprovações é em memória, por instância do validador; o mecanismo sobre persistência segue pendente (ADR-002). |
| RN-06 | **T-RN06-008** | `ReservaAprovacao_RN06_Test#deveRecusarAprovacaoDeRecursoInexistente` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-06 | **T-RN06-009** | `ReservaAprovacao_RN06_Test#deveRecusarAprovacaoDeRecursoForaDaResponsabilidade` | BLOQUEADO_POR_LACUNA | **BLOQUEADO_POR_LACUNA** | — | plano; enunciado consultado (não resolve a lacuna) | — | mvn -B verify 2026-09-25: ignorado (@Disabled) | Bloqueio mantido; @Disabled preservado. |
| RN-06 | **T-RN06-010** | `ReservaAprovacao_RN06_Test#deveRecusarReaprovacaoDeReservaJaAprovada` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-07 | **T-RN07-001** | `ReservaFluxoEstados_RN07_Test#deveExecutarFluxoCompletoDeEstados` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-07 | **T-RN07-002** | `ReservaFluxoEstados_RN07_Test#deveCancelarReservaEmSolicitada` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-07 | **T-RN07-003** | `ReservaFluxoEstados_RN07_Test#deveRejeitarReservaEmSolicitada` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-07 | **T-RN07-004** | `ReservaFluxoEstados_RN07_Test#deveRecusarTransicaoNaoEspecificada` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-07 | **T-RN07-005** | `ReservaFluxoEstados_RN07_Test#deveRecusarEstadoInvalido` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-07 | **T-RN07-006** | `ReservaFluxoEstados_RN07_Test#deveRecusarCancelamentoDeReservaIniciada` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-07 | **T-RN07-007** | `ReservaFluxoEstados_RN07_Test#deveRecusarRejeicaoAposAprovacao` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-07 | **T-RN07-008** | `ReservaFluxoEstados_RN07_Test#deveAceitarTransicaoNaFronteiraDeTempo` | TESTE_INCORRETO | **REGRA_JA_IMPLEMENTADA** | Nenhuma alteração de produção. Teste corrigido: início 23:59:59 e término 00:00:00 do dia seguinte formam período válido (término posterior ao início) e a transição SOLICITADA -> APROVADA é aceita. | PRD RN-01 / enunciado ("o término deve ser posterior ao início"); instrução da fase GREEN | — (Reserva.validarTemporalidade e ValidadorFluxoEstados existentes) | mvn -B verify 2026-09-25: aprovado | Divergência registrada: o plano ainda cita "timestamp de auditoria registra transição"; não foi criada regra de virada do dia, fuso, precisão, desempate nem relógio injetável, e esse trecho não é verificado neste caso. |
| RN-07 | **T-RN07-009** | `ReservaFluxoEstados_RN07_Test#deveRecusarEstadoNullOuVazio` | RED_FALHA_ESPERADA | **GREEN** | Novo estado null ou vazio é recusado com "Estado é obrigatório". | plano T-RN07-009 (mensagem literal) | ValidadorFluxoEstados | mvn -B verify 2026-09-25: aprovado |  |
| RN-07 | **T-RN07-010** | `ReservaFluxoEstados_RN07_Test#deveRecusarNaoCompareceuDiretoDeSolicitada` | BLOQUEADO_POR_LACUNA | **BLOQUEADO_POR_LACUNA** | — | plano; enunciado consultado (não resolve a lacuna) | — | mvn -B verify 2026-09-25: ignorado (@Disabled) | Bloqueio mantido; @Disabled preservado. |
| RN-08 | **T-RN08-001** | `ReservaApagamento_RN08_Test#devePodercancelarReservaAntesDeiniciar` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-08 | **T-RN08-002** | `ReservaApagamento_RN08_Test#deveRecusarCancelamentoAposIniciacao` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-08 | **T-RN08-003** | `ReservaApagamento_RN08_Test#deveRecusarCancelamentoAposConclusao` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-08 | **T-RN08-004** | `ReservaApagamento_RN08_Test#adminNaoPodeApagarRegistroDeReservaIniciada` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-08 | **T-RN08-005** | `ReservaApagamento_RN08_Test#deveRecusarForcarCancelamentoDeReservaEmUso` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-08 | **T-RN08-006** | `ReservaApagamento_RN08_Test#deveRecusarCancelamentoImediatamenteAposIniciacao` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-08 | **T-RN08-007** | `ReservaApagamento_RN08_Test#deveRecusarApagarComIdInvalido` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-08 | **T-RN08-008** | `ReservaApagamento_RN08_Test#deveRegistrarAuditoriaAoTentarApagarReservaIniciada` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-08 | **T-RN08-009** | `ReservaApagamento_RN08_Test#adminNaoPodeApagarReservaConcluida` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-08 | **T-RN08-010** | `ReservaApagamento_RN08_Test#deveRecusarRejeitarReservaJaIniciada` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-09 | **T-RN09-001** | `ReservaAuditoria_RN09_Test#criarReservaDeveGerarAuditoria` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-09 | **T-RN09-002** | `ReservaAuditoria_RN09_Test#transicaoDeEstadoDeveGerarAuditoria` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-09 | **T-RN09-003** | `ReservaAuditoria_RN09_Test#devePoderconsultarHistoricoCompleto` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-09 | **T-RN09-004** | `ReservaAuditoria_RN09_Test#devePreservarOrdemDeMultiplasMudancasEmSucessao` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-09 | **T-RN09-005** | `ReservaAuditoria_RN09_Test#operacaoSemAutenticacaoDeveSerRecusadaNaAuditoria` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-09 | **T-RN09-006** | `ReservaAuditoria_RN09_Test#naoDevePermitirEditarAuditoria` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-09 | **T-RN09-007** | `ReservaAuditoria_RN09_Test#naoDevePermitirApagarAuditoria` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-09 | **T-RN09-008** | `ReservaAuditoria_RN09_Test#operacaoRecusadaDeveGerarRegistroDeAuditoria` | BLOQUEADO_POR_LACUNA | **BLOQUEADO_POR_LACUNA** | — | plano; enunciado consultado (não resolve a lacuna) | — | mvn -B verify 2026-09-25: ignorado (@Disabled) | Bloqueio mantido; @Disabled preservado. |
| RN-09 | **T-RN09-009** | `ReservaAuditoria_RN09_Test#auditoriaComTimestampGranularDeveSerOrdenada` | BLOQUEADO_POR_LACUNA | **BLOQUEADO_POR_LACUNA** | — | plano; enunciado consultado (não resolve a lacuna) | — | mvn -B verify 2026-09-25: ignorado (@Disabled) | Bloqueio mantido; @Disabled preservado. |
| RN-09 | **T-RN09-010** | `ReservaAuditoria_RN09_Test#tentativaDeApagamentoProibidoDeveRegistrarAuditoriaComResultadoRejeitado` | RED_FALHA_ESPERADA | **GREEN** | O registro da tentativa de apagamento passa a expressar o resultado: descrição "REJEITADO - Operação não permitida". | plano T-RN09-010 ("resultado (REJEITADO)"); PRD RN-09 | ValidadorAuditoria | mvn -B verify 2026-09-25: aprovado | O plano não define o campo do resultado; a descrição foi usada sem criar campo novo. |
| RN-10 | **T-RN10-001** | `ReservaRastreabilidade_RN10_Test#todasAsRNsDevemEstarRastreadas` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-10 | **T-RN10-002** | `ReservaRastreabilidade_RN10_Test#RN01DeveEstarLigadaARF10ERF11` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-10 | **T-RN10-003** | `ReservaRastreabilidade_RN10_Test#cadaRFMUSTDeveTerCasos` | RED_FALHA_ESPERADA | **GREEN** | Plano passa a ter casos T-RFnn para os 21 RFs MUST que não tinham (44 casos, seção 3.A). | PRD seção 7 (critérios de aceitação) e seção 9 (matriz); RNF-04; enunciado (100% dos requisitos críticos na RTM) | docs/testes/plano-tdd.md (sem alteração de código) | mvn -B verify 2026-09-25: aprovado | Os novos casos ainda não têm teste JUnit (ver 8.4). |
| RN-10 | **T-RN10-004** | `ReservaRastreabilidade_RN10_Test#requisitoNaoRastreadoDeveSerIdentificadoComoLacuna` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-10 | **T-RN10-005** | `ReservaRastreabilidade_RN10_Test#testeOrfaoDeveSerIdentificadoComoErro` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-10 | **T-RN10-006** | `ReservaRastreabilidade_RN10_Test#ligacoesMultiplasNaoDevemConterCiclos` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-10 | **T-RN10-007** | `ReservaRastreabilidade_RN10_Test#deveAtinzir100PercentoDeCoberturaRNsRFsMUST` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-10 | **T-RN10-008** | `ReservaRastreabilidade_RN10_Test#requisitoCriticoSemTesteDeveSerEscalado` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-10 | **T-RN10-009** | `ReservaRastreabilidade_RN10_Test#divergenciasEAceitesDevemEstarRegistrados` | REGRA_JA_IMPLEMENTADA | **REGRA_JA_IMPLEMENTADA** | Sem alteração (comportamento já implementado e mantido). | plano (fase RED) | — | mvn -B verify 2026-09-25: aprovado |  |
| RN-10 | **T-RN10-010** | `ReservaRastreabilidade_RN10_Test#matrizDeveSerAtualizadaAposMudanca` | BLOQUEADO_POR_LACUNA | **BLOQUEADO_POR_LACUNA** | — | plano; enunciado consultado (não resolve a lacuna) | — | mvn -B verify 2026-09-25: ignorado (@Disabled) | Bloqueio mantido; @Disabled preservado. |

### 8.3 Notas sobre a matriz

- "Status anterior" é o status da fase RED (seção 7). Testes desabilitados nunca são classificados como GREEN.
- Nos 94 casos REGRA_JA_IMPLEMENTADA e nos 16 bloqueados não houve mudança de comportamento nesta fase, exceto nos três casos desbloqueados/corrigidos (8.5); alguns passam agora por código reescrito (por exemplo, T-RN02-001 a 010 e T-RF10-005 passam por `ValidadorSobreposicao`) e continuam aprovados.

### 8.4 Casos T-RFnn adicionados (44) - sem teste ainda

Origem: critérios de aceitação do PRD (seção 7), transcritos na seção 3.A, para que T-RN10-003 (todo RF MUST com casos) fosse atendido sem inventar resultado esperado. Nenhum teste foi criado para esses casos nesta fase: quase todos exigem funcionalidades sem componente de produção (cadastros, pesquisa, retirada/devolução, notificação, relatórios, interface, documentação) ou detalhes não definidos (campos de cadastro, §4.7), e implementá-las excede o escopo da fase GREEN. Devem nascer na próxima fase RED. Por T-RN10-008, a ausência de teste de RF MUST é escalada aqui: **T-RN10-003 está GREEN porque o plano passou a ter os casos, mas os 44 casos permanecem SEM_TESTE.**

| Regra/RF | Caso | Resultado esperado (PRD) | Teste | Status | Cobertura indireta existente (informativa; não substitui o teste do caso) |
|---|---|---|---|---|---|
| RF-02 | **T-RF02-001** | Então o sistema registra a sala e permite sua consulta. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-02 | **T-RF02-002** | Então o sistema recusa a operação. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-03 | **T-RF03-001** | Então o sistema registra o professor e permite sua consulta. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-03 | **T-RF03-002** | Então o sistema recusa a operação. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-04 | **T-RF04-001** | Então o sistema registra o material e permite sua consulta. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-04 | **T-RF04-002** | Então o sistema recusa a operação. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-05 | **T-RF05-001** | Então o sistema registra a alteração para uso na autorização. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-05 | **T-RF05-002** | Então o sistema recusa a operação. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-06 | **T-RF06-001** | Então o sistema registra o bloqueio e o considera indisponível no período correspondente. | — | SEM_TESTE (próxima fase RED) | T-RN05-008 |
| RF-06 | **T-RF06-002** | Então o sistema recusa a operação. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-07 | **T-RF07-001** | Então o sistema bloqueia reservas do recurso nesse período. | — | SEM_TESTE (próxima fase RED) | T-RN05-003 |
| RF-07 | **T-RF07-002** | Então o sistema recusa a reserva e informa a indisponibilidade. | — | SEM_TESTE (próxima fase RED) | T-RN05-003, T-RF10-008 |
| RF-08 | **T-RF08-001** | Então o sistema retorna os recursos cadastrados para consulta. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-08 | **T-RF08-002** | Então o sistema informa que o recurso não pode ser reservado no período de manutenção. | — | SEM_TESTE (próxima fase RED) | T-RN05-008 |
| RF-09 | **T-RF09-001** | Então o sistema retorna os recursos compatíveis com os filtros informados. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-09 | **T-RF09-002** | Então o sistema não o apresenta como disponível para reserva naquele período. | — | SEM_TESTE (próxima fase RED) | T-RN05-008, T-RN02-003 |
| RF-11 | **T-RF11-001** | Então o sistema salva a alteração sem conflito. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-11 | **T-RF11-002** | Então o sistema recusa a operação. | — | SEM_TESTE (próxima fase RED) | T-RN01-010, T-RN02-010, T-RN03-005, T-RN05-007 |
| RF-11 | **T-RF11-003** | Então o sistema gera registro de auditoria. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-12 | **T-RF12-001** | Então o sistema altera a reserva para `CANCELADA` e registra a mudança. | — | SEM_TESTE (próxima fase RED) | T-RN07-002, T-RN08-001 |
| RF-12 | **T-RF12-002** | Então o sistema recusa a operação e informa o motivo. | — | SEM_TESTE (próxima fase RED) | T-RN08-002, T-RN08-003 |
| RF-13 | **T-RF13-001** | Então o sistema recusa a nova reserva e informa o conflito. | — | SEM_TESTE (próxima fase RED) | T-RN02-003, T-RF10-005, T-RF10-006, T-RF10-007 |
| RF-13 | **T-RF13-002** | Então somente uma resulta em reserva aceita e a outra recebe resultado de conflito. | — | SEM_TESTE (próxima fase RED) | T-RN04-001, T-RF10-009 |
| RF-14 | **T-RF14-001** | Então o sistema altera o estado para `APROVADA` e registra a mudança de estado. | — | SEM_TESTE (próxima fase RED) | T-RN06-002, T-RF01-002 |
| RF-14 | **T-RF14-002** | Então o sistema recusa a operação. | — | SEM_TESTE (próxima fase RED) | T-RN06-004, T-RN06-005 |
| RF-14 | **T-RF14-003** | Então o sistema altera o estado para `REJEITADA` e registra a mudança. | — | SEM_TESTE (próxima fase RED) | T-RN06-003 |
| RF-15 | **T-RF15-001** | Então o sistema registra o resultado da validação. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-15 | **T-RF15-002** | Então o sistema informa o conflito e não a considera validada. | — | SEM_TESTE (próxima fase RED) | T-RN03-003 |
| RF-16 | **T-RF16-001** | Então o sistema grava a retirada e permite seu acompanhamento. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-16 | **T-RF16-002** | Então o sistema recusa a operação. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-17 | **T-RF17-001** | Então o sistema grava a devolução e permite seu acompanhamento. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-17 | **T-RF17-002** | Então o sistema recusa a operação e apresenta uma mensagem de erro compreensível. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-18 | **T-RF18-001** | Então o sistema representa os estados oficiais na ordem especificada. | — | SEM_TESTE (próxima fase RED) | T-RN07-001 |
| RF-18 | **T-RF18-002** | Então o sistema recusa a operação. | — | SEM_TESTE (próxima fase RED) | T-RN08-004, T-RN07-005 |
| RF-19 | **T-RF19-001** | Então o sistema cria um registro de auditoria consultável. | — | SEM_TESTE (próxima fase RED) | T-RN09-001, T-RN09-002 |
| RF-19 | **T-RF19-002** | Então o sistema apresenta os registros auditáveis correspondentes. | — | SEM_TESTE (próxima fase RED) | T-RN09-003 |
| RF-20 | **T-RF20-001** | Então o sistema produz a notificação simulada ou realiza a chamada externa definida. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-20 | **T-RF20-002** | Então o sistema registra o resultado da falha de forma segura e observável. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-21 | **T-RF21-001** | Então o sistema apresenta utilização por recurso, carga horária alocada e conflitos evitados. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-21 | **T-RF21-002** | Então o sistema recusa a operação. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-22 | **T-RF22-001** | Então os controles e mensagens permanecem utilizáveis sem sobreposição de conteúdo. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-22 | **T-RF22-002** | Então o sistema apresenta mensagem compreensível e não expõe informação sensível. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-23 | **T-RF23-001** | Então ela descreve os fluxos ou contratos disponibilizados pelo sistema. | — | SEM_TESTE (próxima fase RED) | nenhuma |
| RF-23 | **T-RF23-002** | Então essas condições observáveis estão incluídas na documentação. | — | SEM_TESTE (próxima fase RED) | nenhuma |

### 8.5 Reavaliação dos 18 casos bloqueados e do T-RN07-008

**Desbloqueados (3), todos com origem documental registrada:**

| Caso | Origem da decisão | Resultado |
|---|---|---|
| T-RN05-007 | PRD RF-11 critério 2 (reserva que "usa recurso em manutenção" é recusada ao alterar) e enunciado ("recursos em manutenção não podem ser reservados"). | `@Disabled` removido; passou sem alteração de produção (REGRA_JA_IMPLEMENTADA). |
| T-RN06-006 | docs/arquitetura.md §38 (RF-14/RF-15: "autorização e revalidação antes da decisão") e ADR-005 (revalidar manutenção na confirmação). | `@Disabled` removido; passou sem alteração de produção (REGRA_JA_IMPLEMENTADA). |
| T-RN07-008 | Instrução da fase GREEN: validar só "término posterior ao início" (23:59:59 → 00:00:00 do dia seguinte). Teste corrigido; sem relógio injetável (`java.time.Clock` não foi necessário). | Passou (REGRA_JA_IMPLEMENTADA; era TESTE_INCORRETO). O trecho "timestamp de auditoria" do plano não é verificado (não há regra de virada do dia nem de auditoria temporal). |

**Mantidos bloqueados (16), sem invenção de comportamento:**

| Caso | Por que o enunciado/documentos não resolvem |
|---|---|
| T-RN01-003, T-RF10-010 | O enunciado só exige término posterior ao início e lista a duração mínima entre os detalhes que não pode ser inventada. |
| T-RN02-007, T-RN03-006, T-RN04-005, T-RN05-010 | Política de adjacência (Q-002): nem o enunciado nem a arquitetura dizem se fim = início conflita. |
| T-RN03-008 | "Trata como sem agenda OU recusa": duas interpretações; nada as distingue. |
| T-RN03-009 | "Recusada conforme política": política de alteração de reserva com professor alocado inexistente. |
| T-RN04-006 | Resultado da Request 2 ("conforme status") e critério de recurso restrito (Q-003) indefinidos. |
| T-RN04-008, T-RN09-009 | Política de timestamp, precisão e desempate: listada como não inventável. |
| T-RN06-009 | Responsabilidade do Responsável por recurso (Q-004): o enunciado só define o perfil. |
| T-RN07-010 | Origem, ator e condição de NAO_COMPARECEU (Q-006) indefinidos. |
| T-RN09-008 | Política de auditoria de operações recusadas: listada como não inventável. |
| T-RN10-010 | Sem critério verificável para "matriz atualizada em paralelo à implementação". |
| T-RF01-008 | Política de múltiplos perfis: listada como não inventável. |

### 8.6 Decisões, divergências e conflitos registrados

- **Decisões extraídas do enunciado** (todas já compatíveis com os documentos do projeto): conflito considera sala, material e professor; solicitações simultâneas → uma reserva; recurso em manutenção não pode ser reservado; somente Responsável aprova; toda mudança de estado gera auditoria. O enunciado não substituiu nenhuma decisão existente.
- **Mensagens:** todas as mensagens ajustadas (T-RN02-009, T-RN03-007, T-RN06-004/005, T-RN07-009) são literais do plano; nenhuma frase arbitrária foi tratada como requisito. Nenhuma asserção foi enfraquecida.
- **Conflito com ADR-002 (registrado antes da implementação):** a ADR-002 (PENDENTE DE EXPERIMENTO) proíbe selecionar mecanismo de concorrência antes de experimento com banco containerizado e mantém "implementação definitiva bloqueada". O requisito oficial e T-RF10-009 exigem uma única reserva aceita. Foi implementada somente exclusão mútua local ao processo (seção crítica única de verificação + registro), sem escolher constraint, lock ou isolamento no banco, e a ADR-002 não foi alterada. Continuam pendentes: mecanismo definitivo sobre persistência, teste com Testcontainers (RNF-07, RNF-09) e ADR-004 (mecanismo transacional estado + auditoria).
- **Testcontainers/WireMock:** não foram adicionados. Não existe camada de persistência nem integração externa no projeto e nenhum teste RED do escopo depende delas.
- **RN-03 no serviço:** ao aceitar a reserva, `ServicoCriacaoReserva` passa a registrar o período na agenda do professor alocado; sem isso a verificação de conflito de professor (ADR-005) não protegeria reservas criadas pelo próprio serviço.
- **Cobertura JaCoCo (informativo):** branches em 67,3%, abaixo da meta de 70% do RNF-03; o pipeline não impõe gate e nenhum teste foi adicionado para cobri-la.
- **Contagens do plano:** §5.1 ("100+ casos") e §4.4 (lacunas de cobertura de RFs) refletem o estado de 2026-09-11; os casos adicionais estão na seção 3.A.

### 8.7 Classes de produção alteradas

`ValidadorSobreposicao`, `ReservaSobreposicaoException`, `ServicoCriacaoReserva`, `ValidadorAprovacao`, `Reserva` (setter do motivo), `ValidadorAutorizacao`, `ValidadorAgendaProfessor`, `ValidadorFluxoEstados`, `ValidadorAuditoria`. Testes alterados: apenas `ReservaManutencao_RN05_Test` (T-RN05-007), `ReservaAprovacao_RN06_Test` (T-RN06-006) e `ReservaFluxoEstados_RN07_Test` (T-RN07-008), nos três casos da seção 8.5.

### 8.8 Gate final do QA

| Verificação | Resultado |
|---|---|
| REDs reproduzidos antes da implementação | Sim (13 de 13) |
| REDs com comportamento definido ficaram verdes | Sim (13 de 13) |
| Nenhum teste apagado | Sim (123 casos, 123 testes) |
| Nenhum teste novo desabilitado | Sim (os `@Disabled` caíram de 19 para 16) |
| Nenhuma asserção enfraquecida | Sim; os três testes editados estão detalhados em 8.5 |
| Resultado esperado modificado só com justificativa documental | Sim (apenas T-RN07-008, por instrução expressa) |
| Nenhum comportamento inventado | Sim (16 bloqueios mantidos) |
| Testes aprovados na fase RED continuam aprovados | Sim |
| Casos desbloqueados com origem registrada | Sim (8.5) |
| Matriz atualizada | Sim (8.2 e 8.4) |
| Componentes e ADRs respeitados | Sim, com o conflito de ADR-002 registrado em 8.6 |
| `verify` no estado final | BUILD SUCCESS |
| **Conclusão** | **GREEN concluído para os casos com comportamento definido.** Não significa requisitos concluídos: 16 casos seguem bloqueados por lacunas reais e 44 casos T-RFnn ainda não têm teste. |

---

## 9. Novo ciclo RED - casos T-RFnn da seção 3.A

> As seções 7 (RED) e 8 (GREEN) foram preservadas. Esta seção substitui o status "SEM_TESTE (próxima fase RED)" dos 44 casos da seção 8.4 e reclassifica T-RN10-003, que deixa de ser GREEN.
> **Execução:** 2026-09-25 · `mvn -B -o verify` (sem Maven Wrapper) · **BUILD FAILURE esperado** (fase RED): 154 execuções de teste · 106 aprovadas · 32 falhas · 0 erros · 16 ignorados (os mesmos 16 bloqueios anteriores, intactos).
> **JaCoCo (informativo; gerado com `-Dmaven.test.failure.ignore=true`, pois o verify normal para na falha dos testes):** 79,4% de linhas e 61,8% de branches (antes: 84,0% e 67,3%); a queda vem dos contratos mínimos ainda sem lógica.
> As 32 falhas são 31 execuções dos 22 testes novos (alguns parametrizados) e T-RN10-003. Os 123 testes anteriores continuam como estavam, exceto T-RN10-003.

### 9.1 Revisão individual dos 44 casos

Cada caso foi conferido contra o critério de aceitação do PRD (seção 7), a arquitetura (§12, §13.2, §14.3, §38, ADRs), os testes existentes e o código atual. Todos têm identificador único, requisito e critério de origem, e resultado esperado transcrito do PRD sem acréscimo. Foram corrigidos os "Tipo" de 7 casos que a heurística da fase anterior classificou errado (T-RF06-002, T-RF07-002, T-RF08-002, T-RF11-001, T-RF18-002, T-RF21-001, T-RF22-001).

| Classificação inicial | Casos |
|---|---|
| APTO_PARA_RED | 22 |
| DUPLICADO | 10 |
| BLOQUEADO_POR_LACUNA | 11 |
| SEM_COMPORTAMENTO_VERIFICAVEL | 1 (T-RF22-002) |
| FORA_DO_ESCOPO_DO_REQUISITO | 0 |
| **Total** | **44** |

### 9.2 Resultado por status (após a execução)

| Status | Casos |
|---|---|
| RED_FALHA_ESPERADA | 22 (22 testes JUnit, 31 execuções por parametrização) |
| REGRA_JA_IMPLEMENTADA (teste novo que passou) | 0 |
| DUPLICADO (sem teste novo; coberto por teste existente) | 10 |
| BLOQUEADO_POR_LACUNA (sem teste) | 11 |
| SEM_COMPORTAMENTO_VERIFICAVEL (sem teste) | 1 |
| TESTE_INCORRETO / FALHA_DE_COMPILACAO / FALHA_DE_INFRAESTRUTURA | 0 / 0 / 0 |
| **Total** | **44** |

Nenhum caso válido ficou sem teste. Os 22 testes novos são JUnit 5 com Arrange-Act-Assert, com o identificador T-RFnn no `@DisplayName`. Onde vários valores representam a mesma regra usei `@ParameterizedTest` (T-RF05-001/002, T-RF09-002, T-RF11-002, T-RF12-002).

### 9.3 Matriz RF → caso → teste → status (44 casos)

| RF | Caso | Requisito de origem | Comportamento esperado | Classe de teste | Método ou @DisplayName | Nível de teste | Status | Motivo | Evidência da execução | Lacuna ou duplicidade |
|---|---|---|---|---|---|---|---|---|---|---|
| RF-02 | **T-RF02-001** | RF-02, critério de aceitação 1 (docs/prd.md:7.2) | Então o sistema registra a sala e permite sua consulta. | `GestaoSalas_RF02_Test` | T-RF02-001: Happy Path - Administrador cadastra uma sala e ela pode ser consultada | unitário (contrato CadastroRecursos) | **RED_FALHA_ESPERADA** | Consulta devolve lista vazia: cadastro de sala não implementado. | mvn -B verify 2026-09-25: 1 execução(ões), todas FALHARAM |  |
| RF-02 | **T-RF02-002** | RF-02, critério de aceitação 2 (docs/prd.md:7.2) | Então o sistema recusa a operação. | `GestaoSalas_RF02_Test` | T-RF02-002: Forbidden State - Solicitante sem permissão de gestão não cadastra sala | unitário (contrato CadastroRecursos) | **RED_FALHA_ESPERADA** | Cadastro por Solicitante não é recusado (não lança AcessoNegadoException): autorização não implementada. | mvn -B verify 2026-09-25: 1 execução(ões), todas FALHARAM |  |
| RF-03 | **T-RF03-001** | RF-03, critério de aceitação 1 (docs/prd.md:7.3) | Então o sistema registra o professor e permite sua consulta. | `GestaoProfessores_RF03_Test` | T-RF03-001: Happy Path - Administrador cadastra um professor e ele pode ser consultado | unitário (contrato CadastroRecursos) | **RED_FALHA_ESPERADA** | Consulta devolve lista vazia: cadastro de professor não implementado. | mvn -B verify 2026-09-25: 1 execução(ões), todas FALHARAM |  |
| RF-03 | **T-RF03-002** | RF-03, critério de aceitação 2 (docs/prd.md:7.3) | Então o sistema recusa a operação. | `GestaoProfessores_RF03_Test` | T-RF03-002: Forbidden State - Solicitante sem permissão de gestão não cadastra professor | unitário (contrato CadastroRecursos) | **RED_FALHA_ESPERADA** | Cadastro por Solicitante não é recusado: autorização não implementada. | mvn -B verify 2026-09-25: 1 execução(ões), todas FALHARAM |  |
| RF-04 | **T-RF04-001** | RF-04, critério de aceitação 1 (docs/prd.md:7.4) | Então o sistema registra o material e permite sua consulta. | `GestaoMateriais_RF04_Test` | T-RF04-001: Happy Path - Administrador cadastra um material e ele pode ser consultado | unitário (contrato CadastroRecursos) | **RED_FALHA_ESPERADA** | Consulta devolve lista vazia: cadastro de material não implementado. | mvn -B verify 2026-09-25: 1 execução(ões), todas FALHARAM |  |
| RF-04 | **T-RF04-002** | RF-04, critério de aceitação 2 (docs/prd.md:7.4) | Então o sistema recusa a operação. | `GestaoMateriais_RF04_Test` | T-RF04-002: Forbidden State - Solicitante sem permissão de gestão não cadastra material | unitário (contrato CadastroRecursos) | **RED_FALHA_ESPERADA** | Cadastro por Solicitante não é recusado: autorização não implementada. | mvn -B verify 2026-09-25: 1 execução(ões), todas FALHARAM |  |
| RF-05 | **T-RF05-001** | RF-05, critério de aceitação 1 (docs/prd.md:7.5) | Então o sistema registra a alteração para uso na autorização. | `GestaoUsuarios_RF05_Test` | T-RF05-001: Happy Path - Administrador gerencia usuário com perfil oficial | unitário parametrizado (contrato GestaoUsuarios) | **RED_FALHA_ESPERADA** | Perfil não é alterado (esperado RESPONSAVEL e ADMINISTRADOR, obtido SOLICITANTE) nas 2 execuções parametrizadas: gestão de usuários não implementada. | mvn -B verify 2026-09-25: 2 execução(ões), todas FALHARAM |  |
| RF-05 | **T-RF05-002** | RF-05, critério de aceitação 2 (docs/prd.md:7.5) | Então o sistema recusa a operação. | `GestaoUsuarios_RF05_Test` | T-RF05-002: Forbidden State - Usuário que não é Administrador não gerencia usuários | unitário parametrizado (contrato GestaoUsuarios) | **RED_FALHA_ESPERADA** | Solicitante e Responsável não são recusados (2 execuções parametrizadas): autorização não implementada. | mvn -B verify 2026-09-25: 2 execução(ões), todas FALHARAM |  |
| RF-06 | **T-RF06-001** | RF-06, critério de aceitação 1 (docs/prd.md:7.6) | Então o sistema registra o bloqueio e o considera indisponível no período correspondente. | `GestaoBloqueios_RF06_Test` | T-RF06-001: Happy Path - Administrador cria bloqueio e o recurso fica indisponível no período | unitário (contrato GestaoBloqueios) | **RED_FALHA_ESPERADA** | Recurso continua disponível após o bloqueio: gestão de bloqueios não implementada. | mvn -B verify 2026-09-25: 1 execução(ões), todas FALHARAM |  |
| RF-06 | **T-RF06-002** | RF-06, critério de aceitação 2 (docs/prd.md:7.6) | Então o sistema recusa a operação. | `GestaoBloqueios_RF06_Test` | T-RF06-002: Forbidden State - Solicitante não gerencia bloqueios | unitário (contrato GestaoBloqueios) | **RED_FALHA_ESPERADA** | Registro por Solicitante não é recusado: autorização não implementada. | mvn -B verify 2026-09-25: 1 execução(ões), todas FALHARAM |  |
| RF-07 | **T-RF07-001** | RF-07, critério de aceitação 1 (docs/prd.md:7.7) | Então o sistema bloqueia reservas do recurso nesse período. | — | — | coberto por teste existente | **DUPLICADO** | O comportamento observável já é verificado por teste existente; não foi criado teste novo. | ver testes citados | Coberto por T-RF10-008, T-RN05-003. Registrar manutenção e ver a reserva recusada já é verificado nesses testes. Evidência Testcontainers do plano não coberta (persistência real). |
| RF-07 | **T-RF07-002** | RF-07, critério de aceitação 2 (docs/prd.md:7.7) | Então o sistema recusa a reserva e informa a indisponibilidade. | — | — | coberto por teste existente | **DUPLICADO** | O comportamento observável já é verificado por teste existente; não foi criado teste novo. | ver testes citados | Coberto por T-RF10-008, T-RN05-003. A recusa com mensagem que cita a manutenção é a de T-RF10-008. Evidência Testcontainers do plano não coberta. |
| RF-08 | **T-RF08-001** | RF-08, critério de aceitação 1 (docs/prd.md:7.8) | Então o sistema retorna os recursos cadastrados para consulta. | `ConsultaRecursos_RF08_Test` | T-RF08-001: Happy Path - Solicitante consulta salas, professores e materiais cadastrados | unitário (contrato CadastroRecursos) | **RED_FALHA_ESPERADA** | Consultas devolvem lista vazia: cadastro e consulta de recursos não implementados. | mvn -B verify 2026-09-25: 1 execução(ões), todas FALHARAM |  |
| RF-08 | **T-RF08-002** | RF-08, critério de aceitação 2 (docs/prd.md:7.8) | Então o sistema informa que o recurso não pode ser reservado no período de manutenção. | — | — | coberto por teste existente | **DUPLICADO** | O comportamento observável já é verificado por teste existente; não foi criado teste novo. | ver testes citados | Coberto por T-RN05-008. Mesma verificação: disponibilidade de recurso em manutenção é falsa. |
| RF-09 | **T-RF09-001** | RF-09, critério de aceitação 1 (docs/prd.md:7.9) | Então o sistema retorna os recursos compatíveis com os filtros informados. | — | — | — | **BLOQUEADO_POR_LACUNA** | Os filtros capacidade, localização e competência não têm atributos definidos nos cadastros (plano §4.7; PRD §11: "campos de cadastro não especificados"). | sem teste | Os filtros capacidade, localização e competência não têm atributos definidos nos cadastros (plano §4.7; PRD §11: "campos de cadastro não especificados"). |
| RF-09 | **T-RF09-002** | RF-09, critério de aceitação 2 (docs/prd.md:7.9) | Então o sistema não o apresenta como disponível para reserva naquele período. | `DisponibilidadeRecursos_RF09_Test` | T-RF09-002: Conflicts - Recurso indisponível no período não é apresentado como disponível | integração de componentes parametrizada (contrato ConsultaDisponibilidade) | **RED_FALHA_ESPERADA** | Nos 4 impedimentos (reservado, bloqueado, em manutenção, professor com agenda ocupada) o recurso é apresentado como disponível: pesquisa de disponibilidade não implementada. | mvn -B verify 2026-09-25: 4 execução(ões), todas FALHARAM |  |
| RF-11 | **T-RF11-001** | RF-11, critério de aceitação 1 (docs/prd.md:7.11) | Então o sistema salva a alteração sem conflito. | `AlteracaoReserva_RF11_Test` | T-RF11-001: Happy Path - Solicitante altera a própria reserva para um período livre | integração de componentes (contrato ServicoGestaoReserva) | **RED_FALHA_ESPERADA** | Período da reserva não é alterado (esperado 10:00, obtido 08:00): alteração não implementada. | mvn -B verify 2026-09-25: 1 execução(ões), todas FALHARAM |  |
| RF-11 | **T-RF11-002** | RF-11, critério de aceitação 2 (docs/prd.md:7.11) | Então o sistema recusa a operação. | `AlteracaoReserva_RF11_Test` | T-RF11-002: Forbidden State - Alteração recusada quando não permitida | integração de componentes parametrizada (contrato ServicoGestaoReserva) | **RED_FALHA_ESPERADA** | Nas 4 situações (outro solicitante, iniciada, conflito, manutenção) a alteração não é recusada: regras de alteração não implementadas. | mvn -B verify 2026-09-25: 4 execução(ões), todas FALHARAM |  |
| RF-11 | **T-RF11-003** | RF-11, critério de aceitação 3 (docs/prd.md:7.11) | Então o sistema gera registro de auditoria. | — | — | — | **BLOQUEADO_POR_LACUNA** | Não está definido qual alteração muda o estado da reserva (arquitetura §12, itens 3 e 6: estados que permitem alteração e nova aprovação após alteração). | sem teste | Não está definido qual alteração muda o estado da reserva (arquitetura §12, itens 3 e 6: estados que permitem alteração e nova aprovação após alteração). |
| RF-12 | **T-RF12-001** | RF-12, critério de aceitação 1 (docs/prd.md:7.12) | Então o sistema altera a reserva para `CANCELADA` e registra a mudança. | `CancelamentoReserva_RF12_Test` | T-RF12-001: Happy Path - Solicitante cancela a própria reserva ainda não iniciada | integração de componentes (contrato ServicoGestaoReserva) | **RED_FALHA_ESPERADA** | Estado continua SOLICITADA (esperado CANCELADA) e não há auditoria da mudança: cancelamento não implementado. | mvn -B verify 2026-09-25: 1 execução(ões), todas FALHARAM |  |
| RF-12 | **T-RF12-002** | RF-12, critério de aceitação 2 (docs/prd.md:7.12) | Então o sistema recusa a operação e informa o motivo. | `CancelamentoReserva_RF12_Test` | T-RF12-002: Forbidden State - Cancelamento recusado com motivo informado | integração de componentes parametrizada (contrato ServicoGestaoReserva) | **RED_FALHA_ESPERADA** | Nas 2 situações (iniciada, outro solicitante) o cancelamento não é recusado: regras de cancelamento não implementadas. | mvn -B verify 2026-09-25: 2 execução(ões), todas FALHARAM |  |
| RF-13 | **T-RF13-001** | RF-13, critério de aceitação 1 (docs/prd.md:7.13) | Então o sistema recusa a nova reserva e informa o conflito. | — | — | coberto por teste existente | **DUPLICADO** | O comportamento observável já é verificado por teste existente; não foi criado teste novo. | ver testes citados | Coberto por T-RF10-005, T-RF10-006, T-RF10-007, T-RN02-003. Sobreposição de sala, material e professor já verificada. Limites de intervalo dependem de Q-002 (adjacência). |
| RF-13 | **T-RF13-002** | RF-13, critério de aceitação 2 (docs/prd.md:7.13) | Então somente uma resulta em reserva aceita e a outra recebe resultado de conflito. | — | — | coberto por teste existente | **DUPLICADO** | O comportamento observável já é verificado por teste existente; não foi criado teste novo. | ver testes citados | Coberto por T-RN04-001, T-RF10-009. Dupla simultânea já verificada com threads. Evidência Testcontainers do plano não coberta. |
| RF-14 | **T-RF14-001** | RF-14, critério de aceitação 1 (docs/prd.md:7.14) | Então o sistema altera o estado para `APROVADA` e registra a mudança de estado. | `AprovacaoSolicitacoes_RF14_Test` | T-RF14-001: Happy Path - Responsável aprova solicitação especial pendente | integração de componentes (ValidadorAprovacao, ServicoCriacaoReserva, ValidadorAuditoria) | **RED_FALHA_ESPERADA** | O estado já passa a APROVADA (T-RN06-002), mas ValidadorAprovacao.aprovar não registra a auditoria da mudança de estado. Parte do teste já é atendida; falha só na auditoria. | mvn -B verify 2026-09-25: 1 execução(ões), todas FALHARAM |  |
| RF-14 | **T-RF14-002** | RF-14, critério de aceitação 2 (docs/prd.md:7.14) | Então o sistema recusa a operação. | — | — | coberto por teste existente | **DUPLICADO** | O comportamento observável já é verificado por teste existente; não foi criado teste novo. | ver testes citados | Coberto por T-RN06-004, T-RN06-005. Solicitante e Administrador já cobertos. |
| RF-14 | **T-RF14-003** | RF-14, critério de aceitação 3 (docs/prd.md:7.14) | Então o sistema altera o estado para `REJEITADA` e registra a mudança. | `AprovacaoSolicitacoes_RF14_Test` | T-RF14-003: Happy Path - Responsável rejeita solicitação especial | integração de componentes (ValidadorAprovacao, ServicoCriacaoReserva, ValidadorAuditoria) | **RED_FALHA_ESPERADA** | O estado já passa a REJEITADA, mas ValidadorAprovacao.rejeitar não registra a auditoria da mudança de estado. Parte do teste já é atendida; falha só na auditoria. | mvn -B verify 2026-09-25: 1 execução(ões), todas FALHARAM |  |
| RF-15 | **T-RF15-001** | RF-15, critério de aceitação 1 (docs/prd.md:7.15) | Então o sistema registra o resultado da validação. | — | — | — | **BLOQUEADO_POR_LACUNA** | O PRD diz "alocação é validada ou não validada conforme a verificação do Responsável" mas não define o que é o "resultado" nem onde é registrado. | sem teste | O PRD diz "alocação é validada ou não validada conforme a verificação do Responsável" mas não define o que é o "resultado" nem onde é registrado. |
| RF-15 | **T-RF15-002** | RF-15, critério de aceitação 2 (docs/prd.md:7.15) | Então o sistema informa o conflito e não a considera validada. | `ValidacaoAlocacaoDocente_RF15_Test` | T-RF15-002: Conflicts - Alocação em conflito com a agenda do professor informa o conflito e não é validada | integração de componentes (contrato ValidacaoAlocacaoDocente, Professor) | **RED_FALHA_ESPERADA** | Nenhum conflito é informado (não lança ReservaAgendaProfessorException): validação da alocação não implementada. | mvn -B verify 2026-09-25: 1 execução(ões), todas FALHARAM |  |
| RF-16 | **T-RF16-001** | RF-16, critério de aceitação 1 (docs/prd.md:7.16) | Então o sistema grava a retirada e permite seu acompanhamento. | — | — | — | **BLOQUEADO_POR_LACUNA** | A evidência exige persistência realista (RNF-07); o banco não está definido (o enunciado não autoriza escolher), o projeto não tem camada de persistência e o daemon Docker não está disponível no ambiente. | sem teste | A evidência exige persistência realista (RNF-07); o banco não está definido (o enunciado não autoriza escolher), o projeto não tem camada de persistência e o daemon Docker não está disponível no ambiente. |
| RF-16 | **T-RF16-002** | RF-16, critério de aceitação 2 (docs/prd.md:7.16) | Então o sistema recusa a operação. | `RetiradaMateriais_RF16_Test` | T-RF16-002: Forbidden State - Solicitante sem permissão não registra retirada | unitário (contrato MovimentacaoMateriais) | **RED_FALHA_ESPERADA** | Registro de retirada por Solicitante não é recusado: autorização não implementada. | mvn -B verify 2026-09-25: 1 execução(ões), todas FALHARAM |  |
| RF-17 | **T-RF17-001** | RF-17, critério de aceitação 1 (docs/prd.md:7.17) | Então o sistema grava a devolução e permite seu acompanhamento. | — | — | — | **BLOQUEADO_POR_LACUNA** | Mesma dependência de persistência realista (RNF-07) e associação à retirada persistida; banco indefinido e Docker indisponível. | sem teste | Mesma dependência de persistência realista (RNF-07) e associação à retirada persistida; banco indefinido e Docker indisponível. |
| RF-17 | **T-RF17-002** | RF-17, critério de aceitação 2 (docs/prd.md:7.17) | Então o sistema recusa a operação e apresenta uma mensagem de erro compreensível. | `DevolucaoMateriais_RF17_Test` | T-RF17-002: Invalid Input - Devolução sem retirada correspondente é recusada com mensagem compreensível | unitário (contrato MovimentacaoMateriais) | **RED_FALHA_ESPERADA** | Devolução sem retirada não é recusada: registro de devolução não implementado. | mvn -B verify 2026-09-25: 1 execução(ões), todas FALHARAM |  |
| RF-18 | **T-RF18-001** | RF-18, critério de aceitação 1 (docs/prd.md:7.18) | Então o sistema representa os estados oficiais na ordem especificada. | — | — | coberto por teste existente | **DUPLICADO** | O comportamento observável já é verificado por teste existente; não foi criado teste novo. | ver testes citados | Coberto por T-RN07-001. Fluxo principal de estados já verificado. |
| RF-18 | **T-RF18-002** | RF-18, critério de aceitação 2 (docs/prd.md:7.18) | Então o sistema recusa a operação. | — | — | coberto por teste existente | **DUPLICADO** | O comportamento observável já é verificado por teste existente; não foi criado teste novo. | ver testes citados | Coberto por T-RN08-004, T-RN07-005. Apagar reserva iniciada e estado não oficial já verificados. |
| RF-19 | **T-RF19-001** | RF-19, critério de aceitação 1 (docs/prd.md:7.19) | Então o sistema cria um registro de auditoria consultável. | — | — | coberto por teste existente | **DUPLICADO** | O comportamento observável já é verificado por teste existente; não foi criado teste novo. | ver testes citados | Coberto por T-RN09-001, T-RN09-002, T-RF10-014. Registro consultável já verificado em memória. Evidência Testcontainers do plano não coberta (RNF-07). |
| RF-19 | **T-RF19-002** | RF-19, critério de aceitação 2 (docs/prd.md:7.19) | Então o sistema apresenta os registros auditáveis correspondentes. | — | — | coberto por teste existente | **DUPLICADO** | O comportamento observável já é verificado por teste existente; não foi criado teste novo. | ver testes citados | Coberto por T-RN09-003. Consulta do histórico já verificada. A restrição por perfil Administrador não é exigida pelo PRD (RF-19 atende os três perfis). |
| RF-20 | **T-RF20-001** | RF-20, critério de aceitação 1 (docs/prd.md:7.20) | Então o sistema produz a notificação simulada ou realiza a chamada externa definida. | — | — | — | **BLOQUEADO_POR_LACUNA** | Alternativa de notificação/integração e evento do fluxo pendentes (arquitetura J15 e ADR-009); WireMock/API externa não definidos. | sem teste | Alternativa de notificação/integração e evento do fluxo pendentes (arquitetura J15 e ADR-009); WireMock/API externa não definidos. |
| RF-20 | **T-RF20-002** | RF-20, critério de aceitação 2 (docs/prd.md:7.20) | Então o sistema registra o resultado da falha de forma segura e observável. | — | — | — | **BLOQUEADO_POR_LACUNA** | Mesma pendência (J15, ADR-009); a API externa e o registro seguro da falha não estão definidos. | sem teste | Mesma pendência (J15, ADR-009); a API externa e o registro seguro da falha não estão definidos. |
| RF-21 | **T-RF21-001** | RF-21, critério de aceitação 1 (docs/prd.md:7.21) | Então o sistema apresenta utilização por recurso, carga horária alocada e conflitos evitados. | — | — | — | **BLOQUEADO_POR_LACUNA** | Fórmulas dos indicadores pendentes (arquitetura §38: "fórmula pendente"; §12, item 10). | sem teste | Fórmulas dos indicadores pendentes (arquitetura §38: "fórmula pendente"; §12, item 10). |
| RF-21 | **T-RF21-002** | RF-21, critério de aceitação 2 (docs/prd.md:7.21) | Então o sistema recusa a operação. | `RelatoriosOperacionais_RF21_Test` | T-RF21-002: Forbidden State - Solicitante sem permissão não consulta relatórios operacionais | unitário (contrato ServicoRelatorios) | **RED_FALHA_ESPERADA** | Consulta por Solicitante não é recusada: autorização de relatórios não implementada. | mvn -B verify 2026-09-25: 1 execução(ões), todas FALHARAM |  |
| RF-22 | **T-RF22-001** | RF-22, critério de aceitação 1 (docs/prd.md:7.22) | Então os controles e mensagens permanecem utilizáveis sem sobreposição de conteúdo. | — | — | — | **BLOQUEADO_POR_LACUNA** | Viewports suportados pendentes (arquitetura §38) e não existe interface. | sem teste | Viewports suportados pendentes (arquitetura §38) e não existe interface. |
| RF-22 | **T-RF22-002** | RF-22, critério de aceitação 2 (docs/prd.md:7.22) | Então o sistema apresenta mensagem compreensível e não expõe informação sensível. | — | — | — | **SEM_COMPORTAMENTO_VERIFICAVEL** | "Mensagem compreensível" é critério subjetivo (avaliado por revisão de QA, RNF-17) e "informação sensível" não está definida; não há camada de apresentação para observar a mensagem. | sem teste | "Mensagem compreensível" é critério subjetivo (avaliado por revisão de QA, RNF-17) e "informação sensível" não está definida; não há camada de apresentação para observar a mensagem. |
| RF-23 | **T-RF23-001** | RF-23, critério de aceitação 1 (docs/prd.md:7.23) | Então ela descreve os fluxos ou contratos disponibilizados pelo sistema. | — | — | — | **BLOQUEADO_POR_LACUNA** | API/fluxos públicos e sua documentação não estão definidos (arquitetura §13.2: protocolos e rotas a definir; RNF-18: quantidade de fluxos pendente). | sem teste | API/fluxos públicos e sua documentação não estão definidos (arquitetura §13.2: protocolos e rotas a definir; RNF-18: quantidade de fluxos pendente). |
| RF-23 | **T-RF23-002** | RF-23, critério de aceitação 2 (docs/prd.md:7.23) | Então essas condições observáveis estão incluídas na documentação. | — | — | — | **BLOQUEADO_POR_LACUNA** | Mesma pendência: nenhum fluxo público documentado com estados, autorização e erros foi escolhido. | sem teste | Mesma pendência: nenhum fluxo público documentado com estados, autorização e erros foi escolhido. |

### 9.4 Casos sem teste (destaque)

Não há caso válido sem teste. Os 22 casos abaixo não têm teste JUnit próprio **por não serem testáveis agora**, e não são aprovação: 10 estão cobertos por testes existentes (DUPLICADO), 11 estão bloqueados e 1 não tem comportamento verificável.

- **BLOQUEADO_POR_LACUNA (11):** T-RF09-001, T-RF11-003, T-RF15-001, T-RF16-001, T-RF17-001, T-RF20-001, T-RF20-002, T-RF21-001, T-RF22-001, T-RF23-001, T-RF23-002 (motivos na matriz).
- **SEM_COMPORTAMENTO_VERIFICAVEL (1):** T-RF22-002.
- **DUPLICADO (10):** T-RF07-001, T-RF07-002, T-RF08-002, T-RF13-001, T-RF13-002, T-RF14-002, T-RF18-001, T-RF18-002, T-RF19-001, T-RF19-002.
- **RFs MUST sem nenhum teste automatizado (nem por duplicidade): RF-20, RF-22 e RF-23.** Escalados (T-RN10-008).

### 9.5 Contratos mínimos criados em `src/main/java/` (sem lógica)

Nenhuma regra de negócio foi implementada. Os nomes e assinaturas são propostas mínimas para o teste compilar (o plano e a arquitetura não definem essas interfaces, §13.2: "protocolos, rotas, payloads a definir"); cada método tem corpo vazio ou devolve valor neutro que faz o teste falhar (`List.of()`, `true`, `false`, o próprio argumento):

`CadastroRecursos` (RF-02, 03, 04, 08), `GestaoUsuarios` (RF-05), `GestaoBloqueios` (RF-06), `ConsultaDisponibilidade` (RF-09), `ServicoGestaoReserva` (RF-11, 12), `ValidacaoAlocacaoDocente` (RF-15), `MovimentacaoMateriais` (RF-16, 17), `ServicoRelatorios` (RF-21).

Se a equipe preferir outros nomes ou uma divisão diferente (por exemplo, estender `ServicoCriacaoReserva`), os testes precisam apenas trocar a classe instanciada. Componentes da arquitetura (§14.3): Recursos (RF-02 a 08), Disponibilidade (RF-09), Reservas (RF-10 a 13), Aprovação (RF-14, 15), Movimentação (RF-16, 17), Relatórios (RF-21).

### 9.6 T-RN10-003 (reclassificado)

O teste deixou de aceitar a mera presença de `T-RFnn-001` no plano. Para cada RF MUST (PRD §9) ele confere agora: (1) existe caso no plano; (2) existe pelo menos um teste JUnit real com o identificador em `@DisplayName` em `src/test/java/`, ou um caso DUPLICADO cuja cobertura citada existe em teste real; (3) todo caso válido (status diferente de DUPLICADO, BLOQUEADO_POR_LACUNA, SEM_COMPORTAMENTO_VERIFICAVEL, FORA_DO_ESCOPO_DO_REQUISITO) tem teste; (4) não há identificadores órfãos (teste ou linha da matriz sem caso no plano); (5) a classe registrada na matriz da seção 9.3 é a classe onde o identificador realmente aparece.

- **Status anterior:** GREEN (seção 8). **Status atual:** **RED_FALHA_ESPERADA**.
- **Falha confirmada:** RF-20, RF-22 e RF-23 não têm nenhum teste JUnit real (todos os seus casos estão bloqueados ou sem comportamento verificável). Não houve aprovação documental artificial.
- Quando os bloqueios de RF-20, RF-22 e RF-23 forem resolvidos e ganharem testes, o teste passa.

### 9.7 Decisões e divergências

- **Fontes:** nenhuma decisão foi tirada do enunciado além do já registrado; o enunciado não define banco, fluxos de notificação, viewports, fórmulas de relatório nem atributos de filtro, então nada disso foi inventado.
- **Persistência real:** nenhum teste simula persistência com HashMap: os casos que exigem persistência realista (T-RF16-001, T-RF17-001) ficaram bloqueados. Testcontainers e WireMock não foram adicionados (sem banco definido, sem API externa definida e daemon Docker indisponível neste ambiente).
- **Nível:** os testes novos são unitários ou de integração entre componentes de domínio. Não existe API HTTP nem interface, então não há teste de API caixa-preta nem E2E. A coluna "Camada(s)" dos casos cita esses níveis; é divergência plano × teste.
- **Casos anteriores bloqueados:** os 16 `@Disabled` foram preservados. Nenhum dos 44 casos dependia da mesma decisão a ponto de exigir alterá-los, exceto T-RF11-003 e T-RF09-001, que ficaram bloqueados por decisões próprias (arquitetura §12 e plano §4.7).
- **T-RF14-001 e T-RF14-003:** parte do teste já é atendida (estado APROVADA/REJEITADA); falha só a auditoria, que `ValidadorAprovacao` não registra. `ValidadorAutorizacao.aprovarSolicitacao` já audita (T-RF01-002); a coexistência das duas rotas de aprovação é observação para a arquitetura.
- **Código de produção:** nenhuma classe existente foi alterada nesta fase.

### 9.8 Gate final do QA

| Verificação | Resultado |
|---|---|
| 44 casos revisados individualmente | Sim (9.1 e matriz) |
| Casos inválidos ou duplicados identificados | Sim (10 duplicados, 11 bloqueados, 1 sem comportamento verificável) |
| Todo caso válido possui teste JUnit | Sim (22 de 22) |
| Cada teste tem identificador e Arrange-Act-Assert | Sim |
| Asserções comportamentais | Sim |
| Cada RED falha pelo motivo esperado | Sim: asserção sobre o comportamento ainda não implementado (lista vazia, valor incorreto ou "Expecting code to raise a throwable"); nenhum erro de compilação ou exceção acidental |
| Cada teste que passa foi investigado | Nenhum teste novo passou; T-RF14-001/003 passam parcialmente (estado), falham na auditoria |
| T-RN10-003 verifica testes reais | Sim (9.6) |
| Nenhuma regra de produção implementada; nenhuma decisão inventada | Sim; somente 8 contratos mínimos sem lógica |
| Matriz atualizada | Sim |
| Commit ou push | Nenhum |
| **Conclusão** | Todos os casos válidos entre os 44 têm teste real e classificação. A suíte está vermelha por definição do ciclo RED; RF-20, RF-22 e RF-23 seguem sem teste automatizado. |

---

*Documento finalizado: 2026-09-11*
*Repositório: `/workspaces/Organizacao-de-Recursos-QA/docs/testes/plano-tdd.md`*
