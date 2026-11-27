package com.mosquitobr.controlepoco.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val FLOW_CHECK_STABLE_SEC = 5.0

data class TrendPoint(
    val timeSec: Double,
    val sppKPa: Double,
    val casingKPa: Double,
    val pitM3: Double,
    val chokePct: Double,
    val bhpKPa: Double,
    val shoeKPa: Double,
    val ropMHr: Double = 0.0,
    val influxM3: Double = 0.0
)

data class HydraulicLosses(
    val surfaceKPa: Double = 0.0,
    val stringKPa: Double = 0.0,
    val bitKPa: Double = 0.0,
    val annulusKPa: Double = 0.0,
    val motorKPa: Double = 0.0
) {
    val totalKPa: Double get() = surfaceKPa + stringKPa + bitKPa + annulusKPa + motorKPa
}

data class SimSnapshot(
    val timeSec: Double,
    val phase: Phase,
    val stackType: StackType,
    val method: String,
    val bitDepthM: Double,
    val bitTvdM: Double,
    val holeDepthM: Double,
    val holeTvdM: Double,
    val wobKn: Double,
    val wobKlb: Double,
    val wobSetpointKlb: Double,
    val rpm: Double,
    val rpmSetpoint: Double,
    val rotaryOn: Boolean,
    val copOffsetM: Double,
    val ropMHr: Double,
    val torqueKNm: Double,
    val cuttingsLoad: Double,
    val bitWear: Double,
    val formationName: String,
    val formationType: FormationType,
    val formationHardness: Double,
    val mudDensityKgM3: Double,
    val targetMudDensityKgM3: Double,
    val pitM3: Double,
    val tripTankM3: Double,
    val sppKPa: Double,
    val losses: HydraulicLosses,
    val scrKPa: Double,
    val scrRateLpm: Double,
    val scrTaken: Boolean,
    val scrSource: ScrSource,
    val casingPressureKPa: Double,
    val sidppKPa: Double,
    val sicpKPa: Double,
    val bhpKPa: Double,
    val shoePressureKPa: Double,
    val ecdKgM3: Double,
    val maaspKPa: Double,
    val maspKPa: Double,
    val porePressureKPa: Double,
    val fracPressureKPa: Double,
    val influxM3: Double,
    val influxType: InfluxType,
    val influxDepthM: Double,
    val influxTopTvdM: Double,
    val influxBaseTvdM: Double,
    val influxPath: InfluxPath,
    val kickZoneTopMdM: Double,
    val kickZoneBaseMdM: Double,
    val kickArmed: Boolean,
    val kickTriggered: Boolean,
    val kickTriggerMode: KickTriggerMode,
    val gasAtSurface: Boolean,
    val flowInLpm: Double,
    val flowOutLpm: Double,
    val chokePct: Double,
    val chokeAuto: Boolean,
    val chokeSetpointKPa: Double,
    val hcrOpen: Boolean,
    val pumpOn: Boolean,
    val pumpSpm: Double,
    val pumpRateLpm: Double,
    val pumpPressureKPa: Double,
    val accumulatorKPa: Double,
    val bopClosed: Set<BopElement>,
    val closedBop: Boolean,
    val alarms: List<Alarm>,
    val trends: List<TrendPoint>,
    val score: Int,
    val kickDetected: Boolean,
    val shutIn: Boolean,
    val failures: List<String>,
    val totalStrokes: Double,
    val killSheet: KillSheetResult?,
    val message: String?,
    val recommendations: List<String>,
    val riserMarginKPa: Double,
    val maxCasingKPa: Double,
    val maxShoeKPa: Double,
    val maxPitGainM3: Double,
    val minBhpKPa: Double,
    val alarmSoundEnabled: Boolean,
    val gainLogEnabled: Boolean,
    val standpipeLineup: ManifoldLineup,
    val chokeLineup: ManifoldLineup,
    val lineupValid: Boolean,
    val holeFillM3: Double,
    val milestones: List<String>,
    val procedure: ProcedureSnapshot? = null,
    val bitIncDeg: Double = 0.0,
    val holeIncDeg: Double = 0.0,
    val tempAtBitC: Double = 24.0,
    val tempBottomC: Double = 90.0,
    val temperatureFactor: Double = 1.0,
    val cuttingsFraction: Double = 0.0,
    val annularVelocityMPerS: Double = 0.0,
    val bhpHydrostaticKPa: Double = 0.0,
    val bhpCuttingsKPa: Double = 0.0,
    val bhpAnnularFrictionKPa: Double = 0.0,
    val bhpSurgeKPa: Double = 0.0,
    val bhpChokeKPa: Double = 0.0,
    val gasReachedSurface: Boolean = false,
    val killStage: KillStage = KillStage.NONE,
    val oldMudKgM3: Double = mudDensityKgM3,
    val killMudKgM3: Double = 0.0,
    val effectiveMudKgM3: Double = mudDensityKgM3,
    val killMudPumpedM3: Double = 0.0,
    val stringKillFraction: Double = 0.0,
    val annulusKillFraction: Double = 0.0,
    val chokeLineKillFraction: Double = 0.0,
    val firstCirculationFraction: Double = 0.0,
    val killSheetFrozen: KillSheetResult? = null,
    val killCasingTargetKPa: Double = 0.0,
    val lineProgress: Map<LineKind, Double> = emptyMap(),
    val linesDisplaced: Set<LineKind> = emptySet(),
    val requiredLines: Set<LineKind> = emptySet(),
    val lineDisplacementComplete: Boolean = false,
    val residualPressureKPa: Double = casingPressureKPa,
    val trappedPressureKPa: Double = 0.0,
    val readyToOpen: Boolean = false,
    val openBopBlockedReason: String? = null,
    val firstCirculationReady: Boolean = false,
    val activeChokeIndex: Int = 0,
    val activeChokeName: String = "",
    // --- Fase A: preparo, medições e alarmes -------------------------------
    val prepChecks: List<PrepCheck> = emptyList(),
    val prepComplete: Boolean = false,
    val prepDoneCount: Int = 0,
    val scrAtRateKPa: Map<Int, Double> = emptyMap(),
    val chokeLineDeltaPKPa: Double = 0.0,
    val pitGainAlarmM3: Double = 0.7949,
    val pitLossAlarmM3: Double = 0.7949,
    val returnFlowAlarmPct: Double = 10.0,
    val pitGainM3: Double = 0.0,
    val flowDeviationPct: Double = 0.0,
    val secondaryIndicators: Set<SecondaryIndicator> = emptySet(),
    val poreEquivalentKgM3: Double = 0.0,
    val kickCondition: KickCondition = KickCondition.NONE,
    val bopEverClosed: Set<BopElement> = emptySet(),
    val drillingStopped: Boolean = false,
    val injectionPath: InjectionPath = InjectionPath.NONE,
    val returnPath: ReturnPath = ReturnPath.BLOCKED,
    val standpipeActive: Boolean = false,
    val killLineActive: Boolean = false,
    val chokeManifoldActive: Boolean = false,
    val wellDead: Boolean = false,
    val residualBleedDone: Boolean = false,
    val killSequenceComplete: Boolean = false
) {
    // Campos calculados preenchidos pelo simulador. Ficam no corpo para não
    // estourar o limite de 255 parâmetros do construtor da JVM.
    var firstCirculationPumpedM3: Double = 0.0
    var firstCirculationTotalM3: Double = 0.0
    var firstCirculationStrokes: Double = 0.0
    var firstCirculationTotalStrokes: Double = 0.0
    var firstCirculationRemainingStrokes: Double = 0.0
    var firstCirculationKickOut: Boolean = true
    var kickMaxVolumeM3: Double = 0.0
    var kickCharacteristicRateLpm: Double = 0.0
    var kickClosingTimeSec: Double = 0.0
    var kickStabilizationFactor: Double = 1.0
    var kickZoneGradientKPaPerM: Double = 0.0
    var kickZonePermeabilityMd: Double = 0.0
    var flowCheckRunning: Boolean = false
    var flowCheckProgress: Double = 0.0
    var flowCheckFlowLpm: Double = 0.0

    // Leituras vivas de shut-in e latches do guia. Ficam no corpo para não
    // estourar o limite de 255 parâmetros do construtor da JVM.
    /** SIDPP de gauge: cresce até o valor estabilizado, não salta no fechamento. */
    var sidppGaugeKPa: Double = sidppKPa
    /** SICP de gauge: é a própria pressão de revestimento durante o shut-in. */
    var sicpGaugeKPa: Double = casingPressureKPa
    /** SIDPP/SICP só são leituras válidas com BOP fechado e bomba parada. */
    var sidppValid: Boolean = false
    var sicpValid: Boolean = false
    var shutInStabilized: Boolean = false
    var influxDensityKgM3: Double = 0.0
    /** Cabeça hidrostática extra das linhas/riser já deslocados com lama nova. */
    var lineDisplacedHeadKPa: Double = 0.0
    // Latches do guia: progresso não regride ao abrir o BOP.
    var guideKickHandled: Boolean = false
    var guideDrillingStopped: Boolean = false
    var guideBopClosed: Boolean = false
    var guideStabilized: Boolean = false
    var guideKillSheetReady: Boolean = false
    var guideFirstCirculation: Boolean = false
    var guideWeightedUp: Boolean = false
    var guideSecondCirculation: Boolean = false
    var guideLinesDisplaced: Boolean = false
    var guideBleedDone: Boolean = false
    var guideCycleComplete: Boolean = false
    /** Par estabilizado puro e par registrado (com eventual fator de segurança do operador). */
    var stabilizedSidppKPa: Double = 0.0
    var stabilizedSicpKPa: Double = 0.0
    var recordedSidppKPa: Double = sidppKPa
    var recordedSicpKPa: Double = sicpKPa
    var shutInSafetyOffsetKPa: Double = 0.0
    var maxHoleDepthM: Double = holeDepthM
    var chokeOpenNoPump: Boolean = false
    var guideVolumetricBleedDone: Boolean = false
    var guideLubricateDone: Boolean = false
    var guideEdsProtected: Boolean = false
    var guideTutorialDone: Boolean = false
    /** Sobrepressão de migração de influxo em poço fechado (kPa). */
    var migrationOverpressureKPa: Double = 0.0
}

class Simulator(val scenario: Scenario) {
    private val well = scenario.well
    private val geometry = WellGeometry(well, scenario.drillString)
    private val pump = scenario.rig.pumps.first()
    private val chokes = scenario.rig.chokes
    private var kickConfig = scenario.kick

    fun activeChoke(): ChokeConfig = chokes.getOrElse(activeChokeIndex) { chokes.first() }

    var timeSec: Double = 0.0
        private set
    var phase: Phase = Phase.DRILLING
        private set
    var bitDepthM: Double = scenario.initialBitDepthM
        private set
    var mudDensityKgM3: Double = scenario.mud.densityKgM3
        private set
    var targetMudDensityKgM3: Double = scenario.mud.densityKgM3
        private set
    var pitM3: Double = scenario.initialPitM3
        private set
    var tripTankM3: Double = scenario.initialTripTankM3
        private set
    var casingPressureKPa: Double = 0.0
        private set
    var chokePct: Double = 0.0
        private set
    var chokeAuto: Boolean = false
        private set
    var chokeSetpointKPa: Double = 0.0
        private set
    var hcrOpen: Boolean = false
        private set
    var pumpOn: Boolean = scenario.initialSpm > 0.0
        private set
    var pumpSpm: Double = scenario.initialSpm
        private set
    var targetSpm: Double = scenario.initialSpm
        private set
    var accumulatorKPa: Double = scenario.rig.bop.ratedPressureKPa
        private set
    var influxVolumeM3: Double = 0.0
        private set
    var influxDepthM: Double = well.tvdM
        private set
    var gasAtSurface: Boolean = false
        private set
    var kickDetected: Boolean = false
        private set
    var kickArmed: Boolean = false
        private set
    var shutIn: Boolean = false
        private set
    var wobKn: Double = 0.0
        private set
    var wobKlb: Double = 0.0
        private set
    var wobSetpointKlb: Double = 25.0
        private set
    var rpm: Double = 0.0
        private set
    var rpmSetpoint: Double = 90.0
        private set
    var rotaryOn: Boolean = false
        private set
    var copOffsetM: Double = 0.0
        private set
    var ropMHr: Double = 0.0
        private set
    var torqueKNm: Double = 0.0
        private set
    var cuttingsLoad: Double = 0.0
        private set
    var scrKPa: Double = scenario.scrubPressureKPa
        private set
    var scrRateLpm: Double = scenario.killRateLpm
        private set
    var scrTaken: Boolean = false
        private set
    var scrSource: ScrSource = ScrSource.ESTIMATED
        private set
    private var hydraulicScale: Double = 1.0
    var bitWear: Double = 0.0
        private set
    var formationName: String = ""
        private set
    var formationType: FormationType = FormationType.SANDSTONE
        private set
    var formationHardness: Double = 5.0
        private set
    var influxTopTvdM: Double = 0.0
        private set
    var influxBaseTvdM: Double = 0.0
        private set
    var influxPath: InfluxPath = InfluxPath.FORMATION
        private set
    var alarmSoundEnabled: Boolean = true
        private set
    var gainLogEnabled: Boolean = true
        private set
    var standpipeLineup: ManifoldLineup = ManifoldLineup.DRILLING
        private set
    var chokeLineup: ManifoldLineup = ManifoldLineup.DRILLING
        private set
    var holeFillM3: Double = 0.0
        private set
    var totalStrokes: Double = 0.0
        private set
    var message: String? = null
        private set
    var pumpFailed: Boolean = false
        private set
    var chokePlugged: Boolean = false
        private set
    var chokeWashed: Boolean = false
        private set
    var powerLoss: Boolean = false
        private set

    private val bopClosed = mutableSetOf<BopElement>()
    private val alarms = mutableListOf<Alarm>()
    private val trends = mutableListOf<TrendPoint>()
    private val failures = mutableListOf<String>()
    private val recommendations = mutableListOf<String>()
    private val processedEvents = mutableSetOf<Int>()
    private var alarmSeq = 0L
    private var lastTrendSec = -1.0
    private var messageUntil = 0.0
    private var shutInAt = -1.0
    private var killStartedAt = -1.0
    private var kickAt = -1.0
    private var maxCasing = 0.0
    private var maxShoe = 0.0
    private var maxPit = scenario.initialPitM3
    private var minBhp = Double.MAX_VALUE
    private var maxPitGain = 0.0
    private var bleedVolumeM3 = 0.0
    private var influxRateM3S = 0.0
    private var lossRateM3S = 0.0
    private var chokeFlowLpm = 0.0
    private var flowOutLpm = 0.0
    private var lastAlarmAt = mutableMapOf<AlarmCode, Double>()
    private var gasExpansion = 1.0
    private var kickTriggered = false
    private var previousBitDepthM: Double = scenario.initialBitDepthM
    private val latchedAlarms = mutableSetOf<AlarmCode>()
    private val alarmSuppressedUntil = mutableMapOf<AlarmCode, Double>()
    private val milestones = mutableListOf<String>()
    private var nextPitGainAlarmM3 = 0.25
    private var activeProcedure: ProcedureId? = null
    private var procedureIndex = 0
    private var procedureStepAt = 0.0
    private var procedureStableSec = 0.0
    private var flowCheckFlowLpm = 0.0
    private var flowCheckArmed = false
    private var flowCheckStableSec = 0.0
    private var kickStartedAt = -1.0
    private var kickClosedAt = -1.0
    private var kickClosingTimeSec = 0.0
    private var kickStabilizationFactor = 1.0
    private var kickMaxReached = false
    private var kickVolumeAtCloseM3 = 0.0
    private var kickCasingAtCloseKPa = 0.0
    private var kickTopAtCloseTvdM = 0.0
    private var kickStabilizationApplied = false
    var surgeKPa: Double = 0.0
        private set
    private var lastCopOffsetM: Double = 0.0
    var gasReachedSurface: Boolean = false
        private set

    // --- Segunda circulação e deslocamento de linhas -------------------------
    var killStage: KillStage = KillStage.NONE
        private set
    var oldMudKgM3: Double = scenario.mud.densityKgM3
        private set
    var killMudKgM3: Double = 0.0
        private set
    var effectiveMudKgM3: Double = scenario.mud.densityKgM3
        private set
    var killMudPumpedM3: Double = 0.0
        private set
    var stringKillFraction: Double = 0.0
        private set
    var annulusKillFraction: Double = 0.0
        private set
    var chokeLineKillFraction: Double = 0.0
        private set
    var trappedPressureKPa: Double = 0.0
        private set
    var killSheetFrozen: KillSheetResult? = null
        private set
    var firstCirculationReady: Boolean = false
        private set
    var activeChokeIndex: Int = 0
        private set
    private var mudFrozen = false
    private var killSequenceActive = false
    // Detecta a transição bomba ligada → desligada com o poço fechado para
    // registrar a pressão construída como pressão trapada (poço estanque).
    private var pumpWasOn = false
    private var pumpedVolumeM3 = 0.0
    private var firstCircPumpedM3 = 0.0
    private var firstCirculationReadyAt = -1.0
    private val lineProgressMap = mutableMapOf<LineKind, Double>()
    private val linesDisplacedSet = mutableSetOf<LineKind>()
    private var residualBleedDone = false
    private var killSequenceComplete = false
    private var drillingStopped = false
    private var drillingSeen = false

    // --- Transitório de shut-in, poço morto e latches do guia --------------
    private var maxHoleMdM: Double = max(well.mdM, scenario.initialBitDepthM)
    private var wellKilledLatch = false
    private var sidppGaugeKPa = 0.0
    private var previousSidppGaugeKPa = 0.0
    private var sidppSlopeKPaPerS = 0.0
    private var previousCasingKPa = 0.0
    private var casingSlopeKPaPerS = 0.0
    private var shutInStableSec = 0.0
    private var staticShutInSec = 0.0
    /** Sobrepressão acumulada de migração de influxo em poço fechado (kPa). */
    private var migrationOverpressureKPa = 0.0
    private var shutInStabilized = false
    var stabilizedSidppKPa: Double = 0.0
        private set
    var stabilizedSicpKPa: Double = 0.0
        private set
    private var safetySidppExtraKPa: Double = 0.0
    private var safetySicpExtraKPa: Double = 0.0
    private var scoreBonusLatch = false
    private var scoreOffset = 0.0
    private var guideKickHandled = false
    private var guideDrillingStopped = false
    private var guideBopClosed = false
    private var guideStabilized = false
    private var guideKillSheetReady = false
    private var guideFirstCirculation = false
    private var guideWeightedUp = false
    private var guideSecondCirculation = false
    private var guideLinesDisplaced = false
    private var guideBleedDone = false
    private var guideCycleComplete = false
    private var guideVolumetricBleedDone = false
    private var guideLubricateDone = false
    private var guideEdsProtected = false
    private var guideTutorialDone = false
    private var lubricateVolumeAddedM3 = 0.0
    private var lastOpenChokeWarnAt = -100.0
    private var circInfluxWarned = false

    // --- Fase A: preparo, medições de baixa vazão e alarmes -----------------
    var pitGainAlarmM3: Double = Units.bblToM3(5.0)
        private set
    var pitLossAlarmM3: Double = Units.bblToM3(5.0)
        private set
    var returnFlowAlarmPct: Double = 10.0
        private set
    var chokeLineDeltaPKPa: Double = 0.0
        private set
    private val prepDone = mutableSetOf<PrepCheckId>()
    private val scrAtRate = mutableMapOf<Int, Double>()
    private val bopEverClosed = mutableSetOf<BopElement>()
    private var pitReferenceM3: Double = scenario.initialPitM3
    private var baselineSppKPa = 0.0
    private var baselineRopMHr = 0.0
    private var baselineWobKlb = 0.0
    private var baselineEcdKgM3 = 0.0
    private var baselineSpm = 0.0
    private var baselineReady = false
    private var baselineSinceSec = 0.0

    init {
        scenario.initialBopClosed.forEach { bopClosed.add(it) }
        hcrOpen = scenario.initialHcrOpen

        // Calibra modelo hidráulico inicial se scrKPa estimado/medido estiver disponível
        val initRate = scenario.killRateLpm.coerceAtLeast(1.0)
        val modelInit = HydraulicModel.calculate(well, scenario.drillString, bitDepthM, initRate, mudDensityKgM3, 0.0, 0.0, 1.0).totalKPa
        if (modelInit > 1.0) {
            hydraulicScale = (scenario.scrubPressureKPa / modelInit).coerceIn(0.2, 5.0)
        }

        val immediate = kickConfig.enabled &&
            (kickConfig.triggerMode == KickTriggerMode.IMMEDIATE ||
                (kickConfig.triggerMode == KickTriggerMode.TIME && kickConfig.triggerAtSec <= 0.0))
        if (immediate) {
            if (isClosed()) {
                injectKick(kickConfig.volumeM3, kickConfig.depthM)
                kickClosedAt = 0.0
                kickClosingTimeSec = 1.0
                kickVolumeAtCloseM3 = influxVolumeM3
                kickCasingAtCloseKPa = casingPressureKPa
                kickTopAtCloseTvdM = influxTopTvdM
            } else {
                armInfluxZone(kickConfig.depthM)
            }
            kickTriggered = true
        }
        if (isClosed()) {
            shutIn = true
            shutInAt = 0.0
            if (phase == Phase.DRILLING) phase = Phase.SHUT_IN
        }
        updateFormation()
        if (phase == Phase.DRILLING && pumpOn && !isClosed() && bitDepthM < well.mdM) {
            rotaryOn = true
            rpmSetpoint = 90.0
            wobSetpointKlb = 25.0
            wobKlb = 25.0
            rpm = 90.0
        }
        addMilestone("Cenário iniciado · ${scenario.title("pt")}")
        addTrend(true)
    }

