package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.model.AlarmItem
import com.example.model.AlarmRepeatUnit
import java.util.Calendar

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"
    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val ACTION_ALARM_TRIGGER = "com.example.RGEN_TIME_ALARM_TRIGGER"

    fun computeNextTriggerAtMillis(alarm: AlarmItem, nowMillis: Long = System.currentTimeMillis()): Long {
        val baseCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, alarm.hour.coerceIn(0, 23))
            set(Calendar.MINUTE, alarm.minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If interval repetition is specified (e.g., every 15 minutes / 2 hours) and no specific weekdays constrain it,
        // or after base time has passed, calculate exact next occurrence.
        if (alarm.repeatDays.isEmpty()) {
            if (baseCal.timeInMillis <= nowMillis) {
                val intervalMs = alarm.repeatIntervalUnit.toMillis(alarm.repeatIntervalCount)
                if (intervalMs > 0L) {
                    while (baseCal.timeInMillis <= nowMillis) {
                        baseCal.timeInMillis += intervalMs
                    }
                } else {
                    baseCal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return baseCal.timeInMillis
        }

        // Find the soonest matching day in the next 8 days (or scaled by weeks if repeatIntervalUnit == WEEKS)
        for (dayOffset in 0..7) {
            val candidate = (baseCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val dow = candidate.get(Calendar.DAY_OF_WEEK)
            if (dow in alarm.repeatDays && candidate.timeInMillis > nowMillis) {
                return candidate.timeInMillis
            }
        }
        return baseCal.timeInMillis + 86_400_000L
    }

    fun computeNextSnoozeMillis(minutes: Int = 5, nowMillis: Long = System.currentTimeMillis()): Long {
        return nowMillis + minutes.coerceAtLeast(1) * 60_000L
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return am.canScheduleExactAlarms()
    }

    fun scheduleAlarm(context: Context, alarm: AlarmItem, overrideTriggerAtMillis: Long? = null): Long {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return 0L
        val triggerAt = overrideTriggerAtMillis ?: computeNextTriggerAtMillis(alarm)
        val pendingIntent = createTriggerPendingIntent(context, alarm.id)

        try {
            if (canScheduleExact(context)) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    alarm.id.hashCode(),
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                am.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAt, showIntent),
                    pendingIntent
                )
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (se: SecurityException) {
            Log.w(TAG, "Exact alarm permission missing, falling back to inexact alarm", se)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
        return triggerAt
    }

    fun cancelAlarm(context: Context, alarmId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = createTriggerPendingIntent(context, alarmId)
        am.cancel(pi)
        pi.cancel()
    }

    fun computeRecurringNextAfterFire(alarm: AlarmItem, firedAtMillis: Long = System.currentTimeMillis()): Long? {
        if (alarm.repeatIntervalUnit == AlarmRepeatUnit.MINUTES || alarm.repeatIntervalUnit == AlarmRepeatUnit.HOURS) {
            val stepMs = alarm.repeatIntervalUnit.toMillis(alarm.repeatIntervalCount)
            val candidate = firedAtMillis + stepMs
            if (alarm.repeatDays.isEmpty()) return candidate
            val cal = Calendar.getInstance().apply { timeInMillis = candidate }
            if (cal.get(Calendar.DAY_OF_WEEK) in alarm.repeatDays) return candidate
            return computeNextTriggerAtMillis(alarm, candidate)
        }
        if (alarm.repeatIntervalUnit == AlarmRepeatUnit.DAYS) {
            return firedAtMillis + alarm.repeatIntervalUnit.toMillis(alarm.repeatIntervalCount)
        }
        if (alarm.repeatIntervalUnit == AlarmRepeatUnit.WEEKS) {
            return if (alarm.repeatDays.isEmpty()) {
                firedAtMillis + alarm.repeatIntervalUnit.toMillis(alarm.repeatIntervalCount)
            } else {
                val nextWeekOffset = (alarm.repeatIntervalCount - 1).coerceAtLeast(0) * 7 * 86_400_000L
                computeNextTriggerAtMillis(alarm, firedAtMillis + 60_000L + nextWeekOffset)
            }
        }
        return if (alarm.repeatDays.isNotEmpty()) {
            computeNextTriggerAtMillis(alarm, firedAtMillis + 60_000L)
        } else {
            null
        }
    }

    private fun createTriggerPendingIntent(context: Context, alarmId: String): PendingIntent {
        val intent = Intent(context.applicationContext, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context.applicationContext,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
