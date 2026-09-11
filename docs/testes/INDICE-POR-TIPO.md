# Índice de Testes por Tipo e Camada

> **Propósito:** Localizar rapidamente casos de teste por características  
> **Referência:** [`docs/testes/plano-tdd.md`](plano-tdd.md)  
> **Data:** 2026-09-11

---

## 1. Índice por Camada de Teste

### 1.1 Testes Unitários (Sem Persistência)

Funcionalidade isolada, validação de domínio, lógica pura.

| ID | Regra | Descrição | Localização |
|---|---|---|---|
| T-RN01-001 | RN-01 | Intervalo válido (happy path) | plano-tdd.md, seção 2.1.1 |
| T-RN01-002 | RN-01 | Intervalo longo | plano-tdd.md, seção 2.1.1 |
| T-RN01-003 | RN-01 | Intervalo mínimo (boundary) | plano-tdd.md, seção 2.1.1 |
| T-RN01-004 | RN-01 | Intervalo zero (boundary) | plano-tdd.md, seção 2.1.1 |
| T-RN01-005 | RN-01 | Fim anterior ao início | plano-tdd.md, seção 2.1.1 |
| T-RN01-006 | RN-01 | Data passada | plano-tdd.md, seção 2.1.1 |
| T-RN01-007 | RN-01 | Início nulo | plano-tdd.md, seção 2.1.1 |
| T-RN01-008 | RN-01 | Fim nulo | plano-tdd.md, seção 2.1.1 |
| T-RN01-009 | RN-01 | Formato de hora inválido | plano-tdd.md, seção 2.1.1 |
| T-RN02-003 | RN-02 | Sobreposição total | plano-tdd.md, seção 2.2.1 |
| T-RN03-002 | RN-03 | Professor sem agenda | plano-tdd.md, seção 2.3.1 |
| T-RF01-006 | RF-01 | Token inválido ou expirado | plano-tdd.md, seção 3.1.1 |
| T-RF01-007 | RF-01 | Sem token | plano-tdd.md, seção 3.1.1 |

### 1.2 Testes de Integração (Com Persistência em Testcontainers)

Banco de dados realista, transações, constraints, múltiplas operações.

