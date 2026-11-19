package com.example.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.TelemetryPushMessage

object TelemetryNotificationHelper {

    private const val CHANNEL_ID = "rgen_telemetry_push_log"
    private const val CHANNEL_NAME = "Telemetría & Alarmas RGEN_time"
    private const val ALARM_LOG_NOTIF_ID = 4401
    private const val GENERAL_LOG_NOTIF_ID = 4402

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Registro push de eventos activos y tiempo restante de alarmas"
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun postPushLogNotification(context: Context, log: TelemetryPushMessage, isAlarmCountdown: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val launchIntent = PendingIntent.getActivity(
            context,
            log.category.ordinal + 100,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("[${log.category.tag}] ${log.title}")
            .setContentText(log.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(log.message))
            .setContentIntent(launchIntent)
            .setAutoCancel(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val notifId = if (isAlarmCountdown) ALARM_LOG_NOTIF_ID else GENERAL_LOG_NOTIF_ID
        runCatching { nm.notify(notifId, builder.build()) }
    }
}
