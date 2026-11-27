package com.mosquitobr.controlepoco.core

import kotlin.math.pow

object ThermalModel {
    /**
     * Retorna a temperatura em °C na profundidade TVD especificada.
     */
    fun tempAtTvd(well: WellConfig, tvdM: Double): Double {
        val depth = tvdM.coerceIn(0.0, well.tvdM.coerceAtLeast(1.0))
        val totalTvd = well.tvdM.coerceAtLeast(1.0)

        if (well.isSubsea && well.riser != null) {
            val wd = well.riser.waterDepthM.coerceAtLeast(10.0)
            if (depth <= wd) {
                // Perfil de água do mar com termoclina
                val ratio = depth / wd
                return well.seaSurfaceTempC + (well.seaMudlineTempC - well.seaSurfaceTempC) * ratio.pow(0.7)
            } else {
                // Perfil geotérmico abaixo do mudline
                val subDepth = depth - wd
                val subTotal = totalTvd - wd
                val ratio = if (subTotal > 0) subDepth / subTotal else 0.0
                return well.seaMudlineTempC + (well.tempBottomC - well.seaMudlineTempC) * ratio
            }
        } else {
            // Perfil geotérmico terrestre
            val ratio = depth / totalTvd
            return well.tempSurfaceC + (well.tempBottomC - well.tempSurfaceC) * ratio
        }
    }

    /**
     * Retorna a densidade corrigida por temperatura (°C).
     * alpha = 3.0e-4 por °C (expansão térmica típica de lama base água).
     */
    fun densityAtTemp(densityRefKgM3: Double, tempC: Double, tempRefC: Double = 15.5, alpha: Double = 0.0003): Double {
        val factor = 1.0 - alpha * (tempC - tempRefC)
        return (densityRefKgM3 * factor).coerceAtLeast(600.0)
    }

    /**
     * Calcula a hidrostática integrada em kPa do topo (0) até [tvdM],
     * aplicando o perfil de temperatura e densidade.
     */
    fun integratedHydrostaticKPa(well: WellConfig, mudDensityRefKgM3: Double, tvdM: Double): Double {
        if (tvdM <= 0.0) return 0.0
        val steps = 30
        val dz = tvdM / steps
        var totalKPa = 0.0
        for (i in 0 until steps) {
            val zMid = (i + 0.5) * dz
            val tempC = tempAtTvd(well, zMid)
            val rhoMid = densityAtTemp(mudDensityRefKgM3, tempC)
            totalKPa += rhoMid * Units.G * dz / 1000.0
        }
        return totalKPa
    }

    /**
     * Fator médio de correção de temperatura para a densidade entre 0 e TVD.
     */
    fun temperatureFactor(well: WellConfig, tvdM: Double): Double {
        if (tvdM <= 0.0) return 1.0
        val tempAvg = (tempAtTvd(well, 0.0) + tempAtTvd(well, tvdM)) / 2.0
        return 1.0 - 0.0003 * (tempAvg - 15.5)
    }
}
