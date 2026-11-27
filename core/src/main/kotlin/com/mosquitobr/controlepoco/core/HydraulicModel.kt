package com.mosquitobr.controlepoco.core

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow

data class DetailedHydraulics(
    val surfaceKPa: Double = 0.0,
    val stringKPa: Double = 0.0,
    val bitKPa: Double = 0.0,
    val annulusKPa: Double = 0.0,
    val motorKPa: Double = 0.0,
    val totalKPa: Double = 0.0,
    val annularVelocityMPerS: Double = 0.0,
    val ecdKgM3: Double = 0.0,
    val temperatureFactor: Double = 1.0
)

object HydraulicModel {

    /**
     * Perda de carga em uma linha auxiliar (kill/choke/booster) a partir do
     * diâmetro interno, comprimento e vazão. Usada na manobra de Delta P da
     * linha de controle.
     */
    fun lineFrictionKPa(
        idIn: Double,
        lengthM: Double,
        rateLpm: Double,
        densityKgM3: Double
    ): Double {
        if (rateLpm <= 0.5 || lengthM <= 0.0 || idIn <= 0.0) return 0.0
        val qM3S = rateLpm / 60000.0
        val idM = Units.inToM(idIn).coerceAtLeast(1e-4)
        val areaM2 = PI / 4.0 * idM * idM
        val velocityMPerS = qM3S / areaM2
        val f = 0.024 * (qM3S / 0.015).pow(-0.2)
        return f * (lengthM / idM) * (densityKgM3 * velocityMPerS * velocityMPerS / 2.0) / 1000.0
    }

