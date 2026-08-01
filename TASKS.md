# TASKS.md — SubTrack

Fase atual: **Fase 1 — MVP** (Fase 0 concluída em 2026-07-30)

## Pendente (Fase 1)

- [ ] Tela de adicionar/editar assinatura (nome, valor, moeda, ciclo, everyN, data, categoria, dias de aviso).
- [ ] Arquivar/desarquivar sem apagar histórico.
- [ ] Agendamento de notificação via WorkManager + permissão POST_NOTIFICATIONS em runtime (Android 13+).
- [ ] Ícone do app (lint acusa `MissingApplicationIcon`).
- [ ] Fechar todos os checkboxes de "Critério de aceite (MVP)" no SPEC.md.

## Concluído

### Fase 1 — em andamento
- [x] `kotlinx-datetime` 0.8.0 aprovado e adicionado (matemática de calendário pra ciclo mensal/anual).
- [x] `nextRenewalOnOrAfter()` calculando a próxima renovação a partir da âncora, não da renovação anterior — 10 testes cobrindo mês curto, ano bissexto, ciclo trimestral e virada de ano.
- [x] Schema passou a guardar `anchorEpochDay` (fonte da verdade) em vez de `nextRenewalEpochDay` (valor que envelhece sozinho se o app ficar sem abrir).
- [x] `SubscriptionRepository` sobre SQLDelight expondo `Flow<List<UpcomingSubscription>>` ordenado pela renovação calculada, + add/update/archive/delete.
- [x] `expect`/`actual` do driver SQLDelight (Android pronto; iOS na Fase 4).
- [x] 10 testes de repositório contra SQLite real em memória (driver JDBC) — schema, mapeamento de coluna e re-emissão do Flow exercitados de verdade.
- [x] `formatMoney` via `expect`/`actual`, delegando símbolo/separador pro locale do aparelho. Centavos convertidos por `BigDecimal` de escala fixa, nunca por `Double`.
- [x] `WalletState` totalizando **por moeda** — somar BRL com USD daria número sem significado, e converter exigiria cotação (rede, proibida pelo SPEC).
- [x] Tela de lista em Compose: card de totais, linhas ordenadas por renovação, estado vazio. `SubTrackApp` com DI na mão (grafo de 2 objetos não justifica biblioteca).
- [x] 40 testes no total, `ktlintCheck build` limpo e sem warning.

### Fase 0 — Global Setup ✅ (2026-07-30)
- [x] Versões confirmadas empiricamente rodando build de verdade: AGP 9.3.1, Kotlin 2.3.21, Gradle 9.6.1, SQLDelight 2.3.2, Compose BOM 2026.06.01, JDK 17.
- [x] Projeto KMP gerado à mão (`shared/` + `androidApp/`) na estrutura exigida pelo AGP 9 — o plugin KMP não convive mais com `com.android.application`/`com.android.library`.
- [x] SQLDelight configurado, schema `Subscription.sq` gerando código.
- [x] ktlint configurado + `.editorconfig` (duas regras desativadas com justificativa: `chain-method-continuation` no DSL do Gradle, `function-naming` pra `@Composable`).
- [x] 7 testes unitários em `commonTest` cobrindo anualização de custo recorrente.
- [x] CI GitHub Actions: ktlint + testes + build + artefato de APK.
- [x] `./gradlew ktlintCheck build` limpo, APK debug e release gerados.

**detekt:** não adotado. ktlint já cobre formatação e o `lint` do AGP já cobre análise estática de Android. Adicionar detekt seria uma terceira ferramenta com regras sobrepostas e mais uma matriz de compatibilidade pra manter — reavaliar só se surgir classe de bug que as duas atuais não pegam.

## Dívidas registradas (não bloqueiam a Fase 1)

- **App nunca foi executado em device/emulador.** Tudo até aqui é build verde + teste de host. A tela de lista compila e o APK gera, mas ninguém viu a tela rodando. Validar antes de fechar a Fase 1.
- `today` é lido uma vez na composição — se o app ficar aberto atravessando a meia-noite, os rótulos ("Renova em N dias") só atualizam na próxima abertura.
- Textos da UI estão fixos em português no código, fora de `strings.xml`. Migrar quando/se entrar segundo idioma.

- `targetSdk = 36` — lint acusa `OldTargetApi`. Subir junto com `compileSdk` quando a plataforma mais nova estiver instalada e testada.
- Alvos Apple ainda não declarados no `shared`. **Restrição dura:** compilar alvo Apple exige macOS — no Windows não dá pra validar o caminho iOS localmente. A Fase 4 vai precisar de um Mac ou de um runner macOS no CI. A estrutura `commonMain` já está preparada, mas isso não é o mesmo que estar verificado.

---
Regra: cada item concluído aqui deve corresponder a um commit/PR pequeno e revisável (ver AGENTS.md). Não marcar concluído sem passar pela validação local (`./gradlew ktlintCheck build`).
