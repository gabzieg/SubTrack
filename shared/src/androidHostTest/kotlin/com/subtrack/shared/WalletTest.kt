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
    fun archivedSubscriptionsCanBeListedWithoutCountingTowardsTotals() = runTest {
        // Sem poder listar as arquivadas, arquivar viraria porta de mão única —
        // o botão de desarquivar existiria mas seria inalcançável. Mesmo
        // visíveis, elas não entram no total: não são mais pagas.
        val id = add("Cancelada", 9900)
        add("Ativa", 1000)
        repository.setArchived(id, archived = true)

        val state = repository.observeWallet(today, includeArchived = true).first()
        assertEquals(listOf("Ativa", "Cancelada"), state.upcoming.map { it.subscription.name }.sorted())
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

    // --- categorização ---

    @Test
    fun groupedByCategoryProducesOneGroupPerDistinctCategory() = runTest {
        addWithCategory("Spotify", 2190, category = "Streaming")
        addWithCategory("Netflix", 5490, category = "Streaming")
        addWithCategory("Gympass", 9900, category = "Saúde")

        val groups = repository.observeWallet(today).first().groupedByCategory
        assertEquals(listOf("Saúde", "Streaming"), groups.map { it.category })
        // Spotify e Netflix têm a mesma âncora → mesma nextRenewal → desempate
        // por nome alfabético: Netflix < Spotify.
        assertEquals(listOf("Netflix", "Spotify"), groups.first { it.category == "Streaming" }.items.map { it.subscription.name })
        assertEquals(listOf("Gympass"), groups.first { it.category == "Saúde" }.items.map { it.subscription.name })
    }

    @Test
    fun subscriptionsWithNullCategoryAreGroupedLastRegardlessOfAlphabeticOrder() = runTest {
        // "Sem categoria" (null) deve ser sempre o último grupo, mesmo que
        // venha antes alfabeticamente de qualquer categoria com nome.
        addWithCategory("SemCat", 1000, category = null)
        addWithCategory("Streaming item", 2000, category = "Streaming")
        addWithCategory("Saúde item", 3000, category = "Saúde")

        val categoryOrder = repository.observeWallet(today).first().groupedByCategory.map { it.category }
        assertEquals(listOf("Saúde", "Streaming", null), categoryOrder)
    }

    @Test
    fun itemsWithinGroupAreOrderedByNextRenewalThenByName() = runTest {
        // âncora dia 25 → renova dia 25-ago; âncora dia 10 → renova dia 10-ago
        repository.add(SubscriptionDraft(name = "ZZZ", amountCents = 100, currency = "BRL", cycle = BillingCycle.MONTHLY, anchor = LocalDate.parse("2026-01-25"), category = "X"))
        repository.add(SubscriptionDraft(name = "AAA", amountCents = 100, currency = "BRL", cycle = BillingCycle.MONTHLY, anchor = LocalDate.parse("2026-01-10"), category = "X"))

        val names = repository.observeWallet(today).first().groupedByCategory.single().items.map { it.subscription.name }
        // AAA renova antes (dia 10), ZZZ depois (dia 25)
        assertEquals(listOf("AAA", "ZZZ"), names)
    }

    @Test
    fun emptyWalletProducesEmptyGroupedByCategory() = runTest {
        val groups = repository.observeWallet(today).first().groupedByCategory
        assertEquals(emptyList(), groups)
    }

    @Test
    fun allSubscriptionsWithNullCategoryProducesSingleGroupWithNullKey() = runTest {
        addWithCategory("A", 1000, category = null)
        addWithCategory("B", 2000, category = null)

        val groups = repository.observeWallet(today).first().groupedByCategory
        assertEquals(1, groups.size)
        assertEquals(null, groups.single().category)
        assertEquals(listOf("A", "B"), groups.single().items.map { it.subscription.name })
    }

    // --- ordenação ---

    @Test
    fun alphabeticalSortOrdersItemsByName() = runTest {
        add("Zebra", 100)
        add("Apple", 200)
        add("Mango", 150)

        val names = repository.observeWallet(today, sortOrder = SortOrder.ALPHABETICAL).first().upcoming.map { it.subscription.name }
        assertEquals(listOf("Apple", "Mango", "Zebra"), names)
    }

    @Test
    fun valueDescSortOrdersItemsByAmountHighestFirst() = runTest {
        add("Barato", 500)
        add("Caro", 9900)
        add("Médio", 2990)

        val names = repository.observeWallet(today, sortOrder = SortOrder.VALUE_DESC).first().upcoming.map { it.subscription.name }
        assertEquals(listOf("Caro", "Médio", "Barato"), names)
    }

    @Test
    fun dateAddedSortOrdersItemsByIdDescendingMostRecentFirst() = runTest {
        // id autoincrement: Primeiro < Segundo < Terceiro → DESC = Terceiro, Segundo, Primeiro
        add("Primeiro", 100)
        add("Segundo", 100)
        add("Terceiro", 100)

        val names = repository.observeWallet(today, sortOrder = SortOrder.DATE_ADDED).first().upcoming.map { it.subscription.name }
        assertEquals(listOf("Terceiro", "Segundo", "Primeiro"), names)
    }

    @Test
    fun defaultSortOrderIsRenewal() = runTest {
        repository.add(SubscriptionDraft(name = "Longe", amountCents = 100, currency = "BRL", cycle = BillingCycle.MONTHLY, anchor = LocalDate.parse("2026-01-25")))
        repository.add(SubscriptionDraft(name = "Perto", amountCents = 100, currency = "BRL", cycle = BillingCycle.MONTHLY, anchor = LocalDate.parse("2026-01-05")))

        val names = repository.observeWallet(today).first().upcoming.map { it.subscription.name }
        assertEquals(listOf("Perto", "Longe"), names)
    }

    @Test
    fun sortOrderIsAppliedWithinCategoryGroupsWhileGroupsRemainAlphabetical() = runTest {
        addWithCategory("Zebra", 100, category = "Streaming")
        addWithCategory("Apple", 200, category = "Streaming")
        addWithCategory("Única", 500, category = "Saúde")

        val state = repository.observeWallet(today, sortOrder = SortOrder.ALPHABETICAL).first()
        val groupCategories = state.groupedByCategory.map { it.category }
        assertEquals(listOf("Saúde", "Streaming"), groupCategories)
        val streamingNames = state.groupedByCategory.first { it.category == "Streaming" }.items.map { it.subscription.name }
        assertEquals(listOf("Apple", "Zebra"), streamingNames)
    }

    private suspend fun addWithCategory(
        name: String,
        amountCents: Long,
        category: String?,
    ) = repository.add(
        SubscriptionDraft(
            name = name,
            amountCents = amountCents,
            currency = "BRL",
            cycle = BillingCycle.MONTHLY,
            anchor = LocalDate.parse("2026-01-15"),
            category = category,
        ),
    )
}
