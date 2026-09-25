# Projeto Organização dos Recursos

Documentação em desenvolvimento..
## Evidências de integração contínua

Dados reais obtidos no GitHub Actions em 2026-09-25 (PR [#1](https://github.com/Jefferson-Silva-Geronimo/Organizacao-de-Recursos-QA/pull/1)).

- **Workflow:** `CI` (`.github/workflows/ci.yml`)
- **Check obrigatório:** `Build and test` (job `build`)
- **Comando local equivalente:** `mvn -B verify` (o projeto não possui Maven Wrapper)
- **Artifact:** `test-reports` (`target/surefire-reports/**` e `target/failsafe-reports/**`), retenção de 5 dias, publicado com `if: always()`

| Etapa | Commit | Run | Conclusão |
|-------|--------|-----|-----------|
| 1º run verde | `e25b8c3` | [36157319937](https://github.com/Jefferson-Silva-Geronimo/Organizacao-de-Recursos-QA/actions/runs/36157319937) | success |
| Run vermelho (falha controlada) | `1afeb12` | [36157529745](https://github.com/Jefferson-Silva-Geronimo/Organizacao-de-Recursos-QA/actions/runs/36157529745) | failure |
| Run verde após a reversão | `0af6f81` | [36157791758](https://github.com/Jefferson-Silva-Geronimo/Organizacao-de-Recursos-QA/actions/runs/36157791758) | success |

**Falha controlada:** em `GestaoSalas_RF02_Test.administradorDeveCadastrarEConsultarSala` (linha 31), a asserção `containsExactly(sala)` foi trocada por `isEmpty()`. O run vermelho registrou 163 testes, 1 falha (`Expecting empty but was: [...Recurso@...]`) e 0 erros, e o artifact `test-reports` foi publicado mesmo com a falha.

**Reversão:** a falha foi desfeita com `git revert` (commit `0af6f81`), sem reset, rebase ou force push. O run seguinte executou 163 testes, 0 falhas, 0 erros e 16 ignorados (já `@Disabled` no código).

**Pull request:** o PR #1 não foi mesclado automaticamente.

### Proteção da branch `main`

A proteção foi configurada via API do GitHub, exigindo: pull request antes do merge, o check `Build and test`, branch atualizada antes do merge, sem bypass por administradores, sem force push e sem exclusão da branch. Não é exigida aprovação de revisor, pois o repositório tem um único mantenedor.
