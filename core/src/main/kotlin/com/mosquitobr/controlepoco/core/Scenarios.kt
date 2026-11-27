package com.mosquitobr.controlepoco.core

import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private data class DirectionalPlan(
    val mdM: Double,
    val trajectory: List<TrajectoryStation>
)

private fun buildHoldPlan(
    tvd: Double,
    inclinationDeg: Double,
    kickoffMd: Double = 600.0,
    buildLength: Double = 500.0
): DirectionalPlan {
    if (inclinationDeg <= 0.5) {
        return DirectionalPlan(
            mdM = tvd,
            trajectory = listOf(
                TrajectoryStation(0.0, 0.0, 0.0, 0.0),
                TrajectoryStation(tvd, 0.0, 0.0, tvd)
            )
        )
    }
    val inc = Math.toRadians(inclinationDeg)
    val safeKickoff = kickoffMd.coerceIn(0.0, (tvd * 0.5).coerceAtLeast(1.0))
    val build = buildLength.coerceIn(50.0, (tvd - safeKickoff).coerceAtLeast(100.0) * 0.8)
    val tvdBuild = build * sin(inc) / inc
    val endBuildTvd = (safeKickoff + tvdBuild).coerceAtMost(tvd - 1.0)
    val holdTvd = (tvd - endBuildTvd).coerceAtLeast(1.0)
    val holdMd = holdTvd / cos(inc).coerceAtLeast(0.2)
    val endBuildMd = safeKickoff + build
    val md = endBuildMd + holdMd
    val north = build * (1.0 - cos(inc)) / inc + holdMd * sin(inc)
    return DirectionalPlan(
        mdM = md,
        trajectory = listOf(
            TrajectoryStation(0.0, 0.0, 0.0, 0.0),
            TrajectoryStation(safeKickoff, 0.0, 0.0, safeKickoff),
            TrajectoryStation(endBuildMd, inclinationDeg, 0.0, endBuildTvd, north * 0.35),
            TrajectoryStation(md, inclinationDeg, 0.0, tvd, north)
        )
    )
}

private fun scalePlan(plan: DirectionalPlan, mdTarget: Double?): DirectionalPlan {
    if (mdTarget == null || mdTarget <= 0.0 || plan.trajectory.size < 2) return plan
    if (kotlin.math.abs(mdTarget - plan.mdM) < 1.0) return plan
    val factor = mdTarget / plan.mdM.coerceAtLeast(1.0)
    return DirectionalPlan(
        mdM = mdTarget,
        trajectory = plan.trajectory.map { station ->
            if (station.mdM <= 0.0) station else station.copy(mdM = station.mdM * factor)
        }
    )
}

private fun mdAtTvdIn(trajectory: List<TrajectoryStation>, tvd: Double): Double {
    if (trajectory.size < 2) return tvd
    for (i in 0 until trajectory.size - 1) {
        val a = trajectory[i]
        val b = trajectory[i + 1]
        if (tvd <= b.tvdM) {
            val f = (tvd - a.tvdM) / (b.tvdM - a.tvdM).coerceAtLeast(1e-9)
            return a.mdM + f * (b.mdM - a.mdM)
        }
    }
    return trajectory.last().mdM
}

private fun defaultLayers(
    tvd: Double,
    shoe: Double,
    poreGradient: Double,
    fracGradient: Double,
    deepwater: Boolean = false,
    mdAtTvd: (Double) -> Double = { it }
): List<FormationLayer> {
    val reservoirTopTvd = (tvd - 420.0).coerceAtLeast(shoe * 0.75)
    val shallowTvd = (shoe * 0.18).coerceAtMost(tvd - 2.0)
    val midTvd = (shoe * 0.62).coerceAtMost(reservoirTopTvd - 1.0)
    val mdEnd = mdAtTvd(tvd)
    fun md(value: Double): Double = mdAtTvd(value).coerceIn(0.0, mdEnd)
    val shallow = md(shallowTvd)
    val mid = md(midTvd)
    val reservoirTop = md(reservoirTopTvd)
    val layers = mutableListOf(
        FormationLayer(
            name = if (deepwater) "Argila marinha" else "Argila superficial",
            topMdM = md(0.0),
            baseMdM = shallow,
            type = FormationType.SHALE,
            hardness = 2.0,
            poreGradientKPaPerM = (poreGradient - 1.4).coerceAtLeast(8.5),
            fracGradientKPaPerM = (fracGradient - 1.5).coerceAtLeast(poreGradient + 2.0),
            porosity = 0.34,
            permeabilityMd = 5.0,
            ropFactor = 1.35
        ),
        FormationLayer(
            name = "Folhelho",
            topMdM = shallow,
            baseMdM = mid,
            type = FormationType.SHALE,
            hardness = 5.0,
            poreGradientKPaPerM = (poreGradient - 0.4),
            fracGradientKPaPerM = fracGradient,
            porosity = 0.22,
            permeabilityMd = 15.0,
            ropFactor = 1.0
        ),
        FormationLayer(
            name = "Calcário",
            topMdM = mid,
            baseMdM = reservoirTop,
            type = FormationType.LIMESTONE,
            hardness = if (deepwater) 7.5 else 6.2,
            poreGradientKPaPerM = poreGradient,
            fracGradientKPaPerM = fracGradient,
            porosity = 0.12,
            permeabilityMd = 30.0,
            ropFactor = 0.75
        ),
        FormationLayer(
            name = "Arenito reservatório",
            topMdM = reservoirTop,
            baseMdM = md(tvd),
            type = FormationType.SANDSTONE,
            hardness = 4.2,
            poreGradientKPaPerM = poreGradient,
            fracGradientKPaPerM = fracGradient,
            porosity = 0.24,
            permeabilityMd = 220.0,
            ropFactor = 1.45,
            kickZone = true
        )
    )
    return layers.sortedBy { it.topMdM }
}

private fun pipeString(md: Double, slimHole: Boolean = false): DrillString {
    val bha = 180.0.coerceAtMost(md * 0.25)
    return if (slimHole) {
        DrillString(
            segments = listOf(
                PipeSegment("Drill pipe 3 1/2\"", (md - bha).coerceAtLeast(10.0), 3.5, 2.764),
                PipeSegment("HWDP 3 1/2\"", bha * 0.45, 3.5, 2.063),
                PipeSegment("Drill collar 4 3/4\"", bha * 0.55, 4.75, 2.0)
            ),
            bitTfaIn2 = 0.38
        )
    } else {
        DrillString(
            segments = listOf(
                PipeSegment("Drill pipe 5\"", (md - bha).coerceAtLeast(10.0), 5.0, 4.276),
                PipeSegment("HWDP 5\"", bha * 0.45, 5.0, 3.0),
                PipeSegment("Drill collar 6 1/2\"", bha * 0.55, 6.5, 2.25)
            ),
            bitTfaIn2 = 0.48
        )
    }
}

