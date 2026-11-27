package com.mosquitobr.controlepoco.data

import android.content.Context
import com.mosquitobr.controlepoco.core.UnitSystem

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

object SettingsStore {
    private const val FILE = "controle_poco_settings"
    private const val KEY_UNITS = "units"
    private const val KEY_LANG = "language"
    private const val KEY_SOUND = "sound"
    private const val KEY_HAPTICS = "haptics"
    private const val KEY_VOLUME = "volume"
    private const val KEY_SOUND_PUMP = "sound_pump"
    private const val KEY_SOUND_ROTARY = "sound_rotary"
    private const val KEY_SOUND_WEIGHT = "sound_weight"
    private const val KEY_SOUND_GAS = "sound_gas"

    fun load(context: Context): AppSettings {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        // "FIELD" era o modo antigo (ft/gpm); preserva como US_FIELD.
        val stored = prefs.getString(KEY_UNITS, "PETROBRAS") ?: "PETROBRAS"
        val units = when (stored) {
            "SI" -> UnitSystem.SI
            "FIELD", "US_FIELD" -> UnitSystem.US_FIELD
            else -> UnitSystem.PETROBRAS
        }
        return AppSettings(
            units = units,
            language = prefs.getString(KEY_LANG, "pt") ?: "pt",
            sound = prefs.getBoolean(KEY_SOUND, true),
            haptics = prefs.getBoolean(KEY_HAPTICS, true),
            volume = prefs.getFloat(KEY_VOLUME, 0.8f),
            soundPump = prefs.getBoolean(KEY_SOUND_PUMP, true),
            soundRotary = prefs.getBoolean(KEY_SOUND_ROTARY, true),
            soundWeight = prefs.getBoolean(KEY_SOUND_WEIGHT, true),
            soundGas = prefs.getBoolean(KEY_SOUND_GAS, true)
        )
    }

    fun save(context: Context, settings: AppSettings) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY_UNITS, settings.units.name)
            .putString(KEY_LANG, settings.language)
            .putBoolean(KEY_SOUND, settings.sound)
            .putBoolean(KEY_HAPTICS, settings.haptics)
            .putFloat(KEY_VOLUME, settings.volume)
            .putBoolean(KEY_SOUND_PUMP, settings.soundPump)
            .putBoolean(KEY_SOUND_ROTARY, settings.soundRotary)
            .putBoolean(KEY_SOUND_WEIGHT, settings.soundWeight)
            .putBoolean(KEY_SOUND_GAS, settings.soundGas)
            .apply()
    }
}
