package com.example.model

import java.util.Locale
import java.util.UUID

enum class TelemetryPushCategory(val tag: String) {
    CLOCK("RELOJ"),
    ALARM("ALARMA"),
    TIMER("TIMER"),
    FX("MOTOR FX"),
    MEDIA("MULTIMEDIA"),
    MUSIC("MÚSICA"),
    DOWNLOAD("DESCARGA"),
    CALENDAR("AGENDA"),
    AI("IA"),
    SYSTEM("SISTEMA")
}

data class TelemetryPushMessage(
    val id: String = UUID.randomUUID().toString(),
    val category: TelemetryPushCategory,
    val title: String,
    val message: String,
    val countdownHhMmSs: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)

object TelemetryTimeFormatter {

    /**
     * Computes exact remaining time formatted strictly as hh:mm:ss.
     * Example: 00:00 -> 02:00 yields "02:00:00".
     */
    fun formatRemainingHhMmSs(targetAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diffMillis = (targetAtMillis - nowMillis).coerceAtLeast(0L)
        val totalSeconds = diffMillis / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun buildAlarmActivationLog(
        alarm: AlarmItem,
        triggerAtMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): TelemetryPushMessage {
        val remaining = formatRemainingHhMmSs(triggerAtMillis, nowMillis)
        val label = alarm.label.ifBlank { "Alarma ${alarm.formattedTime()}" }
        return TelemetryPushMessage(
            category = TelemetryPushCategory.ALARM,
            title = "Alarma activada · ${alarm.formattedTime()}",
            message = "$label · Te quedan $remaining para descansar",
            countdownHhMmSs = remaining,
            timestampMillis = nowMillis
        )
    }
}
