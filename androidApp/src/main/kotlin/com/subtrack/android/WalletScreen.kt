package com.subtrack.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.subtrack.shared.SortOrder
import com.subtrack.shared.Subscription
import com.subtrack.shared.UpcomingSubscription
import com.subtrack.shared.WalletState
import com.subtrack.shared.WalletTotals
import com.subtrack.shared.formatMoney
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

@Composable
fun WalletScreen(
    state: WalletState,
    today: LocalDate,
    onSubscriptionClick: (Subscription) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isEmpty) {
        EmptyWallet(modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TotalsCard(state.totals) }

        item { SortChips(current = state.sortOrder, onSelect = onSortOrderChange) }

        // Há mais de um grupo? Mostra o cabeçalho de seção. Se todas as
        // assinaturas forem sem categoria, o grupo único tem category=null e
        // exibir "Sem categoria" como único cabeçalho seria ruído sem valor.
        val showHeaders = state.groupedByCategory.size > 1

        state.groupedByCategory.forEach { group ->
            if (showHeaders) {
                item(key = "header_${group.category}") {
                    SectionHeader(
                        label = group.category ?: "Sem categoria",
                    )
                }
            }
            items(group.items, key = { it.subscription.id }) { upcoming ->
                SubscriptionRow(
                    upcoming = upcoming,
                    today = today,
                    onClick = { onSubscriptionClick(upcoming.subscription) },
                )
            }
        }
    }
}

@Composable
private fun EmptyWallet(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Nenhuma assinatura ainda",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Adicione as suas para ver quanto elas custam por mês e ser avisado antes de cada renovação.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private val sortOrderLabels = mapOf(
    SortOrder.RENEWAL to "Renovação",
    SortOrder.ALPHABETICAL to "A→Z",
    SortOrder.VALUE_DESC to "Valor",
    SortOrder.DATE_ADDED to "Recentes",
)

@Composable
private fun SortChips(
    current: SortOrder,
    onSelect: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SortOrder.entries.forEach { order ->
            FilterChip(
                selected = order == current,
                onClick = { onSelect(order) },
                label = { Text(sortOrderLabels.getValue(order)) },
            )
        }
    }
}

@Composable
private fun SectionHeader(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun TotalsCard(
    totals: List<WalletTotals>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Uma linha por moeda: totais de moedas diferentes não são somados
            // entre si (ver WalletTotals no módulo shared).
            totals.forEachIndexed { index, total ->
                if (index > 0) HorizontalDivider()
                Column {
                    Text(
                        text = formatMoney(total.monthlyCents, total.currencyCode),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "por mês · ${formatMoney(total.yearlyCents, total.currencyCode)} por ano",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionRow(
    upcoming: UpcomingSubscription,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subscription = upcoming.subscription
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.padding(end = 12.dp)) {
            Text(text = subscription.name, style = MaterialTheme.typography.titleMedium)
            Text(
                // Arquivada não tem próxima renovação a anunciar — dizer
                // "renova em N dias" numa assinatura cancelada seria mentira.
                text = if (subscription.archived) {
                    "Arquivada"
                } else {
                    renewalLabel(upcoming.nextRenewal, today)
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = formatMoney(subscription.amountCents, subscription.currency),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private fun renewalLabel(
    nextRenewal: LocalDate,
    today: LocalDate,
): String = when (val days = today.daysUntil(nextRenewal)) {
    0 -> "Renova hoje"
    1 -> "Renova amanhã"
    else -> "Renova em $days dias"
}
