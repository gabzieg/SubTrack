package com.subtrack.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.subtrack.shared.BillingCycle
import com.subtrack.shared.RecurringCost
import com.subtrack.shared.totalMonthlyCents
import com.subtrack.shared.totalYearlyCents

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SkeletonScreen()
                }
            }
        }
    }
}

// Tela provisória da Fase 0 — existe só pra provar que :androidApp enxerga
// :shared e que o build de UI fecha. Substituída na Fase 1 (ver ROADMAP.md).
@Composable
private fun SkeletonScreen() {
    val sample = listOf(
        RecurringCost(amountCents = 5490, cycle = BillingCycle.MONTHLY),
        RecurringCost(amountCents = 12000, cycle = BillingCycle.YEARLY),
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("SubTrack", style = MaterialTheme.typography.headlineMedium)
        Text("Mensal: ${sample.totalMonthlyCents()} centavos")
        Text("Anual: ${sample.totalYearlyCents()} centavos")
    }
}
