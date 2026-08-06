package com.subtrack.android

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.subtrack.shared.WidgetSummary
import com.subtrack.shared.formatMoney
import com.subtrack.shared.observeWallet
import com.subtrack.shared.toWidgetSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SubTrackWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        refresh(context)
    }

    companion object {
        // Escopo próprio porque AppWidgetProvider é um BroadcastReceiver: ele
        // morre assim que onUpdate retorna, então não há ciclo de vida ao qual
        // atrelar a leitura do banco.
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * Redesenha todas as instâncias do widget. Chamado por quem sabe que
         * algo mudou — o worker diário e as edições no app — em vez de um
         * `updatePeriodMillis`, que acordaria o aparelho a cada 30min pra
         * atualizar um texto que muda uma vez por dia.
         */
        fun refresh(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(ComponentName(appContext, SubTrackWidget::class.java))
            if (ids.isEmpty()) return

            scope.launch {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val app = appContext as SubTrackApp
                val repository = app.repository
                val premiumRepository = app.premiumRepository
                val isPremium = premiumRepository.isPremium()
                val summary = repository.observeWallet(today).first().toWidgetSummary()
                val views = render(appContext, summary, today, isPremium)
                ids.forEach { manager.updateAppWidget(it, views) }
            }
        }

        private fun render(
            context: Context,
            summary: WidgetSummary,
            today: LocalDate,
            isPremium: Boolean,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_subtrack)

            if (!isPremium) {
                views.setTextViewText(R.id.widget_total, "SubTrack Pro")
                views.setTextViewText(R.id.widget_total_label, "Desbloqueie o widget no app")
                views.setTextViewText(R.id.widget_next, "")
            } else if (summary.isEmpty) {
                views.setTextViewText(R.id.widget_total, "—")
                views.setTextViewText(R.id.widget_total_label, "Nenhuma assinatura")
                views.setTextViewText(R.id.widget_next, "")
            } else {
                val total = summary.primaryTotal!!
                views.setTextViewText(
                    R.id.widget_total,
                    formatMoney(total.monthlyCents, total.currencyCode),
                )
                views.setTextViewText(
                    R.id.widget_total_label,
                    // Sem este "+ outras moedas" o número seria lido como o
                    // gasto total, que ele não é.
                    if (summary.hasOtherCurrencies) "por mês · + outras moedas" else "por mês",
                )
                views.setTextViewText(R.id.widget_next, summary.next?.let { nextLabel(it.subscription.name, it.nextRenewal, today) } ?: "")
            }

            views.setOnClickPendingIntent(
                R.id.widget_total,
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            return views
        }

        private fun nextLabel(
            name: String,
            renewal: LocalDate,
            today: LocalDate,
        ): String = when (val days = today.daysUntil(renewal)) {
            0 -> "$name renova hoje"
            1 -> "$name renova amanhã"
            else -> "$name renova em $days dias"
        }
    }
}
