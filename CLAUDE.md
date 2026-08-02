# CLAUDE.md

Contexto do SubTrack para o Claude Code — leia isto antes de explorar a árvore.
Objetivo: não precisar redescobrir a cada sessão as decisões que já custaram
investigação.

## O que é o projeto

Rastreador de assinaturas Android (Kotlin Multiplatform, iOS na Fase 4). O
usuário cadastra as assinaturas **na mão**, vê quanto custam por mês/ano, e é
avisado por notificação antes de cada renovação.

Origem: pesquisa de mercado identificou um vácuo entre apps que exigem vínculo
bancário (Rocket Money, Monarch — pesados, precisam de Plaid, dado sensível) e
os simples demais (Bobby — barato mas pobre em recurso). Relatórios em
`~/Documents/AppIdeas_Research_20260730/` e
`~/Documents/Monetizacao_ResumeBuilder_SubscriptionTracker_20260730/`.

## Workflow

Este repo segue o método gated do Chris Titus (https://christitus.com/my-ai-workflow/):
**a IA acelera cada etapa, não pula nenhuma.**

Leia sob demanda, não de cara:
- [AGENTS.md](AGENTS.md) — regras de operação. **Leia sempre antes de codar.**
- [SPEC.md](SPEC.md) — problema, não-objetivos, critério de aceite.
- [ROADMAP.md](ROADMAP.md) — 6 fases com critério de saída.
- [TASKS.md](TASKS.md) — **onde o trabalho está agora** + dívidas registradas.
- [.agents/skills/kotlin-multiplatform-compose.md](.agents/skills/kotlin-multiplatform-compose.md)
  — carregue antes de mexer em estrutura de módulo, SQLDelight ou build KMP/iOS.

## Comandos

```bash
./gradlew ktlintCheck build    # gate obrigatório: lint + testes + APK debug/release
./gradlew ktlintFormat         # corrige formatação (rode antes do check)
./gradlew :shared:allTests     # só os testes
```

**Confirme a contagem de testes, não confie no "BUILD SUCCESSFUL":**
```bash
grep -h 'testsuite name' shared/build/test-results/testAndroidHostTest/*.xml
```
Já aconteceu de o `commonTest` não estar ligado a nenhuma compilação e o build
passar em silêncio sem rodar teste nenhum.

## Arquitetura

| Módulo | Responsabilidade |
|---|---|
| `shared/commonMain` | Modelo, cálculo de renovação, custo recorrente, lembretes, repositório SQLDelight. **Sem código Android-specific** — é o que mantém o caminho pra iOS aberto. |
| `shared/androidMain` | `actual` do driver SQLite e da formatação de moeda. |
| `shared/androidHostTest` | Testes contra SQLite real em memória (driver JDBC), sem emulador. |
| `androidApp` | Compose UI, WorkManager, notificação, DI na mão em `SubTrackApp`. |

Sob **AGP 9** o plugin KMP não convive com `com.android.application`/
`com.android.library`: `:androidApp` é módulo Android puro (o Kotlin já vem no
AGP 9) e `:shared` declara o alvo em `kotlin { android { ... } }`.

## Decisões que custaram investigação — não desfaça sem ler

**Âncora, não "próxima renovação".** O banco guarda uma cobrança que já
aconteceu (`anchorEpochDay`); a próxima é derivada a cada leitura. Guardar a
próxima criaria um valor que apodrece sozinho se o app ficar meses fechado.

**Existem DOIS comportamentos de mês curto no mercado.** A doc do Google é
explícita: a Play Store move a renovação pro dia 28 e **fica nele para sempre**
("continues to renew on the 28th of each month for the duration of the
subscription"). Stripe/Recurly truncam e voltam pro 31 — que é o que
`NextRenewal.kt` implementa. O que segura a diferença é a âncora ser recente:
se o usuário informa a cobrança da fatura, a derivação do provedor já veio
embutida. Detalhes em TASKS.md.

**O formulário pede "data da cobrança", não "primeira cobrança".** Qualquer
ocorrência real serve de âncora (o cálculo anda em ciclos inteiros), e uma data
que está na fatura é mais fácil de responder que uma da memória. Não explicar
esse mecanismo na tela é intencional.

**Totais são por moeda.** Somar BRL com USD daria um número sem significado, e
converter exigiria cotação — ou seja, rede, proibida pelo SPEC.

**Dedup de notificação guarda QUAL renovação foi avisada**, não um booleano. É o
que faz o worker diário não virar spam, e faz a edição se corrigir sozinha:
mudar a data muda a próxima renovação e o aviso volta a valer; mudar só o valor
mantém a data e a mesma cobrança não é anunciada duas vezes.

**Trabalho periódico não roda até o primeiro intervalo fechar.** Por isso existe
uma execução imediata na abertura, além da diária — sem ela uma instalação nova
ficaria 24h sem checagem nenhuma.

**Valor é máscara de dígitos**, centavos preenchendo da direita. Separador nem é
digitável, então não existe entrada ambígua. `parseMoneyToCents` sobrevive só
pro import da Fase 3.

**`ACCESS_NETWORK_STATE` é removida via `tools:node="remove"`.** O WorkManager a
injeta pelo merge, mas ela só serve pra constraint de rede que não usamos — e a
Play Store mostra essa lista ao usuário, sendo "nunca toca na rede" a proposta
central. **Nunca adicione `INTERNET`.**

## Restrições duras

- **Sem backend, sem rede, sem vínculo bancário.** Se uma feature parecer exigir
  isso, ela saiu do escopo do SPEC — questione antes de implementar.
- **iOS não compila no Windows.** A Fase 4 exige macOS ou runner macOS na CI.
  `commonMain` está preparado, mas preparado não é o mesmo que verificado.
- **Verifique no emulador antes de dizer que funciona.** Bugs reais que 60+
  testes verdes não pegaram: o worker periódico nunca rodando nas primeiras 24h,
  e o botão "Desarquivar" ficando inalcançável porque a lista só mostrava ativas.

## Estado atual

Fase 1 (MVP) quase fechada — ver TASKS.md. Falta o ícone do app
(`MissingApplicationIcon` no lint; a notificação usa um ícone do sistema como
provisório). Decisão de arte pendente com o usuário.
