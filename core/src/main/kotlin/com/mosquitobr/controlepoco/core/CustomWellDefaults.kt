package com.mosquitobr.controlepoco.core

/**
 * Valores sugeridos para o poço personalizado.
 *
 * A regra do editor é: campo vazio usa a sugestão; campo preenchido é
 * respeitado. Estas funções são compartilhadas entre a UI (para exibir a
 * sugestão) e o motor (para resolver o cenário quando o campo está vazio).
 */
object CustomWellDefaults {
    const val START_STANDOFF_M = 5.0
    const val DEFAULT_ZONE_SPAN_M = 30.0
    const val DEFAULT_ZONE_OFFSET_FROM_TD_M = 180.0
    const val DEFAULT_KICK_INTENSITY_KPA = 700.0

    fun triggerDepthMd(triggerMd: Double, md: Double): Double {
        val target = if (triggerMd > 0.0) triggerMd else (md - DEFAULT_ZONE_OFFSET_FROM_TD_M)
        return target.coerceIn(0.0, md)
    }

    fun zoneTopMd(zoneTopMd: Double, triggerMd: Double, md: Double): Double {
        if (zoneTopMd > 0.0) return zoneTopMd.coerceIn(0.0, md)
        return triggerDepthMd(triggerMd, md)
    }

    fun zoneBaseMd(zoneBaseMd: Double, zoneTopMd: Double, md: Double): Double {
        if (zoneBaseMd > zoneTopMd) return zoneBaseMd.coerceAtMost(md)
        return (zoneTopMd + DEFAULT_ZONE_SPAN_M).coerceAtMost(md)
    }

    /**
     * Profundidade inicial da broca. Quando não informada, começa alguns
     * metros acima do topo da zona/gatilho para que o operador perfure até
     * o evento, em vez de nascer dentro dele.
     */
    fun startDepthMd(
        startMd: Double?,
        md: Double,
        zoneTopMd: Double,
        triggerMd: Double,
        kickEnabled: Boolean
    ): Double {
        startMd?.let { return it.coerceIn(0.0, md) }
        if (!kickEnabled) return md
        val anchor = listOf(zoneTopMd, triggerMd)
            .filter { it > 0.0 }
            .minOrNull() ?: md
        return (anchor - START_STANDOFF_M).coerceIn(0.0, md)
    }

    fun kickDepthMd(
        kickDepthMd: Double?,
        md: Double,
        zoneTopMd: Double,
        zoneBaseMd: Double,
        triggerMd: Double
    ): Double {
        kickDepthMd?.let { return it.coerceIn(0.0, md) }
        return when {
            zoneBaseMd > zoneTopMd -> zoneBaseMd
            triggerMd > 0.0 -> triggerMd
            else -> md
        }
    }

    fun intensityKPa(intensityKPa: Double?): Double =
        intensityKPa?.coerceAtLeast(0.0) ?: DEFAULT_KICK_INTENSITY_KPA
}