private fun surfaceWell(
    name: String,
    tvd: Double,
    shoe: Double,
    poreGradient: Double,
    fracGradient: Double,
    inclinationDeg: Double = 0.0,
    combinedCasing: Boolean = false,
    mdTarget: Double? = null
): WellConfig {
    val plan = scalePlan(buildHoldPlan(tvd, inclinationDeg), mdTarget)
    val shoeMd = mdAtTvdIn(plan.trajectory, shoe)
    val casings = mutableListOf(
        CasingString(
            name = "13 3/8\"",
            topMdM = 0.0,
            bottomMdM = shoeMd * 0.42,
            idIn = 12.415,
            shoeTvdM = shoe * 0.42,
            odIn = 13.375,
            holeDiameterIn = 17.5
        ),
        CasingString(
            name = "9 5/8\"",
            topMdM = 0.0,
            bottomMdM = shoeMd,
            idIn = 8.681,
            shoeTvdM = shoe,
            odIn = 9.625,
            holeDiameterIn = 12.25
        )
    )
    if (combinedCasing) {
        casings.add(
            CasingString(
                name = "7\" liner",
                topMdM = shoeMd * 0.55,
                bottomMdM = plan.mdM,
                idIn = 6.094,
                shoeTvdM = tvd,
                odIn = 7.0,
                holeDiameterIn = 8.5
            )
        )
    }
    return WellConfig(
        name = name,
        tvdM = tvd,
        mdM = plan.mdM,
        openHoleDiameterIn = if (combinedCasing) 6.0 else 8.5,
        casings = casings,
        poreGradientKPaPerM = poreGradient,
        fracGradientKPaPerM = fracGradient,
        tempBottomC = 95.0,
        surfaceType = SurfaceType.LAND,
        trajectory = plan.trajectory,
        formations = defaultLayers(
            tvd, shoe, poreGradient, fracGradient,
            mdAtTvd = { mdAtTvdIn(plan.trajectory, it) }
        ),
        inclinationDeg = inclinationDeg
    )
}

private fun subseaWell(
    name: String,
    tvd: Double,
    shoe: Double,
    waterDepth: Double,
    poreGradient: Double,
    fracGradient: Double,
    inclinationDeg: Double = 0.0,
    combinedCasing: Boolean = false,
    mdTarget: Double? = null
): WellConfig {
    val plan = scalePlan(buildHoldPlan(tvd, inclinationDeg, kickoffMd = waterDepth + 900.0), mdTarget)
    val shoeMd = mdAtTvdIn(plan.trajectory, shoe)
    val casings = mutableListOf(
        CasingString("20\"", 0.0, shoeMd * 0.35, 19.0, shoe * 0.35, 20.0, 26.0),
        CasingString("13 3/8\"", 0.0, shoeMd * 0.62, 12.415, shoe * 0.62, 13.375, 17.5),
        CasingString("9 5/8\"", 0.0, shoeMd, 8.681, shoe, 9.625, 12.25)
    )
    if (combinedCasing) {
        casings.add(
            CasingString(
                name = "7\" liner",
                topMdM = shoeMd * 0.72,
                bottomMdM = plan.mdM,
                idIn = 6.094,
                shoeTvdM = tvd,
                odIn = 7.0,
                holeDiameterIn = 8.5
            )
        )
    }
    return WellConfig(
        name = name,
        tvdM = tvd,
        mdM = plan.mdM,
        openHoleDiameterIn = if (combinedCasing) 6.0 else 8.5,
        casings = casings,
        riser = RiserConfig(
            waterDepthM = waterDepth,
            airGapM = 28.0,
            riserIdIn = 19.25,
            chokeLineIdIn = 3.0,
            chokeLineLengthM = waterDepth + 40.0
        ),
        poreGradientKPaPerM = poreGradient,
        fracGradientKPaPerM = fracGradient,
        tempBottomC = 78.0,
        surfaceType = SurfaceType.SUBSEA,
        trajectory = plan.trajectory,
        formations = defaultLayers(
            tvd, shoe, poreGradient, fracGradient,
            deepwater = true,
            mdAtTvd = { mdAtTvdIn(plan.trajectory, it) }
        ),
        inclinationDeg = inclinationDeg
    )
}

private fun surfaceRig(deep: Boolean = false): RigSetup = RigSetup(
    name = if (deep) "Sonda terrestre 2000 HP" else "Sonda terrestre 1500 HP",
    rigClass = if (deep) RigClass.LAND_2000 else RigClass.LAND_1500,
    pumps = listOf(
        PumpConfig("Bomba 1", displacementLPerStroke = if (deep) 14.5 else 11.8, maxSpm = 120.0, maxPressureKPa = 34500.0),
        PumpConfig("Bomba 2", displacementLPerStroke = if (deep) 14.5 else 11.8, maxSpm = 120.0, maxPressureKPa = 34500.0)
    ),
    chokes = listOf(
        ChokeConfig("Choke A", maxAreaM2 = 0.00072, cv = 0.86),
        ChokeConfig("Choke B", maxAreaM2 = 0.00060, cv = 0.82)
    ),
    bop = BopConfig(
        elements = listOf(
            BopElement.ANNULAR,
            BopElement.PIPE_RAM,
            BopElement.PIPE_RAM,
            BopElement.BLIND_SHEAR,
            BopElement.HCR
        ),
        accumulatorVolumeL = 2400.0,
        prechargeKPa = 7000.0,
        ratedPressureKPa = 34500.0
    ),
    standpipeManifold = listOf("Pump 1", "Pump 2", "Standpipe", "Kill line", "Choke line"),
    chokeManifold = listOf("Choke A", "Choke B", "Bypass", "MGS", "Flare")
)

private fun subseaRig(): RigSetup = RigSetup(
    name = "Sonda DP — BOP subsea",
    rigClass = RigClass.DEEPWATER_DRILLSHIP,
    pumps = listOf(
        PumpConfig("Bomba 1", 17.5, 130.0, 51700.0),
        PumpConfig("Bomba 2", 17.5, 130.0, 51700.0),
        PumpConfig("Bomba 3", 17.5, 130.0, 51700.0)
    ),
    chokes = listOf(
        ChokeConfig("Choke A", 0.00082, 0.88),
        ChokeConfig("Choke B", 0.00070, 0.84)
    ),
    bop = BopConfig(
        elements = listOf(
            BopElement.ANNULAR,
            BopElement.PIPE_RAM,
            BopElement.PIPE_RAM,
            BopElement.TEST_RAM,
            BopElement.BLIND_SHEAR,
            BopElement.HCR
        ),
        accumulatorVolumeL = 4800.0,
        prechargeKPa = 10500.0,
        ratedPressureKPa = 51700.0
    ),
    standpipeManifold = listOf("Pump 1", "Pump 2", "Pump 3", "Standpipe", "Boost line"),
    chokeManifold = listOf("Choke A", "Choke B", "Kill line", "Choke line", "MGS", "Flare")
)

data class CustomWellSpec(
    val id: String,
    val name: String,
    val surfaceType: SurfaceType = SurfaceType.LAND,
    val tvdM: Double = 3000.0,
    val mdM: Double = 3000.0,
    val shoeTvdM: Double = 2000.0,
    val waterDepthM: Double = 0.0,
    val inclinationDeg: Double = 0.0,
    val combinedCasing: Boolean = false,
    val poreGradientKPaPerM: Double = 11.5,
    val fracGradientKPaPerM: Double = 18.0,
    val mudPpg: Double = 9.6,
    /** Margem de segurança da lama de matar em ppg (padrão 0,1). */
    val killMarginPpg: Double = 0.1,
    val kickEnabled: Boolean = true,
    val kickType: InfluxType = InfluxType.GAS,
    val kickVolumeM3: Double = 2.5,
    val maxKickVolumeM3: Double = kickVolumeM3,
    val characteristicRateLpm: Double = defaultInfluxRateLpm(kickType),
    val zonePoreGradientKPaPerM: Double = 0.0,
    val zonePermeabilityMd: Double = 0.0,
    val kickTriggerMode: KickTriggerMode = KickTriggerMode.TIME,
    val triggerAtSec: Double = 20.0,
    val zoneTopMdM: Double = 0.0,
    val zoneBaseMdM: Double = 0.0,
    val triggerDepthMdM: Double = 0.0,
    val initialBitDepthMdM: Double? = null,
    val kickDepthMdM: Double? = null,
    val kickIntensityKPa: Double? = null,
    val initialSpm: Double = 55.0,
    val openHoleDiameterIn: Double = 8.5,
    val killLineIdIn: Double = 3.0,
    val chokeLineIdIn: Double = 3.0,
    val boosterLineIdIn: Double = 3.0,
    val killLineLengthM: Double = 0.0,
    val chokeLineLengthM: Double = 0.0,
    val boosterLineLengthM: Double = 0.0,
    val riserIdIn: Double = 19.25,
    val riserOdIn: Double = 21.0,
    val tempSurfaceC: Double = 24.0,
    val tempBottomC: Double = 90.0,
    val seaSurfaceTempC: Double = 24.0,
    val seaMudlineTempC: Double = 4.0,
    val customCasings: List<CasingSpec> = emptyList(),
    val drillStringSpec: DrillStringSpec? = null
)

