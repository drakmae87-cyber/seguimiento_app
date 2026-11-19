package com.example.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class FxCategory(val id: String, val label: String) {
    PARTICLES("particles", "Partículas"),
    ATMOSPHERE("atmosphere", "Atmósfera"),
    THREE_D("three_d", "3D"),
    NEON_EFX("neon_efx", "Neón EFX");

    companion object {
        fun fromId(id: String?): FxCategory =
            entries.firstOrNull { it.id == id } ?: PARTICLES
    }
}

data class CustomFxLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: FxCategory,
    val colorHex: String = "#00E5FF",
    val speed: Float = 1.0f,
    val density: Float = 0.8f,
    val enabled: Boolean = true
)

object CustomFxCodec {
    fun encode(layers: List<CustomFxLayer>): String {
        val arr = JSONArray()
        layers.forEach { layer ->
            arr.put(
                JSONObject()
                    .put("id", layer.id)
                    .put("name", layer.name)
                    .put("category", layer.category.id)
                    .put("colorHex", layer.colorHex)
                    .put("speed", layer.speed.toDouble())
                    .put("density", layer.density.toDouble())
                    .put("enabled", layer.enabled)
            )
        }
        return arr.toString()
    }

    fun decode(raw: String?): List<CustomFxLayer> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        CustomFxLayer(
                            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                            name = obj.optString("name", "Capa FX"),
                            category = FxCategory.fromId(obj.optString("category")),
                            colorHex = obj.optString("colorHex", "#00E5FF"),
                            speed = obj.optDouble("speed", 1.0).toFloat().coerceIn(0.3f, 2.5f),
                            density = obj.optDouble("density", 0.8).toFloat().coerceIn(0.2f, 1.5f),
                            enabled = obj.optBoolean("enabled", true)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
