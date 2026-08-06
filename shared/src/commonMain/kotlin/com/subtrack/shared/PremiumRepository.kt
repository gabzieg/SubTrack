package com.subtrack.shared

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.subtrack.shared.db.SubTrackDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PremiumRepository(
    private val database: SubTrackDatabase,
    private val dispatcher: CoroutineDispatcher,
) {
    private val queries get() = database.settingsQueries

    private val premiumKey = "is_premium"

    /** Observa o status Premium reativamente. */
    fun observeIsPremium(): Flow<Boolean> = queries.selectByKey(premiumKey)
        .asFlow()
        .mapToOneOrNull(dispatcher)
        .map { it == "true" }

    /** Retorna o status Premium síncrono. */
    suspend fun isPremium(): Boolean = withContext(dispatcher) {
        queries.selectByKey(premiumKey).executeAsOneOrNull() == "true"
    }

    /** Simula a compra do Premium. */
    suspend fun purchasePremium(): Unit = withContext(dispatcher) {
        queries.upsert(key = premiumKey, value_ = "true")
    }
}
