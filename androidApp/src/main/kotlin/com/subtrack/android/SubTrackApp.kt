package com.subtrack.android

import android.app.Application
import com.subtrack.shared.SubscriptionRepository
import com.subtrack.shared.db.DatabaseDriverFactory
import com.subtrack.shared.db.SubTrackDatabase
import kotlinx.coroutines.Dispatchers

/**
 * Injeção de dependência na mão. O grafo é um banco e um repositório — trazer
 * uma biblioteca de DI pra isso custaria mais manutenção do que resolve.
 */
class SubTrackApp : Application() {
    private val database: SubTrackDatabase by lazy {
        val driver = DatabaseDriverFactory(applicationContext).create()
        SubTrackDatabase(driver)
    }

    val repository: SubscriptionRepository by lazy {
        SubscriptionRepository(database, Dispatchers.IO)
    }

    val premiumRepository: com.subtrack.shared.PremiumRepository by lazy {
        com.subtrack.shared.PremiumRepository(database, Dispatchers.IO)
    }

    override fun onCreate() {
        super.onCreate()
        ensureRenewalChannel()
        RenewalReminderWorker.schedule(this)
    }
}
