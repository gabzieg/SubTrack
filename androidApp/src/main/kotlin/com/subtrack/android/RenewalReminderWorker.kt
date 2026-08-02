package com.subtrack.android

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.subtrack.shared.dueOn
import com.subtrack.shared.remindersOnOrAfter
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Verificação diária: pergunta quais avisos vencem hoje e dispara cada um.
 *
 * Um worker diário em vez de um alarme exato por assinatura porque a precisão
 * necessária é de dia, não de minuto — "3 dias antes" tolera atraso de horas.
 * Em troca, o WorkManager já resolve sozinho o que seria trabalhoso: sobrevive
 * a reboot, reagenda depois do Doze, e um dia perdido com o aparelho desligado
 * é recuperado na próxima execução (o aviso atrasado é puxado pra frente em vez
 * de sumir).
 */
@OptIn(ExperimentalTime::class)
class RenewalReminderWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val repository = (applicationContext as SubTrackApp).repository
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        applicationContext.ensureRenewalChannel()

        val due = repository.observeActive(today).first()
            .remindersOnOrAfter(today)
            .dueOn(today)

        due.forEach { reminder ->
            // Só marca como avisado se a notificação saiu mesmo. Marcar sem
            // entregar faria o aviso desaparecer para sempre.
            if (applicationContext.notifyRenewal(reminder)) {
                repository.markNotified(reminder.subscriptionId, reminder.renewalDate)
            }
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "renewal-reminders"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RenewalReminderWorker>(1, TimeUnit.DAYS).build()

            // KEEP: reagendar a cada abertura do app reiniciaria o intervalo e,
            // num app aberto todo dia, o worker poderia nunca chegar a rodar.
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