| ID | Regra | Descrição | Localização |
|---|---|---|---|
| T-RN01-001 | RN-01 | Intervalo válido | plano-tdd.md, seção 2.1.1 |
| T-RN02-001 | RN-02 | Reserva sem conflito | plano-tdd.md, seção 2.2.1 |
| T-RN02-002 | RN-02 | Diferentes recursos mesmo horário | plano-tdd.md, seção 2.2.1 |
| T-RN02-003 | RN-02 | Sobreposição total | plano-tdd.md, seção 2.2.1 |
| T-RN02-004 | RN-02 | Sobreposição parcial (nova começa dentro) | plano-tdd.md, seção 2.2.1 |
| T-RN02-005 | RN-02 | Sobreposição parcial (nova termina dentro) | plano-tdd.md, seção 2.2.1 |
| T-RN02-006 | RN-02 | Sobreposição: nova envolve existente | plano-tdd.md, seção 2.2.1 |
| T-RN02-009 | RN-02 | Múltiplas existentes, conflita com uma | plano-tdd.md, seção 2.2.1 |
| T-RN02-010 | RN-02 | Alterar existente para criar sobreposição | plano-tdd.md, seção 2.2.1 |
| T-RN03-001 | RN-03 | Professor alocado, agenda sem conflito | plano-tdd.md, seção 2.3.1 |
| T-RN03-003 | RN-03 | Sobreposição com agenda do professor | plano-tdd.md, seção 2.3.1 |
| T-RN03-004 | RN-03 | Múltiplos professores, um com conflito | plano-tdd.md, seção 2.3.1 |
| T-RN03-005 | RN-03 | Alterar reserva criando conflito de agenda | plano-tdd.md, seção 2.3.1 |
| T-RN05-001 | RN-05 | Reserva em recurso sem manutenção | plano-tdd.md, seção 2.5.1 |
| T-RN05-002 | RN-05 | Reserva fora do período de manutenção | plano-tdd.md, seção 2.5.1 |
| T-RN05-003 | RN-05 | Reserva em recurso em manutenção | plano-tdd.md, seção 2.5.1 |
| T-RN05-004 | RN-05 | Reserva com manutenção parcial | plano-tdd.md, seção 2.5.1 |
| T-RN05-005 | RN-05 | Múltiplos períodos de manutenção | plano-tdd.md, seção 2.5.1 |
| T-RN05-006 | RN-05 | Material em manutenção | plano-tdd.md, seção 2.5.1 |
| T-RN06-002 | RN-06 | Recurso restrito: Responsável aprova | plano-tdd.md, seção 2.6.1 |
| T-RN06-006 | RN-06 | Responsável aprova recurso indisponível | plano-tdd.md, seção 2.6.1 |
| T-RN06-007 | RN-06 | Responsável aprova duplicada | plano-tdd.md, seção 2.6.1 |
| T-RN07-002 | RN-07 | Cancelamento em SOLICITADA | plano-tdd.md, seção 2.7.1 |
| T-RN07-003 | RN-07 | Rejeição em SOLICITADA | plano-tdd.md, seção 2.7.1 |
| T-RN08-002 | RN-08 | Cancelar após iniciação (EM_USO) | plano-tdd.md, seção 2.8.1 |
| T-RN08-003 | RN-08 | Cancelar após conclusão (CONCLUIDA) | plano-tdd.md, seção 2.8.1 |
| T-RN08-004 | RN-08 | Apagar registro (DELETE) | plano-tdd.md, seção 2.8.1 |
| T-RN08-008 | RN-08 | Auditoria de tentativa de apagamento | plano-tdd.md, seção 2.8.1 |
| T-RN09-001 | RN-09 | Criar reserva gera auditoria | plano-tdd.md, seção 2.9.1 |
| T-RN09-002 | RN-09 | Mudar de SOLICITADA para APROVADA | plano-tdd.md, seção 2.9.1 |
| T-RN09-003 | RN-09 | Consultar histórico de reserva | plano-tdd.md, seção 2.9.1 |
| T-RN09-004 | RN-09 | Múltiplas mudanças em rápida sucessão | plano-tdd.md, seção 2.9.1 |
| T-RF01-001 | RF-01 | Solicitante autenticado consulta | plano-tdd.md, seção 3.1.1 |
| T-RF01-002 | RF-01 | Responsável autenticado aprova | plano-tdd.md, seção 3.1.1 |
| T-RF01-003 | RF-01 | Administrador autenticado gerencia | plano-tdd.md, seção 3.1.1 |
| T-RF01-009 | RF-01 | Transição de perfil (logout/login) | plano-tdd.md, seção 3.1.1 |
| T-RF01-010 | RF-01 | Usuário desativado tenta acessar | plano-tdd.md, seção 3.1.1 |
| T-RF10-015 | RF-10 | Persistência em banco | plano-tdd.md, seção 3.10.1 |

### 1.3 Testes de API (Black-box HTTP)

Endpoints, autorização, respostas, status codes.

