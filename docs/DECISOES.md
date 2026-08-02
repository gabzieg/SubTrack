# Log de decisões — SubTrack

Registro cronológico de **como o projeto chegou aqui**: o que foi decidido, o
que foi considerado e descartado, e o que só apareceu tentando rodar.

Complementa, não substitui:
- [CLAUDE.md](../CLAUDE.md) — o que é verdade **agora** e não deve ser desfeito.
- [TASKS.md](../TASKS.md) — onde o trabalho está e quais dívidas estão abertas.

Este arquivo existe para responder *"por que não fizemos do outro jeito?"* —
pergunta que os outros dois não respondem.

---

## Antes do código — escolha do produto (2026-07-30)

**Decisão: construir um rastreador de assinaturas.**

Partiu de uma varredura de reviews negativas (Play Store, App Store, Reddit)
atrás de dor validada com baixo custo de manutenção. Relatórios completos em
`~/Documents/AppIdeas_Research_20260730/` e
`~/Documents/Monetizacao_ResumeBuilder_SubscriptionTracker_20260730/`.

Finalistas e por que os outros caíram:

| Ideia | Por que não |
|---|---|
| **Resume/CV builder sem paywall** | Dor maior e mercado maior (Zety cobra ~US$337/ano num trial de US$1,95), mas a aquisição de usuário no nicho é guerra de SEO. Manter um blog é trabalho contínuo — o oposto de "baixa manutenção". Fica como ideia nº2. |
| Leitor de PDF sem assinatura | Mercado já tem concorrente grátis bom (PDFgear, PDF-XChange). |
| Alternativa simples ao Anki | Anki é grátis e dominante; diferenciar exigiria simplicidade radical. |
| Gerenciador de senhas local | Demanda real (1Password subiu 33% em 2026), mas segurança crítica não é baixa manutenção: exige resposta a CVE e auditoria contínua. **Descartado apesar da demanda.** |
| Pomodoro, lembrete de remédio, conversor, lista de compras, renomeador em lote | Todos já têm alternativa grátis, sem anúncio, estabelecida. |

**Por que o rastreador ganhou:** não por mercado maior — é menor. Por CAC menor.
A categoria não tem guerra de conteúdo, o gap (manual + privado + completo) é
nomeado por analistas do próprio nicho, e dá pra validar via App Store orgânico
sem manter blog.

**Descartado no escopo:** varredura de e-mail pra detectar assinatura, e a
expansão "central de lembretes da casa" (garantia, cartão-presente) — só depois
de tração real.

---

## Fase 0 — Fundação (2026-07-30)

**KMP + Compose Multiplatform, não Android puro.** O usuário pediu Android
primeiro com iOS depois; KMP mantém o caminho aberto sem reescrever a lógica.

**AGP 9, não AGP 8.** AGP 9 quebra a estrutura KMP clássica (o plugin KMP não
convive mais com `com.android.application`/`com.android.library`), então dava
pra ficar no 8 e evitar a migração. Não ficamos: o AGP 10 remove as APIs legadas
no 2º semestre de 2026, e nascer no 8 já seria dívida com prazo.

**SQLDelight, não Room.** Suporte multiplataforma maduro e schema simples.

**detekt não adotado.** ktlint cobre formatação, o lint do AGP cobre análise
estática. Uma terceira ferramenta traria regras sobrepostas e mais uma matriz de
compatibilidade. Reavaliar só se aparecer classe de bug que as duas não pegam.

**Versões confirmadas rodando build, não deduzidas.** Duas coisas só apareceram
assim: o nome `androidLibrary {}` que a doc do AGP 9.0 ensina **já saiu
depreciado no 9.3.1**, e `withHostTest {}` é obrigatório — sem ele o `commonTest`
não é ligado a compilação nenhuma e **o build passa em silêncio sem rodar teste**.

---

## Fase 1 — Cálculo de renovação (2026-07-30)

**Âncora, não "próxima renovação".** O banco guarda uma cobrança que já
aconteceu; a próxima é derivada na leitura.

*Alternativa descartada:* persistir `nextRenewalEpochDay` e avançar a partir
dele. Cria um valor que apodrece sozinho — bastaria o app ficar meses fechado.
Além disso, avançar da renovação anterior faz o dia derreter no mês curto
(31/jan → 28/fev → 28/mar → ...).

*Custo aceito:* ordenar por próxima renovação vira ordenação em memória, já que
não é coluna. Carteira pessoal tem dezenas de linhas, não milhares.

**`kotlinx-datetime` aprovado como dependência** (o AGENTS.md exige aprovação).
Mês tem 28-31 dias; fazer na mão com epochDay funciona pra diário/semanal e erra
pra mensal/anual.

### Correção importante (2026-08-01)

Eu tinha escrito no código que derivar o dia "divergiria da cobrança real".
**Estava errado.** A doc oficial do Google diz o contrário:

> "If a subscription is set to renew on the 29th, 30th, or 31st of the month
> going into February of a non-leap year, then the subscription renewal day is
> moved to the 28th of February and **continues to renew on the 28th of each
> month for the duration of the subscription**."

Existem **dois** comportamentos reais: Play Store (e aparentemente App Store)
derivam permanentemente; Stripe e Recurly truncam e voltam pro 31 — que é o que
implementamos.

*Por que não mudamos o algoritmo:* pedir a data da fatura recente absorve a
diferença sozinha. Se a Play Store já derivou pro dia 28, é 28 que está na
fatura. As séries coincidem daí em diante; a divergência só volta com âncora
antiga.