    val bitTvdM: Double get() = well.tvdAtMd(bitDepthM)
    val holeTvdM: Double get() = well.tvdM

    private fun updateFormation() {
        val layer = well.layerAtMd(bitDepthM)
        if (layer != null) {
            formationName = layer.name
            formationType = layer.type
            formationHardness = layer.hardness
        } else {
            formationName = if (bitDepthM >= well.shoeTvdM) "Buraco aberto" else "Formação"
            formationType = FormationType.SANDSTONE
            formationHardness = 5.0
        }
    }

    private fun addMilestone(text: String) {
        if (milestones.lastOrNull() == text) return
        milestones.add(text)
        if (milestones.size > 80) milestones.removeAt(0)
    }

    private fun lineupValid(): Boolean {
        if (chokeLineup == ManifoldLineup.TRIP && !isClosed() &&
            standpipeLineup != ManifoldLineup.TRIP
        ) return false
        if (standpipeLineup == ManifoldLineup.TRIP && chokeLineup != ManifoldLineup.TRIP) {
            return false
        }
        if ((standpipeLineup == ManifoldLineup.LUBRICATE || chokeLineup == ManifoldLineup.LUBRICATE) &&
            !isClosed()
        ) return false
        return true
    }

    private fun poreAtBitKPa(): Double = poreAtDepthKPa(bitDepthM)

    /**
     * Pressão de poros na profundidade informada, incluindo a sobrepressão
     * configurada para a zona de kick. A intensidade é uma pressão adicional
     * (kPa) aplicada apenas dentro da janela topo/base da zona.
     */
    private fun poreAtDepthKPa(depthMd: Double): Double {
        val base = well.porePressureAtMd(depthMd)
        return base + kickOverpressureKPa(depthMd, base)
    }

    private fun kickOverpressureKPa(depthMd: Double, carrierKPa: Double): Double {
        val kick = kickConfig
        if (!kick.enabled) return 0.0
        val top = min(kick.zoneTopMdM, kick.zoneBaseMdM)
        val base = max(kick.zoneTopMdM, kick.zoneBaseMdM)
        if (base <= top || depthMd < top - 0.5 || depthMd > base + 0.5) return 0.0
        // Gradiente próprio da zona interna: garante pressão igual ou superior
        // à formação portadora, sem duplicar a sobrepressão didática.
        val zonePressure = if (kick.zonePoreGradientKPaPerM > 0.0) {
            kick.zonePoreGradientKPaPerM * well.tvdAtMd(depthMd)
        } else 0.0
        return max(kick.intensityKPa, zonePressure - carrierKPa).coerceAtLeast(0.0)
    }

    /** Profundidade de referência do influxo: a broca ou o ponto do gatilho. */
    private fun kickReferenceDepthMd(): Double =
        if (influxVolumeM3 > 0.0 || kickTriggered) max(bitDepthM, kickConfig.depthM) else bitDepthM

    /** TVD de referência para a KMW: a do influxo/broca, não o TD do poço. */
    private fun kickReferenceTvdM(): Double =
        well.tvdAtMd(kickReferenceDepthMd()).coerceIn(1.0, well.tvdM.coerceAtLeast(1.0))

    /**
     * Poço morto para fins de abertura do BOP: sem influxo relevante e sem
     * pressão de casing que a lama não consiga equilibrar.
     */
    private fun wellDead(): Boolean =
        influxVolumeM3 < 0.02 && sicpKPa() <= 100.0

    private fun hydrostaticMudKPa(density: Double = mudDensityKgM3, depth: Double = bitTvdM): Double =
        ThermalModel.integratedHydrostaticKPa(well, density, depth)

    /** Densidade do fluido que já está no poço. Congela no kick para não alterar SIDPP/SICP ao pesar lama. */
    private fun inWellDensity(): Double =
        if (mudFrozen || killSequenceActive) oldMudKgM3 else mudDensityKgM3

    private fun pumpedDensity(): Double =
        if (killSequenceActive) killMudKgM3 else mudDensityKgM3

    /** Densidade média na coluna: lama velha seguida da lama de matar bombeada. */
    private fun effectiveStringDensity(): Double {
        val base = inWellDensity()
        return base + (pumpedDensity() - base) * stringKillFraction
    }

    /** Densidade média no anular: lama de matar entra pela broca e sobe. */
    private fun effectiveAnnulusDensity(): Double {
        val base = inWellDensity()
        return base + (killMudKgM3 - base) * annulusKillFraction
    }

    /** Densidade média na choke/kill line durante a 2ª circulação. */
    private fun effectiveChokeLineDensity(): Double {
        val base = inWellDensity()
        return base + (killMudKgM3 - base) * chokeLineKillFraction
    }

    private fun hydrostaticStringKPa(depth: Double = bitTvdM): Double =
        ThermalModel.integratedHydrostaticKPa(well, effectiveStringDensity(), depth)

    private fun hydrostaticAnnulusKPa(depth: Double = bitTvdM): Double {
        val missingColumnM = if (!isClosed() && holeFillM3 > 0.001) {
            (holeFillM3 / annularAreaAt(0.0)).coerceIn(0.0, depth * 0.5)
        } else 0.0
        val effectiveDepth = (depth - missingColumnM).coerceAtLeast(0.0)
        return ThermalModel.integratedHydrostaticKPa(well, effectiveAnnulusDensity(), effectiveDepth)
    }

    private fun annularAreaAt(depth: Double): Double = geometry.annularAreaM2(depth)

    private fun influxHeightM(): Double {
        if (influxVolumeM3 <= 0.0) return 0.0
        val md = well.mdAtTvd(influxDepthM.coerceIn(0.0, well.tvdM))
        return influxVolumeM3 / annularAreaAt(md)
    }

    private fun cuttingsDeltaKPa(): Double {
        if (cuttingsLoad <= 0.0) return 0.0
        val rhoEffective = ThermalModel.densityAtTemp(mudDensityKgM3, ThermalModel.tempAtTvd(well, bitTvdM / 2.0))
        val deltaDensity = cuttingsLoad.coerceIn(0.0, 0.10) * (2600.0 - rhoEffective)
        return deltaDensity * Units.G * bitTvdM / 1000.0
    }

    private fun hydraulicLosses(
        rateLpm: Double = pumpRateLpm(),
        density: Double = if (killSequenceActive) effectiveStringDensity() else mudDensityKgM3
    ): HydraulicLosses {
        if (rateLpm <= 0.5 || !pumpOn) return HydraulicLosses()

        val sDen = if (killSequenceActive) effectiveStringDensity() else mudDensityKgM3
        val aDen = if (killSequenceActive) effectiveAnnulusDensity() else mudDensityKgM3
        val bDen = if (killSequenceActive && stringKillFraction >= 1.0) killMudKgM3 else mudDensityKgM3
        val cDen = if (killSequenceActive) effectiveChokeLineDensity() else mudDensityKgM3

        val detailed = HydraulicModel.calculate(
            well = well,
            string = scenario.drillString,
            bitDepthM = bitDepthM,
            rateLpm = rateLpm,
            mudDensityRefKgM3 = mudDensityKgM3,
            cuttingsFraction = cuttingsLoad,
            bitWear = bitWear,
            hydraulicScale = hydraulicScale,
            surfaceDensityKgM3 = mudDensityKgM3, // fluido bombeado na superfície
            stringDensityKgM3 = sDen,            // proporção de lama nova na coluna
            bitDensityKgM3 = bDen,               // fluido que atravessa a broca
            annulusDensityKgM3 = aDen,           // proporção de lama nova no anular
            plasticViscosityCp = scenario.mud.plasticViscosityCp,
            yieldPointPa = scenario.mud.yieldPointPa,
            baseDensityKgM3 = mudDensityKgM3
        )
        val ripple = 1.0 + 0.018 * kotlin.math.sin(timeSec * 0.85) +
            0.009 * kotlin.math.sin(timeSec * 3.1 + 0.7)
        return HydraulicLosses(
            surfaceKPa = detailed.surfaceKPa * ripple,
            stringKPa = detailed.stringKPa * ripple,
            bitKPa = detailed.bitKPa * ripple,
            annulusKPa = detailed.annulusKPa * ripple,
            motorKPa = detailed.motorKPa * ripple
        )
    }

    private fun annularLossKPa(): Double =
        hydraulicLosses(
            density = if (killSequenceActive) effectiveAnnulusDensity() else mudDensityKgM3
        ).annulusKPa

    /** ECD em densidade equivalente, com o rebaixamento térmico já aplicado. */
    private fun ecdKgM3(): Double {
        val ecdPressure = if (isClosed()) {
            hydrostaticAnnulusKPa() + annularLossKPa() + cuttingsDeltaKPa() +
                max(0.0, surgeKPa) + casingPressureKPa
        } else {
            hydrostaticAnnulusKPa() + annularLossKPa() + cuttingsDeltaKPa() +
                max(0.0, surgeKPa)
        }
        return if (bitTvdM > 1.0) {
            val thermalDrop = (hydrostaticAnnulusKPa() -
                Units.hydrostaticKpa(effectiveAnnulusDensity(), bitTvdM)).coerceAtMost(0.0)
            (ecdPressure - thermalDrop) * 1000.0 / (Units.G * bitTvdM)
        } else effectiveAnnulusDensity()
    }

    /** Pressão de poros na broca convertida em densidade equivalente. */
    private fun poreEquivalentKgM3(): Double =
        if (bitTvdM > 1.0) poreAtBitKPa() * 1000.0 / (Units.G * bitTvdM) else mudDensityKgM3

    fun pumpRateLpm(): Double = if (pumpOn) pumpSpm * pump.displacementLPerStroke else 0.0

    /** Vazão de bomba em gpm (unidade usada nas manobras de baixa vazão). */
    fun pumpRateGpm(): Double = Units.lpmToGpm(pumpRateLpm())

    private fun bhpKPa(): Double {
        val hydro = hydrostaticAnnulusKPa() - gasHydrostaticLossKPa() + cuttingsDeltaKPa()
        val friction = annularLossKPa()
        val totalBhp = hydro + friction + surgeKPa + (if (isClosed()) casingPressureKPa else 0.0)
        return max(0.0, totalBhp)
    }

    private fun sppKPa(): Double {
        val losses = hydraulicLosses().totalKPa
        return when {
            !pumpOn -> if (isClosed()) sidppKPa() else 0.0
            isClosed() -> sidppKPa() + losses + chokeLineFrictionKPa()
            else -> losses
        }
    }

    private fun gasHydrostaticLossKPa(): Double {
        if (influxVolumeM3 <= 0.0) return 0.0
        val height = influxHeightM()
        return max(0.0, (effectiveAnnulusDensity() - influxDensity()) * Units.G * height / 1000.0)
    }

    /** Densidade do fluido invasor: propriedade do tipo de influxo (ou sobrescrita). */
    private fun influxDensity(): Double =
        if (kickConfig.influxDensityKgM3 > 0.0) kickConfig.influxDensityKgM3
        else defaultInfluxDensityKgM3(kickConfig.type)

    /**
     * Coluna homogênea: sem influxo relevante no anular, sem perda hidrostática
     * de gás e sem cabeça extra de linha deslocada. Nessa condição o poço é um
     * tubo em U e as pressões de superfície do revestimento e do interior da
     * coluna (SPP/bengala) devem ser iguais.
     */
    private fun homogeneousColumn(): Boolean =
        influxVolumeM3 <= 0.08 &&
            gasHydrostaticLossKPa() <= 0.0 &&
            lineDisplacedHeadKPa() <= 0.0

    /**
     * Taxa de migração do influxo em poço fechado (kPa/s), dependente do tipo.
     * A velocidade de ascensão é maior quanto mais leve o fluido invasor em
     * relação à lama. A sobrepressão é o gradiente hidrostático da lama vezes
     * a velocidade: dP/dt = ρ_lama · g · v.
     *   Gás (~90 kg/m³): mais rápido; Óleo (~780): intermediário;
     *   Água (~1020) e salgada (~1080): mais lentas.
     */
    private fun migrationRateKPaPerS(): Double {
        if (wellKilledLatch || influxVolumeM3 <= 0.08 || gasAtSurface) return 0.0
        // Velocidade de ascensão típica por natureza do influxo (m/s):
        // gás borbulha rápido; líquidos imiscíveis segregam muito mais devagar.
        val velocityMPerS = when (kickConfig.type) {
            InfluxType.GAS -> 0.02
            InfluxType.OIL -> 0.005
            InfluxType.WATER -> 0.002
            InfluxType.SALTWATER -> 0.0015
        }
        val mudDensity = effectiveAnnulusDensity().coerceAtLeast(800.0)
        return (mudDensity * Units.G * velocityMPerS / 1000.0).coerceAtLeast(0.0)
    }

    /** SICP viva: equilíbrio físico + sobrepressão de migração. */
    fun liveSicpKPa(): Double = sicpKPa() + migrationOverpressureKPa

    /**
     * Cabeça hidrostática extra das linhas de retorno já deslocadas com lama
     * nova. O gauge de casing enxerga a choke line e, no subsea, o anular do
     * riser; kill line e booster não comunicam com esse gauge.
     */
    private fun lineDisplacedHeadKPa(): Double {
        if (linesDisplacedSet.isEmpty()) return 0.0
        val base = inWellDensity()
        val newDensity = if (killMudKgM3 > base) killMudKgM3 else effectiveAnnulusDensity()
        val delta = (newDensity - base).coerceAtLeast(0.0)
        if (delta <= 0.0) return 0.0
        val relevant = if (well.isSubsea) {
            setOf(LineKind.CHOKE_LINE, LineKind.RISER_ANNULUS)
        } else {
            setOf(LineKind.CHOKE_LINE)
        }
        return relevant.filter { it in linesDisplacedSet }.sumOf { kind ->
            val lengthM = when (kind) {
                LineKind.KILL_LINE -> well.lines.killLineLengthM
                LineKind.CHOKE_LINE -> well.lines.chokeLineLengthM
                LineKind.BOOSTER_LINE -> well.lines.boosterLineLengthM
                LineKind.RISER_ANNULUS -> well.riser?.waterDepthM ?: 0.0
            }
            delta * Units.G * lengthM.coerceAtLeast(0.0) / 1000.0
        }
    }

    fun sidppKPa(): Double {
        val pore = poreAtDepthKPa(kickReferenceDepthMd())
        return max(0.0, pore - hydrostaticStringKPa())
    }

    fun sicpKPa(): Double {
        val pore = poreAtDepthKPa(kickReferenceDepthMd())
        val column = hydrostaticAnnulusKPa() - gasHydrostaticLossKPa() + lineDisplacedHeadKPa()
        return max(0.0, pore - column)
    }

    /** SIDPP registrado após estabilização (+ eventual fator de segurança do operador). */
    fun recordedSidppKPa(): Double =
        if (shutInStabilized) stabilizedSidppKPa + safetySidppExtraKPa else sidppKPa()

    /** SICP registrado após estabilização (+ eventual fator de segurança do operador). */
    fun recordedSicpKPa(): Double =
        if (shutInStabilized) stabilizedSicpKPa + safetySicpExtraKPa else sicpKPa()

    private fun refreshUnfrozenKillSheetWithSafety() {
        if (phase != Phase.CIRCULATING && phase != Phase.DISPLACING_KILL_MUD &&
            phase != Phase.KILLED && killSheetFrozen != null
        ) {
            val updated = computeKillSheet()
            if (updated != null) {
                killSheetFrozen = updated
                killMudKgM3 = updated.killMudKgM3
            }
        }
    }

    /**
     * Permite ao operador aplicar um fator de segurança acima do par SIDPP/SICP
     * estabilizado. Nunca reduz abaixo dos valores físicos estabilizados.
     */
    fun setShutInSafetyOffset(offsetKPa: Double): Boolean {
        if (!kickDetected || (!shutInStabilized && !isClosed())) {
            setMessage("Estabilize o poço fechado antes de ajustar o fator de segurança.")
            return false
        }
        if (offsetKPa < -0.01) {
            setMessage("Fator de segurança não pode ficar abaixo de SIDPP/SICP estabilizados.")
            return false
        }
        val safeOffset = offsetKPa.coerceIn(0.0, 10000.0)
        safetySidppExtraKPa = safeOffset
        safetySicpExtraKPa = safeOffset
        refreshUnfrozenKillSheetWithSafety()
        if (isClosed() && !pumpOn && !wellKilledLatch) {
            val minSicp = recordedSicpKPa()
            val minSidpp = recordedSidppKPa()
            if (casingPressureKPa < minSicp) casingPressureKPa = minSicp
            if (sidppGaugeKPa < minSidpp) sidppGaugeKPa = minSidpp
            if (trappedPressureKPa > 0.0 && trappedPressureKPa < minSicp) {
                trappedPressureKPa = minSicp
            }
            if (chokeAuto && chokeSetpointKPa < minSicp) {
                chokeSetpointKPa = minSicp
            }
        }
        setMessage(
            "Fator de segurança aplicado: +${Units.fmt(Units.kpaToPsi(safeOffset))} psi " +
                "(SIDPP ${Units.fmt(Units.kpaToPsi(recordedSidppKPa()))} · " +
                "SICP ${Units.fmt(Units.kpaToPsi(recordedSicpKPa()))} psi)."
        )
        return true
    }

    /**
     * Define um par explícito (SIDPP, SICP) igual ou acima do par estabilizado
     * para fins de margem operacional de segurança.
     */
    fun applyShutInSafetyPair(sidppTargetKPa: Double, sicpTargetKPa: Double): Boolean {
        if (!kickDetected || (!shutInStabilized && !isClosed())) {
            setMessage("Estabilize o poço fechado antes de definir o par SIDPP/SICP.")
            return false
        }
        val baseSidpp = if (shutInStabilized) stabilizedSidppKPa else sidppKPa()
        val baseSicp = if (shutInStabilized) stabilizedSicpKPa else sicpKPa()
        if (sidppTargetKPa < baseSidpp - 1.0 || sicpTargetKPa < baseSicp - 1.0) {
            setMessage("Os valores de segurança devem permanecer iguais ou acima da estabilização.")
            return false
        }
        safetySidppExtraKPa = (sidppTargetKPa - baseSidpp).coerceIn(0.0, 10000.0)
        safetySicpExtraKPa = (sicpTargetKPa - baseSicp).coerceIn(0.0, 10000.0)
        refreshUnfrozenKillSheetWithSafety()
        if (isClosed() && !pumpOn && !wellKilledLatch) {
            val minSicp = recordedSicpKPa()
            val minSidpp = recordedSidppKPa()
            if (casingPressureKPa < minSicp) casingPressureKPa = minSicp
            if (sidppGaugeKPa < minSidpp) sidppGaugeKPa = minSidpp
            if (trappedPressureKPa > 0.0 && trappedPressureKPa < minSicp) {
                trappedPressureKPa = minSicp
            }
            if (chokeAuto && chokeSetpointKPa < minSicp) {
                chokeSetpointKPa = minSicp
            }
        }
        setMessage(
            "Par seguro registrado: SIDPP ${Units.fmt(Units.kpaToPsi(recordedSidppKPa()))} · " +
                "SICP ${Units.fmt(Units.kpaToPsi(recordedSicpKPa()))} psi."
        )
        return true
    }

    fun confirmKillSheet(): Boolean {
        val sheet = killSheetResult() ?: return false
        if (shutInStabilized || guideStabilized || phase == Phase.KICK_CONTROLLED || killSequenceActive) {
            if (killSheetFrozen == null) {
                killSheetFrozen = sheet
                killMudKgM3 = sheet.killMudKgM3
            }
            guideKillSheetReady = true
            return true
        }
        return false
    }

    /**
     * Constante de tempo do build-up após o fechamento. Depende da natureza e
     * da permeabilidade do influxo e do volume já admitido: gás migra por mais
     * tempo, água/salgada estabiliza rápido e baixa permeabilidade retarda a
     * equalização.
     */
    private fun shutInTauSec(): Double {
        val perm = if (kickConfig.zonePermeabilityMd > 0.0) kickConfig.zonePermeabilityMd else 100.0
        val permFactor = (100.0 / perm).coerceIn(0.5, 3.0)
        val base = when (kickConfig.type) {
            InfluxType.GAS -> 16.0
            InfluxType.OIL -> 11.0
            InfluxType.WATER, InfluxType.SALTWATER -> 7.0
        }
        val volumeFactor = 1.0 + influxVolumeM3.coerceIn(0.0, 3.0) * 0.25
        return (base * permFactor * volumeFactor).coerceIn(4.0, 45.0)
    }

    private fun shoePressureKPa(): Double {
        val shoeDepth = well.shoeTvdM
        val hydroShoe = hydrostaticAnnulusKPa(depth = shoeDepth)
        val gasAtShoe = if (influxDepthM < shoeDepth) gasHydrostaticLossKPa() else 0.0
        return if (isClosed()) {
            casingPressureKPa + hydroShoe - gasAtShoe + annularLossKPa()
        } else {
            hydroShoe - gasAtShoe + annularLossKPa()
        }
    }

    private fun maaspKPa(): Double {
        val shoeMd = well.mdAtTvd(well.shoeTvdM)
        return max(0.0, well.fracGradientAtMd(shoeMd) * well.shoeTvdM -
            hydrostaticAnnulusKPa(depth = well.shoeTvdM))
    }

    private fun chokeLineFrictionKPa(): Double {
        if (!well.isSubsea || chokeFlowLpm <= 0.0) return 0.0
        return 0.00042 * chokeFlowLpm
    }

    // --- Segunda circulação: volumes, pressão de casing e linhas -------------

    private fun stringVolumeToBitM3(): Double =
        geometry.stringVolumeToDepth(bitDepthM).coerceAtLeast(0.001)