private fun baseSurfaceScenario(
    id: String,
    title: String,
    titleEn: String,
    subtitle: String,
    subtitleEn: String,
    description: String,
    descriptionEn: String,
    method: String,
    difficulty: Int,
    objectives: List<String>,
    objectivesEn: List<String>,
    briefing: String,
    briefingEn: String,
    mudPpg: Double,
    poreGradient: Double = 11.5,
    fracGradient: Double = 18.0,
    tvd: Double = 3000.0,
    shoe: Double = 2000.0,
    kickEnabled: Boolean = false,
    kickVolume: Double = 2.0,
    kickTrigger: Double = 20.0,
    kickType: InfluxType = InfluxType.GAS,
    initialSpm: Double = 55.0,
    events: List<ScenarioEvent> = emptyList(),
    killRate: Double = 900.0,
    inclinationDeg: Double = 0.0,
    combinedCasing: Boolean = false,
    kickMode: KickTriggerMode? = null,
    zoneTopMdM: Double? = null,
    zoneBaseMdM: Double? = null,
    triggerDepthMdM: Double? = null,
    initialBitDepthM: Double? = null,
    initialBopClosed: List<BopElement> = emptyList(),
    initialHcrOpen: Boolean = false
): Scenario {
    val well = surfaceWell(id, tvd, shoe, poreGradient, fracGradient, inclinationDeg, combinedCasing)
    val density = Units.ppgToKgM3(mudPpg)
    val mode = kickMode ?: if (kickTrigger > 0.0) KickTriggerMode.TIME else KickTriggerMode.IMMEDIATE
    val zoneTop = zoneTopMdM ?: well.mdM
    val zoneBase = zoneBaseMdM ?: zoneTop
    return Scenario(
        id = id,
        title = title,
        titleEn = titleEn,
        subtitle = subtitle,
        subtitleEn = subtitleEn,
        description = description,
        descriptionEn = descriptionEn,
        stackType = StackType.SURFACE,
        method = method,
        difficulty = difficulty,
        objectives = objectives,
        objectivesEn = objectivesEn,
        briefing = briefing,
        briefingEn = briefingEn,
        well = well,
        drillString = pipeString(well.mdM, combinedCasing),
        mud = Mud(density, 18.0, 8.0),
        formation = Formation(
            porePressureKPa = poreGradient * tvd,
            fracPressureKPa = well.shoeFracPressureKPa,
            productivityIndexM3SPerKPa = 2.0e-6,
            lossIndexM3SPerKPa = 3.0e-6,
            influxType = kickType,
            layers = well.formations
        ),
        rig = surfaceRig(tvd > 2600.0),
        initialBitDepthM = initialBitDepthM ?: well.mdM,
        initialPitM3 = 150.0,
        initialTripTankM3 = 6.0,
        initialSpm = initialSpm,
        scrubPressureKPa = 1650.0,
        killRateLpm = killRate,
        kick = KickConfig(
            enabled = kickEnabled,
            intensityKPa = 700.0,
            volumeM3 = kickVolume,
            depthM = zoneBase,
            type = kickType,
            triggerAtSec = kickTrigger,
            triggerMode = if (kickEnabled) mode else KickTriggerMode.MANUAL,
            zoneTopMdM = zoneTop,
            zoneBaseMdM = zoneBase,
            triggerDepthMdM = triggerDepthMdM ?: zoneTop,
            maxVolumeM3 = kickVolume,
            characteristicRateLpm = defaultInfluxRateLpm(kickType)
        ),
        events = events,
        initialBopClosed = initialBopClosed,
        initialHcrOpen = initialHcrOpen,
        limits = ScenarioLimits(
            maxCasingKPa = 18000.0,
            maxShoeKPa = well.shoeFracPressureKPa,
            maxPitGainM3 = 8.0,
            minBhpKPa = poreGradient * tvd - 1500.0
        )
    )
}

private fun baseSubseaScenario(
    id: String,
    title: String,
    titleEn: String,
    subtitle: String,
    subtitleEn: String,
    description: String,
    descriptionEn: String,
    method: String,
    difficulty: Int,
    objectives: List<String>,
    objectivesEn: List<String>,
    briefing: String,
    briefingEn: String,
    mudPpg: Double,
    poreGradient: Double = 12.3,
    fracGradient: Double = 18.6,
    tvd: Double = 4500.0,
    shoe: Double = 3600.0,
    waterDepth: Double = 1200.0,
    kickEnabled: Boolean = false,
    kickVolume: Double = 2.5,
    kickTrigger: Double = 20.0,
    kickType: InfluxType = InfluxType.GAS,
    initialSpm: Double = 65.0,
    events: List<ScenarioEvent> = emptyList(),
    inclinationDeg: Double = 0.0,
    combinedCasing: Boolean = false,
    kickMode: KickTriggerMode? = null,
    zoneTopMdM: Double? = null,
    zoneBaseMdM: Double? = null,
    triggerDepthMdM: Double? = null,
    initialBitDepthM: Double? = null,
    initialBopClosed: List<BopElement> = emptyList(),
    initialHcrOpen: Boolean = false
): Scenario {
    val well = subseaWell(id, tvd, shoe, waterDepth, poreGradient, fracGradient, inclinationDeg, combinedCasing)
    val density = Units.ppgToKgM3(mudPpg)
    val mode = kickMode ?: if (kickTrigger > 0.0) KickTriggerMode.TIME else KickTriggerMode.IMMEDIATE
    val zoneTop = zoneTopMdM ?: well.mdM
    val zoneBase = zoneBaseMdM ?: zoneTop
    return Scenario(
        id = id,
        title = title,
        titleEn = titleEn,
        subtitle = subtitle,
        subtitleEn = subtitleEn,
        description = description,
        descriptionEn = descriptionEn,
        stackType = StackType.SUBSEA,
        method = method,
        difficulty = difficulty,
        objectives = objectives,
        objectivesEn = objectivesEn,
        briefing = briefing,
        briefingEn = briefingEn,
        well = well,
        drillString = pipeString(well.mdM, combinedCasing),
        mud = Mud(density, 22.0, 10.0),
        formation = Formation(
            porePressureKPa = poreGradient * tvd,
            fracPressureKPa = well.shoeFracPressureKPa,
            productivityIndexM3SPerKPa = 1.5e-6,
            lossIndexM3SPerKPa = 2.5e-6,
            influxType = kickType,
            layers = well.formations
        ),
        rig = subseaRig(),
        initialBitDepthM = initialBitDepthM ?: well.mdM,
        initialPitM3 = 220.0,
        initialTripTankM3 = 8.0,
        initialSpm = initialSpm,
        scrubPressureKPa = 2200.0,
        killRateLpm = 1100.0,
        kick = KickConfig(
            enabled = kickEnabled,
            intensityKPa = 750.0,
            volumeM3 = kickVolume,
            depthM = zoneBase,
            type = kickType,
            triggerAtSec = kickTrigger,
            triggerMode = if (kickEnabled) mode else KickTriggerMode.MANUAL,
            zoneTopMdM = zoneTop,
            zoneBaseMdM = zoneBase,
            triggerDepthMdM = triggerDepthMdM ?: zoneTop,
            maxVolumeM3 = kickVolume,
            characteristicRateLpm = defaultInfluxRateLpm(kickType)
        ),
        events = events,
        initialBopClosed = initialBopClosed,
        initialHcrOpen = initialHcrOpen,
        limits = ScenarioLimits(
            maxCasingKPa = 30000.0,
            maxShoeKPa = well.shoeFracPressureKPa,
            maxPitGainM3 = 10.0,
            minBhpKPa = poreGradient * tvd - 1800.0
        )
    )
}

