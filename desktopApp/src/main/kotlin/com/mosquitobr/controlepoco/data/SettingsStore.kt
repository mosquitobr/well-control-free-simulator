package com.mosquitobr.controlepoco.data

import com.mosquitobr.controlepoco.core.UnitSystem
import com.mosquitobr.controlepoco.platform.AppPaths
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File

data class AppSettings(
    val units: UnitSystem = UnitSystem.PETROBRAS,
    val language: String = "pt",
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val volume: Float = 0.8f,
    val soundPump: Boolean = true,
    val soundRotary: Boolean = true,
    val soundWeight: Boolean = true,
    val soundGas: Boolean = true
)

/**
 * Persistência local das preferências em JSON (Windows %APPDATA% / Linux XDG_CONFIG_HOME).
 */
object SettingsStore {
    private const val FILE_NAME = "settings.json"
    private val prettyJson = Json { prettyPrint = true }

    private fun settingsFile(): File = File(AppPaths.configDir(), FILE_NAME)

    private fun JsonObject.optString(key: String, default: String): String =
        this[key]?.jsonPrimitive?.contentOrNull ?: default

    private fun JsonObject.optBoolean(key: String, default: Boolean): Boolean =
        this[key]?.jsonPrimitive?.booleanOrNull ?: default

    private fun JsonObject.optDouble(key: String, default: Double): Double =
        this[key]?.jsonPrimitive?.doubleOrNull ?: default

    fun load(): AppSettings {
        val file = settingsFile()
        if (!file.exists()) return AppSettings()
        return runCatching {
            val json = prettyJson.parseToJsonElement(file.readText(Charsets.UTF_8)).jsonObject
            val stored = json.optString("units", "PETROBRAS")
            val units = when (stored) {
                "SI" -> UnitSystem.SI
                "FIELD", "US_FIELD" -> UnitSystem.US_FIELD
                else -> UnitSystem.PETROBRAS
            }
            AppSettings(
                units = units,
                language = json.optString("language", "pt"),
                sound = json.optBoolean("sound", true),
                haptics = json.optBoolean("haptics", true),
                volume = json.optDouble("volume", 0.8).toFloat().coerceIn(0f, 1f),
                soundPump = json.optBoolean("sound_pump", true),
                soundRotary = json.optBoolean("sound_rotary", true),
                soundWeight = json.optBoolean("sound_weight", true),
                soundGas = json.optBoolean("sound_gas", true)
            )
        }.getOrDefault(AppSettings())
    }

    fun save(settings: AppSettings) {
        val json = buildJsonObject {
            put("units", settings.units.name)
            put("language", settings.language)
            put("sound", settings.sound)
            put("haptics", settings.haptics)
            put("volume", settings.volume.toDouble())
            put("sound_pump", settings.soundPump)
            put("sound_rotary", settings.soundRotary)
            put("sound_weight", settings.soundWeight)
            put("sound_gas", settings.soundGas)
        }
        val file = settingsFile()
        com.mosquitobr.controlepoco.platform.AtomicFileWriter.writeTextAtomically(
            file,
            prettyJson.encodeToString(JsonObject.serializer(), json)
        )
    }
}
