package com.mosquitobr.controlepoco.core

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Conversões entre unidades de campo e SI. O motor trabalha em SI:
 * pressão em kPa, profundidade em m, volume em m3, vazão em L/min e
 * densidade em kg/m3. A interface converte para psi/ppg/bbl/ft quando
 * o usuário escolhe unidades de campo.
 */
object Units {
    const val G = 9.80665
    const val PSI_TO_KPA = 6.894757293
    const val KPA_TO_PSI = 1.0 / PSI_TO_KPA
    const val PPG_TO_KGM3 = 119.826427
    const val KGM3_TO_PPG = 1.0 / PPG_TO_KGM3
    const val FT_TO_M = 0.3048
    const val M_TO_FT = 1.0 / FT_TO_M
    const val BBL_TO_M3 = 0.1589872949
    const val M3_TO_BBL = 1.0 / BBL_TO_M3
    const val GPM_TO_LPM = 3.785411784
    const val LPM_TO_GPM = 1.0 / GPM_TO_LPM
    const val BBL_PER_MIN_TO_LPM = 158.9872949
    const val LPM_TO_BBL_PER_MIN = 1.0 / BBL_PER_MIN_TO_LPM
    const val GPM_PER_BBL_PER_MIN = 42.0
    const val IN_TO_M = 0.0254
    const val M_TO_IN = 1.0 / IN_TO_M
    const val L_TO_M3 = 0.001
    const val M3_TO_L = 1000.0
    const val KN_TO_KLB = 0.2248089431
    const val KLB_TO_KN = 4.4482216153
    const val KNM_TO_KLB_FT = 0.7375621493
    const val KLB_FT_TO_KNM = 1.3558179483

    fun ppgToKgM3(value: Double): Double = value * PPG_TO_KGM3
    fun kgM3ToPpg(value: Double): Double = value * KGM3_TO_PPG
    fun psiToKpa(value: Double): Double = value * PSI_TO_KPA
    fun kpaToPsi(value: Double): Double = value * KPA_TO_PSI
    fun ftToM(value: Double): Double = value * FT_TO_M
    fun mToFt(value: Double): Double = value * M_TO_FT
    fun bblToM3(value: Double): Double = value * BBL_TO_M3
    fun m3ToBbl(value: Double): Double = value * M3_TO_BBL
    fun gpmToLpm(value: Double): Double = value * GPM_TO_LPM
    fun lpmToGpm(value: Double): Double = value * LPM_TO_GPM
    fun bblMinToLpm(value: Double): Double = value * BBL_PER_MIN_TO_LPM
    fun lpmToBblMin(value: Double): Double = value * LPM_TO_BBL_PER_MIN
    fun knToKlb(value: Double): Double = value * KN_TO_KLB
    fun klbToKn(value: Double): Double = value * KLB_TO_KN
    fun knmToKlbFt(value: Double): Double = value * KNM_TO_KLB_FT
    fun klbFtToKnm(value: Double): Double = value * KLB_FT_TO_KNM
    fun inToM(value: Double): Double = value * IN_TO_M
    fun mToIn(value: Double): Double = value * M_TO_IN

    fun hydrostaticKpa(densityKgM3: Double, tvdM: Double): Double =
        densityKgM3 * G * tvdM / 1000.0

    fun round(value: Double, decimals: Int): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return (value * factor).roundToInt() / factor
    }

    fun fmt(value: Double, decimals: Int = 0): String =
        String.format(Locale.US, "%,.${decimals}f", value)

    fun fmtP(value: Double, decimals: Int = 0): String = "${fmt(value, decimals)} psi"
    fun fmtM3(value: Double, decimals: Int = 1): String = "${fmt(value, decimals)} m³"
    fun fmtBbl(value: Double, decimals: Int = 1): String = "${fmt(value, decimals)} bbl"
}
