package com.subtrack.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.subtrack.shared.BillingCycle
import com.subtrack.shared.SubscriptionDraft
import com.subtrack.shared.formatMoney
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionScreen(
    today: LocalDate,
    onSave: (SubscriptionDraft) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    // Só os dígitos. Os centavos preenchem da direita pra esquerda, então não
    // existe entrada ambígua: digitar 5490 é R$ 54,90, e ponto ou vírgula nem
    // chegam a ser digitáveis.
    var amountDigits by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("BRL") }
    var cycle by remember { mutableStateOf(BillingCycle.MONTHLY) }
    var anchorText by remember { mutableStateOf(today.toString()) }
    var cycleMenuOpen by remember { mutableStateOf(false) }

    val amountCents = amountDigits.toLongOrNull() ?: 0L
    val anchor = runCatching { LocalDate.parse(anchorText) }.getOrNull()
    val canSave = name.isNotBlank() && amountCents > 0 && anchor != null

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Nova assinatura", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        val amountDisplay = if (amountDigits.isEmpty()) "" else formatMoney(amountCents, currency)
        OutlinedTextField(
            // O cursor fica preso no fim: o texto exibido é derivado, não o que
            // foi digitado, e deixá-lo livre faria dígito novo cair no meio do
            // valor formatado.
            value = TextFieldValue(amountDisplay, selection = TextRange(amountDisplay.length)),
            onValueChange = { typed ->
                amountDigits = typed.text.filter { it.isDigit() }.trimStart('0').take(12)
            },
            label = { Text("Valor") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = currency,
            onValueChange = { currency = it.uppercase().take(3) },
            label = { Text("Moeda") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        ExposedDropdownMenuBox(
            expanded = cycleMenuOpen,
            onExpandedChange = { cycleMenuOpen = it },
        ) {
            OutlinedTextField(
                value = cycle.label(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Ciclo") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cycleMenuOpen) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = cycleMenuOpen,
                onDismissRequest = { cycleMenuOpen = false },
            ) {
                BillingCycle.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label()) },
                        onClick = {
                            cycle = option
                            cycleMenuOpen = false
                        },
                    )
                }
            }
        }

        // Qualquer cobrança que já tenha acontecido serve de âncora — o cálculo
        // anda em ciclos inteiros a partir dela, então a série de datas é a
        // mesma. Pedir "a data da cobrança" em vez de "a primeira" é mais fácil
        // de responder: está na fatura do cartão, não na memória.
        OutlinedTextField(
            value = anchorText,
            onValueChange = { anchorText = it },
            label = { Text("Data da cobrança (AAAA-MM-DD)") },
            singleLine = true,
            isError = anchor == null,
            supportingText = {
                if (anchor == null) Text("Data inválida")
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            Button(
                enabled = canSave,
                onClick = {
                    onSave(
                        SubscriptionDraft(
                            name = name.trim(),
                            amountCents = amountCents,
                            currency = currency,
                            cycle = cycle,
                            anchor = anchor!!,
                        ),
                    )
                },
            ) { Text("Salvar") }
        }
    }
}

private fun BillingCycle.label(): String = when (this) {
    BillingCycle.DAILY -> "Diário"
    BillingCycle.WEEKLY -> "Semanal"
    BillingCycle.MONTHLY -> "Mensal"
    BillingCycle.YEARLY -> "Anual"
}
