package com.subtrack.shared

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CsvImportTest {
    private val today = LocalDate.parse("2026-08-05")

    // --- parseCsvRows ---

    @Test
    fun parseCsvRowsHandlesSimpleFields() {
        val result = parseCsvRows("a,b,c\n1,2,3")
        assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "3")), result)
    }

    @Test
    fun parseCsvRowsHandlesQuotedFieldWithComma() {
        val result = parseCsvRows("\"Spotify, família\",5490,BRL")
        assertEquals(listOf(listOf("Spotify, família", "5490", "BRL")), result)
    }

    @Test
    fun parseCsvRowsHandlesDoubleQuoteEscapeInsideField() {
        val result = parseCsvRows("\"He said \"\"hello\"\"\",ok")
        assertEquals(listOf(listOf("He said \"hello\"", "ok")), result)
    }

    @Test
    fun parseCsvRowsHandlesEmptyField() {
        val result = parseCsvRows("a,,c")
        assertEquals(listOf(listOf("a", "", "c")), result)
    }

    @Test
    fun parseCsvRowsHandlesCrLfLineEndings() {
        val result = parseCsvRows("a,b\r\n1,2")
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), result)
    }

    @Test
    fun parseCsvRowsHandlesEmptyContent() {
        assertEquals(emptyList(), parseCsvRows(""))
    }

    @Test
    fun parseCsvRowsHandlesTrailingCrLfWithoutExtraRow() {
        val result = parseCsvRows("a,b\r\n1,2\r\n")
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), result)
    }

    // --- detectSubTrackFormat ---

    @Test
    fun detectSubTrackFormatReturnsTrueForExactHeader() {
        val header = listOf(
            "name", "amount", "currency", "cycle", "every_n",
            "charge_date", "category", "notify_days_before", "archived", "next_renewal",
        )
        assertTrue(detectSubTrackFormat(header))
    }

    @Test
    fun detectSubTrackFormatReturnsFalseForUnknownHeader() {
        assertFalse(detectSubTrackFormat(listOf("Name", "Value", "Date")))
    }

    @Test
    fun detectSubTrackFormatReturnsFalseForPartialHeader() {
        assertFalse(detectSubTrackFormat(listOf("name", "amount")))
    }

    // --- importSubTrackCsv ---

    @Test
    fun importSubTrackCsvParsesAllMandatoryFields() {
        val csv = """
            name,amount,currency,cycle,every_n,charge_date,category,notify_days_before,archived,next_renewal
            Netflix,54.90,BRL,MONTHLY,1,2026-01-15,Streaming,3,false,2026-08-15
        """.trimIndent()
        val rows = parseCsvRows(csv)
        val result = importSubTrackCsv(rows)

        assertEquals(1, result.imported.size)
        assertEquals(0, result.skipped)
        with(result.imported.single()) {
            assertEquals("Netflix", name)
            assertEquals(5490L, amountCents)
            assertEquals("BRL", currency)
            assertEquals(BillingCycle.MONTHLY, cycle)
            assertEquals(1, everyN)
            assertEquals(LocalDate.parse("2026-01-15"), anchor)
            assertEquals("Streaming", category)
            assertEquals(3, notifyDaysBefore)
        }
    }

    @Test
    fun importSubTrackCsvSkipsRowsWithInvalidAmount() {
        val csv = "name,amount,currency,cycle,every_n,charge_date,category,notify_days_before,archived,next_renewal\n" +
            "Netflix,abc,BRL,MONTHLY,1,2026-01-15,,3,false,2026-08-15"
        val result = importSubTrackCsv(parseCsvRows(csv))
        assertEquals(0, result.imported.size)
        assertEquals(1, result.skipped)
    }

    @Test
    fun importSubTrackCsvSkipsRowsWithEmptyName() {
        val csv = "name,amount,currency,cycle,every_n,charge_date,category,notify_days_before,archived,next_renewal\n" +
            ",54.90,BRL,MONTHLY,1,2026-01-15,,3,false,2026-08-15"
        val result = importSubTrackCsv(parseCsvRows(csv))
        assertEquals(0, result.imported.size)
        assertEquals(1, result.skipped)
    }

    @Test
    fun importSubTrackCsvHandlesEmptyCategoryAsNull() {
        val csv = "name,amount,currency,cycle,every_n,charge_date,category,notify_days_before,archived,next_renewal\n" +
            "Netflix,54.90,BRL,MONTHLY,1,2026-01-15,,3,false,2026-08-15"
        val result = importSubTrackCsv(parseCsvRows(csv))
        assertNull(result.imported.single().category)
    }

    @Test
    fun importSubTrackCsvIgnoresArchivedAndNextRenewalFields() {
        // archived = true no arquivo, mas o import deve criar a assinatura como ativa (archived não está no Draft)
        val csv = "name,amount,currency,cycle,every_n,charge_date,category,notify_days_before,archived,next_renewal\n" +
            "Netflix,54.90,BRL,MONTHLY,1,2026-01-15,,3,true,2026-08-15"
        val result = importSubTrackCsv(parseCsvRows(csv))
        assertEquals(1, result.imported.size)
    }

    @Test
    fun importSubTrackCsvReturnsEmptyForHeaderOnly() {
        val csv = "name,amount,currency,cycle,every_n,charge_date,category,notify_days_before,archived,next_renewal"
        val result = importSubTrackCsv(parseCsvRows(csv))
        assertEquals(0, result.imported.size)
        assertEquals(0, result.skipped)
    }

    // --- importGenericCsv ---

    @Test
    fun importGenericCsvWithMinimalMappingUsesDefaults() {
        // CSV externo com só name e amount — outros campos recebem defaults
        val csv = "Serviço,Valor,Observação\nNetflix,54.90,ignorado"
        val rows = parseCsvRows(csv)
        val mapping = ColumnMapping(nameIndex = 0, amountIndex = 1)
        val result = importGenericCsv(rows, mapping, defaultAnchor = today)

        assertEquals(1, result.imported.size)
        with(result.imported.single()) {
            assertEquals("Netflix", name)
            assertEquals(5490L, amountCents)
            assertEquals("BRL", currency)
            assertEquals(BillingCycle.MONTHLY, cycle)
            assertEquals(today, anchor)
            assertNull(category)
            assertEquals(3, notifyDaysBefore)
        }
    }

    @Test
    fun importGenericCsvSkipsRowsWithInvalidAmount() {
        val csv = "name,valor\nNetflix,abc\nSpotify,21.90"
        val mapping = ColumnMapping(nameIndex = 0, amountIndex = 1)
        val result = importGenericCsv(parseCsvRows(csv), mapping, defaultAnchor = today)
        assertEquals(1, result.imported.size)
        assertEquals(1, result.skipped)
    }

    @Test
    fun importGenericCsvUsesOptionalMappingsWhenProvided() {
        val csv = "name,amount,cur,cycle\nGympass,99.00,USD,YEARLY"
        val mapping = ColumnMapping(
            nameIndex = 0,
            amountIndex = 1,
            currencyIndex = 2,
            cycleIndex = 3,
        )
        val result = importGenericCsv(parseCsvRows(csv), mapping, defaultAnchor = today)
        assertEquals(1, result.imported.size)
        with(result.imported.single()) {
            assertEquals("USD", currency)
            assertEquals(BillingCycle.YEARLY, cycle)
        }
    }

    // --- roundtrip ---

    @Test
    fun roundtripExportThenImportPreservesAllMandatoryFields() {
        val draft = SubscriptionDraft(
            name = "Netflix",
            amountCents = 5490L,
            currency = "BRL",
            cycle = BillingCycle.MONTHLY,
            everyN = 1,
            anchor = LocalDate.parse("2026-01-15"),
            category = "Streaming",
            notifyDaysBefore = 3,
        )
        // Simular o export: montar a linha manualmente com os campos do draft
        val header = "name,amount,currency,cycle,every_n,charge_date,category,notify_days_before,archived,next_renewal"
        val row = "${draft.name},${centsToPlainDecimal(draft.amountCents)},${draft.currency}," +
            "${draft.cycle.name},${draft.everyN},${draft.anchor}," +
            "${draft.category.orEmpty()},${draft.notifyDaysBefore},false,2026-08-15"
        val csv = "$header\n$row"

        val result = importSubTrackCsv(parseCsvRows(csv))
        assertEquals(1, result.imported.size)
        val imported = result.imported.single()
        assertEquals(draft.name, imported.name)
        assertEquals(draft.amountCents, imported.amountCents)
        assertEquals(draft.currency, imported.currency)
        assertEquals(draft.cycle, imported.cycle)
        assertEquals(draft.everyN, imported.everyN)
        assertEquals(draft.anchor, imported.anchor)
        assertEquals(draft.category, imported.category)
        assertEquals(draft.notifyDaysBefore, imported.notifyDaysBefore)
    }
}
