# Organização de Recursos

Sistema de alocação de salas, professores e materiais sem conflitos de horário — aplicação
Spring Boot MVC com persistência relacional, autenticação/autorização reais, proteção de
dupla-reserva garantida pelo banco, auditoria transacional, API REST documentada e interface web.

## Status

Migração do domínio (biblioteca Java pura, sem Spring) para uma aplicação executável, em 5 ondas
(ver `docs/arquitetura.md`, ADR-015 a ADR-018, e o histórico de commits na branch
`feat/onda1-esqueleto-spring-boot` para o detalhe de cada uma):

| Onda | Entrega | Status |
|---|---|---|
| 1 | Esqueleto executável (Spring Boot, Flyway, Docker Compose, Actuator) | ✅ |
| 2 | Model persistido (JPA, correções P1-P15, reativação dos testes `@Disabled`) | ✅ |
| 3 | Segurança (JWT, 3 camadas de autorização) e API REST | ✅ |
| 4 | View (Thymeleaf), notificação transacional, relatórios reais | ✅ |
| 5 | Gates de qualidade e documentação | ✅ |

Pendências e próximos passos estão documentados no relatório de status do projeto
(gerado ao final da Onda 5 — ver `docs/`).

## Stack

Java 21 · Spring Boot 3.5 · Maven · PostgreSQL 16 · Flyway · Spring Security (JWT + sessão) ·
Thymeleaf + Bootstrap 5 · springdoc-openapi (Swagger UI) · JUnit 5 + Testcontainers + WireMock.

## Como rodar localmente

```bash
docker compose up -d                 # PostgreSQL em localhost:5432
mvn spring-boot:run                  # aplicação em http://localhost:8080
```

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health check:** http://localhost:8080/actuator/health
- **Páginas web:** http://localhost:8080/login (form login + sessão)

A aplicação sobe com o perfil `dev` por padrão (ver `src/main/resources/application.yml`). Não há
usuário seed seme automático; cadastre o primeiro usuário via `POST /api/v1/usuarios` (exige um
Administrador já autenticado) ou insira diretamente no banco para o primeiro acesso.

### Variáveis de ambiente

| Variável | Uso | Padrão (dev/test) |
|---|---|---|
| `JWT_SECRET` | Chave HMAC para assinatura dos tokens JWT da API (`/api/**`) | placeholder inseguro — **trocar em qualquer ambiente real** |
| `NOTIFICACAO_BASE_URL` | URL base do canal de notificação HTTP (perfis que não sejam `dev`) | `http://localhost:9999` (placeholder) |

Em dev, a notificação usa o canal simulado (`NotificadorSimulado`, apenas loga) — ver
`NotificacaoConfig`.

## Testes

```bash
mvn verify
```

Requer Docker disponível (Testcontainers sobe um PostgreSQL real para os testes de integração,
concorrência e ponta a ponta). Sem Docker, rode só a suíte de domínio puro:

```bash
mvn test -Dtest='!ReservaServiceIntegracaoTest,!ReservaServiceConcorrenciaTest,!FluxoCompletoReservaTest,!ApplicationSmokeTest,!*WebMvcTest'
```

Cobertura (JaCoCo) é gerada em `target/site/jacoco/index.html` a cada `mvn verify`, com gate
mínimo de 80% de linhas / 70% de branches (ver `pom.xml`).

## Documentação

- `docs/prd.md` — requisitos funcionais/não-funcionais e matriz de rastreabilidade
- `docs/arquitetura.md` — arquitetura C4, ADRs (001-018), avaliações ATAM
- `docs/avaliacao-arquitetural/ciclo-01.md` (branch `docs/atam-ciclo-01`) — sondas P1-P15 que
  orientaram as correções das Ondas 2-3

## Evidências de integração contínua

Dados reais obtidos no GitHub Actions em 2026-09-25 (PR [#1](https://github.com/Jefferson-Silva-Geronimo/Organizacao-de-Recursos-QA/pull/1)).

- **Workflow:** `CI` (`.github/workflows/ci.yml`)
- **Check obrigatório:** `Build and test` (job `build`)
- **Comando local equivalente:** `mvn -B verify` (o projeto não possui Maven Wrapper)
- **Artifacts:** `test-reports` (`target/surefire-reports/**` e `target/failsafe-reports/**`) e
  `jacoco-report` (`target/site/jacoco/**`, a partir da Onda 5), ambos com retenção de 5 dias,
  publicados com `if: always()`

| Etapa | Commit | Run | Conclusão |
|-------|--------|-----|-----------|
| 1º run verde | `e25b8c3` | [36157319937](https://github.com/Jefferson-Silva-Geronimo/Organizacao-de-Recursos-QA/actions/runs/36157319937) | success |
| Run vermelho (falha controlada) | `1afeb12` | [36157529745](https://github.com/Jefferson-Silva-Geronimo/Organizacao-de-Recursos-QA/actions/runs/36157529745) | failure |
| Run verde após a reversão | `0af6f81` | [36157791758](https://github.com/Jefferson-Silva-Geronimo/Organizacao-de-Recursos-QA/actions/runs/36157791758) | success |

**Falha controlada:** em `GestaoSalas_RF02_Test.administradorDeveCadastrarEConsultarSala` (linha 31), a asserção `containsExactly(sala)` foi trocada por `isEmpty()`. O run vermelho registrou 163 testes, 1 falha (`Expecting empty but was: [...Recurso@...]`) e 0 erros, e o artifact `test-reports` foi publicado mesmo com a falha.

**Reversão:** a falha foi desfeita com `git revert` (commit `0af6f81`), sem reset, rebase ou force push. O run seguinte executou 163 testes, 0 falhas, 0 erros e 16 ignorados (já `@Disabled` no código, antes da Onda 2 reativá-los).

**Pull request:** o PR #1 não foi mesclado automaticamente.

**SonarCloud:** passo preparado no workflow (comentado/condicional ao secret `SONAR_TOKEN`), ver
seção correspondente em `.github/workflows/ci.yml`. A equipe precisa criar o projeto em
sonarcloud.io e cadastrar o secret para ativá-lo.

### Proteção da branch `main`

A proteção foi configurada via API do GitHub, exigindo: pull request antes do merge, o check `Build and test`, branch atualizada antes do merge, sem bypass por administradores, sem force push e sem exclusão da branch. Não é exigida aprovação de revisor, pois o repositório tem um único mantenedor.
