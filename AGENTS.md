# AGENTS.md — SubTrack

Guia operacional pra qualquer agente de IA (Claude Code, Codex, etc.) trabalhando neste repositório. Baseado no workflow gated de Chris Titus (https://christitus.com/my-ai-workflow/, https://github.com/ChrisTitusTech/titus-ai): **IA acelera cada etapa, mas não pula nenhuma.**

## Sequência obrigatória

1. Especificação (`SPEC.md`) já definida antes de qualquer código — não inventar escopo novo sem atualizar o SPEC primeiro.
2. Scaffolding de teste antes de implementação — nenhuma feature nova sem teste (unitário no mínimo) escrito antes ou junto.
3. Uma mudança focada e revisável por iteração — nada de PR gigante misturando 3 features.
4. Validação local obrigatória antes de considerar tarefa pronta:
   - `./gradlew ktlintCheck detekt` (lint/format)
   - `./gradlew testDebugUnitTest` (testes)
   - `./gradlew assembleDebug` (build)
5. Nenhum merge direto na `main` sem revisão — mesmo trabalhando sozinho, abrir PR e revisar o diff antes de mergear é o gate mínimo.

## Fronteiras de arquitetura

- **Sem backend próprio.** Todo dado é local (SQLDelight). Se alguma feature parecer exigir servidor, isso é sinal de que saiu do escopo do SPEC — voltar e questionar antes de implementar.
- **Sem vínculo bancário, sem OAuth financeiro, sem Plaid/similar.** É a diferenciação central do produto (ver SPEC.md) — nunca introduzir dependência que exija isso.
- **Kotlin Multiplatform (KMP) + Compose Multiplatform.** Lógica de negócio (`shared/`) tem que ficar livre de código Android-specific pra manter o caminho pra iOS aberto. UI pode ter overrides por plataforma quando necessário (`expect`/`actual`), mas o padrão é UI compartilhada.
- **Notificação local, não push.** WorkManager (Android) / equivalente nativo no iOS depois — nunca introduzir serviço de push próprio.

## Preferências de código

- Kotlin idiomático, sem abreviação de nome de variável.
- Sem comentário explicando o que o código faz — só comentar decisão não óbvia (ver regra geral do projeto).
- Testes com Kotlin Test / Turbine pra Flow.
- Commits pequenos, mensagem no padrão `feat:`/`fix:`/`chore:`/`test:`.

## O que NÃO fazer sem perguntar primeiro

- Adicionar dependência nova (principalmente qualquer coisa que fale com rede).
- Mudar o modelo de monetização definido no SPEC.
- Pular a etapa de teste "porque é simples".
- Fazer merge sem o usuário ter revisado o diff.

## Skills disponíveis

Ver `.agents/skills/` — carregar `kotlin-multiplatform-compose.md` antes de qualquer tarefa que toque estrutura de módulo, SQLDelight, ou build KMP/iOS.
