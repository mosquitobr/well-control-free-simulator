package com.mosquitobr.controlepoco.ui

import com.mosquitobr.controlepoco.core.SimSnapshot
import com.mosquitobr.controlepoco.core.UnitSystem
import com.mosquitobr.controlepoco.core.Units

private fun isField(units: UnitSystem): Boolean = units != UnitSystem.SI

fun pressureText(kpa: Double, units: UnitSystem, decimals: Int = 0): String =
    if (isField(units)) {
        "${Units.fmt(Units.kpaToPsi(kpa), decimals)} psi"
    } else {
        "${Units.fmt(kpa, decimals)} kPa"
    }

fun pressureValue(kpa: Double, units: UnitSystem, decimals: Int = 0): String =
    if (isField(units)) Units.fmt(Units.kpaToPsi(kpa), decimals)
    else Units.fmt(kpa, decimals)

/** Pressão a partir de um valor já em psi (ex.: campos legados de resultados salvos). */
fun pressureTextFromPsi(psi: Double, units: UnitSystem, decimals: Int = 0): String =
    if (isField(units)) "${Units.fmt(psi, decimals)} psi"
    else "${Units.fmt(Units.psiToKpa(psi), decimals)} kPa"

/** Volume a partir de um valor já em barris (ex.: campos legados de resultados salvos). */
fun volumeTextFromBbl(bbl: Double, units: UnitSystem, decimals: Int = 1): String =
    if (isField(units)) "${Units.fmt(bbl, decimals)} bbl"
    else "${Units.fmt(Units.bblToM3(bbl), decimals)} m³"

fun depthText(m: Double, units: UnitSystem, decimals: Int = 0): String =
    if (units == UnitSystem.US_FIELD) "${Units.fmt(Units.mToFt(m), decimals)} ft"
    else "${Units.fmt(m, decimals)} m"

fun depthValue(m: Double, units: UnitSystem, decimals: Int = 1): String = when (units) {
    UnitSystem.US_FIELD -> Units.fmt(Units.mToFt(m), decimals)
    else -> Units.fmt(m, decimals)
}

fun volumeText(m3: Double, units: UnitSystem, decimals: Int = 1): String =
    if (isField(units)) "${Units.fmt(Units.m3ToBbl(m3), decimals)} bbl"
    else "${Units.fmt(m3, decimals)} m³"

fun volumeValue(m3: Double, units: UnitSystem, decimals: Int = 1): String =
    if (isField(units)) Units.fmt(Units.m3ToBbl(m3), decimals)
    else Units.fmt(m3, decimals)

/**
 * Vazão no modo de campo Petrobras: gpm e bbl/min na mesma leitura.
 * No SI permanece L/min.
 */
fun rateText(lpm: Double, units: UnitSystem, decimals: Int = 0): String =
    if (isField(units)) {
        val gpm = Units.lpmToGpm(lpm)
        val bblMin = Units.lpmToBblMin(lpm)
        "${Units.fmt(gpm, decimals)} gpm · ${Units.fmt(bblMin, 1)} bbl/min"
    } else {
        "${Units.fmt(lpm, decimals)} L/min"
    }

fun densityText(kgM3: Double, units: UnitSystem, decimals: Int = 1): String =
    if (isField(units)) "${Units.fmt(Units.kgM3ToPpg(kgM3), decimals)} ppg"
    else "${Units.fmt(kgM3, decimals)} kg/m³"

fun densityValue(kgM3: Double, units: UnitSystem, decimals: Int = 1): String =
    if (isField(units)) Units.fmt(Units.kgM3ToPpg(kgM3), decimals)
    else Units.fmt(kgM3, decimals)

fun gradientText(kpaPerM: Double, units: UnitSystem): String = when (units) {
    UnitSystem.PETROBRAS -> {
        val psiM = Units.kpaToPsi(kpaPerM)
        "${Units.fmt(psiM, 3)} psi/m"
    }
    UnitSystem.US_FIELD -> {
        val psiFt = Units.kpaToPsi(kpaPerM) / Units.M_TO_FT
        "${Units.fmt(psiFt, 3)} psi/ft"
    }
    UnitSystem.SI -> "${Units.fmt(kpaPerM, 2)} kPa/m"
}

