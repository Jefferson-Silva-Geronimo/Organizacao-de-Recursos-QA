# Plano de TDD - Organização de Recursos

> **Documento:** Plano de Test-Driven Development (TDD)  
> **Projeto:** Organização de Recursos  
> **Data:** 2026-09-11  
> **Status:** Preparação para ciclo de TDD  
> **Propósito:** Inventário consolidado de regras de negócio com casos de teste e rastreabilidade  
> **Observação:** Neste estágio, nenhum teste foi implementado. Este documento descreve o plano de testes antes da codificação.

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

*Documento finalizado: 2026-09-11*
*Repositório: `/workspaces/Organizacao-de-Recursos-QA/docs/testes/plano-tdd.md`*
