package com.subtrack.shared

import kotlinx.datetime.LocalDate

/**
 * Cabeçalho produzido por [CsvExport]. Usado pra detectar se um arquivo
 * importado é do próprio SubTrack ou de uma fonte externa.
 */
private val SUBTRACK_HEADER = listOf(
    "name", "amount", "currency", "cycle", "every_n",
    "charge_date", "category", "notify_days_before", "archived", "next_renewal",
)

/** Resultado de um import: assinaturas prontas pra inserir + contagem de ignoradas. */
data class ImportResult(
    val imported: List<SubscriptionDraft>,
    val skipped: Int,
)

/**
 * Mapeamento de colunas para o import genérico: o índice de cada coluna no CSV
 * escolhido pelo usuário. Campos opcionais usam `null` quando não mapeados,
 * e o import aplica os defaults definidos em [importGenericCsv].
 */
data class ColumnMapping(
    val nameIndex: Int,
    val amountIndex: Int,
    val currencyIndex: Int? = null,
    val cycleIndex: Int? = null,
    val everyNIndex: Int? = null,
    val chargeDateIndex: Int? = null,
    val categoryIndex: Int? = null,
)

/**
 * Detecta se o cabeçalho do CSV corresponde exatamente ao formato exportado
 * pelo SubTrack. Se sim, o import pode usar [importSubTrackCsv] sem precisar
 * de mapeamento manual.
 */
fun detectSubTrackFormat(header: List<String>): Boolean = header == SUBTRACK_HEADER

/**
 * Parser RFC 4180 — aceita tanto CRLF quanto LF.
 * Campos entre aspas podem conter vírgulas e quebras de linha.
 * Aspas duplas dentro de campo entre aspas são escapadas como `""`.
 */
fun parseCsvRows(content: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val text = content.replace("\r\n", "\n").replace('\r', '\n').trimEnd('\n')
    if (text.isEmpty()) return rows

    var i = 0
    var inQuotes = false
    var currentField = StringBuilder()
    var currentRow = mutableListOf<String>()

    fun finishField() {
        currentRow.add(currentField.toString())
        currentField = StringBuilder()
    }

    fun finishRow() {
        finishField()
        rows.add(currentRow)
        currentRow = mutableListOf()
    }

    while (i < text.length) {
        val c = text[i]
        when {
            inQuotes && c == '"' -> {
                if (i + 1 < text.length && text[i + 1] == '"') {
                    currentField.append('"')
                    i++
                } else {
                    inQuotes = false
                }
            }
            !inQuotes && c == '"' -> inQuotes = true
            !inQuotes && c == ',' -> finishField()
            !inQuotes && c == '\n' -> finishRow()
            else -> currentField.append(c)
        }
        i++
    }
    if (currentRow.isNotEmpty() || currentField.isNotEmpty()) finishRow()

    return rows
}

/**
 * Import do formato próprio do SubTrack (roundtrip). Espera que a primeira
 * linha seja o cabeçalho exato; linhas com name vazio ou amount inválido são
 * ignoradas e contadas em [ImportResult.skipped].
 *
 * O campo `archived` é ignorado — import sempre cria entradas ativas. O
 * campo `next_renewal` é ignorado — é derivado, não é fonte de verdade.
 */
fun importSubTrackCsv(rows: List<List<String>>): ImportResult {
    if (rows.size < 2) return ImportResult(emptyList(), 0)
    val dataRows = rows.drop(1)
    return parseRows(
        dataRows = dataRows,
        nameIdx = 0,
        amountIdx = 1,
        currencyIdx = 2,
        cycleIdx = 3,
        everyNIdx = 4,
        chargeDateIdx = 5,
        categoryIdx = 6,
        notifyDaysIdx = 7,
        defaultAnchor = null,
    )
}

/**
 * Import genérico a partir de qualquer CSV. O usuário informa o mapeamento de
 * colunas; campos não mapeados recebem defaults conservadores (moeda BRL,
 * ciclo mensal, âncora = [defaultAnchor], aviso 3 dias).
 */
fun importGenericCsv(
    rows: List<List<String>>,
    mapping: ColumnMapping,
    defaultAnchor: LocalDate,
): ImportResult {
    if (rows.size < 2) return ImportResult(emptyList(), 0)
    val dataRows = rows.drop(1)
    return parseRows(
        dataRows = dataRows,
        nameIdx = mapping.nameIndex,
        amountIdx = mapping.amountIndex,
        currencyIdx = mapping.currencyIndex,
        cycleIdx = mapping.cycleIndex,
        everyNIdx = mapping.everyNIndex,
        chargeDateIdx = mapping.chargeDateIndex,
        categoryIdx = mapping.categoryIndex,
        notifyDaysIdx = null,
        defaultAnchor = defaultAnchor,
    )
}

private fun parseRows(
    dataRows: List<List<String>>,
    nameIdx: Int,
    amountIdx: Int,
    currencyIdx: Int?,
    cycleIdx: Int?,
    everyNIdx: Int?,
    chargeDateIdx: Int?,
    categoryIdx: Int?,
    notifyDaysIdx: Int?,
    defaultAnchor: LocalDate?,
): ImportResult {
    val imported = mutableListOf<SubscriptionDraft>()
    var skipped = 0

    for (row in dataRows) {
        val name = row.getOrNull(nameIdx)?.trim().orEmpty()
        if (name.isEmpty()) {
            skipped++
            continue
        }

        val amountCents = parseMoneyToCents(row.getOrNull(amountIdx)?.trim().orEmpty())
        if (amountCents == null) {
            skipped++
            continue
        }

        val currency = currencyIdx?.let { row.getOrNull(it)?.trim() }?.ifEmpty { null } ?: "BRL"

        val cycle = cycleIdx?.let { row.getOrNull(it)?.trim() }
            ?.let { raw -> BillingCycle.entries.firstOrNull { it.name == raw.uppercase() } }
            ?: BillingCycle.MONTHLY

        val everyN = everyNIdx?.let { row.getOrNull(it)?.trim()?.toIntOrNull() } ?: 1

        val anchor = chargeDateIdx?.let { row.getOrNull(it)?.trim() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: defaultAnchor
            ?: run {
                skipped++
                continue
            }

        val category = categoryIdx?.let { row.getOrNull(it)?.trim() }?.ifEmpty { null }

        val notifyDaysBefore = notifyDaysIdx?.let { row.getOrNull(it)?.trim()?.toIntOrNull() } ?: 3

        imported.add(
            SubscriptionDraft(
                name = name,
                amountCents = amountCents,
                currency = currency,
                cycle = cycle,
                everyN = everyN,
                anchor = anchor,
                category = category,
                notifyDaysBefore = notifyDaysBefore,
            ),
        )
    }

    return ImportResult(imported = imported, skipped = skipped)
}