fun ropText(mPerHour: Double, units: UnitSystem, decimals: Int = 1): String =
    if (units == UnitSystem.US_FIELD) {
        "${Units.fmt(Units.mToFt(mPerHour), decimals)} ft/h"
    } else {
        "${Units.fmt(mPerHour, decimals)} m/h"
    }

fun ropValue(mPerHour: Double, units: UnitSystem, decimals: Int = 1): String =
    if (units == UnitSystem.US_FIELD) Units.fmt(Units.mToFt(mPerHour), decimals)
    else Units.fmt(mPerHour, decimals)

fun wobText(kn: Double, units: UnitSystem, decimals: Int = 1): String =
    if (isField(units)) "${Units.fmt(Units.knToKlb(kn), decimals)} Klb"
    else "${Units.fmt(kn, decimals)} kN"

fun wobValue(kn: Double, units: UnitSystem, decimals: Int = 1): String =
    if (isField(units)) Units.fmt(Units.knToKlb(kn), decimals)
    else Units.fmt(kn, decimals)

fun torqueText(knm: Double, units: UnitSystem, decimals: Int = 1): String =
    if (isField(units)) "${Units.fmt(Units.knmToKlbFt(knm), decimals)} Klb·ft"
    else "${Units.fmt(knm, decimals)} kN·m"

fun torqueValue(knm: Double, units: UnitSystem, decimals: Int = 1): String =
    if (isField(units)) Units.fmt(Units.knmToKlbFt(knm), decimals)
    else Units.fmt(knm, decimals)