    private fun annularVolumeToBitM3(): Double =
        geometry.annularVolumeToDepth(bitDepthM).coerceAtLeast(0.001)

    private fun annularVolumeToBopM3(): Double =
        geometry.annularVolumeToBop(bitDepthM).coerceAtLeast(0.001)

    /** Volume da 1ª circulação com BOP fechado: anular do poço + choke line. */
    private fun circulationVolumeToSurfaceM3(): Double =
        geometry.circulationVolumeToSurface(bitDepthM).coerceAtLeast(0.001)

    fun lineVolumeM3(kind: LineKind): Double = when (kind) {
        LineKind.KILL_LINE -> geometry.killLineVolumeM3()
        LineKind.CHOKE_LINE -> geometry.chokeLineVolumeM3()
        LineKind.BOOSTER_LINE -> geometry.boosterLineVolumeM3()
        LineKind.RISER_ANNULUS -> {
            val water = well.riser?.waterDepthM ?: 0.0
            if (well.isSubsea && water > 0.0) geometry.annularVolumeBetween(0.0, water) else 0.0
        }
    }

    /** Linhas que precisam estar com lama de matar antes de abrir o poço. */
    fun requiredLineKinds(): Set<LineKind> {
        val candidates = if (well.isSubsea) {
            setOf(LineKind.KILL_LINE, LineKind.CHOKE_LINE, LineKind.BOOSTER_LINE, LineKind.RISER_ANNULUS)
        } else {
            setOf(LineKind.KILL_LINE, LineKind.CHOKE_LINE)
        }
        return candidates.filter { lineVolumeM3(it) > 0.0001 }.toSet()
    }

    private fun allRequiredLinesDisplaced(): Boolean {
        val required = requiredLineKinds()
        return required.isEmpty() || required.all { it in linesDisplacedSet }
    }

    private fun lineProgressOf(kind: LineKind): Double =
        (lineProgressMap[kind] ?: 0.0).coerceIn(0.0, 1.0)

    private fun initializeLineProgress() {
        requiredLineKinds().forEach { kind ->
            if (kind !in lineProgressMap) lineProgressMap[kind] = 0.0
        }
    }

    /**
     * Pressão de revestimento alvo durante a segunda circulação.
     * Antes da lama de matar chegar à broca, mantém a pressão trapada
     * (sobrepressão preservada ao fim da 1ª circulação); depois cai na medida
     * em que o fluido novo substitui a lama velha no anular e o ganho
     * hidrostático reequilibra o poço.
     */
    fun killCasingTargetKPa(): Double {
        val sheet = killSheetFrozen ?: return 0.0
        val base = if (trappedPressureKPa > 0.0) trappedPressureKPa else sheet.sicpKPa
        if (!killSequenceActive) return base
        val baseHydro = hydrostaticMudKPa(inWellDensity(), bitTvdM)
        val currentHydro = hydrostaticAnnulusKPa()
        val relief = (currentHydro - baseHydro).coerceAtLeast(0.0)
        return max(0.0, base - relief)
    }

    private fun updateCirculationProgress(dt: Double) {
        val pumpedNow = if (pumpOn) pumpRateLpm() / 60000.0 * dt else 0.0
        pumpedVolumeM3 += pumpedNow
        if (phase == Phase.CIRCULATING && hcrOpen && chokePct > 0.2 && lineupValid()) {
            firstCircPumpedM3 += pumpedNow
        }

        if (killSequenceActive && phase == Phase.DISPLACING_KILL_MUD &&
            hcrOpen && chokePct > 0.2 && lineupValid()
        ) {
            killMudPumpedM3 += pumpedNow
        }

        val stringVol = stringVolumeToBitM3()
        // Com BOP fechado a lama de matar percorre o anular do poço (broca→BOP)
        // e retorna pela choke line; o riser isolado não recebe a nova lama.
        val annVol = annularVolumeToBopM3()
        val chokeVol = geometry.chokeLineVolumeM3().coerceAtLeast(0.0001)
        if (killSequenceActive) {
            stringKillFraction = (killMudPumpedM3 / stringVol).coerceIn(0.0, 1.0)
            val afterBit = (killMudPumpedM3 - stringVol).coerceAtLeast(0.0)
            annulusKillFraction = (afterBit / annVol).coerceIn(0.0, 1.0)
            val afterAnnulus = (afterBit - annVol).coerceAtLeast(0.0)
            chokeLineKillFraction = (afterAnnulus / chokeVol).coerceIn(0.0, 1.0)

            // Auto-marca a choke line como deslocada quando a lama de matar passou por ela
            if (chokeLineKillFraction >= 0.999 && LineKind.CHOKE_LINE !in linesDisplacedSet) {
                linesDisplacedSet.add(LineKind.CHOKE_LINE)
                addMilestone("Choke line deslocada pela 2ª circulação")
            }

            effectiveMudKgM3 = effectiveAnnulusDensity()
        } else {
            effectiveMudKgM3 = inWellDensity()
        }
    }

    private fun updateKillStage() {
        if (killSequenceActive && phase == Phase.DISPLACING_KILL_MUD) {
            killStage = when {
                stringKillFraction < 0.999 -> KillStage.STRING
                annulusKillFraction < 0.999 -> KillStage.ANNULUS
                chokeLineKillFraction < 0.999 -> KillStage.CHOKE_LINE
                else -> KillStage.WELL_KILLED
            }
            if (killStage == KillStage.WELL_KILLED && influxVolumeM3 < 0.08) {
                phase = Phase.KILLED
                trappedPressureKPa = casingPressureKPa
                killStage = KillStage.LINE_DISPLACEMENT
                guideSecondCirculation = true
                guideWeightedUp = true
                // Poço morto: o resíduo de influxo não pode continuar
                // alimentando migração/gás e devolvendo pressão após a sangria.
                influxVolumeM3 = 0.0
                influxRateM3S = 0.0
                initializeLineProgress()
                if (sicpKPa() > 150.0) {
                    addMilestone(
                        "2ª circulação concluída, mas o poço ainda está pressurizado: " +
                            "a lama de matar não equilibra a formação"
                    )
                    addAlarm(
                        AlarmCode.KILL_COMPLETE,
                        AlarmSeverity.WARNING,
                        "Poço ainda pressurizado (${Units.fmt(Units.kpaToPsi(sicpKPa()))} psi): " +
                            "não sangre nem abra o BOP; ajuste o peso da lama."
                    )
                    setMessage(
                        "Poço ainda pressurizado: a lama não equilibra a formação. " +
                            "Não sangre; ajuste o peso da lama."
                    )
                } else {
                    addMilestone("Poço morto: lama de matar na COP, anular e choke line")
                    addAlarm(AlarmCode.KILL_COMPLETE, AlarmSeverity.INFO, "Poço morto")
                }
            }
            if (chokeAuto) chokeSetpointKPa = killCasingTargetKPa()
        }
        if (phase == Phase.KILLED && killStage == KillStage.LINE_DISPLACEMENT &&
            allRequiredLinesDisplaced()
        ) {
            killStage = KillStage.READY_TO_OPEN
            guideLinesDisplaced = true
            addMilestone("Linhas deslocadas: falta sangrar a pressão residual")
        }
    }

    private fun freezeKillSheet() {
        if (killSheetFrozen != null) return
        val sheet = computeKillSheet() ?: return
        killSheetFrozen = sheet
        killMudKgM3 = sheet.killMudKgM3
        if (shutInStabilized || guideStabilized || phase == Phase.KICK_CONTROLLED || killSequenceActive) {
            guideKillSheetReady = true
        }
    }

    private fun openBopBlockReason(): String? {
        if (!kickDetected || killSequenceComplete) return null
        if (phase == Phase.FAILED) return "Poço em falha: não abra o BOP."
        if (phase != Phase.KILLED) {
            return "Mantenha o poço fechado: conclua a 1ª e a 2ª circulação até o poço morto."
        }
        if (!allRequiredLinesDisplaced()) {
            return "Desloque a kill line, o booster e o anular do riser antes de abrir."
        }
        if (casingPressureKPa > 200.0) {
            return "Sangre a pressão residual pelo choke antes de abrir (${Units.fmt(Units.kpaToPsi(casingPressureKPa))} psi)."
        }
        if (!wellDead()) {
            return "Poço ainda pressurizado (SICP ${Units.fmt(Units.kpaToPsi(sicpKPa()))} psi): " +
                "a lama de matar não equilibra a formação."
        }
        return null
    }

    private fun isClosed(): Boolean = bopClosed.isNotEmpty()

    /** Caminho de injeção coerente com bombas, BOP e alinhamento do standpipe. */
    private fun injectionRoute(): InjectionPath = when {
        !pumpOn -> InjectionPath.NONE
        isClosed() && standpipeLineup == ManifoldLineup.KILL -> InjectionPath.KILL_LINE
        isClosed() && standpipeLineup == ManifoldLineup.CHOKE -> InjectionPath.CHOKE_LINE
        isClosed() && standpipeLineup == ManifoldLineup.TRIP -> InjectionPath.TRIP_LINE
        isClosed() && standpipeLineup == ManifoldLineup.LUBRICATE -> InjectionPath.LUBRICATE_LINE
        else -> InjectionPath.STANDPIPE
    }

    /** Caminho de retorno coerente com BOP, HCR e tipo de sonda. */
    private fun returnRoute(): ReturnPath = when {
        !isClosed() && standpipeLineup == ManifoldLineup.TRIP &&
            chokeLineup == ManifoldLineup.TRIP -> ReturnPath.TRIP_TANK
        !isClosed() && well.isSubsea -> ReturnPath.RISER
        !isClosed() -> ReturnPath.FLOWLINE
        hcrOpen && chokeLineup == ManifoldLineup.LUBRICATE && lineupValid() -> ReturnPath.LUBRICATE_BLEED
        hcrOpen && lineupValid() -> ReturnPath.CHOKE_LINE
        else -> ReturnPath.BLOCKED
    }

    private fun addAlarm(code: AlarmCode, severity: AlarmSeverity, detail: String = "") {
        if (!gainLogEnabled && (code == AlarmCode.PIT_GAIN || code == AlarmCode.FLOW_WITH_PUMPS_OFF)) return
        if ((alarmSuppressedUntil[code] ?: -1.0) > timeSec) return
        if (code != AlarmCode.INFO && code != AlarmCode.PIT_GAIN && code != AlarmCode.KICK_DETECTED &&
            code in latchedAlarms
        ) return
        val last = lastAlarmAt[code] ?: -100.0
        if (timeSec - last < 8.0 && code != AlarmCode.KICK_DETECTED) return
        lastAlarmAt[code] = timeSec
        if (code != AlarmCode.INFO && code != AlarmCode.PIT_GAIN && code != AlarmCode.KICK_DETECTED) {
            latchedAlarms.add(code)
        }
        alarms.add(0, Alarm(++alarmSeq, code, severity, timeSec, detail))
        if (alarms.size > 80) alarms.removeLastOrNull()
        if (severity == AlarmSeverity.CRITICAL) message = detail.ifBlank { code.name }
        messageUntil = timeSec + 8.0
    }

    private fun setMessage(text: String) {
        message = text
        messageUntil = timeSec + 8.0
    }

    /**
     * Ativa a entrada física de influxo pela zona de kick sem anunciar detecção
     * antes dos indícios primários na superfície (ganho, vazão de retorno, fluxo
     * sem bomba ou fechamento do BOP).
     */
    private fun armInfluxZone(depthM: Double) {
        killSequenceComplete = false
        residualBleedDone = false
        wellKilledLatch = false
        shutInStabilized = false
        shutInStableSec = 0.0
        staticShutInSec = 0.0
        stabilizedSidppKPa = 0.0
        stabilizedSicpKPa = 0.0
        migrationOverpressureKPa = 0.0
        safetySidppExtraKPa = 0.0
        safetySicpExtraKPa = 0.0
        casingSlopeKPaPerS = 0.0
        sidppSlopeKPaPerS = 0.0
        kickStabilizationApplied = false
        circInfluxWarned = false
        if (!mudFrozen && !killSequenceActive) {
            oldMudKgM3 = mudDensityKgM3
            mudFrozen = true
        }
        val tvd = well.tvdAtMd(depthM).coerceIn(0.0, well.tvdM)
        if (kickStartedAt < 0.0) {
            kickStartedAt = timeSec
        }
        influxDepthM = tvd
        influxBaseTvdM = tvd
        influxTopTvdM = (tvd - influxHeightM()).coerceAtLeast(0.0)
        influxPath = when {
            !isClosed() -> InfluxPath.ANNULUS
            hcrOpen -> InfluxPath.CHOKE_LINE
            else -> InfluxPath.BOP
        }
    }

    private fun injectKick(volumeM3: Double, depthM: Double) {
        // Um novo evento de kick reabre o ciclo de controle, mesmo depois de
        // um kill anterior ter liberado a abertura do BOP.
        armInfluxZone(depthM)
        val tvd = well.tvdAtMd(depthM).coerceIn(0.0, well.tvdM)
        val cap = max(kickConfig.maxVolumeM3, volumeM3).coerceAtLeast(0.0)
        if (volumeM3 > 0.0) {
            val seed = volumeM3.coerceAtMost(cap)
            influxVolumeM3 = max(influxVolumeM3, seed)
        }
        influxDepthM = tvd
        influxBaseTvdM = tvd
        influxTopTvdM = (tvd - influxHeightM()).coerceAtLeast(0.0)
        kickDetected = true
        if (kickAt < 0.0) kickAt = timeSec
        phase = if (isClosed()) Phase.SHUT_IN else Phase.KICK_DETECTED
        addMilestone("Kick injetado · ${Units.fmt(Units.m3ToBbl(influxVolumeM3), 1)} bbl")
        setMessage("Kick injetado manualmente: ${Units.fmt(Units.m3ToBbl(influxVolumeM3), 1)} bbl.")
    }

    /**
     * Registra a chegada do gás à superfície. Com retorno alinhado ao
     * separador atmosférico (MGS) é operação controlada; sem alinhamento,
     * caracteriza vazamento e penaliza o score.
     */
    private fun markGasAtSurface(controlled: Boolean) {
        if (gasAtSurface) return
        gasAtSurface = true
        if (controlled) {
            gasReachedSurface = false
            addMilestone("Gás no separador atmosférico (MGS) pelo retorno")
            addAlarm(AlarmCode.INFO, AlarmSeverity.INFO, "Gás no separador atmosférico (MGS)")
        } else {
            gasReachedSurface = true
            addMilestone("Gás chegou à superfície sem separador")
            addAlarm(AlarmCode.GAS_AT_SURFACE, AlarmSeverity.CRITICAL, "Gás chegando à superfície")
        }
    }

    private fun processEvents() {
        scenario.events.forEachIndexed { index, event ->
            if (index in processedEvents || timeSec < event.atSec) return@forEachIndexed
            processedEvents.add(index)
            when (event.type) {
                SimEventType.KICK -> injectKick(
                    if (event.value > 0.0) event.value else kickConfig.volumeM3,
                    kickConfig.depthM
                )
                SimEventType.PUMP_FAILURE -> {
                    pumpFailed = true
                    pumpOn = false
                    targetSpm = 0.0
                    failures.add("Falha da bomba de lama")
                    addAlarm(AlarmCode.PUMP_FAILURE, AlarmSeverity.CRITICAL, "Bomba de lama falhou")
                }
                SimEventType.CHOKE_PLUGGED -> {
                    chokePlugged = true
                    failures.add("Choke plugado")
                    addAlarm(AlarmCode.CHOKE_PLUGGED, AlarmSeverity.WARNING, "Choke plugado")
                }
                SimEventType.CHOKE_WASHED -> {
                    chokeWashed = true
                    failures.add("Choke desgastado")
                    addAlarm(AlarmCode.CHOKE_WASHED, AlarmSeverity.WARNING, "Choke desgastado")
                }
                SimEventType.ACCUMULATOR_LEAK -> {
                    accumulatorKPa = max(scenario.rig.bop.prechargeKPa * 1.05, accumulatorKPa - 6000.0)
                    failures.add("Vazamento no acumulador")
                    addAlarm(AlarmCode.ACCUMULATOR_LOW, AlarmSeverity.WARNING, "Pressão do acumulador baixa")
                }
                SimEventType.LOSSES -> {
                    pitM3 = max(0.0, pitM3 - 2.0)
                    failures.add("Perda de circulação")
                    addAlarm(AlarmCode.PIT_LOSS, AlarmSeverity.WARNING, "Perda de circulação")
                }
                SimEventType.STUCK_PIPE -> {
                    failures.add("Coluna travada")
                    setMessage("Coluna travada: avalie rotação e peso.")
                }
                SimEventType.SENSOR_DRIFT -> {
                    failures.add("Deriva de sensor")
                    setMessage("Sensor de pressão com deriva: confirme por segunda fonte.")
                }
                SimEventType.POWER_LOSS -> {
                    powerLoss = true
                    pumpOn = false
                    targetSpm = 0.0
                    failures.add("Perda de energia")
                    addAlarm(AlarmCode.PUMP_FAILURE, AlarmSeverity.CRITICAL, "Perda de energia")
                }
            }
        }
    }

    fun advance(realSeconds: Double) {
        // Até 64x a UI pede ~2,1 s por frame. O limite acomoda esse passo sem
        // perder velocidade; o guard mantém o custo por frame previsível.
        var remaining = realSeconds.coerceIn(0.0, 2.2)
        var guard = 0
        while (remaining > 0.0 && guard < 48) {
            val dt = min(0.05, remaining)
            step(dt)
            remaining -= dt
            guard++
        }
    }

