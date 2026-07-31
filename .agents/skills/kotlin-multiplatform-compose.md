# Skill: Kotlin Multiplatform + Compose Multiplatform (SubTrack)

Conhecimento de domínio pra qualquer tarefa que toque estrutura de módulo, persistência ou build multiplataforma neste projeto. Carregar antes de mexer em `shared/`, `build.gradle.kts`, ou configuração de target iOS.

## Estrutura de módulo esperada

```
SubTrack/
  shared/                  # lógica de negócio + persistência, sem código Android/iOS-specific
    src/
      commonMain/kotlin/    # modelo de dado, cálculo de recorrência, repositório
      androidMain/kotlin/   # actual do driver SQLDelight pra Android
      iosMain/kotlin/       # actual do driver SQLDelight pra iOS (Fase 4)
      commonTest/kotlin/    # testes de lógica pura, rodam em qualquer target
  androidApp/               # Compose UI + entrypoint Android, WorkManager
  iosApp/                   # Xcode project + entrypoint iOS (só existe a partir da Fase 4)
```

**Regra:** se um arquivo em `commonMain` importar algo Android-specific (ex: `android.content.Context` direto), é sinal de vazamento de plataforma — extrair via `expect`/`actual`.

## Persistência: SQLDelight, não Room

Escolha deliberada porque SQLDelight tem suporte multiplataforma maduro (Android + iOS + Desktop) com geração de código type-safe a partir de `.sq` files. Room ganhou suporte KMP mais recentemente mas o ecossistema SQLDelight pra este tipo de app (schema simples, sem relação complexa) é mais direto. Não trocar sem atualizar este arquivo e o SPEC.md.

## Notificação local (Android)

Usar `WorkManager` com `PeriodicWorkRequest` ou `OneTimeWorkRequest` agendado pra data de renovação — nunca `AlarmManager` puro (menos confiável em Android moderno com Doze mode) a menos que precise de precisão exata de horário, o que não é o caso aqui (alerta de "N dias antes" tolera atraso de minutos/horas).

## Compose Multiplatform — cuidado ao portar pra iOS (Fase 4)

- Testar cedo com um `iosApp/` mínimo (mesmo vazio) já na Fase 0/1 evita descobrir tarde que alguma lib do Android não tem equivalente KMP.
- APIs de notificação, storage de arquivo (export/import CSV) e file picker precisam de `expect`/`actual` — não existe API comum pronta pra tudo.
- Não assumir que toda lib Android tem versão KMP — checar antes de adotar dependência nova (ver regra do AGENTS.md: nenhuma dependência nova sem confirmar).

## Build/lint/test — comandos de referência

```
./gradlew ktlintCheck detekt      # lint/format
./gradlew testDebugUnitTest       # testes Android
./gradlew :shared:allTests        # testes commonTest em todos os targets configurados
./gradlew assembleDebug           # build de desenvolvimento
./gradlew assembleRelease         # build de release (Fase 5)
```

Confirmar nomes exatos de task assim que o projeto for gerado pelo wizard/template (Fase 0, primeira task de TASKS.md) — os nomes acima são o padrão comum, mas podem variar por versão do plugin.
