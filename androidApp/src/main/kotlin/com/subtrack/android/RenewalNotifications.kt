package com.subtrack.android

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.subtrack.shared.RenewalReminder
import com.subtrack.shared.formatMoney

private const val CHANNEL_ID = "renewal_reminders"

fun Context.ensureRenewalChannel() {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "Lembretes de renovação",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "Avisa antes de cada assinatura ser cobrada"
    }
    NotificationManagerCompat.from(this).createNotificationChannel(channel)
}

/**
 * @return true se a notificação foi de fato entregue. Falso quando o usuário
 *   negou a permissão — nesse caso o aviso NÃO pode ser marcado como enviado,
 *   senão ele some para sempre sem nunca ter aparecido.
 */
fun Context.notifyRenewal(reminder: RenewalReminder): Boolean {
    if (!canPostNotifications()) return false

    val amount = formatMoney(reminder.amountCents, reminder.currency)
    val text = when (val days = reminder.daysBeforeRenewal) {
        0 -> "$amount é cobrado hoje"
        1 -> "$amount é cobrado amanhã"
        else -> "$amount é cobrado em $days dias"
    }

    val openApp = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle(reminder.subscriptionName)
        .setContentText(text)
        .setContentIntent(openApp)
        .setAutoCancel(true)
        .build()

    // A checagem é repetida aqui, e não só via canPostNotifications(), porque o
    // lint não a enxerga através da fronteira da função. O catch cobre a corrida
    // real: a permissão pode ser revogada entre checar e notificar.
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }

    return try {
        // O id é o da assinatura: um aviso por assinatura, e um novo substitui
        // o anterior em vez de empilhar.
        NotificationManagerCompat.from(this).notify(reminder.subscriptionId.toInt(), notification)
        true
    } catch (e: SecurityException) {
        false
    }
}

fun Context.canPostNotifications(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
    PackageManager.PERMISSION_GRANTED
