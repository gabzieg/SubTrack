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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetSummaryTest {
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
        amountCents: Long,
        anchor: String = "2026-07-20",
        currency: String = "BRL",
    ) = repository.add(
        SubscriptionDraft(
            name = name,
            amountCents = amountCents,
            currency = currency,
            cycle = BillingCycle.MONTHLY,
            anchor = LocalDate.parse(anchor),
        ),
    )

    private suspend fun summary() = repository.observeWallet(today).first().toWidgetSummary()

    @Test
    fun emptyWalletReportsItselfInsteadOfShowingZero() = runTest {
        val summary = summary()
        assertTrue(summary.isEmpty)
        assertNull(summary.primaryTotal)
        assertNull(summary.next)
    }

    @Test
    fun showsTheMonthlyTotalAndTheNextRenewal() = runTest {
        add("Netflix", 5490, anchor = "2026-07-20")
        add("Spotify", 2190, anchor = "2026-07-05")

        val summary = summary()
        assertEquals(7680, summary.primaryTotal?.monthlyCents)
        // 05/08 vem antes de 20/08.
        assertEquals("Spotify", summary.next?.subscription?.name)
    }

    @Test
    fun withSeveralCurrenciesShowsTheLargestAndFlagsTheRest() = runTest {
        // Um widget pequeno não cabe uma linha por moeda, e somar seria mentira.
        // Mostra a de maior gasto e sinaliza que há outras, pra ninguém ler o
        // número como se fosse o total.
        add("Grande", 50000, currency = "BRL")
        add("Pequena", 500, currency = "USD")

        val summary = summary()
        assertEquals("BRL", summary.primaryTotal?.currencyCode)
        assertTrue(summary.hasOtherCurrencies)
    }

    @Test
    fun asingleCurrencyIsNotFlaggedAsHavingOthers() = runTest {
        add("Netflix", 5490)
        assertTrue(!summary().hasOtherCurrencies)
    }

    @Test
    fun archivedSubscriptionsAreNeverTheNextRenewal() = runTest {
        val id = add("Cancelada", 9900, anchor = "2026-07-03")
        add("Ativa", 1000, anchor = "2026-07-20")
        repository.setArchived(id, archived = true)

        val summary = summary()
        assertEquals("Ativa", summary.next?.subscription?.name)
        assertEquals(1000, summary.primaryTotal?.monthlyCents)
    }
}
