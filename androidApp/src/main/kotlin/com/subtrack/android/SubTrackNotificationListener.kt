package com.subtrack.android

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.subtrack.shared.formatMoney
import com.subtrack.shared.parseMoneyToCents

class SubTrackNotificationListener : NotificationListenerService() {

    private val keywords = listOf(
        "Netflix", "Spotify", "Amazon", "Prime", "Disney", "HBO",
        "YouTube", "SmartFit", "Apple", "Google", "Max", "GloboPlay", "Crunchyroll",
    )

    // Regex flexível para capturar valores em R$ ou $
    private val moneyRegex = Regex("""(?:R\$|U\$|USD|BRL)?\s*(\d{1,4}[.,]\d{2})""")

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getString("android.title") ?: ""
        val text = extras.getString("android.text") ?: ""
        val tickerText = sbn.notification?.tickerText?.toString() ?: ""

        val fullText = "$title $text $tickerText"

        // Procurar por palavras-chave
        val matchedKeyword = keywords.find { fullText.contains(it, ignoreCase = true) }

        if (matchedKeyword != null) {
            // Procurar valor monetário
            val matchResult = moneyRegex.find(fullText)
            if (matchResult != null) {
                val amountString = matchResult.groupValues[1]
                val amountCents = parseMoneyToCents(amountString)
                if (amountCents != null && amountCents > 0) {
                    notifySmartDetection(matchedKeyword, amountCents)
                }
            }
        }
    }

    private fun notifySmartDetection(name: String, amountCents: Long) {
        if (!canPostNotifications()) return

        ensureRenewalChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("smart_name", name)
            putExtra("smart_amount", amountCents)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            name.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val amountFormatted = formatMoney(amountCents, "BRL")

        val notification = NotificationCompat.Builder(this, "renewal_reminders")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Assinatura detectada: $name")
            .setContentText("Nova cobrança de $amountFormatted. Toque para salvar no SubTrack.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            NotificationManagerCompat.from(this).notify(name.hashCode(), notification)
        } catch (e: SecurityException) {
            Log.e("SubTrack", "Erro ao notificar smart tracker", e)
        }
    }
}