    private fun step(dt: Double) {
        timeSec += dt
        previousBitDepthM = bitDepthM
        processEvents()

        // Surge / Swab transiente da COP e movimentação de coluna
        val copVel = (copOffsetM - lastCopOffsetM) / dt.coerceAtLeast(0.001)
        lastCopOffsetM = copOffsetM
        val surgeTarget = -copVel * 160.0 * (mudDensityKgM3 / 1000.0)
        surgeKPa += (surgeTarget - surgeKPa) * min(1.0, dt / 0.5)

        // Auto-avanço de procedimentos quando a condição da etapa for satisfeita
        activeProcedure?.let { id ->
            if (procedureStepMet(id, procedureIndex)) {
                procedureStableSec += dt
                if (procedureStableSec >= 1.5) {
                    advanceProcedure()
                    procedureStableSec = 0.0
                }
            } else {
                procedureStableSec = 0.0
            }
        }

        // Rampa de bombas e peso de lama.
        val spmStep = 25.0 * dt
        pumpSpm += (targetSpm - pumpSpm).coerceIn(-spmStep, spmStep)
        if (pumpFailed || powerLoss) pumpSpm = 0.0
        if (pumpOn && pumpSpm > 0.1) {
            totalStrokes += pumpSpm * dt / 60.0
        }
        updateCirculationProgress(dt)
        if (mudDensityKgM3 < targetMudDensityKgM3) {
            mudDensityKgM3 = min(targetMudDensityKgM3, mudDensityKgM3 + 2.4 * dt)
        } else if (mudDensityKgM3 > targetMudDensityKgM3) {
            mudDensityKgM3 = max(targetMudDensityKgM3, mudDensityKgM3 - 2.4 * dt)
        }

        // Rotação, posição da coluna e dinâmica do WOB.
        val rpmTarget = if (rotaryOn) rpmSetpoint else 0.0
        rpm += (rpmTarget - rpm).coerceIn(-45.0 * dt, 45.0 * dt)
        if (rpm < 0.05) rpm = 0.0

        val copFactor = (1.0 - copOffsetM / 1.2).coerceIn(0.0, 1.25)
        val wobTarget = wobSetpointKlb * copFactor
        wobKlb += (wobTarget - wobKlb) * min(1.0, dt / 0.7)
        if (wobKlb < 0.02) wobKlb = 0.0
        val wobOscillation = 1.0 + 0.055 * sin(2.0 * PI * 0.22 * timeSec) +
            0.025 * sin(2.0 * PI * 0.93 * timeSec + 1.1)
        val wobKlbEff = (wobKlb * wobOscillation).coerceAtLeast(0.0)
        wobKn = wobKlbEff * Units.KLB_TO_KN

        // Perfuração: ROP, torque e desgaste respondem ao WOB/RPM reais.
        updateFormation()
        val layer = well.layerAtMd(bitDepthM)
        val hardness = layer?.hardness ?: formationHardness
        val ropFactor = layer?.ropFactor ?: 1.0
        val maxDrillMd = well.mdM + 1000.0
        val canDrill = phase == Phase.DRILLING && !isClosed() && pumpOn && pumpSpm > 5.0 &&
            rotaryOn && rpm > 5.0 && wobKlbEff > 2.0 && bitDepthM < maxDrillMd
        if (canDrill) {
            val wobFactor = ((wobKlbEff - 2.0) / 23.0).coerceIn(0.05, 2.2).pow(0.85)
            val rpmFactor = (rpm / 120.0).coerceIn(0.1, 2.0).pow(0.6)
            val hardFactor = (1.25 - hardness * 0.075).coerceIn(0.3, 1.2)
            val wearFactor = (1.0 - bitWear * 0.7).coerceAtLeast(0.15)
            val founder = if (wobKlbEff > 45.0) {
                (1.0 - (wobKlbEff - 45.0) / 50.0).coerceIn(0.2, 1.0)
            } else 1.0
            val ropRipple = 1.0 + 0.06 * sin(2.0 * PI * 0.15 * timeSec + 0.4) +
                0.025 * sin(2.0 * PI * 1.1 * timeSec)
            ropMHr = (24.0 * wobFactor * rpmFactor * hardFactor * ropFactor * wearFactor *
                founder * ropRipple).coerceIn(0.0, 60.0)
            bitDepthM = min(maxDrillMd, bitDepthM + ropMHr / 3600.0 * dt)
            if (bitDepthM > maxHoleMdM) maxHoleMdM = bitDepthM
            val torqueRipple = 1.0 + 0.03 * sin(2.0 * PI * 0.31 * timeSec + 0.9)
            torqueKNm = ((2.0 + wobKlbEff * 0.28 + rpm * 0.035 +
                bitTvdM * 0.0012 + hardness * 0.25) * torqueRipple).coerceIn(0.0, 80.0)
            bitWear = (bitWear + 0.000007 * hardness * (rpm / 60.0) * dt).coerceAtMost(1.0)
            val targetCuttings = (ropMHr * 0.0008).coerceIn(0.0, 0.025)
            cuttingsLoad += (targetCuttings - cuttingsLoad) * min(1.0, dt / 120.0)
        } else {
            ropMHr = 0.0
            val torqueRipple = 1.0 + 0.03 * sin(2.0 * PI * 0.31 * timeSec + 0.9)
            torqueKNm = if (rotaryOn && rpm > 1.0) {
                ((2.0 + rpm * 0.035 + bitTvdM * 0.0012 + hardness * 0.25) * torqueRipple)
                    .coerceIn(0.0, 80.0)
            } else 0.0
            if (pumpOn) {
                cuttingsLoad += (0.0 - cuttingsLoad) * min(1.0, dt / 240.0)
            }
        }
        if (!pumpOn && !rotaryOn) {
            cuttingsLoad += (0.0 - cuttingsLoad) * min(1.0, dt / 90.0)
        }

        // Trava didática da guia B2: parar a perfuração após detecção de kick
        // não regride ao religar apenas a bomba sem rotação/WOB. Nunca conclui
        // antes de existir kick no ciclo atual.
        if (canDrill) drillingSeen = true
        if (drillingSeen && !pumpOn && !rotaryOn && ropMHr <= 0.5 && wobKlb <= 1.0) {
            drillingStopped = true
        } else if (rotaryOn && wobKlb > 2.0 && ropMHr > 1.0) {
            drillingStopped = false
        }
        if (kickDetected && !pumpOn && pumpSpm <= 0.5 && !rotaryOn && rpm <= 1.0 &&
            ropMHr <= 0.5 && wobKlb <= 1.0
        ) {
            guideDrillingStopped = true
        }
        updateFormation()

        // Gatilho programado de kick (tempo, profundidade, híbrido ou manual).
        // Executado após a atualização da profundidade para que DEPTH dispare
        // antes da entrada de influxo por subbalance.
        val kickMode = kickConfig.triggerMode
        val timeReached = kickConfig.triggerAtSec > 0.0 && timeSec >= kickConfig.triggerAtSec
        // Dispara por cruzamento: estar já abaixo do gatilho (por exemplo,
        // cenário importado) não deve injetar kick no primeiro passo.
        val depthReached = kickConfig.triggerDepthMdM > 0.0 &&
            previousBitDepthM < kickConfig.triggerDepthMdM &&
            bitDepthM >= kickConfig.triggerDepthMdM
        kickArmed = kickConfig.enabled && !kickTriggered && when (kickMode) {
            KickTriggerMode.IMMEDIATE -> true
            KickTriggerMode.TIME -> timeReached
            KickTriggerMode.DEPTH -> depthReached
            KickTriggerMode.HYBRID -> depthReached || timeReached
            KickTriggerMode.MANUAL -> false
        }
        if (kickArmed) {
            kickTriggered = true
            // Ativa a zona de kick na profundidade configurada sem anunciar
            // detecção prematura antes dos indícios primários na superfície.
            armInfluxZone(kickConfig.depthM)
            if (isClosed()) {
                if (kickClosedAt < 0.0) {
                    kickClosedAt = timeSec
                    kickClosingTimeSec = (timeSec - kickStartedAt).coerceAtLeast(0.0)
                    kickVolumeAtCloseM3 = influxVolumeM3
                    kickCasingAtCloseKPa = casingPressureKPa
                    kickTopAtCloseTvdM = influxTopTvdM
                }
            }
        }

        // Entrada de influxo e perdas, regida por equilíbrio de pressões (P_poros vs BHP).
        // O influxo cessa naturalmente quando a pressão no anular iguala a pressão
        // da zona de kick (underbalance <= 0).
        val bhpBefore = bhpKPa()
        val underbalance = poreAtBitKPa() - bhpBefore
        influxRateM3S = 0.0
        lossRateM3S = 0.0
        val carrierLayer = well.layerAtMd(bitDepthM)
        val layerCarrier = carrierLayer == null || carrierLayer.carrierZone
        val inCarrierZone = kickConfig.zoneBaseMdM > kickConfig.zoneTopMdM &&
            bitDepthM >= kickConfig.zoneTopMdM &&
            bitDepthM <= kickConfig.zoneBaseMdM &&
            layerCarrier
        val chokeOpenPath = isClosed() && hcrOpen && chokePct > 0.2 && lineupValid()
        val chokeOpenNoPump = chokeOpenPath && (!pumpOn || pumpSpm <= 0.2)
        val canTakeInflux = kickConfig.enabled && !killSequenceComplete && !wellKilledLatch &&
            (kickTriggered || (inCarrierZone && kickMode != KickTriggerMode.MANUAL))
        val kickCapM3 = kickConfig.maxVolumeM3.coerceAtLeast(0.0)
        val closedCompressCap = if (isClosed() && !chokeOpenNoPump) {
            min(kickCapM3, (if (kickClosedAt >= 0.0) kickVolumeAtCloseM3 else influxVolumeM3) + 0.015)
        } else {
            kickCapM3
        }
        val effectiveCapM3 = closedCompressCap
        val canFlow = underbalance > 0.0 && (!isClosed() || chokeOpenNoPump || (underbalance > 10.0 && influxVolumeM3 < effectiveCapM3 - 1e-9))
        if (kickConfig.enabled && canTakeInflux && canFlow &&
            phase != Phase.KILLED && influxVolumeM3 < effectiveCapM3 - 1e-9
        ) {
            if (kickStartedAt < 0.0) kickStartedAt = timeSec
            val permMd = if (kickConfig.zonePermeabilityMd > 0.0) {
                kickConfig.zonePermeabilityMd
            } else {
                carrierLayer?.permeabilityMd ?: 100.0
            }
            val permScale = (permMd / 100.0).coerceIn(0.2, 3.5)
            val pressureScale = (underbalance / 800.0).coerceIn(0.05, 3.0)
            val closedFactor = if (isClosed() && !chokeOpenNoPump) 0.15 else 1.0
            val physicalRateM3S = (kickConfig.characteristicRateLpm / 60000.0).coerceAtLeast(0.0) *
                permScale * pressureScale * closedFactor
            val remaining = (effectiveCapM3 - influxVolumeM3).coerceAtLeast(0.0)
            influxRateM3S = min(physicalRateM3S, remaining / dt.coerceAtLeast(0.001))
            val added = min(remaining, influxRateM3S * dt)
            if (added > 0.0) {
                val hadInflux = influxVolumeM3 > 0.00001
                val entryMd = if (kickTriggered && kickConfig.depthM > 0.0) {
                    kickConfig.depthM
                } else {
                    bitDepthM
                }
                val entryTvd = well.tvdAtMd(entryMd).coerceIn(0.0, well.tvdM)
                influxVolumeM3 += added
                if (!hadInflux) {
                    influxDepthM = entryTvd
                    influxBaseTvdM = entryTvd
                    influxPath = InfluxPath.ANNULUS
                }
                influxTopTvdM = (influxBaseTvdM - influxHeightM()).coerceAtLeast(0.0)
                kickTriggered = true
                kickArmed = false
                // Congela a lama velha no primeiro influxo (seja por subbalanço,
                // gatilho ou swab) para preservar SIDPP/SICP e kill sheet.
                if (!mudFrozen && !killSequenceActive) {
                    oldMudKgM3 = mudDensityKgM3
                    mudFrozen = true
                }
                if (kickCapM3 - influxVolumeM3 <= 1e-6 && !kickMaxReached) {
                    kickMaxReached = true
                    addMilestone("Volume máximo de kick atingido (${Units.fmt(Units.m3ToBbl(kickCapM3), 1)} bbl)")
                    addAlarm(
                        AlarmCode.KICK_DETECTED,
                        AlarmSeverity.CRITICAL,
                        "Volume máximo de kick atingido: feche o poço imediatamente."
                    )
                }
            }
        }
        val overFrac = shoePressureKPa() - well.fracGradientKPaPerM * well.shoeTvdM
        if (overFrac > 0.0) {
            lossRateM3S = min(0.02, 2.0e-6 * overFrac)
        }

        // Migração, dispersão e expulsão do gás, com caminho explícito.
        if (influxVolumeM3 > 0.00001) {
            val velocity = if (!pumpOn) {
                0.11
            } else {
                (pumpRateLpm() / 60000.0) / annularAreaAt(bitDepthM)
            }
            val move = velocity * dt
            if (influxPath == InfluxPath.CHOKE_LINE) {
                val bopTvd = if (well.isSubsea) well.riser?.waterDepthM ?: 0.0 else 0.0
                influxTopTvdM = max(bopTvd, influxTopTvdM - move * 1.25)
                influxBaseTvdM = max(bopTvd, influxBaseTvdM - move * 1.25)
                if (influxBaseTvdM <= bopTvd + 0.05) {
                    influxTopTvdM = bopTvd
                    influxBaseTvdM = bopTvd
                    if (!gasAtSurface) {
                        // Gás chegou ao separador atmosférico (MGS) via choke line —
                        // operação controlada, não penaliza score nem é vazamento.
                        addMilestone("Gás no separador atmosférico (MGS) pela choke line")
                        gasAtSurface = true
                        gasReachedSurface = false // controlado, não é vazamento
                        addAlarm(AlarmCode.INFO, AlarmSeverity.INFO, "Gás no separador atmosférico (MGS)")
                    }
                }
            } else if (influxPath == InfluxPath.BOP) {
                val bopTvd = if (well.isSubsea) well.riser?.waterDepthM ?: 0.0 else 0.0
                influxTopTvdM = max(bopTvd, influxTopTvdM - move)
                influxBaseTvdM = max(bopTvd, influxBaseTvdM - move)
                if (!well.isSubsea && influxTopTvdM <= 0.0) {
                    // Sem HCR alinhado não há retorno ao separador: vazamento.
                    markGasAtSurface(controlled = false)
                }
            } else {
                influxTopTvdM = max(0.0, influxTopTvdM - move)
                influxBaseTvdM = max(0.0, influxBaseTvdM - move)
                val mudline = well.riser?.waterDepthM ?: 0.0
                if (well.isSubsea && isClosed() && influxTopTvdM <= mudline) {
                    influxPath = InfluxPath.CHOKE_LINE
                    influxTopTvdM = mudline
                    influxBaseTvdM = mudline + influxHeightM()
                    addMilestone("Influxo desviado para a choke line")
                } else if (well.isSubsea && !isClosed() && influxTopTvdM <= mudline) {
                    influxPath = InfluxPath.RISER
                }
                if (!well.isSubsea && influxTopTvdM <= 0.0) {
                    // No poço terrestre o retorno pelo flowline/choke line vai ao
                    // separador atmosférico (MGS), como no submarino: é operação
                    // controlada e não penaliza o score. Só o retorno desalinhado
                    // (sem separador) caracteriza vazamento para a superfície.
                    val controlled = returnRoute() == ReturnPath.FLOWLINE ||
                        returnRoute() == ReturnPath.CHOKE_LINE
                    markGasAtSurface(controlled)
                }
            }
            influxDepthM = influxBaseTvdM
            if (gasAtSurface) {
                if (!chokeAuto && chokePct < 1.0 && isClosed()) {
                    casingPressureKPa += 18.0 * dt
                }
                if (isClosed() && hcrOpen && chokePct > 0.2 && lineupValid()) {
                    val bleedRateM3S = max(0.008, (chokeFlowLpm / 60000.0) * 0.75)
                    influxVolumeM3 = max(0.0, influxVolumeM3 - bleedRateM3S * dt)
                } else if (!isClosed() && pumpOn) {
                    influxVolumeM3 = max(0.0, influxVolumeM3 - 0.005 * dt)
                }
            }
            // Expansão só ocorre com alívio de pressão (caminho aberto), limitada
            // ao teto físico configurado para não fabricar volume artificial.
            if (chokeOpenPath && !pumpOn && influxTopTvdM > 0.0) {
                gasExpansion += 0.002 * dt
                val expCap = max(kickConfig.maxVolumeM3, influxVolumeM3).coerceAtMost(30.0)
                influxVolumeM3 = min(expCap, influxVolumeM3 * (1.0 + 0.0015 * dt))
            }
        } else {
            influxVolumeM3 = 0.0
            if (gasAtSurface) {
                gasAtSurface = false
                addMilestone("Gás expulso do poço — superfície limpa")
            }
        }

        // Fluxo pelo choke.
        val dpPa = (casingPressureKPa - 100.0) * 1000.0
        chokeFlowLpm = 0.0
        val activeChoke = activeChoke()
        if (isClosed() && hcrOpen && chokePct > 0.2 && dpPa > 0.0 && lineupValid()) {
            val area = activeChoke.maxAreaM2 * (chokePct / 100.0).pow(1.15) * activeChoke.cv *
                (if (chokePlugged) 0.10 else 1.0) * (if (chokeWashed) 1.55 else 1.0)
            val density = (if (killSequenceActive) effectiveAnnulusDensity() else mudDensityKgM3)
                .coerceAtLeast(800.0)
            val q = area * sqrt(2.0 * dpPa / density)
            chokeFlowLpm = q * 60000.0
        }

        // Poço estanque: ao desligar a bomba com o BOP fechado, a pressão
        // construída fica trapada se o choke estiver fechado (ou em modo auto).
        // Se o choke manual ficar aberto sem bombeio, o poço despressuriza.
        if (isClosed() && pumpWasOn && !pumpOn) {
            if (chokeAuto) {
                trappedPressureKPa = max(trappedPressureKPa, casingPressureKPa)
                chokeSetpointKPa = trappedPressureKPa
                if (casingPressureKPa <= chokeSetpointKPa + 15.0) {
                    chokePct = 0.0
                }
            } else if (!hcrOpen || chokePct <= 0.2 || !lineupValid()) {
                trappedPressureKPa = max(trappedPressureKPa, casingPressureKPa)
            }
        }
        // Tubo em U: em coluna homogênea a pressão travada não pode exceder o
        // equilíbrio físico (SIDPP). Sem isso, a compressibilidade/fricção
        // durante a circulação deixa o revestimento acima da bengala.
        if (isClosed() && !pumpOn && homogeneousColumn()) {
            trappedPressureKPa = min(trappedPressureKPa, sidppKPa())
        }
        pumpWasOn = pumpOn

        // Controle de pressão no choke / poço fechado.
        if (chokeAuto && isClosed()) {
            // Comando do controlador (mesma lei proporcional), mas o atuador do
            // choke tem inércia: a posição segue o comando com lag de 1ª ordem
            // (~2 s), evitando resposta instantânea a mudanças de bomba.
            val commanded = if (pumpOn && pumpSpm > 0.2) {
                val error = casingPressureKPa - chokeSetpointKPa
                (chokePct + error * 0.0016 * dt).coerceIn(0.0, 100.0)
            } else {
                val excess = casingPressureKPa - chokeSetpointKPa
                if (excess > 15.0) {
                    (chokePct + excess * 0.002 * dt).coerceIn(0.0, 100.0)
                } else {
                    0.0
                }
            }
            val kAct = (1.0 - kotlin.math.exp(-dt / 2.0)).coerceIn(0.0, 1.0)
            chokePct += (commanded - chokePct) * kAct
        }
        val chokeStillOpenNoPump = isClosed() && (!pumpOn || pumpSpm <= 0.2) &&
            hcrOpen && chokePct > 0.2 && lineupValid()
        // Migração de influxo em poço fechado. A sobrepressão cresce com o
        // gradiente da lama vezes a velocidade de ascensão (depende do tipo) e
        // é limitada pela MAASP. Dissipa por sangria, circulação, expulsão ou
        // reabertura do poço.
        if (isClosed() && !pumpOn && !wellKilledLatch && !homogeneousColumn() && !gasAtSurface) {
            val cap = (maaspKPa() - sicpKPa()).coerceAtLeast(0.0)
            migrationOverpressureKPa =
                min(cap, migrationOverpressureKPa + migrationRateKPaPerS() * dt)
        } else if (migrationOverpressureKPa > 0.0) {
            val tau = if (pumpOn || !isClosed()) 15.0 else 30.0
            migrationOverpressureKPa *= kotlin.math.exp(-dt / tau)
            if (migrationOverpressureKPa < 0.5) migrationOverpressureKPa = 0.0
        }

        val inKillOrKilled = killSequenceActive || phase == Phase.KILLED
        // Tubo em U: em coluna homogênea (sem influxo/gás/linha deslocada) a
        // pressão de revestimento iguala o SIDPP; o par registrado do kill sheet
        // continua congelado, mas o manômetro vivo segue a física.
        val liveSicpEquilibrium = when {
            inKillOrKilled -> sicpKPa()
            homogeneousColumn() -> sidppKPa()
            shutInStabilized -> recordedSicpKPa() + migrationOverpressureKPa
            else -> sicpKPa() + migrationOverpressureKPa
        }
        val liveSidppEquilibrium = when {
            inKillOrKilled -> sidppKPa()
            shutInStabilized -> recordedSidppKPa()
            else -> sidppKPa()
        }
        if (isClosed()) {
            if (pumpOn && pumpSpm > 0.2) {
                // Com a bomba ligada o poço parte do equilíbrio de shut-in e
                // recebe a compressão do volume injetado sem retorno. Ao
                // retomar a circulação, a leitura de shut-in já está no gauge.
                val baseTarget = when {
                    wellKilledLatch -> 0.0
                    trappedPressureKPa > 0.0 -> max(trappedPressureKPa, liveSicpEquilibrium)
                    else -> sicpKPa()
                }
                if (casingPressureKPa < 1.0 && baseTarget > 100.0) {
                    casingPressureKPa = baseTarget
                }
                sidppGaugeKPa = 0.0
                val qIn = pumpRateLpm() / 60000.0 + influxRateM3S - lossRateM3S
                val qOut = chokeFlowLpm / 60000.0
                casingPressureKPa += (qIn - qOut) * dt / 0.002
            } else if (chokeStillOpenNoPump) {
                // Sem bombeio e com choke ajustável aberto: não há perda de carga
                // localizada pela restrição do choke -> o poço despressuriza.
                val qOut = chokeFlowLpm / 60000.0
                val dP = min(casingPressureKPa, qOut * dt / 0.0002)
                casingPressureKPa = max(0.0, casingPressureKPa - dP)
                sidppGaugeKPa = max(0.0, sidppGaugeKPa - dP * 0.85)
                pitM3 = min(500.0, pitM3 + dP * 0.0002)
                if (trappedPressureKPa > 0.0 && casingPressureKPa < trappedPressureKPa) {
                    trappedPressureKPa = casingPressureKPa
                }
                shutInStableSec = 0.0
                staticShutInSec = 0.0
                if (!wellKilledLatch && kickDetected && casingPressureKPa > 80.0 &&
                    timeSec - lastOpenChokeWarnAt >= 12.0
                ) {
                    lastOpenChokeWarnAt = timeSec
                    addAlarm(
                        AlarmCode.CASING_HIGH,
                        AlarmSeverity.WARNING,
                        "Choke aberto sem bombeio: poço despressurizando!"
                    )
                    setMessage("Choke aberto sem bombeio: poço despressurizando! Feche o choke para trapar a pressão.")
                }
            } else {
                // Pressão de shut-in com build-up transiente até igualar P_poros:
                // após estabilizar, o par SIDPP/SICP permanece fixo para a mesma lama/kick.
                // Durante ou após a 2ª circulação (lama de matar no poço), o equilíbrio
                // vivo segue sicpKPa()/sidppKPa() e nunca salta de volta para o pré-kill.
                val target = when {
                    wellKilledLatch -> 0.0
                    trappedPressureKPa > 0.0 -> max(trappedPressureKPa, liveSicpEquilibrium)
                    else -> liveSicpEquilibrium
                }
                val sidppTarget = when {
                    wellKilledLatch -> 0.0
                    else -> liveSidppEquilibrium
                }
                if (casingPressureKPa > target + 1.0) {
                    // Decaimento lento por infiltração: usa permeabilidade da zona e
                    // propriedades reológicas da lama (PV, YP) — tempo característico ~minutos.
                    val perm = kickConfig.zonePermeabilityMd.coerceAtLeast(0.1)
                    val pv = scenario.mud.plasticViscosityCp.coerceAtLeast(5.0)
                    val yp = scenario.mud.yieldPointPa.coerceAtLeast(1.0)
                    val filtrateFactor = (perm / 100.0).coerceIn(0.01, 1.0) * (30.0 / (pv + yp * 0.1)).coerceIn(0.02, 0.5)
                    val decayRate = filtrateFactor * dt / 300.0 // TC ~ 5 min base
                    casingPressureKPa -= (casingPressureKPa - target) * decayRate
                } else {
                    val tau = shutInTauSec()
                    val k = (1.0 - kotlin.math.exp(-dt / tau)).coerceIn(0.0, 1.0)
                    casingPressureKPa += (target - casingPressureKPa) * k
                }
                // SIDPP de gauge acompanha o build-up com dinâmica própria
                // (compressibilidade da coluna), sem saltar no fechamento.
                val sidppTau = (shutInTauSec() * 0.7).coerceAtLeast(1.0)
                val kSidpp = (1.0 - kotlin.math.exp(-dt / sidppTau)).coerceIn(0.0, 1.0)
                sidppGaugeKPa += (sidppTarget - sidppGaugeKPa) * kSidpp

                // Migração de gás em poço selado após estabilização no método volumétrico:
                // eleva a pressão viva de revestimento sem alterar o par estabilizado fixo.
                if (shutInStabilized && !wellKilledLatch) {
                    staticShutInSec += dt
                    if (scenario.method == "Volumétrico" && kickConfig.type == InfluxType.GAS &&
                        influxVolumeM3 > 0.1 && staticShutInSec > 5.0
                    ) {
                        casingPressureKPa += 8.0 * dt
                    }
                }
            }
            if (gasAtSurface && chokePct < 0.5 && influxVolumeM3 > 0.05 && !wellKilledLatch &&
                scenario.method == "Volumétrico"
            ) {
                casingPressureKPa += 10.0 * dt
            }
            // Monitor das derivadas primeiras (dP/dt) para a estabilização física.
            val slopeCasing = if (dt > 0.0) (casingPressureKPa - previousCasingKPa) / dt else 0.0
            casingSlopeKPaPerS += (slopeCasing - casingSlopeKPaPerS) * min(1.0, dt / 2.0)
            previousCasingKPa = casingPressureKPa

            val slopeSidpp = if (dt > 0.0) (sidppGaugeKPa - previousSidppGaugeKPa) / dt else 0.0
            sidppSlopeKPaPerS += (slopeSidpp - sidppSlopeKPaPerS) * min(1.0, dt / 2.0)
            previousSidppGaugeKPa = sidppGaugeKPa

            val sealedForStabilization = kickDetected && !pumpOn && !chokeStillOpenNoPump &&
                phase != Phase.KILLED && !wellKilledLatch
            if (sealedForStabilization) {
                val sicpTargetNow = max(trappedPressureKPa, if (shutInStabilized) recordedSicpKPa() else sicpKPa())
                val sidppTargetNow = if (shutInStabilized) recordedSidppKPa() else sidppKPa()
                val sicpTol = max(40.0, sicpTargetNow * 0.03)
                val sidppTol = max(40.0, sidppTargetNow * 0.03)
                val nearTarget = abs(casingPressureKPa - sicpTargetNow) <= sicpTol &&
                    abs(sidppGaugeKPa - sidppTargetNow) <= sidppTol
                val slopeStable = abs(casingSlopeKPaPerS) <= max(2.5, sicpTargetNow * 0.006) &&
                    abs(sidppSlopeKPaPerS) <= max(2.5, sidppTargetNow * 0.006)
                if (nearTarget && slopeStable) shutInStableSec += dt else shutInStableSec = 0.0
                if (!shutInStabilized && shutInStableSec >= 3.0) {
                    shutInStabilized = true
                    stabilizedSidppKPa = sidppKPa()
                    stabilizedSicpKPa = sicpKPa()
                    guideStabilized = true
                }
            } else {
                shutInStableSec = 0.0
            }
            casingPressureKPa = casingPressureKPa.coerceIn(0.0, 90000.0)
        } else {
            casingPressureKPa = max(0.0, casingPressureKPa - 6000.0 * dt)
            sidppGaugeKPa = max(0.0, sidppGaugeKPa - 6000.0 * dt)
            previousCasingKPa = casingPressureKPa
            previousSidppGaugeKPa = sidppGaugeKPa
            casingSlopeKPaPerS = 0.0
            sidppSlopeKPaPerS = 0.0
            shutInStableSec = 0.0
            staticShutInSec = 0.0
        }

        // Acumulador: consumo ao fechar BOP e recarga lenta.
        accumulatorKPa = min(
            scenario.rig.bop.ratedPressureKPa,
            accumulatorKPa + 250.0 * dt
        )

        // Volumes de superfície. A sangria já debita o pit no momento da ação.
        pitM3 += (influxRateM3S - lossRateM3S) * dt
        bleedVolumeM3 = 0.0
        pitM3 = pitM3.coerceIn(0.0, 500.0)

        // Vazão de retorno e pressões de superfície.
        flowOutLpm = when {
            !isClosed() -> pumpRateLpm() + influxRateM3S * 60000.0 - lossRateM3S * 60000.0
            hcrOpen -> chokeFlowLpm + if (gasAtSurface) 25.0 else 0.0
            else -> 0.0
        }
        updateFlowCheck(dt)
        val spp = sppKPa()

        // Limites e alarmes.
        val shoe = shoePressureKPa()
        val maasp = maaspKPa()
        maxCasing = max(maxCasing, casingPressureKPa)
        maxShoe = max(maxShoe, shoe)
        maxPit = max(maxPit, pitM3)
        maxPitGain = max(maxPitGain, influxVolumeM3)
        minBhp = min(minBhp, bhpKPa())

        if (flowOutLpm > 15.0 && pumpSpm < 0.5) {
            addAlarm(AlarmCode.FLOW_WITH_PUMPS_OFF, AlarmSeverity.CRITICAL, "Fluxo com bombas paradas")
        }
        val pitDelta = pitM3 - pitReferenceM3
        val returnDevPct = if (pumpOn && pumpSpm > 10.0 && pumpRateLpm() > 1.0) {
            (flowOutLpm - pumpRateLpm()) / pumpRateLpm() * 100.0
        } else 0.0

        // Detecção primária real: o simulador só declara kick após ocorrer um
        // indício primário observável na superfície (ganho >= alarme, variação
        // de retorno > limite, fluxo com bombas paradas, flow check ou fechamento).
        if (!kickDetected && influxVolumeM3 > 0.002) {
            val pitGainHit = pitDelta >= pitGainAlarmM3
            val returnFlowHit = pumpOn && pumpSpm > 10.0 &&
                kotlin.math.abs(returnDevPct) > returnFlowAlarmPct
            val flowOffHit = !pumpOn && pumpSpm < 0.5 && flowOutLpm > 15.0 && timeSec >= 1.5
            val flowCheckHit = prepDone.contains(PrepCheckId.FLOW_CHECK) && flowCheckFlowLpm > 10.0
            val shutInHit = isClosed() && influxVolumeM3 > 0.005 && timeSec > 0.0
            if (pitGainHit || returnFlowHit || flowOffHit || flowCheckHit || shutInHit) {
                kickDetected = true
                if (kickAt < 0.0) kickAt = timeSec
                if (!isClosed() && phase == Phase.DRILLING) {
                    phase = Phase.KICK_DETECTED
                }
                addMilestone("Kick detectado · ganho de ${Units.fmt(Units.m3ToBbl(influxVolumeM3), 1)} bbl")
                addAlarm(
                    AlarmCode.KICK_DETECTED,
                    AlarmSeverity.CRITICAL,
                    "Kick detectado: indício primário confirmado (${Units.fmt(Units.m3ToBbl(influxVolumeM3), 1)} bbl)"
                )
            }
        }
        if (pitDelta >= pitGainAlarmM3) {
            addAlarm(
                AlarmCode.PIT_GAIN,
                AlarmSeverity.WARNING,
                "Ganho no pit: ${Units.fmt(Units.m3ToBbl(pitDelta), 1)} bbl " +
                    "(limite ${Units.fmt(Units.m3ToBbl(pitGainAlarmM3), 0)} bbl)"
            )
        }
        if (-pitDelta >= pitLossAlarmM3) {
            addAlarm(
                AlarmCode.PIT_LOSS,
                AlarmSeverity.WARNING,
                "Perda no pit: ${Units.fmt(Units.m3ToBbl(-pitDelta), 1)} bbl " +
                    "(limite ${Units.fmt(Units.m3ToBbl(pitLossAlarmM3), 0)} bbl)"
            )
        }
        if (pumpOn && pumpSpm > 10.0) {
            val expected = pumpRateLpm()
            if (expected > 1.0) {
                val deviation = (flowOutLpm - expected) / expected * 100.0
                if (kotlin.math.abs(deviation) > returnFlowAlarmPct) {
                    addAlarm(
                        AlarmCode.RETURN_FLOW_CHANGE,
                        AlarmSeverity.WARNING,
                        "Variação da vazão de retorno: ${Units.fmt(deviation, 0)}% " +
                            "(limite ±${Units.fmt(returnFlowAlarmPct, 0)}%)"
                    )
                }
            }
        }
        if (casingPressureKPa > maasp * 1.02) {
            addAlarm(AlarmCode.MAASP_EXCEEDED, AlarmSeverity.CRITICAL, "MAASP excedida")
        } else if (casingPressureKPa > maasp * 0.9) {
            addAlarm(AlarmCode.CASING_HIGH, AlarmSeverity.WARNING, "Pressão de revestimento próxima da MAASP")
        }
        if (shoe > well.fracGradientKPaPerM * well.shoeTvdM * 1.02) {
            addAlarm(AlarmCode.FRAC_EXCEEDED, AlarmSeverity.CRITICAL, "Pressão na sapata acima da fratura")
        }
        if (accumulatorKPa < scenario.rig.bop.prechargeKPa * 1.2) {
            addAlarm(AlarmCode.ACCUMULATOR_LOW, AlarmSeverity.WARNING, "Acumulador abaixo do mínimo")
        }
        if (chokePlugged) {
            addAlarm(AlarmCode.CHOKE_PLUGGED, AlarmSeverity.WARNING, "Choke plugado")
        }
        if (chokeWashed) {
            addAlarm(AlarmCode.CHOKE_WASHED, AlarmSeverity.WARNING, "Choke desgastado")
        }
        if (spp < 200.0 && pumpOn && pumpSpm > 10.0) {
            addAlarm(AlarmCode.SPP_LOW, AlarmSeverity.WARNING, "SPP baixa com bomba ligada")
        }

        // Estabilização pós-fechamento: se a pressão de revestimento continuar
        // subindo 8 s após o fechamento (poço estático), o volume efetivo
        // recebe um fator de incerteza (até 30%). Se o operador já retomou a
        // circulação, as variações são operacionais e o fator não se aplica.
        if (kickClosedAt >= 0.0 && !kickStabilizationApplied &&
            timeSec - kickClosedAt >= 8.0
        ) {
            kickStabilizationApplied = true
            if (!pumpOn) {
                val reference = max(kickCasingAtCloseKPa, 1.0)
                val pressureRise = (casingPressureKPa - reference) / reference
                // Se o gás migrou, as pressões não estão constantes mesmo que a
                // leitura de superfície varie pouco.
                val migrationM = (kickTopAtCloseTvdM - influxTopTvdM).coerceAtLeast(0.0)
                kickStabilizationFactor = when {
                    pressureRise > 0.02 || migrationM > 3.0 -> 1.30
                    pressureRise > 0.002 || migrationM > 0.5 -> 1.15
                    else -> 1.0
                }
                if (kickStabilizationFactor > 1.0) {
                    val extra = min(
                        kickConfig.maxVolumeM3 - influxVolumeM3,
                        kickVolumeAtCloseM3 * (kickStabilizationFactor - 1.0)
                    ).coerceAtLeast(0.0)
                    if (extra > 0.0) {
                        influxVolumeM3 += extra
                        addMilestone(
                            "Estabilização pós-fechamento: fator ${Units.fmt(kickStabilizationFactor, 2)} aplicado ao volume"
                        )
                    }
                }
            }
        }

        // Linha de base de perfuração: referência dos indícios secundários.
        if (phase == Phase.DRILLING && pumpOn && influxVolumeM3 < 0.001 && !kickDetected) {
            if (!baselineReady) {
                baselineSppKPa = spp
                baselineRopMHr = ropMHr
                baselineWobKlb = Units.knToKlb(wobKn)
                baselineEcdKgM3 = ecdKgM3()
                baselineSpm = pumpSpm
                baselineReady = true
                baselineSinceSec = timeSec
            } else {
                val k = (1.0 - kotlin.math.exp(-dt / 45.0)).coerceIn(0.0, 0.25)
                baselineSppKPa += (spp - baselineSppKPa) * k
                baselineRopMHr += (ropMHr - baselineRopMHr) * k
                baselineWobKlb += (Units.knToKlb(wobKn) - baselineWobKlb) * k
                baselineEcdKgM3 += (ecdKgM3() - baselineEcdKgM3) * k
                baselineSpm += (pumpSpm - baselineSpm) * k
            }
        }

        // Máquina de fases e latches da guia (somente após o fato ocorrer).
        if (!guideKickHandled && kickDetected && timeSec > 0.0) {
            val pitAlarmHit = pitDelta >= pitGainAlarmM3
            val flowOffSeen = !pumpOn && pumpSpm < 0.5 && flowOutLpm > 15.0
            val flowCheckPositive = prepDone.contains(PrepCheckId.FLOW_CHECK) && flowCheckFlowLpm > 10.0
            val shutInOnKick = bopClosed.any { it != BopElement.TEST_RAM && it != BopElement.HCR }
            if (pitAlarmHit || flowOffSeen || flowCheckPositive || shutInOnKick) {
                guideKickHandled = true
            }
        }
        if (!guideBopClosed && kickDetected && timeSec > 0.0 &&
            bopClosed.any { it != BopElement.TEST_RAM && it != BopElement.HCR }
        ) {
            guideBopClosed = true
        }
        if (isClosed() && !pumpOn && phase != Phase.KILLED && phase != Phase.FAILED) {
            if (!shutIn) {
                shutIn = true
                shutInAt = timeSec
                phase = Phase.SHUT_IN
                addMilestone("Poço fechado (shut-in)")
                addAlarm(AlarmCode.INFO, AlarmSeverity.INFO, "Poço fechado")
            } else if (timeSec - shutInAt >= 3.0 && phase == Phase.SHUT_IN && shutInStabilized) {
                phase = Phase.STABILIZED
                guideStabilized = true
                addMilestone("Pressões estabilizadas")
                addAlarm(AlarmCode.STABILIZED, AlarmSeverity.INFO, "Pressões estabilizadas")
            }
        }
        if (kickDetected && pumpOn && isClosed() && hcrOpen && chokePct > 0.2 && lineupValid() &&
            phase != Phase.KILLED &&
            phase != Phase.KICK_CONTROLLED &&
            phase != Phase.DISPLACING_KILL_MUD
        ) {
            if (killStartedAt < 0.0) {
                killStartedAt = timeSec
                firstCircPumpedM3 = 0.0
                firstCirculationReady = false
                firstCirculationReadyAt = -1.0
                circInfluxWarned = false
                freezeKillSheet()
                addMilestone("1ª circulação de kill iniciada")
            }
            phase = Phase.CIRCULATING
            if (!killSequenceActive) killStage = KillStage.FIRST_CIRCULATION
        }
        if (!firstCirculationReady && phase == Phase.CIRCULATING && pumpSpm > 5.0 &&
            firstCircPumpedM3 >= circulationVolumeToSurfaceM3()
        ) {
            if (influxVolumeM3 <= 0.08) {
                firstCirculationReady = true
                firstCirculationReadyAt = timeSec
                addMilestone("1ª circulação em 100% do volume de circulação (anular + choke line)")
            } else if (!circInfluxWarned) {
                circInfluxWarned = true
                addAlarm(
                    AlarmCode.INFO,
                    AlarmSeverity.WARNING,
                    "100% do volume circulado, mas ainda há influxo: mantenha a circulação até expulsar o gás."
                )
            }
        }
        // A etapa conclui sozinha ao atingir 100%, mantendo uma janela curta
        // para o operador ver o contador chegar ao total.
        if (firstCirculationReady && phase == Phase.CIRCULATING &&
            firstCirculationReadyAt >= 0.0 && timeSec - firstCirculationReadyAt >= 2.0
        ) {
            finishFirstCirculation()
        }
        updateKillStage()
        if (!guideWeightedUp && (guideStabilized || guideFirstCirculation) &&
            killSheetFrozen != null && killMudKgM3 > 0.0 &&
            mudDensityKgM3 >= killMudKgM3 - 2.0
        ) {
            guideWeightedUp = true
        }
        if (scenario.method == "Tutorial" && pumpOn && scrTaken) {
            guideTutorialDone = true
        }
        if (scenario.method == "Emergência" && kickDetected && isClosed() &&
            !gasReachedSurface && timeSec > 0.0
        ) {
            guideEdsProtected = true
        }
        if (phase == Phase.KILLED && allRequiredLinesDisplaced()) {
            guideLinesDisplaced = true
        }
        if (maxCasing > maasp * 1.35 && maasp > 0.0) {
            phase = Phase.FAILED
            addMilestone("Falha: limite de pressão excedido")
            addAlarm(AlarmCode.FAILED, AlarmSeverity.CRITICAL, "Limite de pressão excedido")
        }

        // Recomendações didáticas.
        recommendations.clear()
        if (phase == Phase.KICK_DETECTED) {
            recommendations.add("Pare a bomba, faça flow check e feche o BOP.")
            recommendations.add("Registre SIDPP, SICP e ganho no pit antes de circular.")
        }
        if (phase == Phase.SHUT_IN) {
            recommendations.add("Aguarde estabilização e confirme as pressões.")
        }
        if (phase == Phase.STABILIZED) {
            recommendations.add("Calcule a lama de kill e alinhe o choke/kill manifold.")
        }
        if (phase == Phase.CIRCULATING) {
            recommendations.add("1ª circulação: remova o influxo mantendo a lama velha.")
            recommendations.add("Mantenha a pressão de revestimento dentro da MAASP.")
        }
        if (phase == Phase.KICK_CONTROLLED) {
            recommendations.add("Pressão trapada: pese a lama até a KMW antes da 2ª circulação.")
            recommendations.add("Abra a segunda circulação e siga ICP → FCP no kill sheet.")
        }
        if (phase == Phase.DISPLACING_KILL_MUD) {
            recommendations.add("2ª circulação: substitua a lama velha pela lama de matar.")
            recommendations.add("Choke automático segue o kill sheet; não deixe a pressão cair.")
        }
        if (phase == Phase.KILLED && killStage == KillStage.LINE_DISPLACEMENT) {
            recommendations.add("Poço morto: desloque kill line, booster e anular do riser.")
        }
        if (phase == Phase.KILLED && killStage == KillStage.READY_TO_OPEN) {
            recommendations.add("Sangre a pressão residual pelo choke até ≈ 0 e abra o BOP.")
        }
        if (maxCasing > maasp * 0.9) {
            recommendations.add("Abra o choke com cuidado: pressão próxima da MAASP.")
        }
        if (maxShoe > well.fracGradientKPaPerM * well.shoeTvdM) {
            recommendations.add("Houve risco de perda: reduza a pressão na sapata.")
        }

        if (message != null && timeSec > messageUntil) {
            message = null
        }

        if (timeSec - lastTrendSec >= 1.0 || lastTrendSec < 0.0) {
            addTrend()
        }
    }

