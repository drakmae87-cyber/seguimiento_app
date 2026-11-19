package com.example.data

import android.content.Context
import com.example.alarm.AlarmScheduler
import com.example.model.AlarmItem
import com.example.model.AlarmRepeatUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AlarmRepository(private val context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _alarms = MutableStateFlow(loadAlarms())
    val alarmsFlow: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    fun findAlarmById(id: String): AlarmItem? = _alarms.value.firstOrNull { it.id == id }

    fun saveAlarm(alarm: AlarmItem) {
        val current = _alarms.value.toMutableList()
        val finalId = alarm.id.ifBlank { UUID.randomUUID().toString() }
        val nextTrigger = if (alarm.enabled) {
            AlarmScheduler.computeNextTriggerAtMillis(alarm)
        } else {
            0L
        }
        val normalized = alarm.copy(id = finalId, nextTriggerAtMillis = nextTrigger)

        val idx = current.indexOfFirst { it.id == finalId }
        if (idx >= 0) {
            val prev = current[idx]
            if (prev.audioPath != null && prev.audioPath != normalized.audioPath) {
                CustomMediaHelper.deleteAudioFile(prev.audioPath)
            }
            current[idx] = normalized
        } else {
            current.add(normalized)
        }

        val sorted = current.sortedWith(compareBy<AlarmItem> { it.hour }.thenBy { it.minute })
        persist(sorted)

        if (normalized.enabled) {
            AlarmScheduler.scheduleAlarm(context, normalized, nextTrigger)
        } else {
            AlarmScheduler.cancelAlarm(context, normalized.id)
        }
    }

    fun toggleAlarmEnabled(alarmId: String, enabled: Boolean) {
        val current = _alarms.value.toMutableList()
        val idx = current.indexOfFirst { it.id == alarmId }
        if (idx < 0) return
        val existing = current[idx]
        val nextTrigger = if (enabled) AlarmScheduler.computeNextTriggerAtMillis(existing) else 0L
        val updated = existing.copy(enabled = enabled, nextTriggerAtMillis = nextTrigger)
        current[idx] = updated
        persist(current)

        if (enabled) {
            AlarmScheduler.scheduleAlarm(context, updated, nextTrigger)
        } else {
            AlarmScheduler.cancelAlarm(context, alarmId)
        }
    }

    fun updateAlarmTriggerOnly(alarmId: String, nextTriggerAtMillis: Long, enabled: Boolean) {
        val current = _alarms.value.toMutableList()
        val idx = current.indexOfFirst { it.id == alarmId }
        if (idx < 0) return
        current[idx] = current[idx].copy(enabled = enabled, nextTriggerAtMillis = nextTriggerAtMillis)
        persist(current)
    }

    fun deleteAlarm(alarmId: String) {
        val current = _alarms.value.toMutableList()
        val target = current.firstOrNull { it.id == alarmId }
        if (target != null) {
            CustomMediaHelper.deleteAudioFile(target.audioPath)
            current.removeAll { it.id == alarmId }
            persist(current)
        }
        AlarmScheduler.cancelAlarm(context, alarmId)
    }

    fun refreshSchedules() {
        _alarms.value.filter { it.enabled }.forEach { alarm ->
            val next = AlarmScheduler.computeNextTriggerAtMillis(alarm)
            AlarmScheduler.scheduleAlarm(context, alarm, next)
        }
    }

    private fun persist(list: List<AlarmItem>) {
        val arr = JSONArray()
        list.forEach { item ->
            val daysArr = JSONArray().apply {
                item.repeatDays.sorted().forEach { put(it) }
            }
            val obj = JSONObject()
                .put("id", item.id)
                .put("hour", item.hour)
                .put("minute", item.minute)
                .put("label", item.label)
                .put("description", item.description)
                .put("enabled", item.enabled)
                .put("repeatDays", daysArr)
                .put("repeatIntervalCount", item.repeatIntervalCount)
                .put("repeatIntervalUnit", item.repeatIntervalUnit.id)
                .put("audioPath", item.audioPath ?: "")
                .put("audioDisplayName", item.audioDisplayName)
                .put("vibrate", item.vibrate)
                .put("nextTriggerAtMillis", item.nextTriggerAtMillis)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_ALARMS_JSON, arr.toString()).apply()
        _alarms.value = list
    }

    private fun loadAlarms(): List<AlarmItem> {
        val raw = prefs.getString(KEY_ALARMS_JSON, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val daysArr = obj.optJSONArray("repeatDays") ?: JSONArray()
                    val days = buildSet {
                        for (d in 0 until daysArr.length()) {
                            add(daysArr.getInt(d))
                        }
                    }
                    add(
                        AlarmItem(
                            id = obj.getString("id"),
                            hour = obj.getInt("hour"),
                            minute = obj.getInt("minute"),
                            label = obj.optString("label", "Alarma"),
                            description = obj.optString("description", ""),
                            enabled = obj.optBoolean("enabled", true),
                            repeatDays = days,
                            repeatIntervalCount = obj.optInt("repeatIntervalCount", 1).coerceAtLeast(1),
                            repeatIntervalUnit = AlarmRepeatUnit.fromId(obj.optString("repeatIntervalUnit")),
                            audioPath = obj.optString("audioPath").takeIf { it.isNotBlank() },
                            audioDisplayName = obj.optString("audioDisplayName", "Tono del sistema predeterminado"),
                            vibrate = obj.optBoolean("vibrate", true),
                            nextTriggerAtMillis = obj.optLong("nextTriggerAtMillis", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFS_NAME = "rgen_time_alarms"
        const val KEY_ALARMS_JSON = "alarms_json_v1"
    }
}