    /**
     * Calcula as perdas hidráulicas detalhadas considerando geometria,
     * perfil de temperatura e vazão.
     */
    fun calculate(
        well: WellConfig,
        string: DrillString,
        bitDepthM: Double,
        rateLpm: Double,
        mudDensityRefKgM3: Double,
        cuttingsFraction: Double = 0.0,
        bitWear: Double = 0.0,
        hydraulicScale: Double = 1.0,
        surfaceDensityKgM3: Double = mudDensityRefKgM3,
        stringDensityKgM3: Double = mudDensityRefKgM3,
        bitDensityKgM3: Double = stringDensityKgM3,
        annulusDensityKgM3: Double = mudDensityRefKgM3,
        plasticViscosityCp: Double = 20.0,
        yieldPointPa: Double = 9.0,
        baseDensityKgM3: Double = mudDensityRefKgM3
    ): DetailedHydraulics {
        if (rateLpm <= 0.5) return DetailedHydraulics()

        val qM3S = rateLpm / 60000.0
        val bitTvdM = well.tvdAtMd(bitDepthM).coerceAtLeast(1.0)
        val tempAtBit = ThermalModel.tempAtTvd(well, bitTvdM)
        val tempAvg = (well.tempSurfaceC + tempAtBit) / 2.0

        val rhoSurface = ThermalModel.densityAtTemp(surfaceDensityKgM3, well.tempSurfaceC)
        val rhoString = ThermalModel.densityAtTemp(stringDensityKgM3, tempAvg)
        val rhoBit = ThermalModel.densityAtTemp(bitDensityKgM3, tempAtBit)
        val rhoAnnulus = ThermalModel.densityAtTemp(annulusDensityKgM3, tempAvg)

        // Fator reológico: ao adensar a lama com barita (rho > baseDensity), o teor
        // de sólidos aumenta a viscosidade plástica (PV) e o limite de escoamento (YP)
        // de forma compatível com as proporções de fluido novo em cada trecho.
        val baseRef = baseDensityKgM3.coerceAtLeast(800.0)
        val baseRheo = ((plasticViscosityCp + 0.4 * yieldPointPa) / (20.0 + 0.4 * 9.0))
            .coerceIn(0.6, 1.8)
            .pow(0.12)
        fun rheoFactor(rhoSection: Double): Double {
            val weightRatio = (rhoSection / baseRef).coerceIn(0.7, 2.2)
            return baseRheo * weightRatio.pow(0.25)
        }

        val rheoSurf = rheoFactor(surfaceDensityKgM3)
        val rheoStr = rheoFactor(stringDensityKgM3)
        val rheoAnn = rheoFactor(annulusDensityKgM3)

        // 1. Superfície (12% base calibrado, com fluido bombeado na superfície)
        val surfaceLoss = 180.0 * (rateLpm / 900.0).pow(1.8) * (rhoSurface / 1200.0) * rheoSurf * hydraulicScale

        // 2. Coluna (interior do DP, HWDP, DC — proporcional à fração de lama nova na COP)
        var stringLoss = 0.0
        var depthAcc = 0.0
        for (seg in string.segments) {
            val lengthInWell = (bitDepthM - depthAcc).coerceIn(0.0, seg.lengthM)
            if (lengthInWell > 0.0) {
                val idM = Units.inToM(seg.idIn)
                val areaM2 = PI / 4.0 * idM * idM
                val velocityMPerS = qM3S / areaM2
                val f = 0.024 * (qM3S / 0.015).pow(-0.2) * rheoStr
                val deltaP = f * (lengthInWell / idM) * (rhoString * velocityMPerS * velocityMPerS / 2.0) / 1000.0
                stringLoss += deltaP
            }
            depthAcc += seg.lengthM
            if (depthAcc >= bitDepthM) break
        }
        stringLoss *= hydraulicScale

        // 3. Broca (TFA — fluido que atravessa os jatos da broca no instante)
        val tfaM2 = Units.inToM(1.0) * Units.inToM(1.0) * string.bitTfaIn2 * (1.0 + bitWear * 0.2)
        val bitVelocityMPerS = qM3S / tfaM2.coerceAtLeast(1e-5)
        val cd = 0.95
        val bitLoss = (rhoBit * bitVelocityMPerS * bitVelocityMPerS / (2.0 * cd * cd) / 1000.0) * hydraulicScale

        // 4. Anular (pela geometria real: open hole, casing, riser — proporcional à fração no anular)
        var annulusLoss = 0.0
        val geometry = WellGeometry(well, string)
        var z = 0.0
        val dz = max(10.0, bitDepthM / 20.0)
        var lastVelocity = 0.0

        while (z < bitDepthM) {
            val nextZ = (z + dz).coerceAtMost(bitDepthM)
            val zMid = (z + nextZ) / 2.0
            val areaM2 = geometry.annularAreaM2(zMid)
            val v = qM3S / areaM2
            lastVelocity = v
            val dhM = Units.inToM(geometry.holeIdAt(zMid) - geometry.pipeOdAt(zMid)).coerceAtLeast(0.01)
            val f = (0.032 * (v / 1.0).pow(-0.15) * rheoAnn).coerceIn(0.01, 0.09)
            val lenM = nextZ - z
            annulusLoss += f * (lenM / dhM) * (rhoAnnulus * v * v / 2.0) / 1000.0
            z = nextZ
        }
        annulusLoss *= hydraulicScale

        // 5. Motor de fundo (8% base — proporcional ao fluido na coluna/BHA)
        val motorLoss = 120.0 * (rateLpm / 900.0).pow(1.8) * (rhoString / 1200.0) * rheoStr * hydraulicScale

        val totalLoss = surfaceLoss + stringLoss + bitLoss + annulusLoss + motorLoss

        // Carga de cascalhos na densidade anular (rho_rock ~ 2600 kg/m³)
        val rhoRock = 2600.0
        val deltaCuttingsDensity = cuttingsFraction.coerceIn(0.0, 0.10) * (rhoRock - rhoAnnulus)

        // Fator de temperatura
        val tempFactor = ThermalModel.temperatureFactor(well, bitTvdM)

        // Perda de carga anular em equivalência de densidade
        val frictionEquivDensity = annulusLoss * 1000.0 / (Units.G * bitTvdM)

        // ECD reportada em relação ao fluido efetivo no anular:
        // densidade efetiva do anular + carga de cascalhos + perda anular equivalente.
        val ecdKgM3 = (annulusDensityKgM3 + deltaCuttingsDensity + frictionEquivDensity)

        return DetailedHydraulics(
            surfaceKPa = surfaceLoss,
            stringKPa = stringLoss,
            bitKPa = bitLoss,
            annulusKPa = annulusLoss,
            motorKPa = motorLoss,
            totalKPa = totalLoss,
            annularVelocityMPerS = lastVelocity,
            ecdKgM3 = ecdKgM3,
            temperatureFactor = tempFactor
        )
    }
}