    private fun addTrend(force: Boolean = false) {
        if (!force && timeSec - lastTrendSec < 1.0) return
        lastTrendSec = timeSec
        trends.add(
            TrendPoint(
                timeSec = timeSec,
                sppKPa = sppKPa(),
                casingKPa = casingPressureKPa,
                pitM3 = pitM3,
                chokePct = chokePct,
                bhpKPa = bhpKPa(),
                shoeKPa = shoePressureKPa(),
                ropMHr = ropMHr,
                influxM3 = influxVolumeM3
            )
        )
        if (trends.size > 900) trends.removeFirst()
    }

    fun setPump(on: Boolean, spm: Double) {
        if (pumpFailed || powerLoss) {
            setMessage("Bomba indisponível.")
            return
        }
        if (on && !isClosed() && standpipeLineup == ManifoldLineup.TRIP) {
            setMessage("Alinhado em TRIP: volte os manifolds para Perfuração antes de ligar a bomba principal.")
            return
        }
        pumpOn = on
        targetSpm = if (on) spm.coerceIn(0.0, pump.maxSpm) else 0.0
        if (on && phase == Phase.DRILLING) {
            // Nova circulação: reancora o pit de referência para o alarme de ganho/perda.
            pitReferenceM3 = pitM3
        }
        if (on && !isClosed() && influxVolumeM3 < 0.01 &&
            (phase == Phase.SHUT_IN || phase == Phase.CIRCULATING)
        ) {
            phase = Phase.DRILLING
        }
        if (!on) {
            targetSpm = 0.0
            baselineReady = false
            if (phase == Phase.CIRCULATING) phase = Phase.SHUT_IN
        }
        setMessage(if (on) "Bomba ligada: ${Units.fmt(spm)} SPM" else "Bomba parada")
    }

    fun setSpm(spm: Double) {
        targetSpm = spm.coerceIn(0.0, pump.maxSpm)
    }

    fun setDrilling(wobKn: Double, rpm: Double) {
        wobSetpointKlb = Units.knToKlb(wobKn).coerceIn(0.0, 60.0)
        rpmSetpoint = rpm.coerceIn(0.0, 220.0)
        rotaryOn = rpm > 0.5
        setMessage(
            "Perfuração: WOB ${Units.fmt(wobSetpointKlb, 1)} Klb · " +
                "${Units.fmt(rpmSetpoint)} RPM"
        )
    }

    fun setWobSetpointKlb(value: Double) {
        wobSetpointKlb = value.coerceIn(0.0, 60.0)
    }

    fun setRpmSetpoint(value: Double) {
        rpmSetpoint = value.coerceIn(0.0, 220.0)
        if (value > 0.5) rotaryOn = true
    }

    fun setRotary(on: Boolean) {
        rotaryOn = on
        setMessage(if (on) "Rotação ligada: ${Units.fmt(rpmSetpoint)} RPM" else "Rotação desligada")
    }

