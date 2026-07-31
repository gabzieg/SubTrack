# TASKS.md — SubTrack

Fase atual: **Fase 1 — MVP** (Fase 0 concluída em 2026-07-30)

## Pendente (Fase 1)

- [ ] Repositório sobre SQLDelight em `commonMain` expondo `Flow<List<Subscription>>`, já ordenando pela próxima renovação calculada.
- [ ] `expect`/`actual` do driver SQLDelight (Android já resolvido; iOS fica pra Fase 4).
- [ ] Tela de lista: assinaturas ativas ordenadas pela próxima renovação + total mensal/anual no topo.
- [ ] Tela de adicionar/editar assinatura (nome, valor, moeda, ciclo, everyN, data, categoria, dias de aviso).
- [ ] Arquivar/desarquivar sem apagar histórico.
- [ ] Formatação de moeda por locale (o total hoje aparece em centavos crus na tela esqueleto).
- [ ] Agendamento de notificação via WorkManager + permissão POST_NOTIFICATIONS em runtime (Android 13+).
- [ ] Ícone do app (lint acusa `MissingApplicationIcon`).
- [ ] Fechar todos os checkboxes de "Critério de aceite (MVP)" no SPEC.md.

## Concluído

### Fase 1 — em andamento
- [x] `kotlinx-datetime` 0.8.0 aprovado e adicionado (matemática de calendário pra ciclo mensal/anual).
- [x] `nextRenewalOnOrAfter()` calculando a próxima renovação a partir da âncora, não da renovação anterior — 10 testes cobrindo mês curto, ano bissexto, ciclo trimestral e virada de ano.
- [x] Schema passou a guardar `anchorEpochDay` (fonte da verdade) em vez de `nextRenewalEpochDay` (valor que envelhece sozinho se o app ficar sem abrir).

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

- `targetSdk = 36` — lint acusa `OldTargetApi`. Subir junto com `compileSdk` quando a plataforma mais nova estiver instalada e testada.
- Alvos Apple ainda não declarados no `shared`. **Restrição dura:** compilar alvo Apple exige macOS — no Windows não dá pra validar o caminho iOS localmente. A Fase 4 vai precisar de um Mac ou de um runner macOS no CI. A estrutura `commonMain` já está preparada, mas isso não é o mesmo que estar verificado.

---
Regra: cada item concluído aqui deve corresponder a um commit/PR pequeno e revisável (ver AGENTS.md). Não marcar concluído sem passar pela validação local (`./gradlew ktlintCheck build`).
