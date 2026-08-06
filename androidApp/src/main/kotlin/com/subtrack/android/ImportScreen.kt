package com.subtrack.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.subtrack.shared.ColumnMapping

@Composable
fun ImportScreen(
    rows: List<List<String>>,
    onImport: (ColumnMapping) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val header = remember(rows) { rows.firstOrNull().orEmpty() }

    var nameIdx by remember { mutableStateOf<Int?>(null) }
    var amountIdx by remember { mutableStateOf<Int?>(null) }
    var currencyIdx by remember { mutableStateOf<Int?>(null) }
    var cycleIdx by remember { mutableStateOf<Int?>(null) }
    var chargeDateIdx by remember { mutableStateOf<Int?>(null) }
    var categoryIdx by remember { mutableStateOf<Int?>(null) }

    val canImport = nameIdx != null && amountIdx != null

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Mapear colunas do CSV",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Escolha qual coluna do arquivo corresponde a cada campo.",
            style = MaterialTheme.typography.bodyMedium,
        )

        ColumnDropdown("Nome (obrigatório)", header, nameIdx) { nameIdx = it }
        ColumnDropdown("Valor (obrigatório)", header, amountIdx) { amountIdx = it }
        ColumnDropdown("Moeda", header, currencyIdx) { currencyIdx = it }
        ColumnDropdown("Ciclo", header, cycleIdx) { cycleIdx = it }
        ColumnDropdown("Data da cobrança", header, chargeDateIdx) { chargeDateIdx = it }
        ColumnDropdown("Categoria", header, categoryIdx) { categoryIdx = it }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            Button(
                enabled = canImport,
                onClick = {
                    onImport(
                        ColumnMapping(
                            nameIndex = nameIdx!!,
                            amountIndex = amountIdx!!,
                            currencyIndex = currencyIdx,
                            cycleIndex = cycleIdx,
                            chargeDateIndex = chargeDateIdx,
                            categoryIndex = categoryIdx,
                        ),
                    )
                },
            ) { Text("Importar") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnDropdown(
    label: String,
    header: List<String>,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayValue = if (selectedIndex == null) {
        "Não importar"
    } else {
        header.getOrElse(selectedIndex) { "Coluna ${selectedIndex + 1}" }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Não importar") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            header.forEachIndexed { index, colName ->
                val name = colName.ifBlank { "Coluna ${index + 1}" }
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                )
            }
        }
    }
}