    fun setCopOffset(offsetM: Double) {
        copOffsetM = offsetM.coerceIn(-1.5, 2.0)
        setMessage(
            if (copOffsetM >= 0.0) "COP levantada ${Units.fmt(copOffsetM, 1)} m"
            else "COP baixada ${Units.fmt(-copOffsetM, 1)} m"
        )
    }

    fun nudgeCop(deltaM: Double) {
        setCopOffset(copOffsetM + deltaM)
    }

    /** Registra a pressão reduzida de circulação na taxa atual (SCR medida). */
    fun takeScr(): Boolean {
        if (!pumpOn || pumpSpm <= 5.0) {
            setMessage("Ligue a bomba e estabilize a vazão para tomar a SCR.")
            return false
        }
        if (isClosed()) {
            setMessage("Abra o BOP para tomar a SCR com o poço em circulação.")
            return false
        }
        val measured = sppKPa()
        if (measured <= 0.0) {
            setMessage("SPP inválida para registrar a SCR.")
            return false
        }
        val ripple = 1.0 + 0.018 * kotlin.math.sin(timeSec * 0.85) +
            0.009 * kotlin.math.sin(timeSec * 3.1 + 0.7)
        scrKPa = measured / ripple
        scrRateLpm = pumpRateLpm().coerceAtLeast(1.0)
        val modelLoss = HydraulicModel.calculate(
            well = well,
            string = scenario.drillString,
            bitDepthM = bitDepthM,
            rateLpm = scrRateLpm,
            mudDensityRefKgM3 = mudDensityKgM3,
            cuttingsFraction = cuttingsLoad,
            bitWear = bitWear,
            hydraulicScale = 1.0
        ).totalKPa
        if (modelLoss > 1.0) {
            hydraulicScale = (scrKPa / modelLoss).coerceIn(0.2, 5.0)
        }
        scrTaken = true
        scrSource = ScrSource.MEASURED
        val bblMin = Units.lpmToBblMin(scrRateLpm)
        addMilestone(
            "SCR medida: ${Units.fmt(Units.kpaToPsi(measured))} psi a " +
                "${Units.fmt(bblMin, 1)} bbl/min"
        )
        setMessage(
            "SCR registrada: ${Units.fmt(Units.kpaToPsi(measured))} psi a " +
                "${Units.fmt(pumpSpm)} SPM (${Units.fmt(bblMin, 1)} bbl/min)."
        )
        return true
    }

    // --- Fase A: medições de baixa vazão, alarmes e preparo -----------------

    /**
     * Ajusta a bomba para a vazão alvo em gpm. A bomba rampa até o alvo,
     * então aguarde a estabilização antes de registrar a leitura.
     */
    fun setRateGpm(gpm: Double) {
        val lpm = Units.gpmToLpm(gpm.coerceIn(0.0, 2000.0))
        setSpm(lpm / pump.displacementLPerStroke.coerceAtLeast(0.1))
    }

    /**
     * Registra a leitura de SPP em uma das vazões reduzidas do preparo
     * (50, 100, 150 ou 200 gpm). Exige bomba estável, BOP aberto e vazão
     * dentro de 12% do alvo.
     */
    fun takeScrAt(gpm: Int): Boolean {
        val allowed = setOf(50, 100, 150, 200)
        if (gpm !in allowed) {
            setMessage("Vazões de preparo válidas: 50, 100, 150 e 200 gpm.")
            return false
        }
        if (!pumpOn || pumpSpm <= 5.0) {
            setMessage("Ligue a bomba e estabilize a vazão antes de registrar a leitura de $gpm gpm.")
            return false
        }
        if (isClosed()) {
            setMessage("Abra o BOP para tomar as leituras de baixa vazão com o poço circulando.")
            return false
        }
        val target = Units.gpmToLpm(gpm.toDouble())
        val measuredRate = pumpRateLpm()
        if (kotlin.math.abs(measuredRate - target) / target > 0.12) {
            setMessage(
                "Ajuste a bomba para $gpm gpm (atual ${Units.fmt(pumpRateGpm(), 0)} gpm) e aguarde estabilizar."
            )
            return false
        }
        val measured = sppKPa()
        if (measured <= 0.0) {
            setMessage("SPP inválida para registrar a leitura de $gpm gpm.")
            return false
        }
        val ripple = 1.0 + 0.018 * kotlin.math.sin(timeSec * 0.85) +
            0.009 * kotlin.math.sin(timeSec * 3.1 + 0.7)
        val reading = measured / ripple
        scrAtRate[gpm] = reading
        prepDone.add(
            when (gpm) {
                50 -> PrepCheckId.SCR_50
                100 -> PrepCheckId.SCR_100
                150 -> PrepCheckId.SCR_150
                else -> PrepCheckId.SCR_200
            }
        )
        addMilestone(
            "Leitura de $gpm gpm: ${Units.fmt(Units.kpaToPsi(reading))} psi"
        )
        setMessage(
            "Leitura registrada a $gpm gpm: ${Units.fmt(Units.kpaToPsi(reading))} psi."
        )
        return true
    }

    /**
     * Registra o Delta P da linha de controle (choke line) na vazão atual.
     * Exige poço fechado e HCR aberto, condição em que a linha está alinhada.
     */
    fun takeChokeLineDeltaP(): Boolean {
        if (!isClosed()) {
            setMessage("Feche o BOP para medir o Delta P da linha de controle (Delta P C.L.).")
            return false
        }
        if (!hcrOpen) {
            setMessage("Abra o HCR: a linha de controle precisa estar alinhada para a leitura.")
            return false
        }
        if (!pumpOn || pumpSpm <= 5.0) {
            setMessage("Ligue a bomba em vazão reduzida para medir o Delta P C.L.")
            return false
        }
        val deltaP = HydraulicModel.lineFrictionKPa(
            idIn = well.lines.chokeLineIdIn,
            lengthM = well.lines.chokeLineLengthM,
            rateLpm = pumpRateLpm(),
            densityKgM3 = effectiveAnnulusDensity()
        )
        if (deltaP <= 0.05) {
            setMessage("Delta P C.L. desprezível nessa vazão. Aumente a vazão e repita.")
            return false
        }
        chokeLineDeltaPKPa = deltaP
        prepDone.add(PrepCheckId.DELTA_P_CHOKE_LINE)
        addMilestone("Delta P C.L.: ${Units.fmt(Units.kpaToPsi(deltaP))} psi")
        setMessage(
            "Delta P C.L. registrado: ${Units.fmt(Units.kpaToPsi(deltaP))} psi a " +
                "${Units.fmt(pumpRateGpm(), 0)} gpm."
        )
        return true
    }

    /** Alarme de ganho no pit em barris (mínimo 5 bbl, sem limite superior rígido). */
    fun setGainAlarmBbl(bbl: Double) {
        pitGainAlarmM3 = Units.bblToM3(bbl.coerceIn(1.0, 100.0))
        prepDone.add(PrepCheckId.GAIN_ALARM)
        setMessage("Alarme de ganho calibrado em ${Units.fmt(Units.m3ToBbl(pitGainAlarmM3), 0)} bbl.")
    }

    /** Alarme de perda no pit em barris. */
    fun setLossAlarmBbl(bbl: Double) {
        pitLossAlarmM3 = Units.bblToM3(bbl.coerceIn(1.0, 100.0))
        prepDone.add(PrepCheckId.LOSS_ALARM)
        setMessage("Alarme de perda calibrado em ${Units.fmt(Units.m3ToBbl(pitLossAlarmM3), 0)} bbl.")
    }

    /** Alarme de variação da vazão de retorno em percentual (usual ±10%). */
    fun setReturnFlowAlarmPct(pct: Double) {
        returnFlowAlarmPct = pct.coerceIn(2.0, 50.0)
        prepDone.add(PrepCheckId.RETURN_FLOW_ALARM)
        setMessage("Alarme de vazão de retorno calibrado em ±${Units.fmt(returnFlowAlarmPct, 0)}%.")
    }

    /** Reancora o pit de referência após completar o volume de superfície. */
    fun resetPitReference() {
        pitReferenceM3 = pitM3
        setMessage("Pit de referência reancorado em ${Units.fmt(pitM3, 1)} m³.")
    }

    fun prepChecks(): List<PrepCheck> = PrepCheckId.values().map { id ->
        val value = when (id) {
            PrepCheckId.SCR_50 -> scrAtRate[50] ?: 0.0
            PrepCheckId.SCR_100 -> scrAtRate[100] ?: 0.0
            PrepCheckId.SCR_150 -> scrAtRate[150] ?: 0.0
            PrepCheckId.SCR_200 -> scrAtRate[200] ?: 0.0
            PrepCheckId.GAIN_ALARM -> Units.m3ToBbl(pitGainAlarmM3)
            PrepCheckId.LOSS_ALARM -> Units.m3ToBbl(pitLossAlarmM3)
            PrepCheckId.RETURN_FLOW_ALARM -> returnFlowAlarmPct
            PrepCheckId.DELTA_P_CHOKE_LINE -> Units.kpaToPsi(chokeLineDeltaPKPa)
            else -> 0.0
        }
        val done = when (id) {
            PrepCheckId.BOP_TEST -> scenario.rig.bop.elements.all { it in bopEverClosed }
            else -> id in prepDone
        }
        PrepCheck(id = id, done = done, value = value)
    }

    fun prepComplete(): Boolean = prepChecks().all { it.done }

    fun startProcedure(id: ProcedureId) {
        activeProcedure = id
        procedureIndex = 0
        procedureStepAt = timeSec
        setMessage("Procedimento iniciado: ${procedureTitlePt(id)}")
    }

    fun cancelProcedure() {
        activeProcedure = null
        procedureIndex = 0
        setMessage("Procedimento cancelado")
    }

    fun advanceProcedure() {
        val id = activeProcedure ?: return
        if (!procedureStepMet(id, procedureIndex)) {
            setMessage(procedureHintPt(id, procedureIndex))
            return
        }
        if (id == ProcedureId.SCR && procedureIndex == 2) {
            if (!takeScr()) return
        }
        if (id == ProcedureId.FLOW_CHECK && procedureIndex == 2) {
            flowCheckArmed = false
            flowCheckStableSec = FLOW_CHECK_STABLE_SEC
            flowCheckFlowLpm = flowOutLpm
            prepDone.add(PrepCheckId.FLOW_CHECK)
        }
        procedureIndex++
        procedureStepAt = timeSec
        if (procedureIndex >= procedureStepCount(id)) {
            activeProcedure = null
            procedureIndex = 0
            addMilestone("Procedimento concluído: ${procedureTitlePt(id)}")
            setMessage("Procedimento concluído: ${procedureTitlePt(id)}")
        }
    }

    private fun procedureStepCount(id: ProcedureId): Int = when (id) {
        ProcedureId.SCR -> 4
        ProcedureId.FLOW_CHECK -> 3
        ProcedureId.TRIP_TANK -> 3
        ProcedureId.BOP_TEST -> 3
        ProcedureId.MANIFOLD_LINEUP -> 3
    }

    private fun procedureStepMet(id: ProcedureId, step: Int): Boolean = when (id) {
        ProcedureId.SCR -> when (step) {
            0 -> pumpOn && pumpSpm > 5.0 && !isClosed() &&
                abs(pumpSpm - scrRateLpm / pump.displacementLPerStroke) <= 6.0
            1 -> pumpOn && timeSec - procedureStepAt >= 5.0 &&
                abs(pumpSpm - targetSpm) < 1.0
            2 -> pumpOn && !isClosed()
            else -> true
        }
        ProcedureId.FLOW_CHECK -> when (step) {
            0 -> !pumpOn || pumpSpm < 0.5
            1 -> timeSec - procedureStepAt >= 10.0
            else -> true
        }
        ProcedureId.TRIP_TANK -> when (step) {
            0 -> !pumpOn
            1 -> standpipeLineup == ManifoldLineup.TRIP || chokeLineup == ManifoldLineup.TRIP
            else -> true
        }
        ProcedureId.BOP_TEST -> when (step) {
            0 -> BopElement.TEST_RAM in bopClosed
            1 -> accumulatorKPa >= scenario.rig.bop.prechargeKPa * 1.2
            2 -> BopElement.TEST_RAM !in bopClosed
            else -> true
        }
        ProcedureId.MANIFOLD_LINEUP -> when (step) {
            0 -> standpipeLineup == ManifoldLineup.KILL
            1 -> chokeLineup == ManifoldLineup.KILL
            2 -> hcrOpen
            else -> true
        }
    }

    private fun procedureTitlePt(id: ProcedureId): String = when (id) {
        ProcedureId.SCR -> "Tomada de SCR"
        ProcedureId.FLOW_CHECK -> "Flow check"
        ProcedureId.TRIP_TANK -> "Trip tank e preenchimento"
        ProcedureId.BOP_TEST -> "Teste funcional do BOP"
        ProcedureId.MANIFOLD_LINEUP -> "Alinhamento de kill"
    }

    private fun procedureHintPt(id: ProcedureId, step: Int): String = when (id) {
        ProcedureId.SCR -> when (step) {
            0 -> "Ligue a bomba e ajuste para a taxa de kill (~${Units.fmt(scrRateLpm)} L/min)."
            1 -> "Aguarde 5 s com vazão e SPP estáveis."
            2 -> "Confirme para registrar SPP, SPM e vazão."
            else -> "Confirme os valores registrados."
        }
        ProcedureId.FLOW_CHECK -> when (step) {
            0 -> "Pare a bomba antes do flow check."
            1 -> "Aguarde 10 s observando o retorno."
            2 -> "Confirme o resultado do flow check."
            else -> "Confirme."
        }
        ProcedureId.TRIP_TANK -> when (step) {
            0 -> "Pare a bomba para manobrar."
            1 -> "Alinhe o trip tank (standpipe ou choke)."
            2 -> "Confirme o volume e o preenchimento."
            else -> "Confirme."
        }
        ProcedureId.BOP_TEST -> when (step) {
            0 -> "Feche o test ram."
            1 -> "Confirme a pressão do acumulador."
            2 -> "Abra o test ram."
            else -> "Confirme."
        }
        ProcedureId.MANIFOLD_LINEUP -> when (step) {
            0 -> "Alinhe o standpipe para kill."
            1 -> "Alinhe o choke manifold para kill."
            2 -> "Abra o HCR."
            else -> "Confirme."
        }
    }

    private fun procedureSnapshot(): ProcedureSnapshot? {
        val id = activeProcedure ?: return null
        val titlesPt = when (id) {
            ProcedureId.SCR -> listOf(
                "Bomba na taxa de kill", "Estabilizar vazão e SPP",
                "Registrar SCR", "Confirmar valores"
            )
            ProcedureId.FLOW_CHECK -> listOf("Parar a bomba", "Aguardar 10 s", "Registrar resultado")
            ProcedureId.TRIP_TANK -> listOf("Parar a bomba", "Alinhar trip tank", "Confirmar volume")
            ProcedureId.BOP_TEST -> listOf("Fechar test ram", "Checar acumulador", "Abrir test ram")
            ProcedureId.MANIFOLD_LINEUP -> listOf("Alinhar standpipe", "Alinhar choke", "Abrir HCR")
        }
        val titlesEn = when (id) {
            ProcedureId.SCR -> listOf(
                "Pump at kill rate", "Stabilize rate and SPP", "Record SCR", "Confirm values"
            )
            ProcedureId.FLOW_CHECK -> listOf("Stop the pump", "Wait 10 s", "Record result")
            ProcedureId.TRIP_TANK -> listOf("Stop the pump", "Line up trip tank", "Confirm volume")
            ProcedureId.BOP_TEST -> listOf("Close test ram", "Check accumulator", "Open test ram")
            ProcedureId.MANIFOLD_LINEUP -> listOf("Line up standpipe", "Line up choke", "Open HCR")
        }
        val current = procedureIndex.coerceIn(0, procedureStepCount(id) - 1)
        return ProcedureSnapshot(
            id = id,
            titlePt = procedureTitlePt(id),
            titleEn = when (id) {
                ProcedureId.SCR -> "Slow circulating rate"
                ProcedureId.FLOW_CHECK -> "Flow check"
                ProcedureId.TRIP_TANK -> "Trip tank and fill"
                ProcedureId.BOP_TEST -> "BOP function test"
                ProcedureId.MANIFOLD_LINEUP -> "Kill lineup"
            },
            steps = titlesPt.indices.map { i ->
                ProcedureStep(titlesPt[i], titlesEn[i], done = i < procedureIndex)
            },
            currentIndex = current,
            hintPt = procedureHintPt(id, current),
            hintEn = "",
            complete = false
        )
    }

    fun setAlarmSound(enabled: Boolean) {
        alarmSoundEnabled = enabled
        setMessage(if (enabled) "Som de alarme ativado" else "Som de alarme silenciado")
    }

    fun setGainLog(enabled: Boolean) {
        gainLogEnabled = enabled
        if (enabled) {
            alarmSuppressedUntil.remove(AlarmCode.PIT_GAIN)
            alarmSuppressedUntil.remove(AlarmCode.FLOW_WITH_PUMPS_OFF)
            setMessage("Log de ganho retomado")
        } else {
            alarmSuppressedUntil[AlarmCode.PIT_GAIN] = Double.MAX_VALUE
            alarmSuppressedUntil[AlarmCode.FLOW_WITH_PUMPS_OFF] = Double.MAX_VALUE
            setMessage("Log de ganho pausado")
        }
    }

    fun muteAlarm(code: AlarmCode, seconds: Double = 120.0) {
        alarmSuppressedUntil[code] = timeSec + seconds
        setMessage("Alarme ${code.name} silenciado por ${Units.fmt(seconds / 60.0, 1)} min")
    }

    fun setLineups(standpipe: ManifoldLineup, choke: ManifoldLineup) {
        standpipeLineup = standpipe
        chokeLineup = choke
        // O HCR permanece sob controle manual do operador. Se o alinhamento
        // exigir a choke line e o HCR estiver fechado, apenas avisamos.
        val needsChoke = standpipe == ManifoldLineup.KILL || choke == ManifoldLineup.KILL ||
            choke == ManifoldLineup.CHOKE || choke == ManifoldLineup.EDS
        if (!lineupValid()) {
            addAlarm(AlarmCode.INFO, AlarmSeverity.WARNING, "Alinhamento inválido: BOP aberto para manobra")
            setMessage("Alinhamento inválido: feche o BOP antes do alinhamento de manobra.")
        } else if (needsChoke && isClosed() && !hcrOpen) {
            addAlarm(
                AlarmCode.INFO,
                AlarmSeverity.WARNING,
                "HCR fechado: abra o HCR para retorno pela choke line."
            )
            setMessage(
                "Manifolds alinhados: ${standpipe.name} / ${activeChoke().name}. " +
                    "Abra o HCR para alinhar o retorno pela choke line."
            )
        } else {
            setMessage("Manifolds alinhados: ${standpipe.name} / ${activeChoke().name}")
        }
    }

    /**
     * Ajusta a zona/volume/vazão do kick antes do gatilho. Depois que o
     * influxo já entrou, os parâmetros ficam congelados para preservar o
     * histórico do poço.
     */
    fun updateKickConfig(config: KickConfig): Boolean {
        if (kickDetected || kickTriggered) {
            setMessage("Kick já detectado: configure a zona antes do gatilho.")
            return false
        }
        kickConfig = config.copy(
            zoneTopMdM = min(config.zoneTopMdM, config.zoneBaseMdM),
            zoneBaseMdM = max(config.zoneTopMdM, config.zoneBaseMdM),
            maxVolumeM3 = config.maxVolumeM3.coerceIn(0.1, 50.0),
            characteristicRateLpm = config.characteristicRateLpm.coerceIn(1.0, 10000.0)
        )
        setMessage("Configuração do kick atualizada.")
        return true
    }

    fun triggerManualKick(volumeM3: Double = kickConfig.volumeM3) {
        if (kickTriggered) {
            setMessage("Kick já iniciado neste cenário.")
            return
        }
        kickTriggered = true
        kickArmed = false
        injectKick(volumeM3, bitDepthM)
    }

    fun setChoke(pct: Double) {
        chokePct = pct.coerceIn(0.0, 100.0)
        if (chokeAuto) chokeSetpointKPa = casingPressureKPa
        setMessage("Choke: ${Units.fmt(chokePct)}%")
    }

    fun setChokeAuto(enabled: Boolean, setpointKPa: Double = casingPressureKPa) {
        chokeAuto = enabled
        chokeSetpointKPa = setpointKPa.coerceAtLeast(0.0)
        setMessage(if (enabled) "Choke automático ativo" else "Choke manual")
    }

    fun setChokeSetpoint(setpointKPa: Double) {
        chokeSetpointKPa = setpointKPa.coerceAtLeast(0.0)
    }

    fun setActiveChoke(index: Int) {
        if (index in chokes.indices) {
            activeChokeIndex = index
            val name = chokes[index].name
            setMessage("Choke ativo: $name")
            if (pumpOn) {
                setMessage("Choke ativo alterado para $name — reajuste a abertura e observe a pressão de casing.")
            }
        }
    }

    fun closeBop(element: BopElement) {
        if (element == BopElement.HCR) {
            hcrOpen = true
            bopEverClosed.add(BopElement.HCR)
            setMessage("HCR aberto: linha de choke alinhada")
            return
        }
        bopClosed.add(element)
        bopEverClosed.add(element)
        if (influxVolumeM3 > 0.005 && element != BopElement.TEST_RAM) {
            kickDetected = true
        }
        if (kickDetected && element != BopElement.TEST_RAM) {
            guideBopClosed = true
        }
        accumulatorKPa = max(scenario.rig.bop.prechargeKPa, accumulatorKPa - 900.0)
        if (kickStartedAt >= 0.0 && kickClosedAt < 0.0 && element != BopElement.TEST_RAM) {
            kickClosedAt = timeSec
            kickClosingTimeSec = (timeSec - kickStartedAt).coerceAtLeast(0.0)
            kickVolumeAtCloseM3 = influxVolumeM3
            kickCasingAtCloseKPa = casingPressureKPa
            kickTopAtCloseTvdM = influxTopTvdM
            shutInStabilized = false
            shutInStableSec = 0.0
            staticShutInSec = 0.0
            casingSlopeKPaPerS = 0.0
            sidppSlopeKPaPerS = 0.0
            previousCasingKPa = casingPressureKPa
            previousSidppGaugeKPa = sidppGaugeKPa
        }
        setMessage("BOP fechado: ${element.name}")
        if (!pumpOn) {
            shutIn = true
            shutInAt = timeSec
            phase = Phase.SHUT_IN
        }
    }