*Confiança das fontes:* Google e Stripe são doc oficial (alta). **Apple é de
segunda mão** — uma editora descrevendo a própria assinatura, não doc da Apple.

*Ano bissexto nunca foi problema:* regra fechada, kotlinx-datetime implementa,
teste passou de primeira. A pesquisa sobre isso teria sido desperdício — o que
faltava verificar era o comportamento de mês curto dos provedores.

---

## Fase 1 — Dinheiro (2026-08-01)

**Totais por moeda, não um número só.** Somar 54,90 BRL com 12,00 USD dá um
número sem significado. Converter exigiria cotação — rede, proibida pelo SPEC.
Isso desviou do SPEC, que falava em "total mensal e anual" no singular.

**`BigDecimal` de escala fixa, nunca `Double`.** Dividir centavos por 100.0
começa a perder centavo em totais grandes.

**Formatação delegada ao locale do aparelho.** Num aparelho en-US, uma
assinatura em BRL aparece como "R$54.90" — está correto, não é bug.

**Entrada de valor virou máscara de dígitos** (decisão do usuário: sempre 2
casas). Digitar `5490` mostra `R$ 54,90`; separador não é digitável.

*O que isso resolveu:* antes o parser precisava adivinhar se `1.234` era mil
duzentos e trinta e quatro ou um e vinte e três. Com máscara, a ambiguidade
deixa de existir na origem, não por regra de interpretação.

*`parseMoneyToCents` sobreviveu* pro import da Fase 3, com regra estrita: 2
dígitos após separador é decimal, 3 é milhar, o resto é rejeitado.

**Campo pede "data da cobrança", não "primeira cobrança"** (decisão do usuário).
Qualquer ocorrência real serve de âncora — o cálculo anda em ciclos inteiros e
produz a mesma série. Uma data da fatura é conferível; a data em que assinou,
não. **Não explicar esse mecanismo na tela foi pedido explícito.**

*Risco aceito:* se a pessoa informar uma data que foi truncada por mês curto
(28/02 quando a cobrança real é 31), a série passa a ancorar no 28.

---

## Fase 1 — Notificação (2026-08-01)

**Worker diário, não alarme exato por assinatura.** "3 dias antes" precisa de
precisão de dia. Em troca o WorkManager resolve de graça: sobrevive a reboot,
reagenda depois do Doze, recupera dia perdido.

**Dedup guarda QUAL renovação foi avisada, não um booleano.** Sem dedup o worker
diário viraria spam. E guardar a data em vez de um flag fez a edição se corrigir
sozinha: mudar a data muda a próxima renovação e o aviso volta a valer; mudar só
o valor mantém a data e a mesma cobrança não é anunciada duas vezes.

**Só marca como avisado se a notificação saiu.** Marcar sem entregar apagaria o
aviso para sempre sem o usuário ter visto.

**Aviso atrasado é puxado pra hoje, não descartado.** Aparelho desligado não
pode virar cobrança surpresa.

**`ACCESS_NETWORK_STATE` removida** via `tools:node="remove"`. O WorkManager a
injeta pelo merge, mas só serve pra constraint de rede que não usamos. A Play
Store mostra essa lista, e "nunca toca na rede" é a proposta central.
`RECEIVE_BOOT_COMPLETED` ficou — é o que carrega o lembrete pelo reboot.

---

## Bugs que os testes não pegaram

Ambos apareceram **tentando rodar**, com 60+ testes verdes.

**Worker periódico nunca rodava nas primeiras 24h.** O WorkManager avisou:
`"Delaying execution ... because it is being executed before schedule"`. Trabalho
periódico só executa quando o primeiro intervalo fecha — quem instalasse com
cobrança no dia seguinte seria avisado tarde demais. Corrigido com execução
imediata na abertura.

**"Desarquivar" era inalcançável.** A lista só mostrava ativas, então arquivar
virou porta de mão única e o botão que eu tinha acabado de escrever não podia
ser tocado. Corrigido com o alternador "Ver arquivadas".

**Lição registrada no CLAUDE.md:** a checagem no emulador não é opcional.

---

## Erros de processo

**Commitei uma vez com `ktlintCheck` falhando**, o que o AGENTS.md proíbe.
Corrigido e emendado, mas o gate existe justamente pra impedir isso.

---

## Verificado vs. assumido

| Item | Status |
|---|---|
| Lógica de datas, custo, lembrete, repositório | **Testado** — 66 testes contra SQLite real em memória |
| Persistência sobrevivendo a force-stop | **Verificado** no emulador |
| Notificação disparando | **Verificado** — "R$54.90 é cobrado em 3 dias" na gaveta |
| Dedup não repetindo | **Verificado** — worker rodou 2x (log), notificou 1x |
| Editar / arquivar / desarquivar / excluir | **Verificado** no emulador |
| Reboot reagendando o job | **Verificado** — reiniciado sem abrir o app, job voltou |
| Notificação disparando **depois** de reboot | **Não verificado** — os dois mecanismos estão provados, mas não encadeados |
| Formatação pt-BR de moeda | **Só por teste unitário** — emulador está em en-US |
| SQLite do Android vs. do JDBC | Testes usam JDBC; são implementações diferentes |
| Caminho iOS | **Preparado, não verificado** — exige macOS |

---

## Aberto para amanhã

Ícone do app (`MissingApplicationIcon` no lint; a notificação usa ícone do
sistema como provisório). **Decisão de arte pendente com o usuário** — gerar um
vetor simples ou receber a arte pronta.

Depois disso a Fase 1 fecha. Fase 2 é widget de tela inicial e export CSV.
