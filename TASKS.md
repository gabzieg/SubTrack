# TASKS.md — SubTrack

Fase atual: **Fase 1 — MVP** (Fase 0 concluída em 2026-07-30)

## Pendente (Fase 1)

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
- [x] **Rodado em emulador (Pixel 8)**: app sobe sem crash, estado vazio renderiza, e o `subtrack.db` foi criado com 20 KB — ou seja, o `AndroidSqliteDriver` abriu o SQLite do Android e executou o schema de verdade (o estado vazio sozinho não provaria isso, já que o `initial` do `collectAsState` também é vazio).

- [x] **Notificação de renovação** (2026-08-02): lógica pura de "quais avisar" com 11 testes, `lastNotifiedRenewal` no schema pra dedup, worker diário via WorkManager, permissão `POST_NOTIFICATIONS` em runtime. 63 testes no total.
- [x] `ACCESS_NETWORK_STATE` removida via `tools:node="remove"` — confirmado no manifesto final e no `dumpsys` do aparelho. Restam só `POST_NOTIFICATIONS`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `RECEIVE_BOOT_COMPLETED`. Dívida fechada.
- [x] Execução imediata na abertura do app, além da periódica — sem isso a primeira checagem só aconteceria 24h após instalar.
- [x] **Verificado em emulador**: assinatura renovando em 3 dias notificou ("R$54.90 é cobrado em 3 dias"); a que renova em 18 dias não notificou; reabrir o app rodou o worker de novo (confirmado no log) sem repetir o aviso.

- [x] **Editar / arquivar / desarquivar / excluir pela UI** (2026-08-02): mesma tela serve cadastro e edição, agora com `everyN`, dias de aviso e categoria. Toque na linha abre a edição. Excluir pede confirmação; arquivar é a saída padrão.
- [x] Alternar "Ver arquivadas" na barra — sem isso arquivar era porta de mão única, com o botão de desarquivar inalcançável. Arquivada aparece marcada e **não** entra nos totais.
- [x] **Verificado em emulador**: editar prefilled, arquivar (saiu da lista, total caiu de R$76,80 pra R$21,90), listar arquivada, desarquivar (voltou, total restaurado). 66 testes no total.

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

- **Dois comportamentos de mês curto no mercado, e só um está implementado.** Verificado na doc oficial do Google (developer.android.com/google/play/billing/lifecycle/subscriptions): a Play Store move a renovação pro dia 28 e **fica nele para sempre**. A App Store aparenta fazer igual (evidência de segunda mão). Stripe e Recurly truncam e voltam pro dia 31 — que é o que `NextRenewal.kt` faz. O que segura isso na prática é a âncora ser recente: se o usuário informa a cobrança da fatura, a derivação do provedor já veio embutida. A divergência só aparece com âncora antiga. Decidir depois se vale um campo de "provedor" por assinatura, ou re-ancorar quando o usuário confirmar uma cobrança.

- **`ACCESS_NETWORK_STATE` no manifest, vindo do merge do WorkManager** (junto com `WAKE_LOCK`, `FOREGROUND_SERVICE`, `RECEIVE_BOOT_COMPLETED`). Não há `INTERNET`, então o app continua sem conseguir falar com a rede — o critério do SPEC se mantém. Mas a Play Store expõe essa lista pro usuário, e "nunca toca na rede" é o argumento central do produto. Ao implementar a notificação, avaliar remover `ACCESS_NETWORK_STATE` via `tools:node="remove"` (só é usada por constraint de rede, que não usamos). `RECEIVE_BOOT_COMPLETED` deve ficar — é o que faz o lembrete sobreviver a um reboot.
- `today` é lido uma vez na composição — se o app ficar aberto atravessando a meia-noite, os rótulos ("Renova em N dias") só atualizam na próxima abertura.
- Textos da UI estão fixos em português no código, fora de `strings.xml`. Migrar quando/se entrar segundo idioma.

- **Ícone da notificação é `android.R.drawable.ic_popup_reminder`**, do sistema. Trocar junto com o ícone do app.
- **Não testado: sobrevivência a reboot.** `RECEIVE_BOOT_COMPLETED` está no manifesto e o WorkManager reagenda sozinho, mas não reiniciei o emulador pra confirmar. Vale validar antes de publicar.
- `targetSdk = 36` — lint acusa `OldTargetApi`. Subir junto com `compileSdk` quando a plataforma mais nova estiver instalada e testada.
- Alvos Apple ainda não declarados no `shared`. **Restrição dura:** compilar alvo Apple exige macOS — no Windows não dá pra validar o caminho iOS localmente. A Fase 4 vai precisar de um Mac ou de um runner macOS no CI. A estrutura `commonMain` já está preparada, mas isso não é o mesmo que estar verificado.

---
Regra: cada item concluído aqui deve corresponder a um commit/PR pequeno e revisável (ver AGENTS.md). Não marcar concluído sem passar pela validação local (`./gradlew ktlintCheck build`).
