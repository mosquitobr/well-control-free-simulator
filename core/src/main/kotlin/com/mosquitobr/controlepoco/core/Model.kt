package com.mosquitobr.controlepoco.core

enum class StackType { SURFACE, SUBSEA }

enum class UnitSystem { PETROBRAS, SI, US_FIELD }

enum class InfluxType { GAS, OIL, WATER, SALTWATER }

enum class RigClass { LAND_1500, LAND_2000, DEEPWATER_DRILLSHIP, SEMISUBMERSIBLE }

enum class SurfaceType { LAND, OFFSHORE_SURFACE, SUBSEA }

enum class KickTriggerMode { IMMEDIATE, TIME, DEPTH, HYBRID, MANUAL }

enum class InfluxPath { FORMATION, ANNULUS, BOP, RISER, CHOKE_LINE, KILL_LINE, SURFACE }

enum class FormationType { SANDSTONE, SHALE, LIMESTONE, DOLOMITE, SALT, CONGLOMERATE }

enum class ManifoldLineup { DRILLING, KILL, CHOKE, TRIP, LUBRICATE, EDS, CUSTOM }

/** Caminho de injeção desenhado no esquemático (sempre da superfície para baixo). */
enum class InjectionPath { STANDPIPE, KILL_LINE, CHOKE_LINE, TRIP_LINE, LUBRICATE_LINE, NONE }

/** Caminho de retorno desenhado no esquemático (do poço para a superfície). */
enum class ReturnPath { FLOWLINE, RISER, CHOKE_LINE, KILL_LINE, TRIP_TANK, LUBRICATE_BLEED, BLOCKED }

enum class Phase {
    DRILLING,
    KICK_DETECTED,
    SHUT_IN,
    STABILIZED,
    KILL_PREP,
    CIRCULATING,
    KICK_CONTROLLED,
    DISPLACING_KILL_MUD,
    KILLED,
    FAILED
}

/** Etapas da substituição gradual da lama velha pela lama de matar. */
enum class KillStage {
    NONE,
    FIRST_CIRCULATION,
    KICK_CONTROLLED,
    STRING,
    ANNULUS,
    CHOKE_LINE,
    WELL_KILLED,
    LINE_DISPLACEMENT,
    READY_TO_OPEN
}

/** Linhas auxiliares deslocadas antes de abrir o poço. */
enum class LineKind { KILL_LINE, CHOKE_LINE, BOOSTER_LINE, RISER_ANNULUS }

enum class ScrSource { ESTIMATED, MEASURED }

/**
 * Medições e calibrações exigidas antes do kick (Fase A do guia operacional).
 * A ordem de [values] é a ordem de execução sugerida.
 */
enum class PrepCheckId {
    FLOW_CHECK,
    GAIN_ALARM,
    LOSS_ALARM,
    RETURN_FLOW_ALARM,
    SCR_50,
    SCR_100,
    SCR_150,
    SCR_200,
    DELTA_P_CHOKE_LINE,
    BOP_TEST
}

data class PrepCheck(
    val id: PrepCheckId,
    val done: Boolean,
    val value: Double = 0.0,
    val detailPt: String = "",
    val detailEn: String = ""
)

/** Indícios secundários de kick: aparecem antes dos primários. */
enum class SecondaryIndicator {
    ROP_UP,
    WOB_DOWN,
    SPP_DOWN,
    ECD_UP,
    PUMP_ACCEL,
    RETURN_FLOW_CHANGE
}

/**
 * Condição de kick comparando a pressão de poros equivalente (em densidade)
 * com a hidrostática estática e com a dinâmica (ECD corrigida por temperatura).
 */
enum class KickCondition {
    NONE,
    PORE_GT_HYDROSTATIC,
    PORE_GT_ECD,
    INTERNAL_HIGH_ZONE
}

enum class ProcedureId { SCR, FLOW_CHECK, TRIP_TANK, BOP_TEST, MANIFOLD_LINEUP }

