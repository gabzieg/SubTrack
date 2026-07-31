# ROADMAP.md — SubTrack

## Fase 0 — Global Setup
**Objetivo:** repo pronto pra codar.
**Saída:** projeto KMP inicializado (`shared/`, `androidApp/`), Gradle configurado, lint/test/build rodando localmente, CI básico (build + lint + test em push).
**Critério de saída:** `./gradlew build` passa limpo num projeto vazio/esqueleto.
**Depende de:** nada.

## Fase 1 — MVP (ver SPEC.md, critério de aceite)
**Objetivo:** app funcional que resolve o core do problema — adicionar assinatura, ver total, receber alerta.
**Saída:** app instalável, todos os critérios de aceite do SPEC.md marcados.
**Critério de saída:** todos os checkboxes de "Critério de aceite (MVP)" no SPEC.md fechados + testado manualmente num device/emulador real.
**Depende de:** Fase 0.

## Fase 2 — Diferenciação
**Objetivo:** recursos que separam 5★ de 3★ no nicho (achado 4 do relatório de monetização): widget de tela inicial, export CSV, categorização.
**Saída:** widget mostrando próxima renovação + total do mês; export CSV funcional.
**Critério de saída:** widget testado em tela inicial real; CSV exportado abre corretamente numa planilha.
**Depende de:** Fase 1.

## Fase 3 — Import e Monetização
**Objetivo:** import CSV de fatura (sem OAuth bancário) + implementar tier pago.
**Saída:** fluxo de import mapeando colunas de CSV genérico pra assinatura; paywall implementado seguindo compliance Apple/Google (ver SPEC.md, seção segurança).
**Critério de saída:** import testado com CSV de pelo menos 2 formatos diferentes (ex: export de cartão genérico); paywall passa checklist de disclosure (preço/duração/cancelamento visíveis, cancelamento em app).
**Depende de:** Fase 2.

## Fase 4 — Port iOS
**Objetivo:** validar que a escolha de KMP + Compose Multiplatform realmente compensou — subir `iosApp/` reaproveitando `shared/`.
**Saída:** app rodando em iOS (mínimo: simulador) com paridade de feature da Fase 1-2.
**Critério de saída:** build iOS gera IPA/roda no simulador sem reescrever lógica de negócio.
**Depende de:** Fase 2 completa (não precisa esperar Fase 3).

## Fase 5 — Lançamento
**Objetivo:** publicar nas lojas.
**Saída:** listing Android (Google Play) revisado contra checklist de dark pattern; se iOS pronto, listing Apple também.
**Critério de saída:** app aprovado em ambas as lojas (ou só Android, se Fase 4 ainda não terminou).
**Depende de:** Fase 1 no mínimo (pode lançar Android antes do iOS estar pronto).

## Fase 6 — Expansão (avaliar só depois de tração real)
**Objetivo:** avaliar se vale expandir escopo pro gap "central de lembrete da casa" (garantia, cartão-presente, etc — achado do Squirreld).
**Critério de entrada:** só considerar se Fase 5 tiver uso ativo/retenção que justifique aumentar superfície de manutenção.
