package com.subtrack.shared

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.subtrack.shared.db.SubTrackDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import com.subtrack.shared.db.Subscription as SubscriptionRow

class SubscriptionRepository(
    private val database: SubTrackDatabase,
    private val dispatcher: CoroutineDispatcher,
) {
    private val queries get() = database.subscriptionQueries

    /**
     * [today] entra como parâmetro em vez de ser lido de um relógio interno —
     * a próxima renovação é derivada, então quem observa precisa dizer a partir
     * de quando. Também é o que torna a ordenação testável sem mexer no relógio
     * do sistema.
     */
    fun observeActive(today: LocalDate): Flow<List<UpcomingSubscription>> = queries.selectActive()
        .asFlow()
        .mapToList(dispatcher)
        .map { rows -> rows.map { it.toDomain() }.withUpcomingRenewal(today) }

    fun observeAll(today: LocalDate): Flow<List<UpcomingSubscription>> = queries.selectAll()
        .asFlow()
        .mapToList(dispatcher)
        .map { rows -> rows.map { it.toDomain() }.withUpcomingRenewal(today) }

    suspend fun add(draft: SubscriptionDraft): Long = withContext(dispatcher) {
        queries.transactionWithResult {
            queries.insert(
                name = draft.name,
                amountCents = draft.amountCents,
                currency = draft.currency,
                cycle = draft.cycle.name,
                cycleEveryN = draft.everyN.toLong(),
                anchorEpochDay = draft.anchor.toEpochDays(),
                category = draft.category,
                notifyDaysBefore = draft.notifyDaysBefore.toLong(),
                archived = 0L,
            )
            queries.lastInsertedId().executeAsOne()
        }
    }

    suspend fun update(subscription: Subscription): Unit = withContext(dispatcher) {
        queries.update(
            name = subscription.name,
            amountCents = subscription.amountCents,
            currency = subscription.currency,
            cycle = subscription.cycle.name,
            cycleEveryN = subscription.everyN.toLong(),
            anchorEpochDay = subscription.anchor.toEpochDays(),
            category = subscription.category,
            notifyDaysBefore = subscription.notifyDaysBefore.toLong(),
            archived = if (subscription.archived) 1L else 0L,
            id = subscription.id,
        )
    }

    suspend fun setArchived(
        id: Long,
        archived: Boolean,
    ): Unit = withContext(dispatcher) {
        queries.setArchived(archived = if (archived) 1L else 0L, id = id)
    }

    /**
     * Registra que o usuário já foi avisado desta renovação, pro worker diário
     * não repetir o mesmo aviso amanhã.
     */
    suspend fun markNotified(
        id: Long,
        renewal: LocalDate,
    ): Unit = withContext(dispatcher) {
        queries.markNotified(lastNotifiedRenewalEpochDay = renewal.toEpochDays(), id = id)
    }

    suspend fun delete(id: Long): Unit = withContext(dispatcher) {
        queries.deleteById(id)
    }

    suspend fun findById(id: Long): Subscription? = withContext(dispatcher) {
        queries.selectById(id).executeAsOneOrNull()?.toDomain()
    }
}

/**
 * A ordenação por próxima renovação acontece aqui, não no SQL: a próxima
 * renovação não é uma coluna, é derivada da âncora a cada leitura.
 */
private fun List<Subscription>.withUpcomingRenewal(today: LocalDate): List<UpcomingSubscription> = map { UpcomingSubscription(subscription = it, nextRenewal = it.nextRenewalOnOrAfter(today)) }
    .sortedWith(compareBy({ it.nextRenewal }, { it.subscription.name }))

private fun SubscriptionRow.toDomain(): Subscription = Subscription(
    id = id,
    name = name,
    amountCents = amountCents,
    currency = currency,
    cycle = BillingCycle.fromStorage(cycle),
    everyN = cycleEveryN.toInt(),
    anchor = LocalDate.fromEpochDays(anchorEpochDay),
    category = category,
    notifyDaysBefore = notifyDaysBefore.toInt(),
    archived = archived != 0L,
    lastNotifiedRenewal = lastNotifiedRenewalEpochDay?.let { LocalDate.fromEpochDays(it) },
)