    /** Abre um elemento individual do BOP (útil no teste funcional da Fase A e em TEST_RAM). */
    fun openBopElement(element: BopElement) {
        if (element == BopElement.HCR) {
            setHcr(false)
            return
        }
        if (element !in bopClosed) return
        val remainingSealing = bopClosed.count { it != element && it != BopElement.TEST_RAM && it != BopElement.HCR }
        if (kickDetected && !killSequenceComplete && remainingSealing == 0 && element != BopElement.TEST_RAM) {
            val reason = openBopBlockReason()
            if (reason != null) {
                setMessage("Abertura bloqueada: $reason")
                return
            }
            openBop()
            return
        }
        bopClosed.remove(element)
        if (bopClosed.isEmpty()) {
            shutIn = false
            if (!kickDetected && phase == Phase.SHUT_IN) {
                phase = Phase.DRILLING
            }
        }
        setMessage("BOP aberto: ${element.name}")
    }

    fun openBop() {
        val reason = openBopBlockReason()
        if (reason != null) {
            setMessage("Abertura bloqueada: $reason")
            return
        }
        bopClosed.clear()
        shutIn = false
        if (!kickDetected) {
            if (phase == Phase.SHUT_IN) phase = Phase.DRILLING
            casingPressureKPa = 0.0
            sidppGaugeKPa = 0.0
            previousCasingKPa = 0.0
            previousSidppGaugeKPa = 0.0
            casingSlopeKPaPerS = 0.0
            sidppSlopeKPaPerS = 0.0
            shutInStableSec = 0.0
            setMessage("BOP aberto.")
            return
        }
        val finalDensity = effectiveAnnulusDensity()
        killSequenceComplete = true
        guideCycleComplete = true
        scoreBonusLatch = true
        guideBleedDone = true
        guideLinesDisplaced = true
        wellKilledLatch = true
        casingPressureKPa = 0.0
        trappedPressureKPa = 0.0
        migrationOverpressureKPa = 0.0
        chokeSetpointKPa = 0.0
        sidppGaugeKPa = 0.0
        previousCasingKPa = 0.0
        previousSidppGaugeKPa = 0.0
        casingSlopeKPaPerS = 0.0
        sidppSlopeKPaPerS = 0.0
        shutInStabilized = false
        shutInStableSec = 0.0
        staticShutInSec = 0.0
        // O poço passa a conter a lama de matar efetiva; sem isso o pit mostra
        // o peso novo, mas a hidrostática continua com a lama antiga.
        mudFrozen = false
        killSequenceActive = false
        oldMudKgM3 = finalDensity
        mudDensityKgM3 = finalDensity
        targetMudDensityKgM3 = finalDensity
        if (influxVolumeM3 < 0.1) influxVolumeM3 = 0.0
        phase = if (influxVolumeM3 > 0.1) Phase.KICK_DETECTED else Phase.DRILLING
        if (phase == Phase.DRILLING) killStage = KillStage.NONE
        residualBleedDone = true
        addMilestone("BOP aberto com poço morto e linhas deslocadas")
        setMessage("BOP aberto: poço morto, lama de matar estabilizada.")
    }

    fun openBopBlockedReason(): String? = openBopBlockReason()

    /**
     * Reinicia o checklist do guia e retoma a perfuração na profundidade
     * atual, preservando profundidade, lama, picos e score já conquistados.
     * O kill sheet congelado é descartado e o próximo kick fica em modo manual.
     */
    fun resetGuideAndContinue(): Boolean {
        if (phase == Phase.FAILED) {
            setMessage("Simulação em falha: use o reset completo.")
            return false
        }
        val preservedScore = score().toDouble()
        if (killSequenceComplete || phase == Phase.KILLED) scoreBonusLatch = true
        bopClosed.clear()
        bopEverClosed.clear()
        shutIn = false
        hcrOpen = false
        killSequenceComplete = false
        residualBleedDone = false
        wellKilledLatch = false
        kickDetected = false
        kickTriggered = false
        kickArmed = false
        kickMaxReached = false
        kickStartedAt = -1.0
        kickClosedAt = -1.0
        kickClosingTimeSec = 0.0
        kickVolumeAtCloseM3 = 0.0
        kickCasingAtCloseKPa = 0.0
        kickAt = -1.0
        previousBitDepthM = bitDepthM
        influxVolumeM3 = 0.0
        influxRateM3S = 0.0
        influxTopTvdM = 0.0
        influxBaseTvdM = 0.0
        influxDepthM = bitTvdM
        gasAtSurface = false
        casingPressureKPa = 0.0
        trappedPressureKPa = 0.0
        migrationOverpressureKPa = 0.0
        chokeSetpointKPa = 0.0
        chokePct = 0.0
        chokeAuto = false
        sidppGaugeKPa = 0.0
        previousCasingKPa = 0.0
        previousSidppGaugeKPa = 0.0
        casingSlopeKPaPerS = 0.0
        sidppSlopeKPaPerS = 0.0
        shutInStabilized = false
        shutInStableSec = 0.0
        staticShutInSec = 0.0
        stabilizedSidppKPa = 0.0
        stabilizedSicpKPa = 0.0
        migrationOverpressureKPa = 0.0
        safetySidppExtraKPa = 0.0
        safetySicpExtraKPa = 0.0
        kickStabilizationApplied = false
        circInfluxWarned = false
        killSheetFrozen = null
        killMudKgM3 = 0.0
        killMudPumpedM3 = 0.0
        stringKillFraction = 0.0
        annulusKillFraction = 0.0
        chokeLineKillFraction = 0.0
        killSequenceActive = false
        killStage = KillStage.NONE
        killStartedAt = -1.0
        firstCirculationReady = false
        firstCirculationReadyAt = -1.0
        firstCircPumpedM3 = 0.0
        pumpedVolumeM3 = 0.0
        mudFrozen = false
        oldMudKgM3 = mudDensityKgM3
        effectiveMudKgM3 = mudDensityKgM3
        targetMudDensityKgM3 = mudDensityKgM3
        lineProgressMap.clear()
        linesDisplacedSet.clear()
        prepDone.clear()
        scrAtRate.clear()
        flowCheckArmed = false
        flowCheckStableSec = 0.0
        flowCheckFlowLpm = 0.0
        drillingStopped = false
        drillingSeen = false
        baselineReady = false
        recommendations.clear()
        // Próximo kick: manual, na profundidade atual.
        kickConfig = kickConfig.copy(
            triggerMode = KickTriggerMode.MANUAL,
            triggerAtSec = 0.0,
            triggerDepthMdM = 0.0,
            depthM = bitDepthM,
            zoneTopMdM = bitDepthM,
            zoneBaseMdM = bitDepthM
        )
        guideKickHandled = false
        guideDrillingStopped = false
        guideBopClosed = false
        guideStabilized = false
        guideKillSheetReady = false
        guideFirstCirculation = false
        guideWeightedUp = false
        guideSecondCirculation = false
        guideLinesDisplaced = false
        guideBleedDone = false
        guideCycleComplete = false
        guideVolumetricBleedDone = false
        guideLubricateDone = false
        guideEdsProtected = false
        guideTutorialDone = false
        lubricateVolumeAddedM3 = 0.0
        phase = Phase.DRILLING
        pitReferenceM3 = pitM3
        // Fixa os pontos já conquistados: o checklist recomeça, o score não.
        scoreOffset = (preservedScore - rawScore()).coerceAtLeast(0.0)
        addMilestone("Guia reiniciada: perfuração retomada em ${Units.fmt(bitDepthM)} m")
        setMessage(
            "Guia reiniciada. Profundidade e score preservados; " +
                "novo kick disponível no modo manual."
        )
        return true
    }

    fun setHcr(open: Boolean) {
        hcrOpen = open
        if (open) bopEverClosed.add(BopElement.HCR)
        setMessage(if (open) "HCR aberto" else "HCR fechado")
    }

    fun weightUp(targetKgM3: Double) {
        targetMudDensityKgM3 = targetKgM3.coerceIn(900.0, 2200.0)
        setMessage("Pesando lama para ${Units.fmt(Units.kgM3ToPpg(targetMudDensityKgM3), 1)} ppg")
    }

    fun weightUpToKillMud() {
        val calculated = killSheetFrozen?.killMudKgM3
            ?: killSheetResult()?.killMudKgM3
        if (calculated == null || calculated <= 0.0) {
            setMessage("Sem kill sheet ativo: aguarde um kick e estabilize o poço fechado.")
            return
        }
        if (shutInStabilized || guideStabilized || guideFirstCirculation || phase == Phase.KICK_CONTROLLED) {
            if (killSheetFrozen == null) {
                killSheetFrozen = killSheetResult()
                killMudKgM3 = calculated
            }
            guideKillSheetReady = true
        }
        // Nunca reduz um alvo manual mais pesado já definido pelo operador.
        weightUp(max(calculated, targetMudDensityKgM3))
    }

    /**
     * Conclui a 1ª circulação manualmente.
     * Só disponível quando firstCirculationReady = true (influxo removido e volume bombeado suficiente).
     * Mantém bomba ligada enquanto houver bombeio; se a bomba estiver parada com
     * choke automático, o choke fecha para trapar a pressão até a 2ª circulação.
     */
    fun finishFirstCirculation(): Boolean {
        if (!firstCirculationReady || phase != Phase.CIRCULATING) {
            setMessage("A 1ª circulação ainda não está pronta. Bombeie 100% do volume de circulação (anular + choke line).")
            return false
        }
        firstCirculationReady = false
        firstCirculationReadyAt = -1.0
        if (influxVolumeM3 <= 0.08) {
            influxVolumeM3 = 0.0
            gasAtSurface = false
        }
        phase = Phase.KICK_CONTROLLED
        killStage = KillStage.KICK_CONTROLLED
        trappedPressureKPa = casingPressureKPa.coerceAtLeast(recordedSicpKPa())
        if (chokeAuto) {
            chokeSetpointKPa = trappedPressureKPa
            if (!pumpOn || pumpSpm <= 0.2) {
                chokePct = 0.0
            }
        }
        freezeKillSheet()
        guideKillSheetReady = true
        guideFirstCirculation = true
        guideStabilized = true
        addMilestone(
            "1ª circulação concluída: pressão trapada preservada " +
                "(${Units.fmt(Units.kpaToPsi(trappedPressureKPa))} psi); pese a lama"
        )
        addAlarm(AlarmCode.KILL_COMPLETE, AlarmSeverity.INFO, "1ª circulação concluída: pressão trapada")
        setMessage(
            "Pressão trapada: ${Units.fmt(Units.kpaToPsi(trappedPressureKPa))} psi. " +
                "Pese a lama até a KMW; ao parar a bomba, mantenha o choke fechado para não despressurizar."
        )
        return true
    }

    /**
     * Inicia a circulação com lama de matar:
     * - No Método do Sondador, exige concluir a 1ª circulação (`KICK_CONTROLLED`) e pesar a lama.
     * - No Wait & Weight, permite iniciar diretamente após estabilizar e pesar a lama.
     */
    fun startSecondCirculation(): Boolean {
        val waitAndWeightReady = scenario.method == "Wait & Weight" &&
            (phase == Phase.STABILIZED || (phase == Phase.SHUT_IN && shutInStabilized))
        if (phase != Phase.KICK_CONTROLLED && !waitAndWeightReady) {
            setMessage(
                if (scenario.method == "Wait & Weight") {
                    "Aguarde estabilizar SIDPP/SICP e pese a lama antes da circulação Wait & Weight."
                } else {
                    "Conclua a 1ª circulação e mantenha a pressão trapada antes da 2ª circulação."
                }
            )
            return false
        }
        freezeKillSheet()
        val sheet = killSheetFrozen
        val calculated = sheet?.killMudKgM3 ?: killMudKgM3
        // O peso manual mais pesado prevalece; a KMW é o piso de segurança.
        val target = max(calculated, targetMudDensityKgM3)
        if (target <= 0.0) {
            setMessage("Kill sheet indisponível: estabilize SIDPP/SICP antes de circular.")
            return false
        }
        if (mudDensityKgM3 < target - 2.0) {
            setMessage(
                "Pese a lama até ${Units.fmt(Units.kgM3ToPpg(target), 1)} ppg " +
                    "(KMW + margem) antes de iniciar a circulação com lama de matar."
            )
            return false
        }
        if (trappedPressureKPa <= 0.0) {
            trappedPressureKPa = casingPressureKPa.coerceAtLeast(recordedSicpKPa())
        }
        killSequenceActive = true
        killMudKgM3 = target
        killMudPumpedM3 = 0.0
        stringKillFraction = 0.0
        annulusKillFraction = 0.0
        chokeLineKillFraction = 0.0
        phase = Phase.DISPLACING_KILL_MUD
        killStage = KillStage.STRING
        guideKillSheetReady = true
        guideWeightedUp = true
        if (chokeAuto) chokeSetpointKPa = killCasingTargetKPa()
        addMilestone(
            if (scenario.method == "Wait & Weight") {
                "Circulação Wait & Weight iniciada: lama de matar a caminho da broca"
            } else {
                "2ª circulação iniciada: lama de matar a caminho da broca"
            }
        )
        setMessage(
            if (scenario.method == "Wait & Weight") {
                "Wait & Weight: siga ICP → FCP e mantenha o choke no kill sheet."
            } else {
                "2ª circulação: siga ICP → FCP e mantenha o choke no kill sheet."
            }
        )
        return true
    }

    /** Desloca uma fração da linha (0,25 = 25%) com lama de matar. */
    fun displaceLine(kind: LineKind, fraction: Double = 0.25): Boolean {
        if (phase != Phase.KILLED) {
            setMessage("Desloque as linhas somente depois de matar o poço.")
            return false
        }
        if (kind !in requiredLineKinds()) {
            setMessage("Linha não exigida neste cenário.")
            return false
        }
        if (killStage == KillStage.READY_TO_OPEN) {
            setMessage("Linhas já deslocadas: sangre a pressão residual e abra o BOP.")
            return false
        }
        val current = lineProgressOf(kind)
        val next = (current + fraction.coerceIn(0.05, 1.0)).coerceAtMost(1.0)
        lineProgressMap[kind] = next
        if (next >= 0.999 && kind !in linesDisplacedSet) {
            linesDisplacedSet.add(kind)
            addMilestone("Linha deslocada: ${lineKindName(kind)}")
        }
        if (allRequiredLinesDisplaced()) {
            killStage = KillStage.READY_TO_OPEN
            guideLinesDisplaced = true
            addMilestone("Todas as linhas deslocadas com lama de matar")
        }
        setMessage(
            "${lineKindName(kind)}: ${Units.fmt(next * 100.0)}% deslocada" +
                if (allRequiredLinesDisplaced()) " · sangre a pressão residual" else ""
        )
        return true
    }

    /** Sangra a pressão residual trapada, liberando a abertura do BOP. */
    fun bleedResidualPressure(): Boolean {
        if (phase != Phase.KILLED) {
            setMessage("Sangre a pressão residual somente com o poço morto.")
            return false
        }
        if (!allRequiredLinesDisplaced()) {
            setMessage("Desloque todas as linhas antes de sangrar a pressão residual.")
            return false
        }
        // Sangrar não resolve subbalanço: se a formação ainda empurra o casing,
        // a pressão volta em segundos. Primeiro o poço precisa estar morto.
        val formationPressure = sicpKPa()
        if (formationPressure > 100.0) {
            residualBleedDone = false
            addAlarm(
                AlarmCode.KILL_COMPLETE,
                AlarmSeverity.WARNING,
                "Poço ainda pressurizado: a lama não equilibra a formação."
            )
            setMessage(
                "Poço ainda pressurizado (SICP ${Units.fmt(Units.kpaToPsi(formationPressure))} psi). " +
                    "Não sangre: ajuste o peso da lama até a KMW."
            )
            return false
        }
        val before = casingPressureKPa
        casingPressureKPa = 0.0
        trappedPressureKPa = 0.0
        migrationOverpressureKPa = 0.0
        chokeSetpointKPa = 0.0
        sidppGaugeKPa = 0.0
        previousCasingKPa = 0.0
        casingSlopeKPaPerS = 0.0
        shutInStabilized = false
        shutInStableSec = 0.0
        if (influxVolumeM3 < 0.1) influxVolumeM3 = 0.0
        wellKilledLatch = true
        residualBleedDone = true
        guideBleedDone = true
        if (before > 0.0) {
            addMilestone("Pressão residual sangrada: pronto para abrir o BOP")
        }
        setMessage(
            "Pressão residual sangrada (${Units.fmt(Units.kpaToPsi(before))} → 0 psi). " +
                "BOP liberado para abertura."
        )
        return true
    }

    private fun lineKindName(kind: LineKind): String = when (kind) {
        LineKind.KILL_LINE -> "Kill line"
        LineKind.CHOKE_LINE -> "Choke line"
        LineKind.BOOSTER_LINE -> "Booster line"
        LineKind.RISER_ANNULUS -> "Anular do riser"
    }

    fun bleed(volumeM3: Double) {
        if (!isClosed()) {
            setMessage("Feche o BOP antes de sangrar.")
            return
        }
        if (!hcrOpen || chokePct <= 0.2 || !lineupValid()) {
            setMessage("Abra o HCR e o choke (e valide o alinhamento) para sangrar.")
            return
        }
        if (volumeM3 <= 0.0 || casingPressureKPa <= 5.0) {
            setMessage("Sem pressão suficiente para sangrar.")
            return
        }
        pitM3 = min(500.0, pitM3 + volumeM3)
        bleedVolumeM3 += volumeM3
        casingPressureKPa = max(0.0, casingPressureKPa - volumeM3 / 0.002 * 0.35)
        if (gasAtSurface && influxVolumeM3 > 0.0) {
            influxVolumeM3 = max(0.0, influxVolumeM3 - volumeM3 * 0.8)
        }
        if (scenario.method == "Volumétrico") {
            guideVolumetricBleedDone = true
        }
        if (scenario.method == "Lubrificar & sangrar" && lubricateVolumeAddedM3 > 0.0) {
            guideLubricateDone = true
        }
        setMessage("Sangrado ${Units.fmt(Units.m3ToBbl(volumeM3), 1)} bbl")
    }

    fun lubricate(volumeM3: Double) {
        if (!isClosed()) {
            setMessage("Feche o BOP antes de lubrificar.")
            return
        }
        if (pitM3 <= volumeM3) {
            setMessage("Volume insuficiente no pit.")
            return
        }
        pitM3 -= volumeM3
        lubricateVolumeAddedM3 += volumeM3
        casingPressureKPa += volumeM3 / 0.002 * 0.25
        trappedPressureKPa = max(trappedPressureKPa, casingPressureKPa)
        setMessage("Lubrificado ${Units.fmt(Units.m3ToBbl(volumeM3), 1)} bbl")
    }

    /**
     * Arma o flow check. Se as bombas estiverem rodando, elas são paradas e
     * a medição começa assim que a rampa chegar a zero. A conclusão do item
     * A1 acontece após [FLOW_CHECK_STABLE_SEC] segundos de poço estático.
     */
    fun flowCheck() {
        if (prepDone.contains(PrepCheckId.FLOW_CHECK)) {
            setMessage("Flow check já registrado para este poço.")
            return
        }
        if (pumpSpm > 0.5 || pumpOn) {
            setPump(false, 0.0)
            flowCheckStableSec = 0.0
            flowCheckArmed = true
            setMessage("Flow check armado: aguardando as bombas pararem para medir o retorno.")
            return
        }
        flowCheckStableSec = 0.0
        flowCheckArmed = true
        setMessage("Flow check em andamento: meça o retorno com as bombas paradas.")
    }

    private fun updateFlowCheck(dt: Double) {
        if (!flowCheckArmed) return
        if (pumpSpm > 0.5) {
            flowCheckStableSec = 0.0
            return
        }
        flowCheckStableSec += dt
        if (flowCheckStableSec < FLOW_CHECK_STABLE_SEC) return
        flowCheckArmed = false
        flowCheckFlowLpm = flowOutLpm
        prepDone.add(PrepCheckId.FLOW_CHECK)
        setMessage(
            if (flowOutLpm > 10.0) {
                "FLOW CHECK: fluxo presente (${Units.fmt(flowOutLpm)} L/min). Feche o poço."
            } else {
                "FLOW CHECK: sem fluxo. Poço estático."
            }
        )
    }

    fun lineUpKill() {
        if (!isClosed()) {
            setMessage("Feche o BOP antes de alinhar o kill manifold para controle.")
            return
        }
        standpipeLineup = ManifoldLineup.KILL
        chokeLineup = ManifoldLineup.KILL
        hcrOpen = true
        setMessage("Linhas de kill alinhadas: standpipe e choke em KILL, HCR aberto.")
    }

