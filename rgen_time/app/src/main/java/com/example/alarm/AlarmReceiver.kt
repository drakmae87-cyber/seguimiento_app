package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AlarmRepository

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action.orEmpty()
        val alarmId = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID)

        when (action) {
            ACTION_STOP_ALARM -> {
                AlarmPlaybackManager.stopRinging(context)
                dismissNotification(context)
                return
            }

            ACTION_SNOOZE_ALARM -> {
                val ringing = AlarmPlaybackManager.ringingAlarm.value
                AlarmPlaybackManager.stopRinging(context)
                dismissNotification(context)
                if (ringing != null) {
                    val snoozeAt = AlarmScheduler.computeNextSnoozeMillis(5)
                    AlarmScheduler.scheduleAlarm(context, ringing, snoozeAt)
                }
                return
            }

            AlarmScheduler.ACTION_ALARM_TRIGGER -> {
                if (alarmId.isNullOrBlank()) return
                val repo = AlarmRepository(context.applicationContext)
                val alarm = repo.findAlarmById(alarmId) ?: return

                AlarmPlaybackManager.startRinging(context, alarm)
                postAlarmNotification(context, alarm.label, alarm.description, alarm.formattedTime(), alarm.id)

                val nextRecurring = AlarmScheduler.computeRecurringNextAfterFire(alarm)
                if (nextRecurring != null) {
                    val updated = alarm.copy(nextTriggerAtMillis = nextRecurring)
                    repo.updateAlarmTriggerOnly(alarm.id, nextRecurring, enabled = true)
                    AlarmScheduler.scheduleAlarm(context, updated, nextRecurring)
                } else {
                    repo.updateAlarmTriggerOnly(alarm.id, 0L, enabled = false)
                }
            }
        }
    }

    private fun postAlarmNotification(
        context: Context,
        title: String,
        description: String,
        timeText: String,
        alarmId: String
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarmas RGEN_time",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = "Alertas de alarmas programadas en RGEN_time"
            }
            nm.createNotificationChannel(channel)
        }

        val openIntent = PendingIntent.getActivity(
            context,
            101,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getBroadcast(
            context,
            102,
            Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_STOP_ALARM
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            103,
            Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_SNOOZE_ALARM
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = if (description.isNotBlank()) "$timeText · $description" else "Alarma activa · $timeText"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title.ifBlank { "Alarma RGEN_time" })
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, "Posponer 5m", snoozeIntent)
            .addAction(0, "Desactivar", stopIntent)
            .build()

        runCatching { nm.notify(NOTIFICATION_ID, notification) }
    }

    companion object {
        const val CHANNEL_ID = "rgen_time_alarms_channel"
        const val NOTIFICATION_ID = 4091
        const val ACTION_STOP_ALARM = "com.example.RGEN_TIME_STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.RGEN_TIME_SNOOZE_ALARM"

        fun dismissNotification(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(NOTIFICATION_ID)
        }
    }
}
