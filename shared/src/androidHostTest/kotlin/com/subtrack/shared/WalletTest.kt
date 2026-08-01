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
class WalletTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: SubscriptionRepository

    private val today = LocalDate.parse("2026-07-30")

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
        currency: String = "BRL",
        cycle: BillingCycle = BillingCycle.MONTHLY,
    ) = repository.add(
        SubscriptionDraft(
            name = name,
            amountCents = amountCents,
            currency = currency,
            cycle = cycle,
            anchor = LocalDate.parse("2026-01-15"),
        ),
    )

    @Test
    fun emptyWalletReportsItself() = runTest {
        val state = repository.observeWallet(today).first()
        assertTrue(state.isEmpty)
        assertEquals(emptyList(), state.totals)
    }

    @Test
    fun singleCurrencyProducesOneTotalLine() = runTest {
        add("Netflix", 5490)
        add("Anual", 12000, cycle = BillingCycle.YEARLY)

        val totals = repository.observeWallet(today).first().totals
        assertEquals(
            listOf(WalletTotals(currencyCode = "BRL", monthlyCents = 6490, yearlyCents = 77880)),
            totals,
        )
    }

    @Test
    fun currenciesAreTotalledSeparatelyInsteadOfBeingAddedTogether() = runTest {
        // O ponto: 5490 BRL + 1200 USD não pode virar 6690 de nada.
        // Converter exigiria cotação, ou seja, rede — proibido pelo SPEC.
        add("Streaming BR", 5490, currency = "BRL")
        add("SaaS US", 1200, currency = "USD")

        val totals = repository.observeWallet(today).first().totals
        assertEquals(
            listOf(
                WalletTotals(currencyCode = "BRL", monthlyCents = 5490, yearlyCents = 65880),
                WalletTotals(currencyCode = "USD", monthlyCents = 1200, yearlyCents = 14400),
            ),
            totals,
        )
    }

    @Test
    fun totalLinesAreOrderedByCurrencyCodeSoTheListDoesNotJumpAround() = runTest {
        add("C", 100, currency = "USD")
        add("A", 100, currency = "BRL")
        add("B", 100, currency = "EUR")

        val codes = repository.observeWallet(today).first().totals.map { it.currencyCode }
        assertEquals(listOf("BRL", "EUR", "USD"), codes)
    }

    @Test
    fun archivedSubscriptionsDoNotCountTowardsTotals() = runTest {
        val id = add("Cancelada", 9900)
        add("Ativa", 1000)
        repository.setArchived(id, archived = true)

        val state = repository.observeWallet(today).first()
        assertEquals(1, state.upcoming.size)
        assertEquals(
            listOf(WalletTotals(currencyCode = "BRL", monthlyCents = 1000, yearlyCents = 12000)),
            state.totals,
        )
    }

    @Test
    fun walletKeepsTheRenewalOrderFromTheRepository() = runTest {
        repository.add(
            SubscriptionDraft(name = "Depois", amountCents = 100, currency = "BRL", cycle = BillingCycle.MONTHLY, anchor = LocalDate.parse("2026-01-25")),
        )
        repository.add(
            SubscriptionDraft(name = "Antes", amountCents = 100, currency = "BRL", cycle = BillingCycle.MONTHLY, anchor = LocalDate.parse("2026-01-02")),
        )

        val names = repository.observeWallet(today).first().upcoming.map { it.subscription.name }
        assertEquals(listOf("Antes", "Depois"), names)
    }
}
