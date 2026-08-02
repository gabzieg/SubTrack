package com.subtrack.shared

/**
 * O que cabe num widget de tela inicial.
 *
 * Um widget pequeno não comporta uma linha por moeda como a tela principal.
 * Mostrar só a primeira seria arbitrário, e somar seria mentira (ver
 * [WalletTotals]). A saída escolhida: mostra a moeda de maior gasto anual e
 * sinaliza que existem outras, para o número exibido nunca ser lido como se
 * fosse o gasto total.
 */
data class WidgetSummary(
    val primaryTotal: WalletTotals?,
    val hasOtherCurrencies: Boolean,
    val next: UpcomingSubscription?,
) {
    val isEmpty: Boolean get() = primaryTotal == null
}

fun WalletState.toWidgetSummary(): WidgetSummary {
    val active = upcoming.filterNot { it.subscription.archived }
    return WidgetSummary(
        primaryTotal = totals.maxByOrNull { it.yearlyCents },
        hasOtherCurrencies = totals.size > 1,
        // A lista já vem ordenada por renovação, então a próxima é a primeira.
        next = active.firstOrNull(),
    )
}