fun durationText(seconds: Double): String {
    val s = seconds.toInt().coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

// ---------------------------------------------------------------------------
// Entrada de dados do editor. O estado do editor sempre exibe o valor no
// sistema escolhido em Ajustes; estas funções fazem a ponte com o SI do motor.
// ---------------------------------------------------------------------------

fun depthUnit(units: UnitSystem): String =
    if (units == UnitSystem.US_FIELD) "ft" else "m"

fun gradientUnit(units: UnitSystem): String = when (units) {
    UnitSystem.PETROBRAS -> "psi/m"
    UnitSystem.US_FIELD -> "psi/ft"
    UnitSystem.SI -> "kPa/m"
}

fun mudWeightUnit(units: UnitSystem): String =
    if (isField(units)) "ppg" else "kg/m³"

fun volumeUnit(units: UnitSystem): String =
    if (isField(units)) "bbl" else "m³"

fun pressureUnit(units: UnitSystem): String =
    if (isField(units)) "psi" else "kPa"

private fun plain(value: Double, decimals: Int): String {
    val text = String.format(java.util.Locale.US, "%.${decimals}f", value)
    return if (text.contains('.')) text.trimEnd('0').trimEnd('.') else text
}

private fun parseNumber(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()

fun depthInput(m: Double, units: UnitSystem, decimals: Int = 1): String =
    plain(depthInputValue(m, units), decimals)

fun depthInputValue(m: Double, units: UnitSystem): Double =
    if (units == UnitSystem.US_FIELD) Units.mToFt(m) else m

fun parseDepthInput(text: String, units: UnitSystem): Double? =
    parseNumber(text)?.let { if (units == UnitSystem.US_FIELD) Units.ftToM(it) else it }

fun gradientInput(kpaPerM: Double, units: UnitSystem): String = when (units) {
    UnitSystem.PETROBRAS -> plain(Units.kpaToPsi(kpaPerM), 3)
    UnitSystem.US_FIELD -> plain(Units.kpaToPsi(kpaPerM) / Units.M_TO_FT, 3)
    UnitSystem.SI -> plain(kpaPerM, 2)
}

fun parseGradientInput(text: String, units: UnitSystem): Double? {
    val value = parseNumber(text) ?: return null
    return when (units) {
        UnitSystem.PETROBRAS -> Units.psiToKpa(value)
        UnitSystem.US_FIELD -> Units.psiToKpa(value) * Units.M_TO_FT
        UnitSystem.SI -> value
    }
}

fun mudWeightInput(ppg: Double, units: UnitSystem): String =
    if (isField(units)) plain(ppg, 1)
    else plain(Units.ppgToKgM3(ppg), 0)

fun parseMudWeightToPpg(text: String, units: UnitSystem): Double? {
    val value = parseNumber(text) ?: return null
    return if (isField(units)) value else Units.kgM3ToPpg(value)
}

fun volumeInput(m3: Double, units: UnitSystem, decimals: Int = 1): String =
    if (isField(units)) plain(Units.m3ToBbl(m3), decimals)
    else plain(m3, decimals)

fun parseVolumeInput(text: String, units: UnitSystem): Double? {
    val value = parseNumber(text) ?: return null
    return if (isField(units)) Units.bblToM3(value) else value
}

fun pressureInput(kPa: Double, units: UnitSystem, decimals: Int = 0): String =
    if (isField(units)) plain(Units.kpaToPsi(kPa), decimals)
    else plain(kPa, decimals)

fun parsePressureInput(text: String, units: UnitSystem): Double? {
    val value = parseNumber(text) ?: return null
    return if (isField(units)) Units.psiToKpa(value) else value
}

// ---------------------------------------------------------------------------
// Entrada de poros/fratura por gradiente OU densidade equivalente.
// Petrobras: G[psi/m] = ED[ppg] × 0,170434.
// US field:  G[psi/ft] = ED[ppg] × 0,051948.
// SI:        G[kPa/m]  = ρ[kg/m³] × 0,00980665.
// ---------------------------------------------------------------------------

enum class GradientEntryMode { GRADIENT, DENSITY }

fun gradientEntryUnit(units: UnitSystem, mode: GradientEntryMode): String =
    if (mode == GradientEntryMode.DENSITY) mudWeightUnit(units) else gradientUnit(units)

fun gradientInputForMode(kpaPerM: Double, units: UnitSystem, mode: GradientEntryMode): String =
    if (mode == GradientEntryMode.GRADIENT) {
        gradientInput(kpaPerM, units)
    } else {
        val kgM3 = kpaPerM * 1000.0 / Units.G
        if (isField(units)) plain(Units.kgM3ToPpg(kgM3), 2) else plain(kgM3, 0)
    }

fun parseGradientForMode(text: String, units: UnitSystem, mode: GradientEntryMode): Double? {
    if (mode == GradientEntryMode.GRADIENT) return parseGradientInput(text, units)
    val value = parseNumber(text) ?: return null
    val kgM3 = if (isField(units)) Units.ppgToKgM3(value) else value
    return kgM3 * Units.G / 1000.0
}

fun gradientDensityFormula(units: UnitSystem): String = when (units) {
    UnitSystem.PETROBRAS -> "G[psi/m] = ED[ppg] × 0,1704"
    UnitSystem.US_FIELD -> "G[psi/ft] = ED[ppg] × 0,05195"
    UnitSystem.SI -> "G[kPa/m] = ρ[kg/m³] × 0,00980665"
}

fun rateUnit(units: UnitSystem): String = if (isField(units)) "gpm" else "L/min"

fun rateInput(lpm: Double, units: UnitSystem, decimals: Int = 0): String =
    if (isField(units)) plain(Units.lpmToGpm(lpm), decimals) else plain(lpm, decimals)

fun parseRateInput(text: String, units: UnitSystem): Double? {
    val value = parseNumber(text) ?: return null
    return if (isField(units)) Units.gpmToLpm(value) else value
}

/**
 * Fonte única de SIDPP para exibição. Com bomba ligada e poço estabilizado o
 * gauge é zerado; nesse caso mostramos o par registrado de referência. Caso
 * contrário, mostramos a leitura viva, garantindo o mesmo valor em todas as abas.
 */
fun SimSnapshot.sidppDisplayKPa(): Double =
    if (pumpOn && shutInStabilized) recordedSidppKPa else sidppGaugeKPa

/** Fonte única de SICP para exibição: sempre o manômetro vivo de revestimento. */
fun SimSnapshot.sicpDisplayKPa(): Double = sicpGaugeKPa
