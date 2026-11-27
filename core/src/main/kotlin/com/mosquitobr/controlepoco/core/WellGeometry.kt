package com.mosquitobr.controlepoco.core

import kotlin.math.PI

/**
 * Geometria simplificada, mas consistente, para volumes, capacidades,
 * strokes e posição dos eventos no poço.
 */
class WellGeometry(
    val well: WellConfig,
    val string: DrillString
) {
    private val casings = well.casings.sortedBy { it.bottomMdM }

    fun holeIdAt(depthM: Double): Double {
        if (well.isSubsea && well.riser != null && depthM < well.riser.waterDepthM) {
            return well.riser.riserIdIn
        }
        val covering = casings.filter { depthM >= it.topMdM && depthM <= it.bottomMdM }
        if (covering.isNotEmpty()) return covering.minOf { it.idIn }
        val next = casings.firstOrNull { depthM < it.topMdM }
        if (next != null) return next.idIn
        return well.openHoleDiameterIn
    }

    fun pipeOdAt(depthM: Double): Double = string.odAtDepth(depthM)

    fun pipeIdAt(depthM: Double): Double = string.idAtDepth(depthM)

    fun pipeSteelAreaM2(depthM: Double): Double {
        val od = Units.inToM(pipeOdAt(depthM))
        val id = Units.inToM(pipeIdAt(depthM))
        return (PI / 4.0 * (od * od - id * id)).coerceAtLeast(1e-5)
    }

    fun pipeSteelVolumeBetween(fromM: Double, toM: Double): Double {
        if (toM <= fromM) return 0.0
        var volume = 0.0
        var depth = fromM
        while (depth < toM) {
            val next = minOf(toM, depth + 1.0)
            volume += pipeSteelAreaM2(depth + 0.5) * (next - depth)
            depth = next
        }
        return volume
    }

    fun annularAreaM2(depthM: Double): Double {
        val hole = Units.inToM(holeIdAt(depthM))
        val pipe = Units.inToM(pipeOdAt(depthM))
        val area = PI / 4.0 * (hole * hole - pipe * pipe)
        return area.coerceAtLeast(1e-6)
    }

    fun annularCapacityM3PerM(depthM: Double): Double = annularAreaM2(depthM)

    fun annularVolumeBetween(fromM: Double, toM: Double): Double {
        if (toM <= fromM) return 0.0
        var volume = 0.0
        var depth = fromM
        while (depth < toM) {
            val next = minOf(toM, depth + 1.0)
            volume += annularAreaM2(depth + 0.5) * (next - depth)
            depth = next
        }
        return volume
    }

    fun annularVolumeToDepth(depthM: Double): Double = annularVolumeBetween(0.0, depthM)

    /** Profundidade do BOP: mudline em subsea e superfície em terra. */
    val bopMdM: Double
        get() = well.riser?.waterDepthM ?: 0.0

    /**
     * Volume anular do poço entre o BOP e a broca. Em subsea o riser fica
     * isolado pelo BOP fechado e não entra nesse volume.
     */
    fun annularVolumeToBop(bitDepthM: Double): Double =
        annularVolumeBetween(bopMdM, bitDepthM)

    /**
     * Volume de circulação até a superfície com o BOP fechado: anular do poço
     * (broca→BOP) mais a linha de retorno (choke line). Kill line e riser não
     * participam do retorno.
     */
    fun circulationVolumeToSurface(bitDepthM: Double): Double =
        annularVolumeToBop(bitDepthM) + chokeLineVolumeM3()

    fun stringAreaM2(depthM: Double): Double {
        val id = Units.inToM(string.idAtDepth(depthM))
        return PI / 4.0 * id * id
    }

    fun stringVolumeBetween(fromM: Double, toM: Double): Double {
        if (toM <= fromM) return 0.0
        var volume = 0.0
        var depth = fromM
        while (depth < toM) {
            val next = minOf(toM, depth + 1.0)
            volume += stringAreaM2(depth + 0.5) * (next - depth)
            depth = next
        }
        return volume
    }

    fun stringVolumeToDepth(depthM: Double): Double = stringVolumeBetween(0.0, depthM)

    fun strokesForVolume(volumeM3: Double, pumpOutputLPerStroke: Double): Double {
        if (pumpOutputLPerStroke <= 0.0) return 0.0
        return volumeM3 / (pumpOutputLPerStroke * Units.L_TO_M3)
    }

    fun strokesToBit(bitDepthM: Double, pumpOutputLPerStroke: Double): Double =
        strokesForVolume(stringVolumeToDepth(bitDepthM), pumpOutputLPerStroke)

    fun strokesToShoe(bitDepthM: Double, pumpOutputLPerStroke: Double): Double {
        val shoeMd = well.shoeMdM.coerceAtMost(bitDepthM)
        if (bitDepthM <= shoeMd) return 0.0
        return strokesForVolume(annularVolumeBetween(shoeMd, bitDepthM), pumpOutputLPerStroke)
    }

    fun totalAnnularStrokes(bitDepthM: Double, pumpOutputLPerStroke: Double): Double =
        strokesForVolume(annularVolumeToDepth(bitDepthM), pumpOutputLPerStroke)

    fun annularCapacityAtDepth(depthM: Double): Double = annularAreaM2(depthM)

    fun chokeLineVolumeM3(): Double {
        val d = Units.inToM(well.lines.chokeLineIdIn)
        return PI / 4.0 * d * d * well.lines.chokeLineLengthM
    }

    fun killLineVolumeM3(): Double {
        val d = Units.inToM(well.lines.killLineIdIn)
        return PI / 4.0 * d * d * well.lines.killLineLengthM
    }

    fun boosterLineVolumeM3(): Double {
        val d = Units.inToM(well.lines.boosterLineIdIn)
        return PI / 4.0 * d * d * well.lines.boosterLineLengthM
    }
}
