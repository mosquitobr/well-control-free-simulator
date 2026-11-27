package com.mosquitobr.controlepoco.core

import kotlin.math.max

data class KillSheetInput(
    val currentMudKgM3: Double,
    val tvdM: Double,
    val sidppKPa: Double,
    val sicpKPa: Double,
    val scrKPa: Double,
    val killRateLpm: Double,
    val pumpOutputLPerStroke: Double,
    val shoeTvdM: Double,
    val fracGradientKPaPerM: Double,
    val strokesToBit: Double,
    val strokesToShoe: Double,
    val totalStrokes: Double,
    val chokeLineFrictionKPa: Double = 0.0,
    val waterDepthM: Double = 0.0,
    val subsea: Boolean = false,
    val scrEstimated: Boolean = false,
    /** Margem de segurança somada à KMW (kg/m³). */
    val marginKgM3: Double = 0.0
)

data class KillScheduleRow(
    val stroke: Double,
    val sppKPa: Double,
    val casingKPa: Double,
    val stage: String
)

data class KillSheetResult(
    val killMudKgM3: Double,
    val killMudPpg: Double,
    val killMudNoMarginKgM3: Double = killMudKgM3,
    val marginKgM3: Double = 0.0,
    val icpKPa: Double,
    val fcpKPa: Double,
    val maaspKPa: Double,
    val maspKPa: Double,
    val sidppKPa: Double = 0.0,
    val sicpKPa: Double = 0.0,
    val strokesToBit: Double,
    val strokesToShoe: Double,
    val totalStrokes: Double,
    val killRateLpm: Double,
    val schedule: List<KillScheduleRow>,
    val warnings: List<String>
)

object KillSheetCalculator {
    fun calculate(input: KillSheetInput): KillSheetResult {
        val hydrostatic = Units.hydrostaticKpa(input.currentMudKgM3, input.tvdM)
        val killMudNoMargin = input.currentMudKgM3 +
            (input.sidppKPa * 1000.0) / (Units.G * input.tvdM).coerceAtLeast(1.0)
        val margin = input.marginKgM3.coerceAtLeast(0.0)
        val killMud = killMudNoMargin + margin
        val icp = max(0.0, input.sidppKPa + input.scrKPa)
        val fcp = if (input.currentMudKgM3 > 0.0) {
            input.scrKPa * (killMud / input.currentMudKgM3)
        } else 0.0
        val maasp = max(
            0.0,
            input.fracGradientKPaPerM * input.shoeTvdM -
                Units.hydrostaticKpa(input.currentMudKgM3, input.shoeTvdM)
        )
        val masp = max(
            input.sicpKPa + input.chokeLineFrictionKPa,
            maasp * 0.95
        )

        val warnings = mutableListOf<String>()
        if (input.sicpKPa + 100.0 < input.sidppKPa) {
            warnings.add("SICP menor que SIDPP: verifique estabilização e sensores.")
        }
        if (input.sidppKPa <= 0.0) {
            warnings.add("SIDPP não positivo: confirme se o poço está fechado e estabilizado.")
        }
        if (Units.hydrostaticKpa(killMud, input.shoeTvdM) > input.fracGradientKPaPerM * input.shoeTvdM) {
            warnings.add("Lama de kill acima da pressão de fratura na sapata: risco de perda.")
        }
        if (margin > 0.0) {
            warnings.add(
                "Margem de segurança aplicada: ${Units.fmt(Units.kgM3ToPpg(margin), 2)} ppg."
            )
        }
        if (input.subsea && input.chokeLineFrictionKPa <= 0.0) {
            warnings.add("Informe a fricção da choke/kill line para o cenário submarino.")
        }
        if (input.scrEstimated) {
            warnings.add("SCR estimada: faça a tomada de SCR na taxa de kill antes de circular.")
        }
        if (hydrostatic <= 0.0) {
            warnings.add("Dados de lama/profundidade inválidos.")
        }

        val rows = mutableListOf<KillScheduleRow>()
        val steps = 12
        val bitStrokes = input.strokesToBit.coerceAtLeast(1.0)
        for (i in 0..steps) {
            val fraction = i.toDouble() / steps
            val stroke = input.totalStrokes * fraction
            val stage = when {
                stroke <= bitStrokes -> "Lama de kill até a broca"
                stroke <= input.strokesToShoe -> "Lama de kill até a sapata"
                else -> "Deslocamento até a superfície"
            }
            val spp = when {
                stroke <= bitStrokes -> icp - (icp - fcp) * (stroke / bitStrokes)
                else -> fcp
            }
            val casing = max(
                0.0,
                input.sicpKPa * (1.0 - 0.35 * fraction) + input.chokeLineFrictionKPa
            )
            rows.add(KillScheduleRow(stroke, spp, casing, stage))
        }

        return KillSheetResult(
            killMudKgM3 = killMud,
            killMudPpg = Units.kgM3ToPpg(killMud),
            killMudNoMarginKgM3 = killMudNoMargin,
            marginKgM3 = margin,
            icpKPa = icp,
            fcpKPa = fcp,
            maaspKPa = maasp,
            maspKPa = masp,
            sidppKPa = input.sidppKPa,
            sicpKPa = input.sicpKPa,
            strokesToBit = input.strokesToBit,
            strokesToShoe = input.strokesToShoe,
            totalStrokes = input.totalStrokes,
            killRateLpm = input.killRateLpm,
            schedule = rows,
            warnings = warnings
        )
    }
}
