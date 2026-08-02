package com.subtrack.shared

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.subtrack.shared.db.SubTrackDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CsvExportTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: SubscriptionRepository

    private val today = LocalDate.parse("2026-08-02")

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SubTrackDatabase.Schema.create(driver)
        repository = SubscriptionRepository(SubTrackDatabase(driver), UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private suspend fun add(
        name: String,
        amountCents: Long = 5490,
        category: String? = null,
    ) = repository.add(
        SubscriptionDraft(
            name = name,
            amountCents = amountCents,
            currency = "BRL",
            cycle = BillingCycle.MONTHLY,
            anchor = LocalDate.parse("2026-07-20"),
            category = category,
        ),
    )

    private suspend fun csv() = repository.observeAll(today).first().toCsv()

    private suspend fun lines() = csv().trimEnd('\r', '\n').split("\r\n")

    @Test
    fun emptyWalletStillProducesTheHeader() = runTest {
        // Um arquivo vazio pareceria falha de export; só o cabeçalho diz
        // "exportou, e não havia nada".
        val lines = lines()
        assertEquals(1, lines.size)
        assertTrue(lines.first().startsWith("name,amount,currency"))
    }

    @Test
    fun writesOneRowPerSubscription() = runTest {
        add("Netflix")
        add("Spotify")
        assertEquals(3, lines().size)
    }

    @Test
    fun amountIsPlainDecimalWithTwoPlaces() = runTest {
        add("Netflix", amountCents = 5490)
        assertTrue(lines()[1].contains(",54.90,"), "linha: ${lines()[1]}")
    }

    @Test
    fun thousandsAreNotGroupedBecauseTheSeparatorWouldCollideWithTheFieldSeparator() = runTest {
        add("Caro", amountCents = 123450)
        assertTrue(lines()[1].contains(",1234.50,"), "linha: ${lines()[1]}")
    }

    @Test
    fun aNameWithACommaIsQuotedInsteadOfSplittingTheRow() = runTest {
        // Sem aspas isto viraria duas colunas e desalinharia tudo em silêncio.
        add("Spotify, plano família")
        assertTrue(lines()[1].startsWith("\"Spotify, plano família\","), "linha: ${lines()[1]}")
    }

    @Test
    fun aQuoteInsideANameIsDoubled() = runTest {
        add("Plano \"Premium\"")
        assertTrue(lines()[1].startsWith("\"Plano \"\"Premium\"\"\","), "linha: ${lines()[1]}")
    }

    @Test
    fun aNewlineInsideANameDoesNotBreakTheRow() = runTest {
        add("Duas\nlinhas")
        // Escapado com aspas, então a quebra não conta como fim de registro:
        // o arquivo continua com cabeçalho + 1 registro.
        assertTrue(csv().contains("\"Duas\nlinhas\""))
    }

    @Test
    fun archivedSubscriptionsAreExportedBecauseABackupThatDropsHistoryIsNotABackup() = runTest {
        val id = add("Cancelada")
        repository.setArchived(id, archived = true)

        val row = lines()[1]
        assertTrue(row.contains(",true,"), "linha: $row")
    }

    @Test
    fun anAbsentCategoryBecomesAnEmptyFieldNotTheWordNull() = runTest {
        add("Sem categoria", category = null)
        assertTrue(!lines()[1].contains("null"), "linha: ${lines()[1]}")
    }

    @Test
    fun theDerivedNextRenewalIsIncludedForWhoeverOpensTheFile() = runTest {
        add("Netflix")
        // Âncora 20/07 mensal, hoje 02/08 → próxima em 20/08.
        assertTrue(lines()[1].endsWith(",2026-08-20"), "linha: ${lines()[1]}")
    }

    @Test
    fun fieldsWithoutSpecialCharactersAreNotQuoted() = runTest {
        add("Netflix")
        assertTrue(lines()[1].startsWith("Netflix,"), "linha: ${lines()[1]}")
    }

    @Test
    fun centsFormattingHandlesEdgeValues() {
        assertEquals("0.00", centsToPlainDecimal(0))
        assertEquals("0.05", centsToPlainDecimal(5))
        assertEquals("0.99", centsToPlainDecimal(99))
        assertEquals("1.00", centsToPlainDecimal(100))
    }

    @Test
    fun fileNameCarriesTheDateSoExportsSortAndDoNotOverwrite() {
        assertEquals("subtrack-2026-08-02.csv", csvFileName(today))
    }
}
