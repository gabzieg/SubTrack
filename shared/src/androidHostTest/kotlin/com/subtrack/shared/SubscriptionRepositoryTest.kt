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
class SubscriptionRepositoryTest {
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

    private fun draft(
        name: String,
        amountCents: Long = 1000,
        cycle: BillingCycle = BillingCycle.MONTHLY,
        everyN: Int = 1,
        anchor: String = "2026-01-15",
    ) = SubscriptionDraft(
        name = name,
        amountCents = amountCents,
        currency = "BRL",
        cycle = cycle,
        everyN = everyN,
        anchor = LocalDate.parse(anchor),
    )

    @Test
    fun addReturnsTheGeneratedIdAndRoundTripsEveryField() = runTest {
        val id = repository.add(
            SubscriptionDraft(
                name = "Netflix",
                amountCents = 5490,
                currency = "BRL",
                cycle = BillingCycle.MONTHLY,
                everyN = 2,
                anchor = LocalDate.parse("2026-01-31"),
                category = "streaming",
                notifyDaysBefore = 5,
            ),
        )

        val stored = repository.findById(id)
        assertEquals(
            Subscription(
                id = id,
                name = "Netflix",
                amountCents = 5490,
                currency = "BRL",
                cycle = BillingCycle.MONTHLY,
                everyN = 2,
                anchor = LocalDate.parse("2026-01-31"),
                category = "streaming",
                notifyDaysBefore = 5,
                archived = false,
            ),
            stored,
        )
    }

    @Test
    fun consecutiveAddsGetDistinctIds() = runTest {
        val first = repository.add(draft("A"))
        val second = repository.add(draft("B"))
        assertTrue(second != first, "ids repetidos: $first e $second")
        assertEquals("B", repository.findById(second)?.name)
    }

    @Test
    fun activeListIsOrderedByComputedRenewalNotByInsertion() = runTest {
        // Inseridas fora de ordem de propósito. A ordenação não pode vir do
        // banco — a próxima renovação é derivada da âncora.
        repository.add(draft("Depois", anchor = "2026-01-20")) // renova 20/ago
        repository.add(draft("Antes", anchor = "2026-01-05")) // renova 05/ago
        repository.add(draft("Meio", anchor = "2026-01-10")) // renova 10/ago

        val names = repository.observeActive(today).first().map { it.subscription.name }
        assertEquals(listOf("Antes", "Meio", "Depois"), names)
    }

    @Test
    fun upcomingRenewalIsDerivedFromTheAnchor() = runTest {
        repository.add(draft("Anual", cycle = BillingCycle.YEARLY, anchor = "2024-02-29"))

        val upcoming = repository.observeActive(today).first().single()
        // Âncora em 29/fev: 2027 não é bissexto, então trunca pro dia 28.
        assertEquals(LocalDate.parse("2027-02-28"), upcoming.nextRenewal)
    }

    @Test
    fun archivedSubscriptionsLeaveTheActiveListButSurviveInStorage() = runTest {
        val id = repository.add(draft("Cancelada"))
        repository.setArchived(id, archived = true)

        assertTrue(repository.observeActive(today).first().isEmpty())
        assertEquals(1, repository.observeAll(today).first().size)
        assertEquals(true, repository.findById(id)?.archived)
    }

    @Test
    fun observeActiveEmitsAgainWhenDataChanges() = runTest {
        val flow = repository.observeActive(today)
        assertTrue(flow.first().isEmpty())

        repository.add(draft("Spotify"))
        assertEquals(listOf("Spotify"), flow.first().map { it.subscription.name })
    }

    @Test
    fun updateOverwritesEveryMutableField() = runTest {
        val id = repository.add(draft("Antigo", amountCents = 1000))
        val stored = repository.findById(id)!!

        repository.update(
            stored.copy(
                name = "Novo",
                amountCents = 2500,
                cycle = BillingCycle.YEARLY,
                everyN = 3,
                anchor = LocalDate.parse("2025-06-01"),
                category = "produtividade",
                notifyDaysBefore = 7,
            ),
        )

        val updated = repository.findById(id)!!
        assertEquals("Novo", updated.name)
        assertEquals(2500, updated.amountCents)
        assertEquals(BillingCycle.YEARLY, updated.cycle)
        assertEquals(3, updated.everyN)
        assertEquals(LocalDate.parse("2025-06-01"), updated.anchor)
        assertEquals("produtividade", updated.category)
        assertEquals(7, updated.notifyDaysBefore)
    }

    @Test
    fun deleteRemovesTheRow() = runTest {
        val id = repository.add(draft("Temporária"))
        repository.delete(id)
        assertNull(repository.findById(id))
    }

    @Test
    fun walletTotalsUseTheAnnualizedCostOfEachSubscription() = runTest {
        repository.add(draft("Mensal", amountCents = 5490, cycle = BillingCycle.MONTHLY))
        repository.add(draft("Anual", amountCents = 12000, cycle = BillingCycle.YEARLY))

        val costs = repository.observeActive(today).first().map { it.subscription.toRecurringCost() }
        assertEquals(77880, costs.totalYearlyCents())
        assertEquals(6490, costs.totalMonthlyCents())
    }

    @Test
    fun nullCategoryRoundTripsAsNull() = runTest {
        val id = repository.add(draft("Sem categoria"))
        assertNull(repository.findById(id)?.category)
    }
}
