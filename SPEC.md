# SPEC.md — SubTrack

## Problema

Usuário perde dinheiro com assinatura recorrente esquecida. Estatística de mercado: 42% dos consumidores esquecem que pagam por pelo menos uma assinatura, custo médio de US$204/ano em cobrança não usada (C+R Research / CNET, ver `Monetizacao_ResumeBuilder_SubscriptionTracker_20260730.md`, achado 2).

Concorrentes existentes falham em dois extremos: (a) linkam conta bancária (Rocket Money, Monarch) — complexo, exige Plaid/dado sensível, modelo pesado de manter; (b) são simples demais (Bobby) — cobrança única barata mas pobre em feature (sem widget, sem export, sem alerta configurável). Gap identificado: app manual, privado (nunca pede banco), com feature completa (alerta, widget, export/import CSV) sem virar SaaS pesado.

## Não-objetivos (v1)

- Não linka banco, não usa Plaid ou qualquer agregador financeiro.
- Não faz scan de e-mail/Gmail pra detectar assinatura automaticamente (fica pra avaliação futura, fora do escopo de baixa manutenção).
- Não vira "central de lembrete da casa" (garantia, cartão-presente, etc. — gap identificado pelo Squirreld) até v1-v3 provarem tração real de uso.
- Não tem conta de usuário / login / sync em nuvem própria no MVP — dado fica no dispositivo.

## Comportamento esperado (MVP — Fase 1)

1. Usuário adiciona assinatura manualmente: nome, valor, moeda, ciclo de cobrança (semanal/mensal/anual/customizado), data da próxima renovação, categoria opcional.
2. App calcula e mostra gasto recorrente total (mensal e anual) somando todas as assinaturas ativas.
3. App dispara notificação local configurável (padrão: 3 dias antes da renovação) por assinatura.
4. Usuário pode pausar/cancelar/arquivar assinatura sem apagar o histórico.
5. Todo dado persiste localmente (SQLDelight) e sobrevive a reinstalo via backup/restore manual (export/import de arquivo, não sync automático no MVP).

## Arquitetura

- **Kotlin Multiplatform** — módulo `shared/` com modelo de dados, lógica de cálculo de recorrência, e camada de persistência (SQLDelight, roda em Android e iOS sem reescrever).
- **Compose Multiplatform** pra UI — alvo inicial Android (`androidApp/`), estrutura já preparada pra adicionar `iosApp/` na Fase 4 sem refatorar o core.
- **Notificação:** WorkManager no Android (agendamento local, sem push).
- **Sem backend.** Sem API própria. Sem autenticação.

## Dependências e versões (fixar na Fase 0, atualizar aqui quando decidido)

- Kotlin: versão estável mais recente compatível com Compose Multiplatform no momento da Fase 0.
- Compose Multiplatform: última versão estável.
- SQLDelight: última versão estável com suporte Android+iOS confirmado.
- Sem dependência de rede em nenhuma lib core.

## Necessidades de segurança/privacidade

- Nenhum dado sai do dispositivo no MVP — sem telemetria, sem analytics de terceiro que capture dado financeiro.
- Se export/import de arquivo for implementado (Fase 2/3), o arquivo é local (compartilhado pelo próprio usuário via mecanismo do SO) — sem upload pra servidor próprio.
- Se no futuro adicionar assinatura paga própria (compra do app), seguir as regras de disclosure descritas no relatório de monetização (preço, duração, cancelamento visíveis antes da cobrança — requisito Apple desde 28/04/2026 e Google Play 2026 de cancelamento em até 2 toques).

## Critério de aceite (MVP)

- [ ] Adicionar, editar, arquivar assinatura funciona e persiste após fechar o app.
- [ ] Total mensal/anual recalcula corretamente ao adicionar/remover/arquivar.
- [ ] Notificação dispara na data configurada (testável simulando data do sistema).
- [ ] Zero chamada de rede em todo o app (auditável — nenhuma permissão de INTERNET no manifest, ou permissão presente mas nenhuma chamada real).
- [ ] Build de release gera APK instalável sem erro de lint/teste.

## Modelo de monetização (referência, decisão final na Fase 5)

Grátis até N assinaturas ativas (definir N, sugestão inicial: 5, alinhado ao Bobby); versão "pro" via compra única ou anual barato (US$2.99–4.99 único ou ~US$15/ano) desbloqueando: assinaturas ilimitadas, widget de tela inicial, export/import CSV. Ver relatório de monetização pra benchmark completo.
