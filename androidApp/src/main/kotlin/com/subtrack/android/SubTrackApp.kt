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
    val repository: SubscriptionRepository by lazy {
        val driver = DatabaseDriverFactory(applicationContext).create()
        SubscriptionRepository(SubTrackDatabase(driver), Dispatchers.IO)
    }

    override fun onCreate() {
        super.onCreate()
        ensureRenewalChannel()
        RenewalReminderWorker.schedule(this)
    }
}