data class ProcedureStep(
    val titlePt: String,
    val titleEn: String,
    val done: Boolean = false
)

data class ProcedureSnapshot(
    val id: ProcedureId,
    val titlePt: String,
    val titleEn: String,
    val steps: List<ProcedureStep>,
    val currentIndex: Int,
    val hintPt: String,
    val hintEn: String,
    val complete: Boolean = false
)

enum class BopElement { ANNULAR, PIPE_RAM, BLIND_SHEAR, TEST_RAM, HCR }

enum class AlarmSeverity { INFO, WARNING, CRITICAL }

enum class SimEventType { KICK, PUMP_FAILURE, CHOKE_PLUGGED, CHOKE_WASHED, ACCUMULATOR_LEAK, LOSSES, STUCK_PIPE, SENSOR_DRIFT, POWER_LOSS }

enum class AlarmCode {
    KICK_DETECTED,
    FLOW_WITH_PUMPS_OFF,
    PIT_GAIN,
    PIT_LOSS,
    RETURN_FLOW_CHANGE,
    SPP_LOW,
    SPP_HIGH,
    CASING_HIGH,
    MAASP_EXCEEDED,
    FRAC_EXCEEDED,
    RETURN_PATH_CLOSED,
    ACCUMULATOR_LOW,
    PUMP_FAILURE,
    CHOKE_PLUGGED,
    CHOKE_WASHED,
    GAS_AT_SURFACE,
    STABILIZED,
    KILL_COMPLETE,
    FAILED,
    INFO
}

data class Alarm(
    val id: Long,
    val code: AlarmCode,
    val severity: AlarmSeverity,
    val timeSec: Double,
    val detail: String = ""
)

data class CasingString(
    val name: String,
    val topMdM: Double,
    val bottomMdM: Double,
    val idIn: Double,
    val shoeTvdM: Double,
    val odIn: Double = idIn + 0.5,
    val holeDiameterIn: Double = idIn
)

data class TrajectoryStation(
    val mdM: Double,
    val incDeg: Double,
    val aziDeg: Double = 0.0,
    val tvdM: Double,
    val northM: Double = 0.0,
    val eastM: Double = 0.0
)

data class FormationLayer(
    val name: String,
    val topMdM: Double,
    val baseMdM: Double,
    val type: FormationType = FormationType.SANDSTONE,
    val hardness: Double = 5.0,
    val poreGradientKPaPerM: Double,
    val fracGradientKPaPerM: Double,
    val porosity: Double = 0.20,
    val permeabilityMd: Double = 100.0,
    val ropFactor: Double = 1.0,
    val kickZone: Boolean = false,
    val carrierZone: Boolean = kickZone
)

data class WellLines(
    val killLineIdIn: Double = 3.0,
    val killLineLengthM: Double = 100.0,
    val chokeLineIdIn: Double = 3.0,
    val chokeLineLengthM: Double = 100.0,
    val boosterLineIdIn: Double = 3.0,
    val boosterLineLengthM: Double = 100.0
)

data class RiserConfig(
    val waterDepthM: Double,
    val airGapM: Double,
    val riserIdIn: Double,
    val chokeLineIdIn: Double,
    val chokeLineLengthM: Double,
    val killLineIdIn: Double = chokeLineIdIn,
    val killLineLengthM: Double = chokeLineLengthM,
    val boosterLineIdIn: Double = 3.0,
    val boosterLineLengthM: Double = chokeLineLengthM,
    val riserOdIn: Double = 21.0
)

data class CasingSpec(
    val name: String = "Revestimento",
    val topMdM: Double = 0.0,
    val bottomMdM: Double = 2000.0,
    val idIn: Double = 8.681,
    val odIn: Double = 9.625,
    val holeDiameterIn: Double = 12.25,
    val shoeTvdM: Double = bottomMdM
)