| ID | Regra | Descrição | Localização |
|---|---|---|---|
| T-RN01-004 | RN-01 | Intervalo zero (boundary) | plano-tdd.md, seção 2.1.1 |
| T-RN01-005 | RN-01 | Fim anterior ao início | plano-tdd.md, seção 2.1.1 |
| T-RN02-001 | RN-02 | Reserva sem conflito | plano-tdd.md, seção 2.2.1 |
| T-RN02-003 | RN-02 | Sobreposição total | plano-tdd.md, seção 2.2.1 |
| T-RN02-004 | RN-02 | Sobreposição parcial (nova começa dentro) | plano-tdd.md, seção 2.2.1 |
| T-RN02-005 | RN-02 | Sobreposição parcial (nova termina dentro) | plano-tdd.md, seção 2.2.1 |
| T-RN02-006 | RN-02 | Sobreposição: nova envolve existente | plano-tdd.md, seção 2.2.1 |
| T-RN03-003 | RN-03 | Sobreposição com agenda | plano-tdd.md, seção 2.3.1 |
| T-RN03-004 | RN-03 | Múltiplos professores, um com conflito | plano-tdd.md, seção 2.3.1 |
| T-RN03-007 | RN-03 | Professor inexistente | plano-tdd.md, seção 2.3.1 |
| T-RN05-003 | RN-05 | Reserva em recurso em manutenção | plano-tdd.md, seção 2.5.1 |
| T-RN05-004 | RN-05 | Reserva com manutenção parcial | plano-tdd.md, seção 2.5.1 |
| T-RN05-007 | RN-05 | Alterar reserva introduzindo manutenção | plano-tdd.md, seção 2.5.1 |
| T-RN06-004 | RN-06 | Solicitante tenta aprovar | plano-tdd.md, seção 2.6.1 |
| T-RN06-005 | RN-06 | Admin tenta aprovar | plano-tdd.md, seção 2.6.1 |
| T-RN06-008 | RN-06 | Recurso inexistente | plano-tdd.md, seção 2.6.1 |
| T-RN07-004 | RN-07 | Transição não especificada | plano-tdd.md, seção 2.7.1 |
| T-RN07-005 | RN-07 | Estado inválido | plano-tdd.md, seção 2.7.1 |
| T-RN07-006 | RN-07 | Cancelar EM_USO | plano-tdd.md, seção 2.7.1 |
| T-RN08-002 | RN-08 | Cancelar EM_USO | plano-tdd.md, seção 2.8.1 |
| T-RN08-005 | RN-08 | Alterar EM_USO para CANCELADA | plano-tdd.md, seção 2.8.1 |
| T-RN08-007 | RN-08 | Apagar com ID inválido | plano-tdd.md, seção 2.8.1 |
| T-RN09-005 | RN-09 | Operação sem autenticação | plano-tdd.md, seção 2.9.1 |
| T-RF01-004 | RF-01 | Solicitante tenta aprovar | plano-tdd.md, seção 3.1.1 |
| T-RF01-005 | RF-01 | Responsável tenta gerenciar usuários | plano-tdd.md, seção 3.1.1 |
| T-RF01-006 | RF-01 | Token inválido | plano-tdd.md, seção 3.1.1 |
| T-RF01-007 | RF-01 | Sem token | plano-tdd.md, seção 3.1.1 |
| T-RF10-005 | RF-10 | Sobreposição em sala | plano-tdd.md, seção 3.10.1 |
| T-RF10-006 | RF-10 | Sobreposição em material | plano-tdd.md, seção 3.10.1 |
| T-RF10-007 | RF-10 | Sobreposição em professor | plano-tdd.md, seção 3.10.1 |
| T-RF10-008 | RF-10 | Recurso em manutenção | plano-tdd.md, seção 3.10.1 |
| T-RF10-011 | RF-10 | Fim anterior ao início | plano-tdd.md, seção 3.10.1 |
| T-RF10-012 | RF-10 | Recurso inexistente | plano-tdd.md, seção 3.10.1 |
| T-RF10-013 | RF-10 | Solicitante cria para outro | plano-tdd.md, seção 3.10.1 |

### 1.4 Testes de Concorrência (Threads + Testcontainers)

Dupla reserva simultânea, race conditions, atomicidade.

