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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
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
import com.subtrack.shared.Subscription
import com.subtrack.shared.SubscriptionDraft
import com.subtrack.shared.formatMoney
import kotlinx.datetime.LocalDate

/**
 * Serve tanto pra cadastro quanto pra edição. São a mesma tela porque os campos
 * são os mesmos — duplicar renderia duas telas divergindo com o tempo.
 *
 * @param editing null pra uma assinatura nova.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionFormScreen(
    today: LocalDate,
    editing: Subscription?,
    onSave: (SubscriptionDraft) -> Unit,
    onArchiveToggle: (Subscription) -> Unit,
    onDelete: (Subscription) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember(editing) { mutableStateOf(editing?.name ?: "") }
    // Só os dígitos. Os centavos preenchem da direita pra esquerda, então não
    // existe entrada ambígua: digitar 5490 é R$ 54,90, e ponto ou vírgula nem
    // chegam a ser digitáveis.
    var amountDigits by remember(editing) { mutableStateOf(editing?.amountCents?.toString() ?: "") }
    var currency by remember(editing) { mutableStateOf(editing?.currency ?: "BRL") }
    var cycle by remember(editing) { mutableStateOf(editing?.cycle ?: BillingCycle.MONTHLY) }
    var everyNText by remember(editing) { mutableStateOf((editing?.everyN ?: 1).toString()) }
    var anchorText by remember(editing) { mutableStateOf((editing?.anchor ?: today).toString()) }
    var category by remember(editing) { mutableStateOf(editing?.category ?: "") }
    var notifyDaysText by remember(editing) { mutableStateOf((editing?.notifyDaysBefore ?: 3).toString()) }
    var cycleMenuOpen by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    val amountCents = amountDigits.toLongOrNull() ?: 0L
    val anchor = runCatching { LocalDate.parse(anchorText) }.getOrNull()
    val everyN = everyNText.toIntOrNull()
    val notifyDays = notifyDaysText.toIntOrNull()
    val canSave = name.isNotBlank() &&
        amountCents > 0 &&
        anchor != null &&
        everyN != null &&
        everyN >= 1 &&
        notifyDays != null &&
        notifyDays >= 0

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (editing == null) "Nova assinatura" else "Editar assinatura",
            style = MaterialTheme.typography.headlineSmall,
        )

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

        OutlinedTextField(
            value = everyNText,
            onValueChange = { everyNText = it.filter { char -> char.isDigit() }.take(2) },
            label = { Text("A cada quantos ciclos") },
            singleLine = true,
            isError = everyN == null || everyN < 1,
            supportingText = { Text(cycle.everyNHint(everyN ?: 1)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )

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

        OutlinedTextField(
            value = notifyDaysText,
            onValueChange = { notifyDaysText = it.filter { char -> char.isDigit() }.take(2) },
            label = { Text("Avisar quantos dias antes") },
            singleLine = true,
            isError = notifyDays == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Categoria (opcional)") },
            singleLine = true,
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
                            everyN = everyN ?: 1,
                            anchor = anchor!!,
                            category = category.trim().ifBlank { null },
                            notifyDaysBefore = notifyDays ?: 3,
                        ),
                    )
                },
            ) { Text("Salvar") }
        }

        if (editing != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Arquivar é a saída padrão pra assinatura cancelada: tira da
                // lista e dos totais sem apagar o histórico (ver SPEC.md).
                TextButton(onClick = { onArchiveToggle(editing) }) {
                    Text(if (editing.archived) "Desarquivar" else "Arquivar")
                }
                TextButton(onClick = { confirmingDelete = true }) { Text("Excluir") }
            }
        }
    }

    if (confirmingDelete && editing != null) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Excluir ${editing.name}?") },
            text = { Text("Isso apaga a assinatura de vez. Para só tirar da lista, use Arquivar.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    onDelete(editing)
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text("Cancelar") }
            },
        )
    }
}

private fun BillingCycle.label(): String = when (this) {
    BillingCycle.DAILY -> "Diário"
    BillingCycle.WEEKLY -> "Semanal"
    BillingCycle.MONTHLY -> "Mensal"
    BillingCycle.YEARLY -> "Anual"
}

private fun BillingCycle.everyNHint(everyN: Int): String {
    val unit = when (this) {
        BillingCycle.DAILY -> if (everyN == 1) "dia" else "dias"
        BillingCycle.WEEKLY -> if (everyN == 1) "semana" else "semanas"
        BillingCycle.MONTHLY -> if (everyN == 1) "mês" else "meses"
        BillingCycle.YEARLY -> if (everyN == 1) "ano" else "anos"
    }
    return if (everyN == 1) "Cobrado todo $unit" else "Cobrado a cada $everyN $unit"
}