data class DrillStringSpec(
    val drillPipeOdIn: Double = 5.0,
    val drillPipeIdIn: Double = 4.276,
    val hwdpOdIn: Double = 5.0,
    val hwdpIdIn: Double = 3.0,
    val drillCollarOdIn: Double = 6.5,
    val drillCollarIdIn: Double = 2.25,
    val bhaLengthM: Double = 180.0,
    val bitTfaIn2: Double = 0.48
)

data class WellConfig(
    val name: String,
    val tvdM: Double,
    val mdM: Double,
    val openHoleDiameterIn: Double,
    val casings: List<CasingString>,
    val riser: RiserConfig? = null,
    val poreGradientKPaPerM: Double,
    val fracGradientKPaPerM: Double,
    val tempSurfaceC: Double = 24.0,
    val tempBottomC: Double = 90.0,
    val seaSurfaceTempC: Double = 24.0,
    val seaMudlineTempC: Double = 4.0,
    val lines: WellLines = WellLines(
        killLineIdIn = riser?.killLineIdIn ?: 3.0,
        killLineLengthM = riser?.killLineLengthM ?: 100.0,
        chokeLineIdIn = riser?.chokeLineIdIn ?: 3.0,
        chokeLineLengthM = riser?.chokeLineLengthM ?: 100.0,
        boosterLineIdIn = riser?.boosterLineIdIn ?: 3.0,
        boosterLineLengthM = riser?.boosterLineLengthM ?: 100.0
    ),
    val surfaceType: SurfaceType = if (riser != null) SurfaceType.SUBSEA else SurfaceType.LAND,
    val trajectory: List<TrajectoryStation> = emptyList(),
    val formations: List<FormationLayer> = emptyList(),
    val inclinationDeg: Double = 0.0
) {
    val isSubsea: Boolean get() = riser != null

    /**
     * TVD da sapata exposta mais profunda. As sapatas rasas já estão cimentadas
     * e isoladas pelo revestimento seguinte; o ponto aberto mais profundo é o
     * que limita a MAASP e os strokes até a sapata.
     */
    val shoeTvdM: Double
        get() = (casings.maxOfOrNull { it.shoeTvdM } ?: tvdM).coerceIn(0.0, tvdM)

    /** MD correspondente à sapata exposta, coerente com a trajetória. */
    val shoeMdM: Double get() = mdAtTvd(shoeTvdM).coerceAtMost(mdM)

    /** Gradiente de fratura da formação na sapata exposta. */
    val shoeFracGradientKPaPerM: Double get() = fracGradientAtMd(shoeMdM)

    /** Pressão de fratura na sapata exposta (kPa). */
    val shoeFracPressureKPa: Double get() = shoeFracGradientKPaPerM * shoeTvdM

    val shoeFracKPa: Double get() = shoeFracPressureKPa
    val porePressureKPa: Double get() = porePressureAtMd(mdM)

    fun tvdAtMd(md: Double): Double {
        if (md <= 0.0) return 0.0
        if (trajectory.size >= 2) {
            val sorted = trajectory.sortedBy { it.mdM }
            val first = sorted.first()
            val last = sorted.last()
            if (md <= first.mdM) return first.tvdM
            if (md >= last.mdM) {
                val rad = Math.toRadians(last.incDeg)
                return last.tvdM + (md - last.mdM) * kotlin.math.cos(rad)
            }
            for (i in 0 until sorted.size - 1) {
                val a = sorted[i]
                val b = sorted[i + 1]
                if (md <= b.mdM) {
                    val f = (md - a.mdM) / (b.mdM - a.mdM).coerceAtLeast(1e-9)
                    return a.tvdM + f * (b.tvdM - a.tvdM)
                }
            }
        }
        val ratio = if (mdM > 0.0) tvdM / mdM else 1.0
        return md * ratio
    }

    fun mdAtTvd(tvd: Double): Double {
        val target = tvd.coerceIn(0.0, tvdM)
        if (trajectory.size >= 2) {
            val sorted = trajectory.sortedBy { it.tvdM }
            for (i in 0 until sorted.size - 1) {
                val a = sorted[i]
                val b = sorted[i + 1]
                if (target <= b.tvdM) {
                    val f = (target - a.tvdM) / (b.tvdM - a.tvdM).coerceAtLeast(1e-9)
                    return a.mdM + f * (b.mdM - a.mdM)
                }
            }
        }
        if (tvdM <= 0.0) return 0.0
        return (target / tvdM) * mdM
    }

    fun incAtMd(md: Double): Double {
        if (trajectory.size >= 2) {
            val sorted = trajectory.sortedBy { it.mdM }
            for (i in 0 until sorted.size - 1) {
                val a = sorted[i]
                val b = sorted[i + 1]
                if (md <= b.mdM) {
                    val f = ((md - a.mdM) / (b.mdM - a.mdM).coerceAtLeast(1e-9)).coerceIn(0.0, 1.0)
                    return a.incDeg + f * (b.incDeg - a.incDeg)
                }
            }
            return sorted.last().incDeg
        }
        return inclinationDeg
    }

    fun layerAtMd(md: Double): FormationLayer? {
        if (formations.isEmpty()) return null
        return formations.firstOrNull { md >= it.topMdM && md <= it.baseMdM }
            ?: formations.minByOrNull { kotlin.math.abs((it.topMdM + it.baseMdM) / 2.0 - md) }
    }

    fun poreGradientAtMd(md: Double): Double = layerAtMd(md)?.poreGradientKPaPerM ?: poreGradientKPaPerM

    fun fracGradientAtMd(md: Double): Double = layerAtMd(md)?.fracGradientKPaPerM ?: fracGradientKPaPerM

    fun porePressureAtMd(md: Double): Double = poreGradientAtMd(md) * tvdAtMd(md)

    fun fracPressureAtMd(md: Double): Double = fracGradientAtMd(md) * tvdAtMd(md)
}