| ID | Regra | Descrição | Localização |
|---|---|---|---|
| T-RN04-001 | RN-04 | Dupla simultânea: ambas no mesmo recurso | plano-tdd.md, seção 2.4.1 |
| T-RN04-002 | RN-04 | Tripla simultânea | plano-tdd.md, seção 2.4.1 |
| T-RN04-004 | RN-04 | Dupla em recursos diferentes | plano-tdd.md, seção 2.4.1 |
| T-RN04-005 | RN-04 | Dupla em períodos adjacentes | plano-tdd.md, seção 2.4.1 |
| T-RN04-006 | RN-04 | Dupla: um restrito, um sem | plano-tdd.md, seção 2.4.1 |
| T-RN04-007 | RN-04 | Consistência após aceitar uma | plano-tdd.md, seção 2.4.1 |
| T-RN04-008 | RN-04 | Auditoria sem race condition | plano-tdd.md, seção 2.4.1 |
| T-RN06-007 | RN-06 | Responsável aprova duplicada | plano-tdd.md, seção 2.6.1 |
| T-RF10-009 | RF-10 | Dupla simultânea (RN-04) | plano-tdd.md, seção 3.10.1 |

### 1.5 Testes End-to-End (Fluxo Completo)

Interface, usuário real, jornada completa.

| ID | Regra | Descrição | Localização |
|---|---|---|---|
| T-RN01-001 | RN-01 | Intervalo válido | plano-tdd.md, seção 2.1.1 (camadas) |
| T-RN02-001 | RN-02 | Reserva sem conflito | plano-tdd.md, seção 2.2.1 (camadas) |
| T-RN07-001 | RN-07 | Fluxo principal completo | plano-tdd.md, seção 2.7.1 |
| (Mais a definir conforme viewport/interface) | | | |

---

## 2. Índice por Tipo de Cenário

### 2.1 Happy Path (Caminho Feliz)

Testes onde a operação é bem-sucedida com dados válidos.

| T-ID | Regra | Cenário |
|---|---|---|
| T-RN01-001 | RN-01 | Reserva com intervalo válido |
| T-RN01-002 | RN-01 | Intervalo longo |
| T-RN02-001 | RN-02 | Reserva em sala sem conflito |
| T-RN02-002 | RN-02 | Diferentes recursos mesmo horário |
| T-RN03-001 | RN-03 | Professor alocado, agenda sem conflito |
| T-RN03-002 | RN-03 | Professor sem agenda registrada |
| T-RN04-001 | RN-04 | Dupla simultânea - apenas 1 aceita |
| T-RN04-004 | RN-04 | Dupla em recursos diferentes |
| T-RN05-001 | RN-05 | Reserva em recurso sem manutenção |
| T-RN05-002 | RN-05 | Reserva fora do período de manutenção |
| T-RN06-001 | RN-06 | Recurso comum aceita direto |
| T-RN06-002 | RN-06 | Recurso restrito aguarda aprovação |
| T-RN06-003 | RN-06 | Responsável rejeita |
| T-RN07-001 | RN-07 | Fluxo principal completo |
| T-RN07-002 | RN-07 | Cancelamento em SOLICITADA |
| T-RN07-003 | RN-07 | Rejeição em SOLICITADA |
| T-RN08-001 | RN-08 | Cancelar antes de iniciar |
| T-RN09-001 | RN-09 | Criar reserva gera auditoria |
| T-RN09-002 | RN-09 | Mudar SOLICITADA para APROVADA |
| T-RF01-001 | RF-01 | Solicitante consulta disponibilidade |
| T-RF01-002 | RF-01 | Responsável aprova |
| T-RF01-003 | RF-01 | Admin gerencia recurso |
| T-RF10-001 | RF-10 | Criar reserva simples |
| T-RF10-002 | RF-10 | Criar com múltiplos recursos |
| T-RF10-003 | RF-10 | Criar com professor |

### 2.2 Boundary (Valores-Limite)

Testes de casos extremos nos limites de aceitação.