object Scenarios {
    val all: List<Scenario> = listOf(
        baseSurfaceScenario(
            id = "fundamentos",
            title = "Fundamentos: pressões e lama",
            titleEn = "Fundamentals: pressures and mud",
            subtitle = "Pressão hidrostática, ECD e MAASP",
            subtitleEn = "Hydrostatic pressure, ECD and MAASP",
            description = "Aprenda a relação entre peso de lama, profundidade e pressão. O poço está estável; observe SPP, ECD e MAASP enquanto circula.",
            descriptionEn = "Learn the relationship between mud weight, depth and pressure. The well is stable; observe SPP, ECD and MAASP while circulating.",
            method = "Tutorial",
            difficulty = 1,
            objectives = listOf("Circular e observar SPP", "Comparar ECD com o peso de lama", "Identificar a MAASP"),
            objectivesEn = listOf("Circulate and observe SPP", "Compare ECD with mud weight", "Identify MAASP"),
            briefing = "Poço vertical de 3.000 m. Lama de 9,6 ppg. Sem influxo. Use o console para partir a bomba e acompanhar as pressões.",
            briefingEn = "3,000 m vertical well. 9.6 ppg mud. No influx. Use the console to start the pump and monitor pressures.",
            mudPpg = 9.6,
            poreGradient = 10.8,
            initialSpm = 55.0
        ),
        baseSurfaceScenario(
            id = "kill-sheet",
            title = "Kill sheet na prática",
            titleEn = "Kill sheet practice",
            subtitle = "SIDPP, SICP, KMW, ICP e FCP",
            subtitleEn = "SIDPP, SICP, KMW, ICP and FCP",
            description = "Um kick pequeno foi fechado. Estabilize as pressões, abra o kill sheet e calcule a lama de kill antes de circular.",
            descriptionEn = "A small kick has been shut in. Stabilize pressures, open the kill sheet and calculate kill mud before circulating.",
            method = "Driller's",
            difficulty = 2,
            objectives = listOf("Fechar o poço", "Estabilizar SIDPP/SICP", "Calcular KMW, ICP e FCP"),
            objectivesEn = listOf("Shut in the well", "Stabilize SIDPP/SICP", "Calculate KMW, ICP and FCP"),
            briefing = "Kick de gás de 1,5 m³. Feche o BOP, aguarde a estabilização e use o kill sheet.",
            briefingEn = "1.5 m³ gas kick. Close the BOP, wait for stabilization and use the kill sheet.",
            mudPpg = 9.55,
            kickEnabled = true,
            kickVolume = 1.5,
            kickTrigger = 8.0
        ),
        baseSurfaceScenario(
            id = "drillers-surface",
            title = "Kick perfurando — Driller's",
            titleEn = "Drilling kick — Driller's",
            subtitle = "Detecção, shut-in e circulação",
            subtitleEn = "Detection, shut-in and circulation",
            description = "Ganho de 3 m³ durante a perfuração. Detecte, pare a bomba, feche o BOP e controle o poço pelo método do sondador.",
            descriptionEn = "3 m³ gain while drilling. Detect, stop the pump, close the BOP and control the well using the Driller's method.",
            method = "Driller's",
            difficulty = 2,
            objectives = listOf("Detectar o ganho", "Executar o shut-in", "Circular o kick mantendo pressão"),
            objectivesEn = listOf("Detect the gain", "Perform shut-in", "Circulate the kick while maintaining pressure"),
            briefing = "Kick de gás de 3 m³. O poço está em perfuração com 55 SPM. Ação rápida reduz a migração.",
            briefingEn = "3 m³ gas kick. The well is drilling at 55 SPM. Quick action reduces migration.",
            mudPpg = 9.55,
            kickEnabled = true,
            kickVolume = 3.0,
            kickTrigger = 18.0
        ),
        baseSurfaceScenario(
            id = "ww-surface",
            title = "Kick perfurando — Wait & Weight",
            titleEn = "Drilling kick — Wait & Weight",
            subtitle = "Pesar a lama antes de circular",
            subtitleEn = "Weight up before circulating",
            description = "Kick fechado em poço de 3.200 m. Pese a lama até a KMW calculada e desloque com o schedule de SPP.",
            descriptionEn = "Shut-in kick in a 3,200 m well. Weight the mud to the calculated KMW and displace with the SPP schedule.",
            method = "Wait & Weight",
            difficulty = 3,
            objectives = listOf("Estabilizar o poço", "Pesar a lama para KMW", "Seguir o schedule de SPP"),
            objectivesEn = listOf("Stabilize the well", "Weight mud to KMW", "Follow the SPP schedule"),
            briefing = "Kick de gás de 2,2 m³. A lama atual é 9,7 ppg. Calcule a KMW e prepare a lama antes de abrir o choke.",
            briefingEn = "2.2 m³ gas kick. Current mud is 9.7 ppg. Calculate KMW and prepare mud before opening the choke.",
            mudPpg = 9.7,
            kickEnabled = true,
            kickVolume = 2.2,
            kickTrigger = 15.0,
            tvd = 3200.0,
            shoe = 2100.0
        ),
        baseSurfaceScenario(
            id = "swab-trip",
            title = "Kick de manobra (swab)",
            titleEn = "Trip kick (swab)",
            subtitle = "Trip tank, flow check e shut-in",
            subtitleEn = "Trip tank, flow check and shut-in",
            description = "Durante a retirada da coluna, o swab reduz a pressão de fundo. Monitore o trip tank e feche o poço.",
            descriptionEn = "While pulling out, swab reduces bottom-hole pressure. Monitor the trip tank and shut in.",
            method = "Driller's",
            difficulty = 3,
            objectives = listOf("Monitorar o trip tank", "Fazer flow check", "Fechar e estabilizar"),
            objectivesEn = listOf("Monitor the trip tank", "Perform flow check", "Shut in and stabilize"),
            briefing = "A coluna está sendo retirada. Use os botões de manobra e observe o trip tank; o kick pode aparecer sem perfuração.",
            briefingEn = "The string is being pulled. Use the trip buttons and watch the trip tank; the kick can appear without drilling.",
            mudPpg = 9.8,
            kickEnabled = true,
            kickVolume = 1.0,
            kickTrigger = 0.0,
            kickMode = KickTriggerMode.MANUAL,
            initialSpm = 0.0
        ),
        baseSurfaceScenario(
            id = "volumetric",
            title = "Gás migrando — volumétrico",
            titleEn = "Gas migration — volumetric",
            subtitle = "Pumps off, sangria controlada",
            subtitleEn = "Pumps off, controlled bleed",
            description = "Com as bombas paradas, o gás migra e a pressão de revestimento sobe. Aplique o método volumétrico: sangria pequena e pressão controlada.",
            descriptionEn = "With pumps off, gas migrates and casing pressure rises. Apply the volumetric method: small bleed and controlled pressure.",
            method = "Volumétrico",
            difficulty = 4,
            objectives = listOf("Manter o poço fechado", "Sangrar volume controlado", "Evitar MAASP"),
            objectivesEn = listOf("Keep the well shut in", "Bleed controlled volume", "Avoid MAASP"),
            briefing = "Kick de gás de 2 m³, bombas paradas. O gás vai migrar. Use sangrias de 0,2 m³ e acompanhe o casing.",
            briefingEn = "2 m³ gas kick, pumps off. Gas will migrate. Use 0.2 m³ bleeds and monitor casing pressure.",
            mudPpg = 9.6,
            kickEnabled = true,
            kickVolume = 2.0,
            kickTrigger = 5.0,
            initialSpm = 0.0
        ),
        baseSurfaceScenario(
            id = "lubricate",
            title = "Lubrificar e sangrar",
            titleEn = "Lubricate and bleed",
            subtitle = "Coluna travada com gás",
            subtitleEn = "Stuck pipe with gas",
            description = "A coluna está travada e há gás no anular. Lubrifique e sangre mantendo a pressão de fundo acima da pressão de poros.",
            descriptionEn = "The string is stuck and gas is in the annulus. Lubricate and bleed while keeping bottom-hole pressure above pore pressure.",
            method = "Lubrificar & sangrar",
            difficulty = 4,
            objectives = listOf("Não abrir o poço", "Lubrificar em pequenos volumes", "Manter BHP > poros"),
            objectivesEn = listOf("Do not open the well", "Lubricate in small volumes", "Keep BHP > pore"),
            briefing = "Kick de gás de 1,5 m³ e coluna travada. O BOP está fechado. Use lubrificação e sangria.",
            briefingEn = "1.5 m³ gas kick and stuck string. The BOP is closed. Use lubrication and bleed.",
            mudPpg = 9.7,
            kickEnabled = true,
            kickVolume = 1.5,
            kickTrigger = 10.0,
            events = listOf(ScenarioEvent(35.0, SimEventType.STUCK_PIPE))
        ),
        baseSurfaceScenario(
            id = "losses",
            title = "Perda de circulação e kick",
            titleEn = "Lost circulation and kick",
            subtitle = "Limites da sapata e lama",
            subtitleEn = "Shoe limits and mud",
            description = "O poço começa a perder lama e a pressão de fundo cai. Evite transformar perda em kick e mantenha a sapata abaixo da fratura.",
            descriptionEn = "The well starts losing mud and bottom-hole pressure drops. Avoid turning losses into a kick and keep the shoe below fracture.",
            method = "Driller's",
            difficulty = 4,
            objectives = listOf("Identificar a perda", "Reduzir peso/ECD", "Evitar influxo"),
            objectivesEn = listOf("Identify losses", "Reduce weight/ECD", "Avoid influx"),
            briefing = "Perda de 2 m³ no pit aos 20 s. O kick pode ocorrer se a pressão cair demais.",
            briefingEn = "2 m³ pit loss at 20 s. A kick may occur if pressure drops too far.",
            mudPpg = 10.2,
            poreGradient = 11.2,
            kickEnabled = true,
            kickVolume = 0.5,
            kickTrigger = 45.0,
            events = listOf(ScenarioEvent(20.0, SimEventType.LOSSES, 2.0))
        ),
        baseSurfaceScenario(
            id = "failures",
            title = "Falhas de equipamento",
            titleEn = "Equipment failures",
            subtitle = "Bomba e choke",
            subtitleEn = "Pump and choke",
            description = "Kick com falha de bomba e choke plugado. Diagnostique, use o choke reserva e mantenha o controle.",
            descriptionEn = "Kick with pump failure and plugged choke. Diagnose, use the backup choke and maintain control.",
            method = "Driller's",
            difficulty = 4,
            objectives = listOf("Diagnosticar a falha", "Usar o choke reserva", "Controlar a pressão"),
            objectivesEn = listOf("Diagnose the failure", "Use the backup choke", "Control pressure"),
            briefing = "Kick de 2,5 m³. Aos 30 s a bomba 1 falha; aos 50 s o choke A pluga.",
            briefingEn = "2.5 m³ kick. At 30 s pump 1 fails; at 50 s choke A plugs.",
            mudPpg = 9.6,
            kickEnabled = true,
            kickVolume = 2.5,
            kickTrigger = 12.0,
            events = listOf(
                ScenarioEvent(30.0, SimEventType.PUMP_FAILURE),
                ScenarioEvent(50.0, SimEventType.CHOKE_PLUGGED)
            )
        ),
        baseSubseaScenario(
            id = "subsea-drillers",
            title = "Submarino — Driller's",
            titleEn = "Subsea — Driller's",
            subtitle = "BOP no fundo, choke line e riser",
            subtitleEn = "Subsea BOP, choke line and riser",
            description = "Kick em lâmina d'água de 1.200 m. Feche o BOP subsea, estabilize e circule pelo método do sondador corrigindo a choke line.",
            descriptionEn = "Kick in 1,200 m water depth. Close the subsea BOP, stabilize and circulate with the Driller's method correcting choke line friction.",
            method = "Driller's",
            difficulty = 4,
            objectives = listOf("Fechar o BOP subsea", "Corrigir fricção da choke line", "Circular sem exceder MASP"),
            objectivesEn = listOf("Close the subsea BOP", "Correct choke line friction", "Circulate without exceeding MASP"),
            briefing = "Poço de 4.500 m TVD, lâmina de 1.200 m, lama 10,3 ppg. Kick de gás de 2,5 m³. A choke line adiciona fricção.",
            briefingEn = "4,500 m TVD well, 1,200 m water depth, 10.3 ppg mud. 2.5 m³ gas kick. The choke line adds friction.",
            mudPpg = 10.3,
            kickEnabled = true,
            kickVolume = 2.5,
            kickTrigger = 22.0
        ),
        baseSubseaScenario(
            id = "subsea-ww",
            title = "Submarino — Wait & Weight",
            titleEn = "Subsea — Wait & Weight",
            subtitle = "Riser margin e MASP",
            subtitleEn = "Riser margin and MASP",
            description = "Kick submarino com poço profundo. Pese a lama, acompanhe a MASP e mantenha a margem do riser.",
            descriptionEn = "Deepwater subsea kick. Weight the mud, monitor MASP and maintain riser margin.",
            method = "Wait & Weight",
            difficulty = 5,
            objectives = listOf("Calcular KMW", "Pesar a lama", "Respeitar MASP e riser margin"),
            objectivesEn = listOf("Calculate KMW", "Weight the mud", "Respect MASP and riser margin"),
            briefing = "Kick de 2 m³ em 4.800 m TVD, lâmina 1.500 m, lama 10,2 ppg. A janela operacional é estreita.",
            briefingEn = "2 m³ kick at 4,800 m TVD, 1,500 m water depth, 10.2 ppg mud. The operating window is narrow.",
            mudPpg = 10.2,
            tvd = 4800.0,
            shoe = 3800.0,
            waterDepth = 1500.0,
            kickEnabled = true,
            kickVolume = 2.0,
            kickTrigger = 18.0
        ),
        baseSubseaScenario(
            id = "subsea-eds",
            title = "Submarino — EDS e emergência",
            titleEn = "Subsea — EDS and emergency",
            subtitle = "Desconexão, gás no riser e limite",
            subtitleEn = "Disconnect, riser gas and limits",
            description = "Perda de energia e gás no riser. Avalie EDS, feche o BOP e proteja a sonda. Cenário avançado de decisão.",
            descriptionEn = "Power loss and riser gas. Evaluate EDS, close the BOP and protect the rig. Advanced decision scenario.",
            method = "Emergência",
            difficulty = 5,
            objectives = listOf("Reconhecer a emergência", "Fechar BOP/EDS", "Evitar gás na sonda"),
            objectivesEn = listOf("Recognize the emergency", "Close BOP/EDS", "Avoid gas on the rig floor"),
            briefing = "Kick submarino de 3 m³. Aos 40 s há perda de energia e o gás chega ao riser. Decida rápido.",
            briefingEn = "3 m³ subsea kick. At 40 s power is lost and gas reaches the riser. Decide quickly.",
            mudPpg = 10.4,
            kickEnabled = true,
            kickVolume = 3.0,
            kickTrigger = 15.0,
            events = listOf(ScenarioEvent(40.0, SimEventType.POWER_LOSS))
        ),
        baseSurfaceScenario(
            id = "assessment",
            title = "Avaliação final",
            titleEn = "Final assessment",
            subtitle = "Kick oculto, falhas e score",
            subtitleEn = "Hidden kick, failures and score",
            description = "Avaliação integrada: detecção, shut-in, kill sheet, método e debrief. Sem dicas durante a execução.",
            descriptionEn = "Integrated assessment: detection, shut-in, kill sheet, method and debrief. No hints during execution.",
            method = "Avaliação",
            difficulty = 5,
            objectives = listOf(
                "Tomar SCR na taxa de kill",
                "Monitorar WOB, ROP, torque e SPP",
                "Detectar o kick e fechar o poço",
                "Controlar o poço com score ≥ 70"
            ),
            objectivesEn = listOf(
                "Take SCR at kill rate",
                "Monitor WOB, ROP, torque and SPP",
                "Detect kick and shut in",
                "Control well with score ≥ 70"
            ),
            briefing = "Poço de 3.400 m. Kick de intensidade desconhecida. Falhas podem ocorrer. O debrief mostra o resultado.",
            briefingEn = "3,400 m well. Kick of unknown intensity. Failures may occur. The debrief shows the result.",
            mudPpg = 9.65,
            tvd = 3400.0,
            shoe = 2200.0,
            kickEnabled = true,
            kickVolume = 2.8,
            kickTrigger = 25.0,
            events = listOf(
                ScenarioEvent(45.0, SimEventType.CHOKE_WASHED),
                ScenarioEvent(70.0, SimEventType.PUMP_FAILURE)
            )
        ),
        baseSurfaceScenario(
            id = "directional-hp",
            title = "Direcional — zona de alta pressão",
            titleEn = "Directional — high-pressure zone",
            subtitle = "TMD/TVD, inclinação e kick na zona",
            subtitleEn = "MD/TVD, inclination and zone kick",
            description = "Poço build-and-hold com 34° de inclinação. A zona de alta pressão está próxima ao TD; acompanhe TMD/TVD, WOB e ROP até o kick.",
            descriptionEn = "Build-and-hold well at 34° inclination. The high-pressure zone is near TD; monitor MD/TVD, WOB and ROP up to the kick.",
            method = "Driller's",
            difficulty = 4,
            objectives = listOf("Acompanhar TMD e TVD", "Perfurar com WOB/ROP controlados", "Detectar o kick na zona HP"),
            objectivesEn = listOf("Monitor MD and TVD", "Drill with controlled WOB/ROP", "Detect the kick in the HP zone"),
            briefing = "Poço direcional de 3.200 m TVD e cerca de 3.500 m TMD. Lama 9,4 ppg; a zona HP começa perto de 3.180 m TMD.",
            briefingEn = "Directional well at 3,200 m TVD and about 3,500 m MD. 9.4 ppg mud; the HP zone starts near 3,180 m TVD.",
            mudPpg = 9.4,
            poreGradient = 11.8,
            fracGradient = 18.2,
            tvd = 3200.0,
            shoe = 2200.0,
            inclinationDeg = 34.0,
            kickEnabled = true,
            kickVolume = 3.2,
            kickTrigger = 24.0,
            kickMode = KickTriggerMode.TIME,
            zoneTopMdM = 3350.0,
            zoneBaseMdM = 3460.0,
            initialSpm = 60.0
        ),
        baseSubseaScenario(
            id = "subsea-choke-line",
            title = "Submarino — gás pela choke line",
            titleEn = "Subsea — gas through choke line",
            subtitle = "BOP fechado, HCR aberto, riser isolado",
            subtitleEn = "BOP closed, HCR open, isolated riser",
            description = "Poço submarino fechado com HCR aberto. O influxo sobe pelo anular e deve sair pela choke line até o manifold, nunca pelo riser acima do BOP.",
            descriptionEn = "Shut-in subsea well with HCR open. The influx rises through the annulus and must exit through the choke line to the manifold, never through the riser above the BOP.",
            method = "Driller's",
            difficulty = 4,
            objectives = listOf("Confirmar o caminho pela choke line", "Monitorar topo e base do gás", "Sangrar gás no manifold"),
            objectivesEn = listOf("Confirm the choke-line path", "Monitor gas top and base", "Bleed gas at the manifold"),
            briefing = "Lâmina d'água 1.300 m, BOP fechado e HCR aberto. Observe o painel Kick: o gás vai para a choke line, não para o riser.",
            briefingEn = "1,300 m water depth, BOP closed and HCR open. Watch the Kick panel: gas goes to the choke line, not the riser.",
            mudPpg = 10.3,
            poreGradient = 12.6,
            fracGradient = 18.4,
            tvd = 4200.0,
            shoe = 3200.0,
            waterDepth = 1300.0,
            kickEnabled = true,
            kickVolume = 3.0,
            kickTrigger = 0.0,
            kickMode = KickTriggerMode.IMMEDIATE,
            initialSpm = 0.0,
            initialBopClosed = listOf(BopElement.ANNULAR, BopElement.PIPE_RAM),
            initialHcrOpen = true
        ),
        baseSurfaceScenario(
            id = "depth-trigger",
            title = "Kick por profundidade",
            titleEn = "Depth-triggered kick",
            subtitle = "Zona HP e gatilho por TMD",
            subtitleEn = "HP zone and MD trigger",
            description = "A broca está a poucos metros da zona de alta pressão. O kick só dispara quando a profundidade medida atinge o topo da zona, com a lama subbalanceada.",
            descriptionEn = "The bit is a few metres above the high-pressure zone. The kick only triggers when measured depth reaches the zone top, with underbalanced mud.",
            method = "Driller's",
            difficulty = 3,
            objectives = listOf("Perfurar até a zona HP", "Confirmar o gatilho por profundidade", "Fechar o poço"),
            objectivesEn = listOf("Drill to the HP zone", "Confirm the depth trigger", "Shut the well in"),
            briefing = "A broca está 0,5 m acima do gatilho. Perfure com WOB e RPM; o kick dispara ao atingir a profundidade da zona.",
            briefingEn = "The bit is 0.5 m above the trigger. Drill with WOB and RPM; the kick triggers when the zone depth is reached.",
            mudPpg = 9.6,
            poreGradient = 11.7,
            tvd = 3000.0,
            shoe = 2000.0,
            kickEnabled = true,
            kickVolume = 2.5,
            kickTrigger = 0.0,
            kickMode = KickTriggerMode.DEPTH,
            zoneTopMdM = 2999.5,
            zoneBaseMdM = 3100.0,
            triggerDepthMdM = 2999.5,
            initialBitDepthM = 2999.0,
            initialSpm = 55.0
        ),
        baseSubseaScenario(
            id = "narrow-window",
            title = "Águas profundas — janela estreita",
            titleEn = "Deepwater — narrow window",
            subtitle = "Poros altos, fratura baixa e MASP",
            subtitleEn = "High pore, low fracture and MASP",
            description = "Poço profundo com janela operacional muito estreita. Controle a pressão de revestimento e o ECD para não perder lama nem fraturar a sapata.",
            descriptionEn = "Deep well with a very narrow operating window. Control casing pressure and ECD to avoid losses and shoe fracturing.",
            method = "Wait & Weight",
            difficulty = 5,
            objectives = listOf("Calcular KMW com margem", "Pesar a lama sem exceder a fratura", "Manter BHP na janela"),
            objectivesEn = listOf("Calculate KMW with margin", "Weight mud without exceeding fracture", "Keep BHP in the window"),
            briefing = "5.200 m TVD, lâmina de 1.800 m, lama 10,5 ppg. A diferença entre poros e fratura é pequena; cada ação no choke importa.",
            briefingEn = "5,200 m TVD, 1,800 m water depth, 10.5 ppg mud. The pore-to-fracture margin is small; every choke action matters.",
            mudPpg = 10.5,
            poreGradient = 12.75,
            fracGradient = 14.6,
            tvd = 5200.0,
            shoe = 4200.0,
            waterDepth = 1800.0,
            kickEnabled = true,
            kickVolume = 2.2,
            kickTrigger = 20.0
        ),
        baseSurfaceScenario(
            id = "combined-casing",
            title = "Casing combinado e liner",
            titleEn = "Combined casing and liner",
            subtitle = "Sapata intermediário, liner 7\" e zona HP",
            subtitleEn = "Intermediate shoe, 7\" liner and HP zone",
            description = "Poço com casing combinado e liner de produção. Verifique volumes, sapatas e o caminho do influxo em geometria com múltiplos diâmetros.",
            descriptionEn = "Well with combined casing and production liner. Check volumes, shoes and influx path in a multi-diameter geometry.",
            method = "Driller's",
            difficulty = 4,
            objectives = listOf("Identificar cada casing e sapata", "Calcular strokes com liner", "Controlar o kick"),
            objectivesEn = listOf("Identify each casing and shoe", "Calculate strokes with liner", "Control the kick"),
            briefing = "3.600 m TVD com 13 3/8\", 9 5/8\" e liner 7\". Lama 9,8 ppg e kick de 2,4 m³ na zona HP.",
            briefingEn = "3,600 m TVD with 13 3/8\", 9 5/8\" and 7\" liner. 9.8 ppg mud and a 2.4 m³ kick in the HP zone.",
            mudPpg = 9.8,
            poreGradient = 11.7,
            fracGradient = 18.3,
            tvd = 3600.0,
            shoe = 2500.0,
            inclinationDeg = 20.0,
            combinedCasing = true,
            kickEnabled = true,
            kickVolume = 2.4,
            kickTrigger = 18.0,
            zoneTopMdM = 3300.0,
            zoneBaseMdM = 3450.0
        ),
        baseSurfaceScenario(
            id = "sandbox",
            title = "Sandbox",
            titleEn = "Sandbox",
            subtitle = "Poço livre para experimentar",
            subtitleEn = "Free well for experimentation",
            description = "Sem kick inicial, sem falhas e sem pontuação rígida. Parta a bomba, ajuste o choke e observe a dinâmica.",
            descriptionEn = "No initial kick, no failures and no strict scoring. Start the pump, adjust the choke and observe the dynamics.",
            method = "Sandbox",
            difficulty = 1,
            objectives = listOf("Explorar os painéis", "Testar o choke", "Criar um kick manualmente"),
            objectivesEn = listOf("Explore the panels", "Test the choke", "Create a kick manually"),
            briefing = "Poço de 2.800 m com lama 9,8 ppg. Use o modo instrutor para injetar um kick.",
            briefingEn = "2,800 m well with 9.8 ppg mud. Use instructor mode to inject a kick.",
            mudPpg = 9.8,
            poreGradient = 10.2,
            kickEnabled = false,
            initialSpm = 40.0
        )
    )

