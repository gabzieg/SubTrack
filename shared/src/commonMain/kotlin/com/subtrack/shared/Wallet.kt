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

/**
 * Assinaturas de uma mesma categoria, na ordem definida pelo [SortOrder] ativo.
 * [category] é null para assinaturas sem categoria — exibidas sempre como
 * último grupo na UI.
 */
data class CategoryGroup(
    val category: String?,
    val items: List<UpcomingSubscription>,
)

data class WalletState(
    val upcoming: List<UpcomingSubscription> = emptyList(),
    val totals: List<WalletTotals> = emptyList(),
    val groupedByCategory: List<CategoryGroup> = emptyList(),
    val sortOrder: SortOrder = SortOrder.RENEWAL,
) {
    val isEmpty: Boolean get() = upcoming.isEmpty()
}

/**
 * @param includeArchived quando true, a lista traz também as arquivadas. Os
 *   totais continuam contando só as ativas: arquivada é assinatura que não é
 *   mais paga, então somá-la mentiria sobre o gasto.
 * @param sortOrder critério de ordenação dos itens dentro de cada grupo de
 *   categoria. Os grupos em si continuam em ordem alfabética com null por último.
 */
fun SubscriptionRepository.observeWallet(
    today: LocalDate,
    includeArchived: Boolean = false,
    sortOrder: SortOrder = SortOrder.RENEWAL,
): Flow<WalletState> {
    val source = if (includeArchived) observeAll(today) else observeActive(today)
    return source.map { upcoming ->
        val sorted = upcoming.applySortOrder(sortOrder)
        WalletState(
            upcoming = sorted,
            totals = sorted.filterNot { it.subscription.archived }.totalsByCurrency(),
            groupedByCategory = sorted.groupedByCategory(),
            sortOrder = sortOrder,
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

/**
 * Aplica o critério de ordenação à lista. RENEWAL não reordena: o repositório
 * já entrega ordenado por nextRenewal → nome. Os demais critérios reordenam
 * a lista inteira; o agrupamento por categoria acontece depois, então a ordem
 * dentro de cada grupo reflete o critério escolhido.
 *
 * DATE_ADDED usa [Subscription.id] como proxy de data de inserção — o id é
 * INTEGER PRIMARY KEY autoincrement, então id maior = adicionado mais recente.
 * Não exige coluna extra nem migration.
 */
internal fun List<UpcomingSubscription>.applySortOrder(order: SortOrder): List<UpcomingSubscription> = when (order) {
    SortOrder.RENEWAL -> this
    SortOrder.ALPHABETICAL -> sortedBy { it.subscription.name }
    SortOrder.VALUE_DESC -> sortedByDescending { it.subscription.amountCents }
    SortOrder.DATE_ADDED -> sortedByDescending { it.subscription.id }
}

/**
 * Agrupa por categoria em ordem alfabética; null (sem categoria) sempre por
 * último. A ordenação interna de cada grupo herda o critério já aplicado por
 * [applySortOrder] — não reordenamos aqui.
 */
internal fun List<UpcomingSubscription>.groupedByCategory(): List<CategoryGroup> = groupBy { it.subscription.category }
    .entries
    .sortedWith(
        compareBy(
            // null fica depois de qualquer String (true > false)
            { (category, _) -> category == null },
            { (category, _) -> category },
        ),
    )
    .map { (category, items) -> CategoryGroup(category = category, items = items) }