data class PipeSegment(
    val name: String,
    val lengthM: Double,
    val odIn: Double,
    val idIn: Double
)

data class DrillString(
    val segments: List<PipeSegment>,
    val bitTfaIn2: Double = 0.5
) {
    val totalLengthM: Double get() = segments.sumOf { it.lengthM }

    fun odAtDepth(depthM: Double): Double {
        var top = 0.0
        for (segment in segments) {
            if (depthM <= top + segment.lengthM) return segment.odIn
            top += segment.lengthM
        }
        return segments.lastOrNull()?.odIn ?: 5.0
    }

    fun idAtDepth(depthM: Double): Double {
        var top = 0.0
        for (segment in segments) {
            if (depthM <= top + segment.lengthM) return segment.idIn
            top += segment.lengthM
        }
        return segments.lastOrNull()?.idIn ?: 4.276
    }
}

data class Mud(
    val densityKgM3: Double,
    val plasticViscosityCp: Double,
    val yieldPointPa: Double,
    val gelPa: Double = 3.0,
    /** Margem de segurança aplicada à lama de matar (padrão 0,1 ppg ≈ 12 kg/m³). */
    val killMarginKgM3: Double = 12.0
)

data class Formation(
    val porePressureKPa: Double,
    val fracPressureKPa: Double,
    val productivityIndexM3SPerKPa: Double,
    val lossIndexM3SPerKPa: Double,
    val influxType: InfluxType,
    val porosity: Double = 0.20,
    val layers: List<FormationLayer> = emptyList()
)

data class PumpConfig(
    val name: String,
    val displacementLPerStroke: Double,
    val maxSpm: Double,
    val maxPressureKPa: Double
)

data class ChokeConfig(
    val name: String,
    val maxAreaM2: Double = 0.00075,
    val cv: Double = 0.85
)

data class BopConfig(
    val elements: List<BopElement>,
    val accumulatorVolumeL: Double = 2400.0,
    val prechargeKPa: Double = 7000.0,
    val ratedPressureKPa: Double = 34500.0
)

data class RigSetup(
    val name: String,
    val rigClass: RigClass,
    val pumps: List<PumpConfig>,
    val chokes: List<ChokeConfig>,
    val bop: BopConfig,
    val standpipeManifold: List<String>,
    val chokeManifold: List<String>
)