| T-ID | Regra | Cenário |
|---|---|---|
| T-RN01-003 | RN-01 | Intervalo mínimo (1 minuto) |
| T-RN01-004 | RN-01 | Intervalo zero (início = fim) |
| T-RN02-007 | RN-02 | Reservas adjacentes (fim = início) |
| T-RN02-008 | RN-02 | Limite exato de coincidência |
| T-RN03-006 | RN-03 | Professor com agenda até minuto exato |
| T-RN05-010 | RN-05 | Manutenção adjacente à reserva |
| T-RN06-009 | RN-06 | Responsável autorizado para recurso A, tenta B |
| T-RN07-008 | RN-07 | Transição na fronteira de tempo |
| T-RN08-006 | RN-08 | Alteração imediatamente após iniciação |
| T-RN09-009 | RN-09 | Auditoria com timestamp duvidoso |
| T-RN09-010 | RN-09 | Auditoria após tentativa de apagamento |
| T-RF10-010 | RF-10 | Período mínimo válido |

### 2.3 Invalid Input (Entradas Inválidas)

Testes com dados malformados, nulos ou inválidos.

| T-ID | Regra | Cenário |
|---|---|---|
| T-RN01-006 | RN-01 | Data passada |
| T-RN01-007 | RN-01 | Início nulo |
| T-RN01-008 | RN-01 | Fim nulo |
| T-RN01-009 | RN-01 | Formato de hora inválido |
| T-RN03-007 | RN-03 | Professor inexistente |
| T-RN03-008 | RN-03 | Agenda com formato inválido |
| T-RN05-009 | RN-05 | Bloqueio/manutenção com período inválido |
| T-RN06-008 | RN-06 | Recurso inexistente |
| T-RN07-009 | RN-07 | Estado null ou vazio |
| T-RN08-007 | RN-08 | Tentar apagar com ID inválido |
| T-RN09-005 | RN-09 | Operação sem autenticação |
| T-RF01-006 | RF-01 | Token inválido ou expirado |
| T-RF01-007 | RF-01 | Sem token |
| T-RF01-008 | RF-01 | Usuário com múltiplos perfis |
| T-RF10-011 | RF-10 | Fim anterior ao início |
| T-RF10-012 | RF-10 | Recurso inexistente |

### 2.4 Conflicts (Conflitos)

Testes onde múltiplos requisitos competem ou violam regras.

| T-ID | Regra | Cenário |
|---|---|---|
| T-RN01-005 | RN-01 | Fim anterior ao início |
| T-RN01-010 | RN-01 | Alteração de reserva já iniciada |
| T-RN02-003 | RN-02 | Sobreposição total |
| T-RN02-004 | RN-02 | Sobreposição parcial (nova começa dentro) |
| T-RN02-005 | RN-02 | Sobreposição parcial (nova termina dentro) |
| T-RN02-006 | RN-02 | Sobreposição: nova envolve existente |
| T-RN02-009 | RN-02 | Múltiplas existentes, conflita com uma |
| T-RN02-010 | RN-02 | Alterar existente criando sobreposição |
| T-RN03-003 | RN-03 | Sobreposição com agenda |
| T-RN03-004 | RN-03 | Múltiplos professores, um com conflito |
| T-RN03-005 | RN-03 | Alterar criando conflito de agenda |
| T-RN04-001 | RN-04 | Dupla simultânea - testes de concorrência |
| T-RN04-002 | RN-04 | Tripla simultânea |
| T-RN04-006 | RN-04 | Dupla: um com recurso restrito |
| T-RN05-003 | RN-05 | Reserva em recurso em manutenção |
| T-RN05-004 | RN-05 | Reserva com manutenção parcial |
| T-RN05-005 | RN-05 | Múltiplos períodos de manutenção |
| T-RN05-006 | RN-05 | Material em manutenção |
| T-RN05-007 | RN-05 | Alterar introduzindo manutenção |
| T-RN06-006 | RN-06 | Aprova recurso que ficou indisponível |
| T-RN06-007 | RN-06 | Aprova duplicada |
| T-RN07-004 | RN-07 | Transição não especificada |
| T-RN07-007 | RN-07 | Rejeitar após APROVADA |
| T-RF10-005 | RF-10 | Sobreposição em sala |
| T-RF10-006 | RF-10 | Sobreposição em material |
| T-RF10-007 | RF-10 | Sobreposição em professor |
| T-RF10-008 | RF-10 | Recurso em manutenção |
| T-RF10-009 | RF-10 | Dupla simultânea |

