package com.subtrack.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
        items(state.upcoming, key = { it.subscription.id }) { upcoming ->
            SubscriptionRow(upcoming = upcoming, today = today)
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
    modifier: Modifier = Modifier,
) {
    val subscription = upcoming.subscription
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.padding(end = 12.dp)) {
            Text(text = subscription.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = renewalLabel(upcoming.nextRenewal, today),
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