/** Vazão característica de entrada de cada natureza de influxo (SI: L/min). */
fun defaultInfluxRateLpm(type: InfluxType): Double = when (type) {
    InfluxType.GAS -> 1200.0
    InfluxType.OIL -> 500.0
    InfluxType.WATER, InfluxType.SALTWATER -> 200.0
}

/**
 * Densidade típica do influxo (SI: kg/m³). É a densidade que define a perda
 * hidrostática no anular e, portanto, o SICP: gás alivia muito mais coluna
 * que óleo, água ou água salgada.
 */
fun defaultInfluxDensityKgM3(type: InfluxType): Double = when (type) {
    InfluxType.GAS -> 90.0
    InfluxType.OIL -> 780.0
    InfluxType.WATER -> 1020.0
    InfluxType.SALTWATER -> 1080.0
}

data class KickConfig(
    val enabled: Boolean,
    val intensityKPa: Double,
    /** Volume máximo potencial do kick. O volume efetivo cresce com o tempo de fechamento. */
    val volumeM3: Double,
    val depthM: Double,
    val type: InfluxType,
    val triggerAtSec: Double = 0.0,
    val triggerMode: KickTriggerMode =
        if (triggerAtSec > 0.0) KickTriggerMode.TIME else KickTriggerMode.IMMEDIATE,
    val zoneTopMdM: Double = depthM,
    val zoneBaseMdM: Double = depthM,
    val triggerDepthMdM: Double = zoneTopMdM,
    /** Limite físico de volume que pode entrar antes do controle total. */
    val maxVolumeM3: Double = volumeM3,
    /** Vazão característica da natureza do influxo (SI: L/min). */
    val characteristicRateLpm: Double = defaultInfluxRateLpm(type),
    /** Gradiente próprio da zona interna. 0 usa o gradiente da formação portadora. */
    val zonePoreGradientKPaPerM: Double = 0.0,
    /** Permeabilidade própria da zona interna. 0 usa a camada portadora. */
    val zonePermeabilityMd: Double = 0.0,
    /** Densidade do influxo (SI: kg/m³). 0 usa o padrão da natureza do influxo. */
    val influxDensityKgM3: Double = 0.0
)

data class ScenarioEvent(
    val atSec: Double,
    val type: SimEventType,
    val value: Double = 0.0,
    val detail: String = ""
)

data class ScenarioLimits(
    val maxCasingKPa: Double,
    val maxShoeKPa: Double,
    val maxPitGainM3: Double,
    val minBhpKPa: Double
)

data class Scenario(
    val id: String,
    val title: String,
    val titleEn: String,
    val subtitle: String,
    val subtitleEn: String,
    val description: String,
    val descriptionEn: String,
    val stackType: StackType,
    val method: String,
    val difficulty: Int,
    val objectives: List<String>,
    val objectivesEn: List<String>,
    val briefing: String,
    val briefingEn: String,
    val well: WellConfig,
    val drillString: DrillString,
    val mud: Mud,
    val formation: Formation,
    val rig: RigSetup,
    val initialBitDepthM: Double,
    val initialPitM3: Double,
    val initialTripTankM3: Double,
    val initialSpm: Double = 0.0,
    val scrubPressureKPa: Double = 1800.0,
    val killRateLpm: Double = 900.0,
    val kick: KickConfig,
    val events: List<ScenarioEvent> = emptyList(),
    val limits: ScenarioLimits,
    val initialBopClosed: List<BopElement> = emptyList(),
    val initialHcrOpen: Boolean = false,
    val instructorNotes: String = ""
) {
    fun title(lang: String) = if (lang == "en") titleEn else title
    fun subtitle(lang: String) = if (lang == "en") subtitleEn else subtitle
    fun description(lang: String) = if (lang == "en") descriptionEn else description
    fun objectives(lang: String) = if (lang == "en") objectivesEn else objectives
    fun briefing(lang: String) = if (lang == "en") briefingEn else briefing
}