### 2.5 Forbidden State (Estados Proibidos)

Testes onde operação viola autorização, permissão ou regra de estado.

| T-ID | Regra | Cenário |
|---|---|---|
| T-RN06-004 | RN-06 | Solicitante tenta aprovar |
| T-RN06-005 | RN-06 | Admin tenta aprovar |
| T-RN07-005 | RN-07 | Tentar estado inválido |
| T-RN07-006 | RN-07 | Cancelar em EM_USO |
| T-RN07-007 | RN-07 | Rejeitar após APROVADA |
| T-RN08-002 | RN-08 | Cancelar após iniciação |
| T-RN08-003 | RN-08 | Cancelar após conclusão |
| T-RN08-004 | RN-08 | Apagar (DELETE) |
| T-RN08-005 | RN-08 | Alterar EM_USO para CANCELADA |
| T-RN08-009 | RN-08 | Admin não pode contornar proteção |
| T-RN08-010 | RN-08 | Rejeitar após iniciada |
| T-RN09-006 | RN-09 | Tentar editar auditoria |
| T-RN09-007 | RN-09 | Tentar apagar auditoria |
| T-RN09-008 | RN-09 | Operação recusada gera auditoria? |
| T-RF01-004 | RF-01 | Solicitante tenta aprovar |
| T-RF01-005 | RF-01 | Responsável tenta gerenciar usuários |
| T-RF01-008 | RF-01 | Usuário com múltiplos perfis |
| T-RF10-013 | RF-10 | Solicitante cria para outro usuário |

---

## 3. Índice por Regra de Negócio (RN)

Localização rápida de todos os casos de teste para uma RN específica.

| RN | Casos de Teste | Localização | Total |
|---|---|---|---|
| **RN-01** | T-RN01-001 a T-RN01-010 | plano-tdd.md, seção 2.1.1 | 10 |
| **RN-02** | T-RN02-001 a T-RN02-010 | plano-tdd.md, seção 2.2.1 | 10 |
| **RN-03** | T-RN03-001 a T-RN03-010 | plano-tdd.md, seção 2.3.1 | 10 |
| **RN-04** | T-RN04-001 a T-RN04-008 | plano-tdd.md, seção 2.4.1 | 8 |
| **RN-05** | T-RN05-001 a T-RN05-010 | plano-tdd.md, seção 2.5.1 | 10 |
| **RN-06** | T-RN06-001 a T-RN06-010 | plano-tdd.md, seção 2.6.1 | 10 |
| **RN-07** | T-RN07-001 a T-RN07-010 | plano-tdd.md, seção 2.7.1 | 10 |
| **RN-08** | T-RN08-001 a T-RN08-010 | plano-tdd.md, seção 2.8.1 | 10 |
| **RN-09** | T-RN09-001 a T-RN09-010 | plano-tdd.md, seção 2.9.1 | 10 |
| **RN-10** | T-RN10-001 a T-RN10-010 | plano-tdd.md, seção 2.10.1 | 10 |

---

## 4. Índice por Requisito Funcional (RF)

Localização rápida de todos os casos de teste para um RF específico.

| RF | Casos de Teste | Localização | Total |
|---|---|---|---|
| **RF-01** | T-RF01-001 a T-RF01-010 | plano-tdd.md, seção 3.1.1 | 10 |
| **RF-10** | T-RF10-001 a T-RF10-015 | plano-tdd.md, seção 3.10.1 | 15 |

