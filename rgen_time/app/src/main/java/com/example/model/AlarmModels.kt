package com.example.model

import java.util.Calendar
import java.util.Locale

enum class AlarmRepeatUnit(
    val id: String,
    val labelSingular: String,
    val labelPlural: String
) {
    NONE("none", "Sin repetición por intervalo", "Sin repetición por intervalo"),
    MINUTES("minutes", "minuto", "minutos"),
    HOURS("hours", "hora", "horas"),
    DAYS("days", "día", "días"),
    WEEKS("weeks", "semana", "semanas");

    fun formatInterval(count: Int): String = when (this) {
        NONE -> "Solo por días o única"
        else -> if (count <= 1) "Cada 1 $labelSingular" else "Cada $count $labelPlural"
    }

    fun toMillis(count: Int): Long {
        val safe = count.coerceAtLeast(1).toLong()
        return when (this) {
            NONE -> 0L
            MINUTES -> safe * 60_000L
            HOURS -> safe * 3_600_000L
            DAYS -> safe * 86_400_000L
            WEEKS -> safe * 7L * 86_400_000L
        }
    }

    companion object {
        fun fromId(id: String?): AlarmRepeatUnit =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}

data class AlarmDayOption(
    val calendarDay: Int,
    val shortLabel: String,
    val fullLabel: String
)

object AlarmDays {
    val ordered = listOf(
        AlarmDayOption(Calendar.MONDAY, "L", "Lunes"),
        AlarmDayOption(Calendar.TUESDAY, "M", "Martes"),
        AlarmDayOption(Calendar.WEDNESDAY, "X", "Miércoles"),
        AlarmDayOption(Calendar.THURSDAY, "J", "Jueves"),
        AlarmDayOption(Calendar.FRIDAY, "V", "Viernes"),
        AlarmDayOption(Calendar.SATURDAY, "S", "Sábado"),
        AlarmDayOption(Calendar.SUNDAY, "D", "Domingo")
    )

    fun formatSelectedDays(days: Set<Int>): String {
        if (days.isEmpty()) return "Una vez"
        if (days.size == 7) return "Todos los días"
        val weekdays = setOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY
        )
        if (days == weekdays) return "Lun–Vie"
        val weekend = setOf(Calendar.SATURDAY, Calendar.SUNDAY)
        if (days == weekend) return "Fin de semana"
        return ordered.filter { it.calendarDay in days }.joinToString(" · ") { it.shortLabel }
    }
}

data class AlarmItem(
    val id: String,
    val hour: Int,
    val minute: Int,
    val label: String,
    val description: String = "",
    val enabled: Boolean = true,
    val repeatDays: Set<Int> = emptySet(),
    val repeatIntervalCount: Int = 1,
    val repeatIntervalUnit: AlarmRepeatUnit = AlarmRepeatUnit.NONE,
    val audioPath: String? = null,
    val audioDisplayName: String = "Tono del sistema predeterminado",
    val vibrate: Boolean = true,
    val nextTriggerAtMillis: Long = 0L
) {
    fun formattedTime(): String = String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))

    fun scheduleSummary(): String {
        val daysPart = AlarmDays.formatSelectedDays(repeatDays)
        return if (repeatIntervalUnit != AlarmRepeatUnit.NONE) {
            "$daysPart · ${repeatIntervalUnit.formatInterval(repeatIntervalCount)}"
        } else {
            daysPart
        }
    }
}
