package com.subtrack.shared

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * Total recorrente de uma única moeda.
 *
 * Os totais são por moeda, não um número só: somar 54,90 BRL com 12,00 USD
 * daria 66,90 de coisa nenhuma. Converter exigiria cotação de câmbio, ou seja,
 * rede — e o SPEC proíbe qualquer dado saindo do dispositivo. Então cada moeda
 * é totalizada por conta própria e a tela mostra as linhas que existirem.
 */
data class WalletTotals(
    val currencyCode: String,
    val monthlyCents: Long,
    val yearlyCents: Long,
)

data class WalletState(
    val upcoming: List<UpcomingSubscription> = emptyList(),
    val totals: List<WalletTotals> = emptyList(),
) {
    val isEmpty: Boolean get() = upcoming.isEmpty()
}

/**
 * @param includeArchived quando true, a lista traz também as arquivadas. Os
 *   totais continuam contando só as ativas: arquivada é assinatura que não é
 *   mais paga, então somá-la mentiria sobre o gasto.
 */
fun SubscriptionRepository.observeWallet(
    today: LocalDate,
    includeArchived: Boolean = false,
): Flow<WalletState> {
    val source = if (includeArchived) observeAll(today) else observeActive(today)
    return source.map { upcoming ->
        WalletState(
            upcoming = upcoming,
            totals = upcoming.filterNot { it.subscription.archived }.totalsByCurrency(),
        )
    }
}

internal fun List<UpcomingSubscription>.totalsByCurrency(): List<WalletTotals> = groupBy { it.subscription.currency }
    .map { (currency, items) ->
        val costs = items.map { it.subscription.toRecurringCost() }
        WalletTotals(
            currencyCode = currency,
            monthlyCents = costs.totalMonthlyCents(),
            yearlyCents = costs.totalYearlyCents(),
        )
    }
    .sortedBy { it.currencyCode }
