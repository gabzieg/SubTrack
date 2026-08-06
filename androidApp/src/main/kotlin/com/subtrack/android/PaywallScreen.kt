package com.subtrack.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PaywallScreen(
    onBuyClick: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Desbloqueie o SubTrack Pro",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        FeatureRow("Assinaturas ilimitadas", "Acompanhe quantas assinaturas quiser (grátis até 5).")
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow("Widget de tela inicial", "Veja a próxima renovação e o total do mês direto na sua home.")
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow("Backup e Importação CSV", "Exporte e importe seus dados para planilhas sempre que precisar.")

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "R$ 14,90",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Pagamento único. Sem mensalidades.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBuyClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("Comprar agora", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onCancel) {
            Text("Agora não")
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Esta compra desbloqueia os recursos permanentemente nesta conta Google. Não é uma assinatura recorrente, portanto não exige cancelamento.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun FeatureRow(title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Fallback pra não precisar adicionar um drawable novo só pro icone (usa a fonte)
        Text("✨", modifier = Modifier.padding(end = 16.dp), style = MaterialTheme.typography.headlineSmall)
        Column {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