---

## 5. Matriz de Decisão: Qual Teste Usar?

### "Preciso testar se RN-01 (ordem temporal) funciona"

| Se quiser... | Use este teste | Localização |
|---|---|---|
| Apenas lógica de validação | T-RN01-004, T-RN01-005 | Unitário |
| Dentro de contexto de banco | T-RN01-001, T-RN01-002 | Integração |
| Via API HTTP | T-RN01-004, T-RN01-005 | API |
| Fluxo completo (UI) | T-RN07-001 (que inclui RN-01) | E2E |

### "Preciso testar se sobreposição (RN-02) é bloqueada"

| Se quiser... | Use este teste | Localização |
|---|---|---|
| Diversos cenários | T-RN02-003 a T-RN02-006 | Integração + API |
| Múltiplas reservas | T-RN02-009, T-RN02-010 | Integração |
| Resposta via API | T-RN02-003 a T-RN02-006 | API |

### "Preciso testar concorrência (RN-04)"

**Use exclusivamente:** T-RN04-001 a T-RN04-008 (Concorrência, Testcontainers)

### "Preciso testar auditoria (RN-09)"

| Se quiser... | Use este teste | Localização |
|---|---|---|
| Criar auditoria | T-RN09-001 | Integração |
| Mudança de estado | T-RN09-002, T-RN09-003 | Integração |
| Sucessão de eventos | T-RN09-004 | Integração |
| Proteção contra edição | T-RN09-006, T-RN09-007 | Integração |

---

## 6. Checklist de Implementação TDD

Use este checklist para acompanhar implementação:

### Fase 1: Validação (Pré-implementação)
- [ ] Q-001 aprovado (estado inicial)
- [ ] Q-003 aprovado (critério de recurso restrito)
- [ ] Q-009 aprovado (mecanismo de concorrência)
- [ ] Demais Qs (004, 006, 007, 008, 010) resolvidas

### Fase 2: RN-01 (Ordem Temporal)
- [ ] Implementar testes unitários (T-RN01-001 a T-RN01-009)
- [ ] Testes passando
- [ ] Implementar código de produção
- [ ] Cobertura ≥ 80% (linhas), ≥ 70% (branches)

### Fase 3: RN-04 (Concorrência)
- [ ] Implementar testes de concorrência (T-RN04-001 a T-RN04-008)
- [ ] Testes passando com Testcontainers
- [ ] Implementar código de produção com mecanismo aprovado (pessimistic lock, optimistic, etc.)
- [ ] Cobertura validada

### Fase 4: RN-06 (Aprovação)
- [ ] Implementar testes de autorização (T-RN06-001 a T-RN06-010)
- [ ] Testes passando
- [ ] Implementar RF-14 (aprovação de solicitações)
- [ ] Auditoria integrada (RN-09)

### Fase 5: RN-02, RN-03, RN-05 (Sobreposição, Professor, Manutenção)
- [ ] RN-02: T-RN02-001 a T-RN02-010
- [ ] RN-03: T-RN03-001 a T-RN03-010
- [ ] RN-05: T-RN05-001 a T-RN05-010

### Fase 6: RN-07, RN-08, RN-09 (Estados, Proteção, Auditoria)
- [ ] RN-07: T-RN07-001 a T-RN07-010
- [ ] RN-08: T-RN08-001 a T-RN08-010
- [ ] RN-09: T-RN09-001 a T-RN09-010

### Fase 7: Validação Final
- [ ] 100% de RNs críticas com testes
- [ ] 100% de RFs MUST com testes
- [ ] Cobertura de código: 80% linhas + 70% branches
- [ ] Matriz de rastreabilidade atualizada
- [ ] Zero bugs críticos conhecidos

---

**Data de atualização:** 2026-09-11  
**Versão:** 1.0  
**Status:** Índices completos, pronto para uso em TDD