    fun tripPipe(deltaM: Double) {
        if (isClosed()) {
            setMessage("Abra o BOP antes de manobrar a coluna.")
            return
        }
        if (pumpOn || pumpSpm > 0.5) {
            setMessage("Pare as bombas antes de manobrar.")
            return
        }
        if (rotaryOn || rpm > 1.0) {
            setMessage("Desligue a rotação antes de manobrar.")
            return
        }
        if (!lineupValid()) {
            setMessage("Alinhamento incompatível para manobra com BOP aberto.")
            return
        }
        if (standpipeLineup == ManifoldLineup.KILL || standpipeLineup == ManifoldLineup.CHOKE ||
            chokeLineup == ManifoldLineup.KILL || chokeLineup == ManifoldLineup.CHOKE
        ) {
            setMessage("Volte os manifolds para Perfuração ou Trip antes de manobrar.")
            return
        }
        val previous = bitDepthM
        val limitMd = max(well.mdM, maxHoleMdM)
        bitDepthM = (bitDepthM + deltaM).coerceIn(0.0, limitMd)
        val moved = abs(bitDepthM - previous)
        if (moved < 1e-6) {
            setMessage("Coluna no limite do poço.")
            return
        }
        // Deslocamento real de aço da coluna (OD² - ID²): retirada cria déficit
        // de preenchimento (reduzindo coluna hidrostática até ser preenchido);
        // descida preenche o déficit e desloca excedente para trip tank ou pit.
        val steelDispM3 = geometry.pipeSteelVolumeBetween(
            min(previous, bitDepthM),
            max(previous, bitDepthM)
        )
        if (deltaM < 0.0) {
            holeFillM3 += steelDispM3
            // Swab: reduz a pressão de fundo e pode puxar influxo da zona.
            val swab = 180.0 * moved / 9.0
            if (kickConfig.enabled && !kickTriggered &&
                poreAtBitKPa() - (bhpKPa() - swab) > 0.0
            ) {
                influxVolumeM3 += 0.03
                influxDepthM = bitTvdM
                influxBaseTvdM = bitTvdM
                influxTopTvdM = (bitTvdM - influxHeightM()).coerceAtLeast(0.0)
                influxPath = InfluxPath.ANNULUS
                kickDetected = true
                kickTriggered = true
                // Congela lama velha também no influxo por swab
                if (!mudFrozen && !killSequenceActive) {
                    oldMudKgM3 = mudDensityKgM3
                    mudFrozen = true
                }
                addMilestone("Swab: influxo durante manobra")
                addAlarm(AlarmCode.KICK_DETECTED, AlarmSeverity.CRITICAL, "Swab: influxo durante manobra")
            }
        } else {
            val fill = min(holeFillM3, steelDispM3)
            holeFillM3 = max(0.0, holeFillM3 - fill)
            val overflow = (steelDispM3 - fill).coerceAtLeast(0.0)
            if (overflow > 0.0) {
                if (standpipeLineup == ManifoldLineup.TRIP && chokeLineup == ManifoldLineup.TRIP) {
                    tripTankM3 = min(50.0, tripTankM3 + overflow)
                } else {
                    pitM3 = min(500.0, pitM3 + overflow)
                }
            }
            surgeKPa = max(surgeKPa, 160.0 * moved / 9.0)
        }
        setMessage("Coluna em ${Units.fmt(bitDepthM)} m MD (${Units.fmt(bitTvdM)} m TVD)")
    }

    fun fillHole(volumeM3: Double) {
        if (isClosed()) {
            setMessage("Abra o BOP antes de preencher o poço pelo trip tank.")
            return
        }
        if (pumpOn || pumpSpm > 0.5) {
            setMessage("Com a bomba ligada o poço já está em circulação.")
            return
        }
        if (!lineupValid()) {
            setMessage("Alinhamento inválido para preenchimento.")
            return
        }
        if (holeFillM3 <= 0.001) {
            setMessage("Poço já está cheio.")
            return
        }
        val requested = min(volumeM3.coerceAtLeast(0.0), holeFillM3)
        if (requested <= 0.0 || tripTankM3 < requested) {
            setMessage("Volume insuficiente no Trip Tank: reabasteça pelo pit antes de preencher.")
            return
        }
        tripTankM3 -= requested
        holeFillM3 = max(0.0, holeFillM3 - requested)
        setMessage("Poço preenchido pelo Trip Tank: ${Units.fmt(Units.m3ToBbl(requested), 2)} bbl")
    }

    fun transferPitToTripTank(volumeM3: Double = 1.0) {
        if (volumeM3 <= 0.0 || pitM3 <= volumeM3) {
            setMessage("Volume insuficiente no pit para transferir ao Trip Tank.")
            return
        }
        pitM3 -= volumeM3
        tripTankM3 += volumeM3
        setMessage("Transferido ${Units.fmt(Units.m3ToBbl(volumeM3), 1)} bbl do pit para o Trip Tank.")
    }

    fun acknowledgeAlarm(id: Long) {
        alarms.removeAll { it.id == id }
    }

    fun clearMessage() {
        message = null
    }

    fun killSheetResult(): KillSheetResult? {
        // Sem kick no ciclo atual não existe kill sheet: a planilha é um
        // documento do poço fechado, não um cálculo livre de lama.
        if (!kickDetected && !killSequenceActive) return killSheetFrozen
        return killSheetFrozen ?: computeKillSheet()
    }

    private fun computeKillSheet(): KillSheetResult? {
        val sidpp = recordedSidppKPa()
        val sicp = recordedSicpKPa()
        if (sidpp <= 0.1 && sicp <= 0.1) return null
        val output = pump.displacementLPerStroke
        val strokesToBit = geometry.strokesToBit(bitDepthM, output)
        val strokesToShoe = geometry.strokesToShoe(bitDepthM, output)
        val total = geometry.totalAnnularStrokes(bitDepthM, output)
        return KillSheetCalculator.calculate(
            KillSheetInput(
                currentMudKgM3 = inWellDensity(),
                tvdM = kickReferenceTvdM(),
                sidppKPa = sidpp,
                sicpKPa = sicp,
                scrKPa = scrKPa,
                killRateLpm = scrRateLpm,
                pumpOutputLPerStroke = output,
                shoeTvdM = well.shoeTvdM,
                fracGradientKPaPerM = well.fracGradientKPaPerM,
                strokesToBit = strokesToBit,
                strokesToShoe = strokesToShoe,
                totalStrokes = total,
                chokeLineFrictionKPa = chokeLineFrictionKPa(),
                waterDepthM = well.riser?.waterDepthM ?: 0.0,
                subsea = well.isSubsea,
                scrEstimated = !scrTaken,
                marginKgM3 = scenario.mud.killMarginKgM3
            )
        )
    }

    /**
     * Score bruto do ciclo atual, sem o offset acumulado. É recalculado do
     * zero a cada snapshot, então não guarda memória entre ciclos.
     */
    private fun rawScore(): Double {
        var value = 100.0
        val maasp = maaspKPa().coerceAtLeast(1.0)
        value -= ((maxCasing / maasp) * 12.0).coerceIn(0.0, 45.0)
        value -= (maxPitGain / scenario.limits.maxPitGainM3 * 10.0).coerceIn(0.0, 20.0)
        if (phase == Phase.FAILED) value -= 25.0
        if (!kickDetected) value -= 5.0
        if (phase == Phase.KILLED || killSequenceComplete || scoreBonusLatch) value += 5.0
        if (shutIn && pumpSpm > 0.5 && chokePct <= 0.5) value -= 8.0
        if (gasReachedSurface) value -= 8.0
        return value.coerceIn(0.0, 100.0)
    }

    /**
     * Score exibido. O offset é fixado no reset parcial da guia para que os
     * pontos já conquistados não sejam perdidos ao reiniciar o checklist.
     */
    private fun score(): Int =
        (rawScore() + scoreOffset).coerceIn(0.0, 100.0).toInt()

    private fun riserMarginKPa(): Double {
        val riser = well.riser ?: return 0.0
        val seawater = 1025.0
        val density = effectiveAnnulusDensity()
        val seawaterHydro = Units.hydrostaticKpa(seawater, riser.waterDepthM)
        val mudAtMudline = Units.hydrostaticKpa(density, riser.waterDepthM)
        return max(0.0, (mudAtMudline - seawaterHydro) - (well.porePressureKPa - Units.hydrostaticKpa(density, well.tvdM - riser.waterDepthM)))
    }

    fun snapshot(): SimSnapshot {
        val bhp = bhpKPa()
        val maasp = maaspKPa()
        val spp = sppKPa()
        val losses = hydraulicLosses()
        // ECD em densidade equivalente (rebaixamento térmico embutido).
        val ecd = ecdKgM3()
        // Indícios secundários: comparação com a linha de base de perfuração.
        val secondary = mutableSetOf<SecondaryIndicator>()
        if (baselineReady && timeSec - baselineSinceSec >= 15.0) {
            if (ropMHr > baselineRopMHr * 1.35 && ropMHr - baselineRopMHr > 1.0) {
                secondary.add(SecondaryIndicator.ROP_UP)
            }
            if (baselineWobKlb > 1.0 && wobKlb < baselineWobKlb * 0.75) {
                secondary.add(SecondaryIndicator.WOB_DOWN)
            }
            if (pumpOn && baselineSppKPa > 0.0 && spp < baselineSppKPa * 0.88) {
                secondary.add(SecondaryIndicator.SPP_DOWN)
            }
            if (ecd > baselineEcdKgM3 + 12.0) {
                secondary.add(SecondaryIndicator.ECD_UP)
            }
            if (pumpSpm - baselineSpm > 6.0) {
                secondary.add(SecondaryIndicator.PUMP_ACCEL)
            }
            if (pumpOn && pumpRateLpm() > 1.0 &&
                kotlin.math.abs(flowOutLpm - pumpRateLpm()) / pumpRateLpm() * 100.0 > returnFlowAlarmPct
            ) {
                secondary.add(SecondaryIndicator.RETURN_FLOW_CHANGE)
            }
        }

        // Condição de kick por densidade equivalente.
        val poreEq = poreEquivalentKgM3()
        val layer = well.layerAtMd(bitDepthM)
        val insideKickWindow = kickConfig.enabled &&
            kickConfig.zoneBaseMdM > kickConfig.zoneTopMdM &&
            bitDepthM >= kickConfig.zoneTopMdM &&
            bitDepthM <= kickConfig.zoneBaseMdM &&
            (layer == null || layer.carrierZone)
        val kickCondition = when {
            poreEq <= maxOf(ecd, mudDensityKgM3) + 2.0 -> KickCondition.NONE
            insideKickWindow && poreEq > mudDensityKgM3 + 2.0 -> KickCondition.INTERNAL_HIGH_ZONE
            pumpOn -> KickCondition.PORE_GT_ECD
            else -> KickCondition.PORE_GT_HYDROSTATIC
        }
        val flowDeviation = if (pumpOn && pumpRateLpm() > 1.0) {
            (flowOutLpm - pumpRateLpm()) / pumpRateLpm() * 100.0
        } else 0.0
        val circulationTotalM3 = circulationVolumeToSurfaceM3()
        val circulationPumpedM3 = firstCircPumpedM3.coerceIn(0.0, circulationTotalM3)
        val pumpDisplacement = pump.displacementLPerStroke.coerceAtLeast(0.001)
        val circulationStrokes = geometry.strokesForVolume(circulationPumpedM3, pumpDisplacement)
        val circulationTotalStrokes = geometry.strokesForVolume(circulationTotalM3, pumpDisplacement)
        val injection = injectionRoute()
        val returnPath = returnRoute()

        return SimSnapshot(
            timeSec = timeSec,
            phase = phase,
            stackType = scenario.stackType,
            method = scenario.method,
            bitDepthM = bitDepthM,
            bitTvdM = bitTvdM,
            holeDepthM = max(maxHoleMdM, bitDepthM),
            holeTvdM = well.tvdAtMd(max(maxHoleMdM, bitDepthM)),
            wobKn = wobKn,
            wobKlb = Units.knToKlb(wobKn),
            wobSetpointKlb = wobSetpointKlb,
            rpm = rpm,
            rpmSetpoint = rpmSetpoint,
            rotaryOn = rotaryOn,
            copOffsetM = copOffsetM,
            ropMHr = ropMHr,
            torqueKNm = torqueKNm,
            cuttingsLoad = cuttingsLoad,
            bitWear = bitWear,
            formationName = formationName,
            formationType = formationType,
            formationHardness = formationHardness,
            mudDensityKgM3 = mudDensityKgM3,
            targetMudDensityKgM3 = targetMudDensityKgM3,
            pitM3 = pitM3,
            tripTankM3 = tripTankM3,
            sppKPa = spp,
            losses = losses,
            scrKPa = scrKPa,
            scrRateLpm = scrRateLpm,
            scrTaken = scrTaken,
            scrSource = scrSource,
            casingPressureKPa = casingPressureKPa,
            sidppKPa = sidppKPa(),
            sicpKPa = sicpKPa(),
            bhpKPa = bhp,
            shoePressureKPa = shoePressureKPa(),
            ecdKgM3 = ecd,
            maaspKPa = maasp,
            maspKPa = sicpKPa() + chokeLineFrictionKPa(),
            porePressureKPa = well.porePressureKPa,
            fracPressureKPa = well.fracGradientKPaPerM * well.shoeTvdM,
            influxM3 = influxVolumeM3,
            influxType = kickConfig.type,
            influxDepthM = influxDepthM,
            influxTopTvdM = influxTopTvdM,
            influxBaseTvdM = influxBaseTvdM,
            influxPath = influxPath,
            kickZoneTopMdM = kickConfig.zoneTopMdM,
            kickZoneBaseMdM = kickConfig.zoneBaseMdM,
            kickArmed = kickArmed,
            kickTriggered = kickTriggered,
            kickTriggerMode = kickConfig.triggerMode,
            gasAtSurface = gasAtSurface,
            flowInLpm = pumpRateLpm(),
            flowOutLpm = flowOutLpm,
            chokePct = chokePct,
            chokeAuto = chokeAuto,
            chokeSetpointKPa = chokeSetpointKPa,
            hcrOpen = hcrOpen,
            pumpOn = pumpOn,
            pumpSpm = pumpSpm,
            pumpRateLpm = pumpRateLpm(),
            pumpPressureKPa = spp,
            accumulatorKPa = accumulatorKPa,
            bopClosed = bopClosed.toSet(),
            closedBop = isClosed(),
            alarms = alarms.toList(),
            trends = trends.toList(),
            score = score(),
            kickDetected = kickDetected,
            shutIn = shutIn,
            failures = failures.toList(),
            totalStrokes = totalStrokes,
            killSheet = killSheetResult(),
            message = message,
            recommendations = recommendations.toList(),
            riserMarginKPa = riserMarginKPa(),
            maxCasingKPa = maxCasing,
            maxShoeKPa = maxShoe,
            maxPitGainM3 = maxPitGain,
            minBhpKPa = if (minBhp == Double.MAX_VALUE) bhp else minBhp,
            alarmSoundEnabled = alarmSoundEnabled,
            gainLogEnabled = gainLogEnabled,
            standpipeLineup = standpipeLineup,
            chokeLineup = chokeLineup,
            lineupValid = lineupValid(),
            holeFillM3 = holeFillM3,
            milestones = milestones.toList(),
            procedure = procedureSnapshot(),
            bitIncDeg = well.incAtMd(bitDepthM),
            holeIncDeg = well.incAtMd(max(well.mdM, bitDepthM)),
            tempAtBitC = ThermalModel.tempAtTvd(well, bitTvdM),
            tempBottomC = well.tempBottomC,
            temperatureFactor = ThermalModel.temperatureFactor(well, bitTvdM),
            cuttingsFraction = cuttingsLoad,
            annularVelocityMPerS = if (pumpOn) (pumpRateLpm() / 60000.0) / annularAreaAt(bitDepthM) else 0.0,
            bhpHydrostaticKPa = hydrostaticAnnulusKPa() - gasHydrostaticLossKPa(),
            bhpCuttingsKPa = cuttingsDeltaKPa(),
            bhpAnnularFrictionKPa = annularLossKPa(),
            bhpSurgeKPa = surgeKPa,
            bhpChokeKPa = if (isClosed()) casingPressureKPa else 0.0,
            gasReachedSurface = gasReachedSurface,
            killStage = killStage,
            oldMudKgM3 = oldMudKgM3,
            killMudKgM3 = killMudKgM3,
            effectiveMudKgM3 = effectiveMudKgM3,
            killMudPumpedM3 = killMudPumpedM3,
            stringKillFraction = stringKillFraction,
            annulusKillFraction = annulusKillFraction,
            chokeLineKillFraction = chokeLineKillFraction,
            firstCirculationFraction = (circulationPumpedM3 / circulationTotalM3).coerceIn(0.0, 1.0),
            killSheetFrozen = killSheetFrozen,
            killCasingTargetKPa = killCasingTargetKPa(),
            lineProgress = requiredLineKinds().associateWith { lineProgressOf(it) },
            linesDisplaced = linesDisplacedSet.toSet(),
            requiredLines = requiredLineKinds(),
            lineDisplacementComplete = allRequiredLinesDisplaced(),
            residualPressureKPa = casingPressureKPa,
            trappedPressureKPa = trappedPressureKPa,
            readyToOpen = phase == Phase.KILLED && allRequiredLinesDisplaced() &&
                casingPressureKPa <= 200.0 && wellDead(),
            openBopBlockedReason = openBopBlockReason(),
            firstCirculationReady = firstCirculationReady,
            activeChokeIndex = activeChokeIndex,
            activeChokeName = activeChoke().name,
            prepChecks = prepChecks(),
            prepComplete = prepComplete(),
            prepDoneCount = prepDone.size,
            scrAtRateKPa = scrAtRate.toMap(),
            chokeLineDeltaPKPa = chokeLineDeltaPKPa,
            pitGainAlarmM3 = pitGainAlarmM3,
            pitLossAlarmM3 = pitLossAlarmM3,
            returnFlowAlarmPct = returnFlowAlarmPct,
            pitGainM3 = pitM3 - pitReferenceM3,
            flowDeviationPct = flowDeviation,
            secondaryIndicators = secondary,
            poreEquivalentKgM3 = poreEq,
            kickCondition = kickCondition,
            bopEverClosed = bopEverClosed.toSet(),
            drillingStopped = drillingStopped,
            injectionPath = injection,
            returnPath = returnPath,
            standpipeActive = injection == InjectionPath.STANDPIPE,
            killLineActive = injection == InjectionPath.KILL_LINE,
            chokeManifoldActive = returnPath == ReturnPath.CHOKE_LINE,
            wellDead = wellDead(),
            residualBleedDone = residualBleedDone,
            killSequenceComplete = killSequenceComplete
        ).also { snap ->
            snap.firstCirculationPumpedM3 = circulationPumpedM3
            snap.firstCirculationTotalM3 = circulationTotalM3
            snap.firstCirculationStrokes = circulationStrokes
            snap.firstCirculationTotalStrokes = circulationTotalStrokes
            snap.firstCirculationRemainingStrokes =
                (circulationTotalStrokes - circulationStrokes).coerceAtLeast(0.0)
            snap.firstCirculationKickOut = influxVolumeM3 <= 0.08
            snap.kickMaxVolumeM3 = kickConfig.maxVolumeM3
            snap.kickCharacteristicRateLpm = kickConfig.characteristicRateLpm
            snap.kickClosingTimeSec = kickClosingTimeSec
            snap.kickStabilizationFactor = kickStabilizationFactor
            snap.kickZoneGradientKPaPerM = kickConfig.zonePoreGradientKPaPerM
            snap.kickZonePermeabilityMd = kickConfig.zonePermeabilityMd
            snap.flowCheckRunning = flowCheckArmed
            snap.flowCheckProgress =
                (flowCheckStableSec / FLOW_CHECK_STABLE_SEC).coerceIn(0.0, 1.0)
            snap.flowCheckFlowLpm = flowCheckFlowLpm
            snap.sidppGaugeKPa = sidppGaugeKPa
            snap.sicpGaugeKPa = casingPressureKPa
            snap.sidppValid = isClosed() && (!pumpOn || shutInStabilized)
            snap.sicpValid = isClosed()
            snap.shutInStabilized = shutInStabilized
            snap.influxDensityKgM3 = influxDensity()
            snap.lineDisplacedHeadKPa = lineDisplacedHeadKPa()
            snap.guideKickHandled = guideKickHandled
            snap.guideDrillingStopped = guideDrillingStopped
            snap.guideBopClosed = guideBopClosed
            snap.guideStabilized = guideStabilized
            snap.guideKillSheetReady = guideKillSheetReady
            snap.guideFirstCirculation = guideFirstCirculation
            snap.guideWeightedUp = guideWeightedUp
            snap.guideSecondCirculation = guideSecondCirculation
            snap.guideLinesDisplaced = guideLinesDisplaced
            snap.guideBleedDone = guideBleedDone
            snap.guideCycleComplete = guideCycleComplete
            snap.stabilizedSidppKPa = stabilizedSidppKPa
            snap.stabilizedSicpKPa = stabilizedSicpKPa
            snap.migrationOverpressureKPa = migrationOverpressureKPa
            snap.recordedSidppKPa = recordedSidppKPa()
            snap.recordedSicpKPa = recordedSicpKPa()
            snap.shutInSafetyOffsetKPa = max(safetySidppExtraKPa, safetySicpExtraKPa)
            snap.maxHoleDepthM = max(maxHoleMdM, bitDepthM)
            snap.chokeOpenNoPump = isClosed() && (!pumpOn || pumpSpm <= 0.2) &&
                hcrOpen && chokePct > 0.2 && lineupValid()
            snap.guideVolumetricBleedDone = guideVolumetricBleedDone
            snap.guideLubricateDone = guideLubricateDone
            snap.guideEdsProtected = guideEdsProtected
            snap.guideTutorialDone = guideTutorialDone
        }
    }
}