    fun byId(id: String): Scenario = all.first { it.id == id }

    fun custom(spec: CustomWellSpec): Scenario {
        val tvd = spec.tvdM.coerceIn(300.0, 9000.0)
        val md = spec.mdM.coerceIn(tvd, 12000.0)
        val shoe = spec.shoeTvdM.coerceIn(100.0, tvd - 10.0)
        val inclination = spec.inclinationDeg.coerceIn(0.0, 80.0)
        val water = if (spec.surfaceType == SurfaceType.SUBSEA) {
            spec.waterDepthM.coerceIn(50.0, tvd * 0.75)
        } else 0.0
        val baseWell = if (spec.surfaceType == SurfaceType.SUBSEA) {
            subseaWell(
                spec.name, tvd, shoe, water,
                spec.poreGradientKPaPerM, spec.fracGradientKPaPerM,
                inclination, spec.combinedCasing, md
            )
        } else {
            surfaceWell(
                spec.name, tvd, shoe,
                spec.poreGradientKPaPerM, spec.fracGradientKPaPerM,
                inclination, spec.combinedCasing, md
            )
        }

        val customCasings = if (spec.customCasings.isNotEmpty()) {
            val valid = spec.customCasings.mapNotNull { casing ->
                val top = casing.topMdM.coerceIn(0.0, md)
                val bottom = casing.bottomMdM.coerceIn(0.0, md)
                val id = casing.idIn
                val od = casing.odIn
                val hole = casing.holeDiameterIn
                if (bottom <= top || id <= 0.0 || od <= id || hole <= id) {
                    null
                } else {
                    CasingString(
                        name = casing.name,
                        topMdM = top,
                        bottomMdM = bottom,
                        idIn = id,
                        // A sapata é sempre derivada da trajetória; TMD digitado
                        // na UI nunca é tratado como TVD.
                        shoeTvdM = baseWell.tvdAtMd(bottom),
                        odIn = od,
                        holeDiameterIn = hole
                    )
                }
            }
            valid.ifEmpty { baseWell.casings }
        } else baseWell.casings

        val lines = WellLines(
            killLineIdIn = spec.killLineIdIn,
            killLineLengthM = if (spec.killLineLengthM > 0) spec.killLineLengthM else (water + 40.0).coerceAtLeast(100.0),
            chokeLineIdIn = spec.chokeLineIdIn,
            chokeLineLengthM = if (spec.chokeLineLengthM > 0) spec.chokeLineLengthM else (water + 40.0).coerceAtLeast(100.0),
            boosterLineIdIn = spec.boosterLineIdIn,
            boosterLineLengthM = if (spec.boosterLineLengthM > 0) spec.boosterLineLengthM else (water + 40.0).coerceAtLeast(100.0)
        )

        val riser = if (spec.surfaceType == SurfaceType.SUBSEA) {
            RiserConfig(
                waterDepthM = water,
                airGapM = 28.0,
                riserIdIn = spec.riserIdIn,
                chokeLineIdIn = spec.chokeLineIdIn,
                chokeLineLengthM = lines.chokeLineLengthM,
                killLineIdIn = spec.killLineIdIn,
                killLineLengthM = lines.killLineLengthM,
                boosterLineIdIn = spec.boosterLineIdIn,
                boosterLineLengthM = lines.boosterLineLengthM,
                riserOdIn = spec.riserOdIn
            )
        } else null

        val triggerDepth = CustomWellDefaults.triggerDepthMd(spec.triggerDepthMdM, baseWell.mdM)
        val requestedZoneTop = CustomWellDefaults.zoneTopMd(spec.zoneTopMdM, triggerDepth, baseWell.mdM)
        val requestedZoneBase = CustomWellDefaults.zoneBaseMd(spec.zoneBaseMdM, requestedZoneTop, baseWell.mdM)
        // A zona de kick é um intervalo interno mais fino que a formação
        // portadora. Se o usuário informar a formação inteira, reduzimos para
        // 80% da espessura da portadora, centrado no intervalo pedido.
        val carrier = baseWell.formations.firstOrNull { layer ->
            layer.carrierZone &&
                layer.baseMdM >= requestedZoneTop && layer.topMdM <= requestedZoneBase
        }
        val (zoneTop, zoneBase) = if (carrier != null) {
            val carrierTop = carrier.topMdM
            val carrierBase = carrier.baseMdM
            val maxThickness = (carrierBase - carrierTop) * 0.8
            var top = requestedZoneTop.coerceIn(carrierTop, carrierBase)
            var base = requestedZoneBase.coerceIn(carrierTop, carrierBase)
            if (base - top < 5.0) {
                val center = ((top + base) / 2.0).coerceIn(carrierTop, carrierBase)
                top = (center - 5.0).coerceAtLeast(carrierTop)
                base = (center + 5.0).coerceAtMost(carrierBase)
            }
            if (base - top > maxThickness) {
                val center = (top + base) / 2.0
                top = (center - maxThickness / 2.0).coerceAtLeast(carrierTop)
                base = (center + maxThickness / 2.0).coerceAtMost(carrierBase)
            }
            top to base
        } else requestedZoneTop to requestedZoneBase
        val carrierGradient = carrier?.poreGradientKPaPerM ?: spec.poreGradientKPaPerM
        val zoneGradient = if (spec.zonePoreGradientKPaPerM > 0.0) {
            maxOf(spec.zonePoreGradientKPaPerM, carrierGradient)
        } else 0.0
        val zonePermeability = if (spec.zonePermeabilityMd > 0.0) {
            spec.zonePermeabilityMd
        } else carrier?.permeabilityMd ?: 0.0
        val alignedFormations = baseWell.formations.map { layer ->
            val overlapsZone = layer.baseMdM >= zoneTop && layer.topMdM <= zoneBase
            layer.copy(
                kickZone = overlapsZone || layer.kickZone,
                carrierZone = overlapsZone || layer.carrierZone
            )
        }

        val well = baseWell.copy(
            openHoleDiameterIn = spec.openHoleDiameterIn,
            casings = customCasings,
            riser = riser,
            tempSurfaceC = spec.tempSurfaceC,
            tempBottomC = spec.tempBottomC,
            seaSurfaceTempC = spec.seaSurfaceTempC,
            seaMudlineTempC = spec.seaMudlineTempC,
            lines = lines,
            formations = alignedFormations
        )

        val dsSpec = spec.drillStringSpec
        val drillString = if (dsSpec != null) {
            val bha = dsSpec.bhaLengthM.coerceIn(20.0, md * 0.5)
            val dpLen = (md - bha).coerceAtLeast(10.0)
            DrillString(
                segments = listOf(
                    PipeSegment("Drill pipe", dpLen, dsSpec.drillPipeOdIn, dsSpec.drillPipeIdIn),
                    PipeSegment("HWDP", bha * 0.45, dsSpec.hwdpOdIn, dsSpec.hwdpIdIn),
                    PipeSegment("Drill collar", bha * 0.55, dsSpec.drillCollarOdIn, dsSpec.drillCollarIdIn)
                ),
                bitTfaIn2 = dsSpec.bitTfaIn2
            )
        } else pipeString(well.mdM, spec.combinedCasing)

        val density = Units.ppgToKgM3(spec.mudPpg.coerceIn(8.0, 18.0))
        val mode = if (spec.kickEnabled) spec.kickTriggerMode else KickTriggerMode.MANUAL
        val requestedStart = CustomWellDefaults.startDepthMd(
            spec.initialBitDepthMdM,
            well.mdM,
            zoneTop,
            triggerDepth,
            spec.kickEnabled
        )
        // Em gatilho por profundidade/híbrido a broca nunca pode nascer no
        // gatilho; o editor bloqueia esse caso, e o motor se protege aqui
        // para não injetar kick no primeiro passo.
        val startDepth = if (
            spec.kickEnabled &&
            (mode == KickTriggerMode.DEPTH || mode == KickTriggerMode.HYBRID) &&
            triggerDepth > 0.0
        ) {
            requestedStart.coerceAtMost((triggerDepth - 0.5).coerceAtLeast(0.0))
        } else requestedStart
        val kickDepth = CustomWellDefaults.kickDepthMd(
            spec.kickDepthMdM,
            well.mdM,
            zoneTop,
            zoneBase,
            triggerDepth
        )
        val intensity = CustomWellDefaults.intensityKPa(spec.kickIntensityKPa)

        return Scenario(
            id = spec.id,
            title = spec.name,
            titleEn = spec.name,
            subtitle = "Poço personalizado · ${if (spec.surfaceType == SurfaceType.SUBSEA) "submarino" else "superfície"}",
            subtitleEn = "Custom well · ${if (spec.surfaceType == SurfaceType.SUBSEA) "subsea" else "surface"}",
            description = "Poço criado no editor local. Ajuste WOB, RPM, lama e choke; o kick segue o gatilho configurado.",
            descriptionEn = "Well created in the local editor. Adjust WOB, RPM, mud and choke; the kick follows the configured trigger.",
            stackType = if (spec.surfaceType == SurfaceType.SUBSEA) StackType.SUBSEA else StackType.SURFACE,
            method = "Custom",
            difficulty = 3,
            objectives = listOf("Perfurar e monitorar o poço", "Detectar o kick configurado", "Fechar e circular com segurança"),
            objectivesEn = listOf("Drill and monitor the well", "Detect the configured kick", "Shut in and circulate safely"),
            briefing = "Poço personalizado com ${Units.fmt(tvd)} m TVD e ${Units.fmt(md)} m TMD. Lama ${Units.fmt(spec.mudPpg, 1)} ppg.",
            briefingEn = "Custom well with ${Units.fmt(tvd)} m TVD and ${Units.fmt(md)} m MD. ${Units.fmt(spec.mudPpg, 1)} ppg mud.",
            well = well,
            drillString = drillString,
            mud = Mud(
                densityKgM3 = density,
                plasticViscosityCp = 20.0,
                yieldPointPa = 9.0,
                killMarginKgM3 = Units.ppgToKgM3(spec.killMarginPpg.coerceIn(0.0, 1.0))
            ),
            formation = Formation(
                porePressureKPa = spec.poreGradientKPaPerM * tvd,
                fracPressureKPa = well.shoeFracPressureKPa,
                productivityIndexM3SPerKPa = 1.8e-6,
                lossIndexM3SPerKPa = 2.8e-6,
                influxType = spec.kickType,
                layers = well.formations
            ),
            rig = if (spec.surfaceType == SurfaceType.SUBSEA) subseaRig() else surfaceRig(tvd > 2600.0),
            initialBitDepthM = startDepth,
            initialPitM3 = if (spec.surfaceType == SurfaceType.SUBSEA) 220.0 else 150.0,
            initialTripTankM3 = if (spec.surfaceType == SurfaceType.SUBSEA) 8.0 else 6.0,
            initialSpm = spec.initialSpm.coerceIn(0.0, 120.0),
            scrubPressureKPa = if (spec.surfaceType == SurfaceType.SUBSEA) 2200.0 else 1650.0,
            killRateLpm = if (spec.surfaceType == SurfaceType.SUBSEA) 1100.0 else 900.0,
            kick = KickConfig(
                enabled = spec.kickEnabled,
                intensityKPa = intensity,
                volumeM3 = spec.maxKickVolumeM3.coerceIn(0.2, 20.0),
                depthM = kickDepth,
                type = spec.kickType,
                triggerAtSec = spec.triggerAtSec.coerceAtLeast(0.0),
                triggerMode = mode,
                zoneTopMdM = zoneTop,
                zoneBaseMdM = zoneBase,
                triggerDepthMdM = triggerDepth,
                maxVolumeM3 = spec.maxKickVolumeM3.coerceIn(0.2, 20.0),
                characteristicRateLpm = spec.characteristicRateLpm.coerceAtLeast(1.0),
                zonePoreGradientKPaPerM = zoneGradient,
                zonePermeabilityMd = zonePermeability
            ),
            events = emptyList(),
            limits = ScenarioLimits(
                maxCasingKPa = if (spec.surfaceType == SurfaceType.SUBSEA) 30000.0 else 18000.0,
                maxShoeKPa = well.shoeFracPressureKPa,
                maxPitGainM3 = 10.0,
                minBhpKPa = spec.poreGradientKPaPerM * tvd - 1800.0
            )
        )
    }
}
