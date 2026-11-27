package com.mosquitobr.controlepoco.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosquitobr.controlepoco.R
import com.mosquitobr.controlepoco.audio.RigAudioEngine
import com.mosquitobr.controlepoco.core.AlarmSeverity
import com.mosquitobr.controlepoco.core.KillStage
import com.mosquitobr.controlepoco.core.KickCondition
import com.mosquitobr.controlepoco.core.LineKind
import com.mosquitobr.controlepoco.core.ManifoldLineup
import com.mosquitobr.controlepoco.core.Phase
import com.mosquitobr.controlepoco.core.PrepCheckId
import com.mosquitobr.controlepoco.core.Scenario
import com.mosquitobr.controlepoco.core.SecondaryIndicator
import com.mosquitobr.controlepoco.core.SimSnapshot
import com.mosquitobr.controlepoco.core.Simulator
import com.mosquitobr.controlepoco.core.StackType
import com.mosquitobr.controlepoco.core.UnitSystem
import com.mosquitobr.controlepoco.core.Units
import com.mosquitobr.controlepoco.core.BopElement
import com.mosquitobr.controlepoco.data.AppSettings
import com.mosquitobr.controlepoco.data.SimulationResult
import com.mosquitobr.controlepoco.data.SimulationResultStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SimulatorScreen(
    scenario: Scenario,
    settings: AppSettings,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onOpenResults: () -> Unit = onHome
) {
    val lang = LocalAppLanguage.current
    var generation by remember { mutableIntStateOf(0) }
    val simulator = remember(scenario.id, generation) { Simulator(scenario) }
    var snapshot by remember(scenario.id, generation) { mutableStateOf(simulator.snapshot()) }
    var started by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(1f) }
    var tab by remember { mutableIntStateOf(0) }
    var showKillSheet by remember { mutableStateOf(false) }
    var showDebrief by remember { mutableStateOf(false) }
    var instructor by remember { mutableStateOf(false) }
    var savedResult by remember { mutableStateOf<SimulationResult?>(null) }
    var pendingExport by remember { mutableStateOf<SimulationResult?>(null) }
    var exportDone by remember { mutableStateOf(false) }
    var showResetGuideConfirm by remember { mutableStateOf(false) }
    var showResetSimConfirm by remember { mutableStateOf(false) }
    val units = settings.units
    val haptic = LocalHapticFeedback.current
    val audio = remember { RigAudioEngine() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lastFeedbackAlarm by remember { mutableLongStateOf(-1L) }

    fun refresh() {
        snapshot = simulator.snapshot()
    }

    fun saveResult() {
        val result = SimulationResultStore.createFromSnapshot(
            scenario = scenario,
            snapshot = simulator.snapshot(),
            initialMudPpg = Units.kgM3ToPpg(scenario.mud.densityKgM3)
        )
        SimulationResultStore.save(context, result)
        savedResult = result
        exportDone = false
        refresh()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val result = pendingExport
        if (uri != null && result != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(
                                SimulationResultStore.exportJson(listOf(result)).toByteArray()
                            )
                            true
                        } ?: false
                    }.getOrDefault(false)
                }
                exportDone = ok
            }
        }
        pendingExport = null
    }

    DisposableEffect(Unit) {
        onDispose { audio.stop() }
    }

    LaunchedEffect(
        running, started, showKillSheet, showDebrief,
        settings.sound, settings.volume, settings.soundPump, settings.soundRotary,
        settings.soundWeight, settings.soundGas
    ) {
        audio.enabled = settings.sound
        audio.volume = settings.volume
        audio.pumpChannel = settings.soundPump
        audio.rotaryChannel = settings.soundRotary
        audio.weightChannel = settings.soundWeight
        audio.gasChannel = settings.soundGas
        val active = running && started && !showKillSheet && !showDebrief && settings.sound
        if (active) audio.start() else audio.stop()
    }

    LaunchedEffect(
        snapshot.pumpOn, snapshot.pumpSpm, snapshot.rotaryOn,
        snapshot.rpm, snapshot.wobKlb, snapshot.gasAtSurface
    ) {
        audio.update(
            pumpOn = snapshot.pumpOn,
            pumpSpm = snapshot.pumpSpm,
            rotaryOn = snapshot.rotaryOn,
            rpm = snapshot.rpm,
            wobKlb = snapshot.wobKlb,
            gasAtSurface = snapshot.gasAtSurface
        )
    }

    LaunchedEffect(snapshot.alarms.firstOrNull()?.id, snapshot.alarmSoundEnabled) {
        val alarm = snapshot.alarms.firstOrNull() ?: return@LaunchedEffect
        if (alarm.id == lastFeedbackAlarm || alarm.severity == AlarmSeverity.INFO) return@LaunchedEffect
        lastFeedbackAlarm = alarm.id
        if (settings.haptics) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (settings.sound && snapshot.alarmSoundEnabled) {
            audio.triggerAlarm()
        }
    }

    LaunchedEffect(running, started, speed, showKillSheet, showDebrief) {
        if (!running || !started || showKillSheet || showDebrief) return@LaunchedEffect
        while (isActive) {
            simulator.advance(0.033 * speed)
            snapshot = simulator.snapshot()
            delay(33)
        }
    }

    if (showKillSheet) {
        LaunchedEffect(showKillSheet) {
            if (simulator.confirmKillSheet()) {
                refresh()
            }
        }
        KillSheetScreen(
            scenario = scenario,
            snapshot = snapshot,
            result = simulator.killSheetResult(),
            units = units,
            onBack = { showKillSheet = false },
            onApply = {
                simulator.weightUpToKillMud()
                snapshot = simulator.snapshot()
                showKillSheet = false
            }
        )
        return
    }

    if (showDebrief) {
        DebriefScreen(
            scenario = scenario,
            snapshot = snapshot,
            units = units,
            onRepeat = {
                generation++
                showDebrief = false
                started = true
                running = true
            },
            onHome = onHome,
            onClose = { showDebrief = false }
        )
        return
    }

    if (!started) {
        BriefingDialog(
            scenario = scenario,
            lang = lang,
            onBegin = {
                started = true
                running = true
            },
            onBack = onBack
        )
    }

    Column(Modifier.fillMaxSize()) {
        SimTopBar(
            scenario = scenario,
            lang = lang,
            snapshot = snapshot,
            running = running,
            speed = speed,
            onBack = onBack,
            onToggleRun = {
                if (!started) {
                    started = true
                    running = true
                } else {
                    running = !running
                }
            },
            onSpeed = {
                speed = when {
                    speed < 1.5f -> 2f
                    speed < 3f -> 5f
                    speed < 6f -> 8f
                    speed < 12f -> 16f
                    speed < 24f -> 32f
                    speed < 48f -> 64f
                    else -> 1f
                }
            },
            onKillSheet = { showKillSheet = true },
            onDebrief = { showDebrief = true },
            onInstructor = { instructor = !instructor }
        )

        snapshot.message?.let { message ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Amber.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(message, color = Amber, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text(
                    "×",
                    color = Amber,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        BoxWithConstraints(Modifier.weight(1f)) {
            val wide = maxWidth > 720.dp
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    SchematicPane(
                        scenario, snapshot, units,
                        Modifier
                            .weight(0.55f)
                            .fillMaxSize()
                    )
                    PanelArea(
                        tab, { tab = it }, simulator, snapshot, scenario, units, lang, instructor,
                        Modifier
                            .weight(0.45f)
                            .fillMaxSize(),
                        refresh = { snapshot = simulator.snapshot() },
                        onKillSheet = { showKillSheet = true },
                        onSaveResult = { saveResult() },
                        onResetGuide = { showResetGuideConfirm = true },
                        onResetSimulation = { showResetSimConfirm = true }
                    )
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    SchematicPane(
                        scenario, snapshot, units,
                        Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                    PanelArea(
                        tab, { tab = it }, simulator, snapshot, scenario, units, lang, instructor,
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        refresh = { snapshot = simulator.snapshot() },
                        onKillSheet = { showKillSheet = true },
                        onSaveResult = { saveResult() },
                        onResetGuide = { showResetGuideConfirm = true },
                        onResetSimulation = { showResetSimConfirm = true }
                    )
                }
            }
        }
    }

    savedResult?.let { result ->
        ResultSavedDialog(
            result = result,
            units = units,
            exportDone = exportDone,
            onOpenList = {
                savedResult = null
                onOpenResults()
            },
            onExport = {
                pendingExport = result
                exportLauncher.launch(SimulationResultStore.exportFileName())
            },
            onClose = { savedResult = null }
        )
    }

    if (showResetGuideConfirm) {
        ConfirmActionDialog(
            title = stringResource(R.string.guide_reset_guide),
            message = stringResource(R.string.reset_guide_confirm),
            confirmLabel = stringResource(R.string.guide_reset_guide),
            confirmColor = Amber,
            onConfirm = {
                simulator.resetGuideAndContinue()
                tab = TAB_GUIDE
                refresh()
                showResetGuideConfirm = false
            },
            onDismiss = { showResetGuideConfirm = false }
        )
    }

    if (showResetSimConfirm) {
        ConfirmActionDialog(
            title = stringResource(R.string.guide_reset_simulation),
            message = stringResource(R.string.reset_simulation_confirm),
            confirmLabel = stringResource(R.string.guide_reset_simulation),
            confirmColor = Red,
            onConfirm = {
                generation++
                started = false
                running = false
                tab = TAB_GUIDE
                showResetSimConfirm = false
            },
            onDismiss = { showResetSimConfirm = false }
        )
    }
}

@Composable
private fun ResultSavedDialog(
    result: SimulationResult,
    units: UnitSystem,
    exportDone: Boolean,
    onOpenList: () -> Unit,
    onExport: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Card(Modifier.padding(20.dp).fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    stringResource(R.string.result_saved_title),
                    color = TextMain,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.result_saved_summary, result.scenarioName),
                    color = Teal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${stringResource(R.string.score)} ${result.score} · " +
                        "${stringResource(R.string.phase)} ${result.phase} · " +
                        durationText(result.timeSec) + " · " +
                        "${Units.fmt(result.bitDepthM)} m",
                    color = TextMain,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.result_saved_location),
                    color = Muted,
                    fontSize = 10.sp
                )
                if (exportDone) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.result_export_done),
                        color = Green,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionButton(
                        stringResource(R.string.result_open_list),
                        Modifier.weight(1f),
                        TealDark,
                        onOpenList
                    )
                    ActionButton(
                        stringResource(R.string.result_export_json),
                        Modifier.weight(1f),
                        Panel3,
                        onExport
                    )
                    ActionButton(
                        stringResource(R.string.close),
                        Modifier.weight(1f),
                        Panel3,
                        onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Card(Modifier.padding(20.dp).fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(title, color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(message, color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = Muted)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = confirmColor,
                            contentColor = TextMain
                        )
                    ) {
                        Text(confirmLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun SimTopBar(
    scenario: Scenario,
    lang: String,
    snapshot: SimSnapshot,
    running: Boolean,
    speed: Float,
    onBack: () -> Unit,
    onToggleRun: () -> Unit,
    onSpeed: () -> Unit,
    onKillSheet: () -> Unit,
    onDebrief: () -> Unit,
    onInstructor: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Panel)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextMain)
        }
        Column(Modifier.weight(1f)) {
            Text(
                scenario.title(lang),
                color = TextMain,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(phaseText(snapshot.phase), phaseColor(snapshot.phase))
                Spacer(Modifier.width(6.dp))
                Text(
                    durationText(snapshot.timeSec),
                    color = Muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${stringResource(R.string.score)} ${snapshot.score}",
                    color = if (snapshot.score >= 70) Green else Amber,
                    fontSize = 9.sp
                )
            }
        }
        TextButton(onClick = onKillSheet, contentPadding = PaddingValues(horizontal = 5.dp)) {
            Text(stringResource(R.string.ks_short), color = Teal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onDebrief, contentPadding = PaddingValues(horizontal = 5.dp)) {
            Text(stringResource(R.string.db_short), color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onInstructor, contentPadding = PaddingValues(horizontal = 5.dp)) {
            Text(stringResource(R.string.instr_short), color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onToggleRun, contentPadding = PaddingValues(horizontal = 5.dp)) {
            Text(if (running) "II" else "▶", color = Green, fontSize = 13.sp)
        }
        TextButton(onClick = onSpeed, contentPadding = PaddingValues(horizontal = 5.dp)) {
            Text("${speed.toInt()}x", color = Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SchematicPane(
    scenario: Scenario,
    snapshot: SimSnapshot,
    units: UnitSystem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .background(Panel)
            .padding(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (scenario.stackType == StackType.SUBSEA) "SUBSEA SCHEMATIC" else "SURFACE SCHEMATIC",
                color = Teal,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (snapshot.closedBop) stringResource(R.string.bop_closed) else stringResource(R.string.bop_open),
                color = if (snapshot.closedBop) Red else Green,
                fontSize = 9.sp
            )
        }
        WellSchematic(scenario, snapshot, units, Modifier.weight(1f).fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val sidppDisplayKPa = snapshot.sidppDisplayKPa()
            val sidppText = if (snapshot.sidppValid) pressureText(sidppDisplayKPa, units) else "—"
            val sicpText = if (snapshot.sicpValid) pressureText(snapshot.sicpDisplayKPa(), units) else "—"
            MetricCard(stringResource(R.string.sidpp), sidppText, Modifier.weight(1f), Teal)
            MetricCard(stringResource(R.string.sicp), sicpText, Modifier.weight(1f), Amber)
            MetricCard(stringResource(R.string.maasp), pressureText(snapshot.maaspKPa, units), Modifier.weight(1f), Red)
        }
    }
}

@Composable
private fun PanelArea(
    selectedTab: Int,
    onTab: (Int) -> Unit,
    simulator: Simulator,
    snapshot: SimSnapshot,
    scenario: Scenario,
    units: UnitSystem,
    lang: String,
    instructor: Boolean,
    modifier: Modifier = Modifier,
    refresh: () -> Unit,
    onKillSheet: () -> Unit,
    onSaveResult: () -> Unit,
    onResetGuide: () -> Unit,
    onResetSimulation: () -> Unit
) {
    val tabs = listOf(
        stringResource(R.string.tab_guide),
        stringResource(R.string.tab_driller),
        stringResource(R.string.tab_kick),
        stringResource(R.string.tab_trip),
        stringResource(R.string.tab_choke),
        stringResource(R.string.tab_bop),
        stringResource(R.string.tab_mud),
        stringResource(R.string.tab_trends),
        stringResource(R.string.tab_log)
    )
    Column(modifier.background(Bg)) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Panel,
            contentColor = Teal,
            edgePadding = 4.dp,
            divider = { Divider(color = Panel3) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTab(index) },
                    text = { Text(title, fontSize = 10.sp) }
                )
            }
        }
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                TAB_GUIDE -> GuidePanel(
                    simulator, snapshot, scenario, units, onTab, onKillSheet, refresh,
                    onSaveResult = onSaveResult,
                    onResetGuide = onResetGuide,
                    onResetSimulation = onResetSimulation
                )
                TAB_DRILLER -> DrillerPanel(simulator, snapshot, units, lang, refresh)
                TAB_KICK -> KickPanel(simulator, snapshot, scenario, units, refresh)
                TAB_TRIP -> TripPanel(simulator, snapshot, scenario, units, refresh)
                TAB_CHOKE -> ChokePanel(simulator, snapshot, scenario, units, refresh)
                TAB_BOP -> BopPanel(simulator, snapshot, scenario, units, refresh)
                TAB_MUD -> MudPanel(simulator, snapshot, scenario, units, refresh, onKillSheet)
                TAB_TRENDS -> TrendsPanel(snapshot, units)
                else -> LogPanel(simulator, snapshot, units, refresh)
            }
        }
        if (instructor) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Panel2)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.instructor), color = Amber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        simulator.triggerManualKick()
                        refresh()
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) { Text(stringResource(R.string.inject_kick), color = Red, fontSize = 9.sp) }
                Text(
                    "${stringResource(R.string.score)} ${snapshot.score}",
                    color = Muted,
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DrillerPanel(
    simulator: Simulator,
    snapshot: SimSnapshot,
    units: UnitSystem,
    lang: String,
    refresh: () -> Unit
) {
    val pitGain = snapshot.influxM3 > 0.1
    val flowOff = snapshot.flowOutLpm > 15.0 && snapshot.pumpSpm < 0.5
    val ropBreak = snapshot.ropMHr > 32.0
    val torqueHigh = snapshot.torqueKNm > 22.0
    val sppLow = snapshot.pumpOn && snapshot.pumpSpm > 10.0 && snapshot.sppKPa < 1200.0
    val gas = snapshot.influxM3 > 0.01
    val drilling = snapshot.pumpOn && snapshot.rotaryOn && snapshot.wobKlb > 2.0 &&
        snapshot.copOffsetM < 1.2 && snapshot.phase == com.mosquitobr.controlepoco.core.Phase.DRILLING
    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Gauge(
                    stringResource(R.string.spp),
                    pressureValue(snapshot.sppKPa, units),
                    (snapshot.sppKPa / 20000.0).toFloat(),
                    Teal,
                    Modifier.weight(1f).height(96.dp)
                )
                val sidppDisplayKPa = snapshot.sidppDisplayKPa()
                val sicpDisplayKPa = snapshot.sicpDisplayKPa()
                Gauge(
                    stringResource(R.string.sidpp),
                    if (snapshot.sidppValid) pressureValue(sidppDisplayKPa, units) else "—",
                    if (snapshot.sidppValid) (sidppDisplayKPa / 12000.0).toFloat() else 0f,
                    Blue,
                    Modifier.weight(1f).height(96.dp)
                )
                Gauge(
                    stringResource(R.string.sicp),
                    if (snapshot.sicpValid) pressureValue(sicpDisplayKPa, units) else "—",
                    if (snapshot.sicpValid) (sicpDisplayKPa / 15000.0).toFloat() else 0f,
                    Amber,
                    Modifier.weight(1f).height(96.dp)
                )
                Gauge(
                    stringResource(R.string.bhp),
                    pressureValue(snapshot.bhpKPa, units),
                    (snapshot.bhpKPa / 60000.0).toFloat(),
                    Purple,
                    Modifier.weight(1f).height(96.dp)
                )
            }
        }
        item {
            SectionCard(stringResource(R.string.drilling_params)) {
                Text(
                    "${stringResource(R.string.formation)}: ${snapshot.formationName.ifBlank { "—" }} · " +
                        "${stringResource(R.string.hardness)} ${Units.fmt(snapshot.formationHardness, 1)}",
                    color = Teal,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetricCard(stringResource(R.string.rop), ropText(snapshot.ropMHr, units), Modifier.weight(1f), Green)
                    MetricCard(stringResource(R.string.torque), torqueText(snapshot.torqueKNm, units), Modifier.weight(1f), Amber)
                    MetricCard(stringResource(R.string.bit_wear), "${Units.fmt(snapshot.bitWear * 100, 1)}%", Modifier.weight(1f), Muted)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "${stringResource(R.string.wob)}: ${wobText(snapshot.wobKn, units)} · " +
                        "${stringResource(R.string.wob_setpoint)} ${Units.fmt(snapshot.wobSetpointKlb, 1)} Klb",
                    color = TextMain,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Slider(
                    value = snapshot.wobSetpointKlb.toFloat(),
                    onValueChange = {
                        simulator.setWobSetpointKlb(it.toDouble())
                        refresh()
                    },
                    valueRange = 0f..60f
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${stringResource(R.string.rpm)}: ${Units.fmt(snapshot.rpm)} · " +
                                "${stringResource(R.string.rpm_setpoint)} ${Units.fmt(snapshot.rpmSetpoint)}",
                            color = TextMain,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            if (snapshot.rotaryOn) stringResource(R.string.rotary_on)
                            else stringResource(R.string.rotary_off),
                            color = if (snapshot.rotaryOn) Green else Muted,
                            fontSize = 9.sp
                        )
                    }
                    Switch(
                        checked = snapshot.rotaryOn,
                        onCheckedChange = {
                            simulator.setRotary(it)
                            refresh()
                        }
                    )
                }
                Slider(
                    value = snapshot.rpmSetpoint.toFloat(),
                    onValueChange = {
                        simulator.setRpmSetpoint(it.toDouble())
                        refresh()
                    },
                    valueRange = 0f..180f
                )
                Text(
                    "${stringResource(R.string.cop_position)}: " +
                        if (snapshot.copOffsetM >= 0.0) {
                            "levantada ${Units.fmt(snapshot.copOffsetM, 1)} m"
                        } else {
                            "assentada ${Units.fmt(-snapshot.copOffsetM, 1)} m"
                        },
                    color = TextMain,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ActionButton(stringResource(R.string.cop_down), Modifier.weight(1f), Amber) {
                        simulator.nudgeCop(-0.5)
                        refresh()
                    }
                    ActionButton(stringResource(R.string.cop_up), Modifier.weight(1f), TealDark) {
                        simulator.nudgeCop(0.5)
                        refresh()
                    }
                }
                Slider(
                    value = snapshot.copOffsetM.toFloat(),
                    onValueChange = {
                        simulator.setCopOffset(it.toDouble())
                        refresh()
                    },
                    valueRange = -1.5f..2f
                )
                Text(
                    if (drilling) stringResource(R.string.drilling_active)
                    else stringResource(R.string.drilling_stopped),
                    color = if (drilling) Green else Muted,
                    fontSize = 9.sp
                )
            }
        }
        item {
            SectionCard(stringResource(R.string.driller_geometry)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetricCard(
                        stringResource(R.string.bit_depth_tmd),
                        depthText(snapshot.bitDepthM, units),
                        Modifier.weight(1f),
                        Teal
                    )
                    MetricCard(
                        stringResource(R.string.bit_tvd),
                        depthText(snapshot.bitTvdM, units),
                        Modifier.weight(1f),
                        Purple
                    )
                    MetricCard(
                        stringResource(R.string.ecd),
                        densityText(snapshot.ecdKgM3, units),
                        Modifier.weight(1f),
                        Purple
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetricCard(
                        stringResource(R.string.bit_inclination),
                        "${Units.fmt(snapshot.bitIncDeg, 1)}°",
                        Modifier.weight(1f),
                        Amber
                    )
                    MetricCard(
                        stringResource(R.string.hole_inclination),
                        "${Units.fmt(snapshot.holeIncDeg, 1)}°",
                        Modifier.weight(1f),
                        Amber
                    )
                    MetricCard(
                        stringResource(R.string.temp_at_bit),
                        "${Units.fmt(snapshot.tempAtBitC, 1)} °C",
                        Modifier.weight(1f),
                        Red
                    )
                }
                Text(
                    "${stringResource(R.string.cuttings_load)}: ${Units.fmt(snapshot.cuttingsFraction * 100, 1)}% · " +
                        "${stringResource(R.string.annular_velocity)}: ${Units.fmt(snapshot.annularVelocityMPerS, 2)} m/s",
                    color = Muted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        item {
            SectionCard(stringResource(R.string.hydraulics)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetricCard(stringResource(R.string.ecd), densityText(snapshot.ecdKgM3, units), Modifier.weight(1f), Purple)
                    MetricCard(
                        if (snapshot.scrTaken) stringResource(R.string.scr_measured)
                        else stringResource(R.string.scr_estimated),
                        pressureText(snapshot.scrKPa, units),
                        Modifier.weight(1f),
                        if (snapshot.scrTaken) Green else Amber
                    )
                    MetricCard(stringResource(R.string.cuttings_load), "${Units.fmt(snapshot.cuttingsLoad * 100, 0)}%", Modifier.weight(1f), Muted)
                }
                Text(
                    "${stringResource(R.string.scr_rate)}: ${rateText(snapshot.scrRateLpm, units)}",
                    color = Muted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "${stringResource(R.string.hydraulic_losses)} · ${pressureText(snapshot.losses.totalKPa, units)}",
                    color = TextMain,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                LossLine(stringResource(R.string.loss_surface), snapshot.losses.surfaceKPa, snapshot.losses.totalKPa, units)
                LossLine(stringResource(R.string.loss_string), snapshot.losses.stringKPa, snapshot.losses.totalKPa, units)
                LossLine(stringResource(R.string.loss_bit), snapshot.losses.bitKPa, snapshot.losses.totalKPa, units)
                LossLine(stringResource(R.string.loss_annulus), snapshot.losses.annulusKPa, snapshot.losses.totalKPa, units)
                LossLine(stringResource(R.string.loss_motor), snapshot.losses.motorKPa, snapshot.losses.totalKPa, units)
                Spacer(Modifier.height(5.dp))
                Text(
                    stringResource(R.string.bhp_breakdown),
                    color = TextMain,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                BhpLine(stringResource(R.string.bhp_hydrostatic), snapshot.bhpHydrostaticKPa, units)
                BhpLine(stringResource(R.string.bhp_cuttings), snapshot.bhpCuttingsKPa, units)
                BhpLine(stringResource(R.string.bhp_friction), snapshot.bhpAnnularFrictionKPa, units)
                BhpLine(stringResource(R.string.bhp_surge), snapshot.bhpSurgeKPa, units)
                BhpLine(stringResource(R.string.bhp_choke), snapshot.bhpChokeKPa, units)
                Spacer(Modifier.height(5.dp))
                ActionButton(stringResource(R.string.take_scr), Modifier.fillMaxWidth(), TealDark) {
                    simulator.takeScr()
                    refresh()
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.procedures)) {
                val procedure = snapshot.procedure
                if (procedure != null) {
                    Text(
                        if (lang == "en") procedure.titleEn else procedure.titlePt,
                        color = Teal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    procedure.steps.forEachIndexed { index, step ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when {
                                    step.done -> "✓"
                                    index == procedure.currentIndex -> "▶"
                                    else -> "·"
                                },
                                color = when {
                                    step.done -> Green
                                    index == procedure.currentIndex -> Amber
                                    else -> Muted
                                },
                                fontSize = 11.sp,
                                modifier = Modifier.width(18.dp)
                            )
                            Text(
                                if (lang == "en") step.titleEn else step.titlePt,
                                color = if (index == procedure.currentIndex) TextMain else Muted,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Text(
                        if (lang == "en") procedure.hintEn.ifBlank { "Complete the current step." }
                        else procedure.hintPt,
                        color = Amber,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                    Text(
                        stringResource(R.string.procedure_auto),
                        color = Muted,
                        fontSize = 8.sp
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        ActionButton(stringResource(R.string.complete_step), Modifier.weight(1f), TealDark) {
                            simulator.advanceProcedure()
                            refresh()
                        }
                        ActionButton(stringResource(R.string.cancel_procedure), Modifier.weight(1f)) {
                            simulator.cancelProcedure()
                            refresh()
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        ActionButton(stringResource(R.string.start_scr), Modifier.weight(1f), TealDark) {
                            simulator.startProcedure(com.mosquitobr.controlepoco.core.ProcedureId.SCR)
                            refresh()
                        }
                        ActionButton(stringResource(R.string.start_flow_check), Modifier.weight(1f)) {
                            simulator.startProcedure(com.mosquitobr.controlepoco.core.ProcedureId.FLOW_CHECK)
                            refresh()
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        ActionButton(stringResource(R.string.start_trip_tank), Modifier.weight(1f)) {
                            simulator.startProcedure(com.mosquitobr.controlepoco.core.ProcedureId.TRIP_TANK)
                            refresh()
                        }
                        ActionButton(stringResource(R.string.start_bop_test), Modifier.weight(1f)) {
                            simulator.startProcedure(com.mosquitobr.controlepoco.core.ProcedureId.BOP_TEST)
                            refresh()
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    ActionButton(stringResource(R.string.start_kill_lineup), Modifier.fillMaxWidth()) {
                        simulator.startProcedure(com.mosquitobr.controlepoco.core.ProcedureId.MANIFOLD_LINEUP)
                        refresh()
                    }
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.pump)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (snapshot.pumpOn) stringResource(R.string.pump_running)
                            else stringResource(R.string.pump_stopped),
                            color = if (snapshot.pumpOn) Green else Muted,
                            fontSize = 11.sp
                        )
                        Text(
                            "${Units.fmt(snapshot.pumpSpm)} ${stringResource(R.string.spm)} · ${rateText(snapshot.pumpRateLpm, units)}",
                            color = Muted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Switch(
                        checked = snapshot.pumpOn,
                        onCheckedChange = { on ->
                            simulator.setPump(on, if (on) maxOf(45.0, snapshot.pumpSpm) else 0.0)
                            refresh()
                        }
                    )
                }
                Slider(
                    value = snapshot.pumpSpm.toFloat(),
                    onValueChange = {
                        simulator.setSpm(it.toDouble())
                        if (snapshot.pumpOn) simulator.setPump(true, it.toDouble())
                        refresh()
                    },
                    valueRange = 0f..120f
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ActionButton(stringResource(R.string.flow_check), Modifier.weight(1f)) {
                        simulator.flowCheck(); refresh()
                    }
                    ActionButton(stringResource(R.string.line_up_kill), Modifier.weight(1f)) {
                        simulator.lineUpKill(); refresh()
                    }
                    ActionButton(stringResource(R.string.weight_up_kill), Modifier.weight(1f)) {
                        simulator.weightUpToKillMud(); refresh()
                    }
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.primary_indicators)) {
                IndicatorLine(stringResource(R.string.indicator_gain), pitGain)
                IndicatorLine(stringResource(R.string.indicator_flow), flowOff)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.secondary_indicators),
                    color = Muted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                IndicatorLine(stringResource(R.string.indicator_rop), ropBreak)
                IndicatorLine(stringResource(R.string.indicator_torque), torqueHigh)
                IndicatorLine(stringResource(R.string.indicator_spp), sppLow)
                IndicatorLine(stringResource(R.string.indicator_gas), gas)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MetricCard(stringResource(R.string.pit), volumeText(snapshot.pitM3, units), Modifier.weight(1f), Blue)
                MetricCard(stringResource(R.string.trip_tank), volumeText(snapshot.tripTankM3, units), Modifier.weight(1f), Muted)
                MetricCard(stringResource(R.string.flow_out), rateText(snapshot.flowOutLpm, units), Modifier.weight(1f), Amber)
                MetricCard(stringResource(R.string.strokes), Units.fmt(snapshot.totalStrokes, 0), Modifier.weight(1f), TextMain)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MetricCard(stringResource(R.string.md), depthText(snapshot.bitDepthM, units), Modifier.weight(1f), Teal)
                MetricCard(stringResource(R.string.tvd), depthText(snapshot.bitTvdM, units), Modifier.weight(1f), Purple)
                MetricCard(stringResource(R.string.ecd), densityText(snapshot.ecdKgM3, units), Modifier.weight(1f), Purple)
                MetricCard(stringResource(R.string.shoe), pressureText(snapshot.shoePressureKPa, units), Modifier.weight(1f), Red)
            }
        }
    }
}

@Composable
private fun LossLine(label: String, valueKPa: Double, totalKPa: Double, units: UnitSystem) {
    val fraction = if (totalKPa > 0.0) (valueKPa / totalKPa).toFloat() else 0f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 9.sp, modifier = Modifier.width(64.dp))
        Box(
            Modifier
                .weight(1f)
                .height(4.dp)
                .background(Panel3, RoundedCornerShape(50))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(Teal, RoundedCornerShape(50))
            )
        }
        Text(
            pressureText(valueKPa, units),
            color = TextMain,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(62.dp)
        )
    }
}

@Composable
private fun BhpLine(label: String, valueKPa: Double, units: UnitSystem) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 9.sp, modifier = Modifier.weight(1f))
        Text(
            pressureText(valueKPa, units),
            color = if (valueKPa >= 0.0) TextMain else Amber,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun IndicatorLine(label: String, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .background(if (active) Amber else Panel3, RoundedCornerShape(50))
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (active) Amber else Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text(if (active) "ON" else "—", color = if (active) Amber else Muted, fontSize = 9.sp)
    }
}

@Composable
private fun KickPanel(
    simulator: Simulator,
    snapshot: SimSnapshot,
    scenario: Scenario,
    units: UnitSystem,
    refresh: () -> Unit
) {
    val height = (snapshot.influxBaseTvdM - snapshot.influxTopTvdM).coerceAtLeast(0.0)
    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item {
            SectionCard(stringResource(R.string.kick_monitor)) {
                KickMonitor(
                    scenario = scenario,
                    snapshot = snapshot,
                    units = units,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                )
            }
        }
        item {
            SectionCard(stringResource(R.string.guide_secondary)) {
                if (snapshot.secondaryIndicators.isEmpty()) {
                    Text("—", color = Muted, fontSize = 10.sp)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        snapshot.secondaryIndicators.take(4).forEach { indicator ->
                            StatusPill(secondaryLabel(indicator), Amber)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    when (snapshot.kickCondition) {
                        KickCondition.NONE -> stringResource(R.string.kick_cond_none)
                        KickCondition.PORE_GT_HYDROSTATIC -> stringResource(R.string.kick_cond_hydro)
                        KickCondition.PORE_GT_ECD -> stringResource(R.string.kick_cond_ecd)
                        KickCondition.INTERNAL_HIGH_ZONE -> stringResource(R.string.kick_cond_internal)
                    },
                    color = if (snapshot.kickCondition == KickCondition.NONE) Muted else Red,
                    fontSize = 9.sp
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MetricCard(stringResource(R.string.kick_top), depthText(snapshot.influxTopTvdM, units), Modifier.weight(1f), Amber)
                MetricCard(stringResource(R.string.kick_base), depthText(snapshot.influxBaseTvdM, units), Modifier.weight(1f), Amber)
                MetricCard(stringResource(R.string.kick_height), depthText(height, units), Modifier.weight(1f), Teal)
                MetricCard(stringResource(R.string.kick_volume), volumeText(snapshot.influxM3, units), Modifier.weight(1f), Red)
            }
        }
        item {
            SectionCard(stringResource(R.string.kick_path)) {
                Text(
                    pathLabel(snapshot.influxPath),
                    color = when (snapshot.influxPath) {
                        com.mosquitobr.controlepoco.core.InfluxPath.CHOKE_LINE -> Teal
                        com.mosquitobr.controlepoco.core.InfluxPath.RISER -> Red
                        com.mosquitobr.controlepoco.core.InfluxPath.BOP -> Amber
                        else -> TextMain
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        snapshot.gasAtSurface && snapshot.influxPath == com.mosquitobr.controlepoco.core.InfluxPath.CHOKE_LINE ->
                            stringResource(R.string.gas_at_manifold)
                        snapshot.gasAtSurface && !snapshot.gasReachedSurface ->
                            stringResource(R.string.gas_at_mgs)
                        snapshot.gasAtSurface -> stringResource(R.string.gas_at_surface)
                        snapshot.gasReachedSurface && snapshot.influxM3 <= 0.01 ->
                            stringResource(R.string.gas_expelled)
                        snapshot.influxM3 <= 0.01 -> stringResource(R.string.no_kick)
                        else -> "${stringResource(R.string.influx)}: ${influxTypeLabel(scenario.formation.influxType)}"
                    },
                    color = when {
                        !snapshot.gasAtSurface -> Muted
                        snapshot.gasReachedSurface -> Red
                        else -> Teal
                    },
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetricCard(stringResource(R.string.bhp), pressureText(snapshot.bhpKPa, units), Modifier.weight(1f), Purple)
                    MetricCard(stringResource(R.string.pore_pressure), pressureText(snapshot.porePressureKPa, units), Modifier.weight(1f), Amber)
                    MetricCard(stringResource(R.string.casing), pressureText(snapshot.casingPressureKPa, units), Modifier.weight(1f), Red)
                }
            }
        }
        if (snapshot.shutInStabilized || snapshot.sidppValid || snapshot.sicpValid) {
            item {
                ShutInGaugesCard(simulator, snapshot, units, refresh)
            }
        }
        item {
            SectionCard(stringResource(R.string.kick_settings)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (snapshot.kickTriggered) stringResource(R.string.kick_triggered)
                        else if (snapshot.kickArmed) stringResource(R.string.kick_armed)
                        else stringResource(R.string.kick_not_armed),
                        color = if (snapshot.kickTriggered) Red else if (snapshot.kickArmed) Amber else Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(
                        triggerLabel(snapshot.kickTriggerMode),
                        if (snapshot.kickArmed) Amber else Muted
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    "${stringResource(R.string.kick_zone)}: ${depthText(scenario.well.tvdAtMd(snapshot.kickZoneTopMdM), units)} – " +
                        "${depthText(scenario.well.tvdAtMd(snapshot.kickZoneBaseMdM), units)}",
                    color = TextMain,
                    fontSize = 10.sp
                )
                Text(
                    "${stringResource(R.string.trigger_depth_md)}: ${depthText(snapshot.kickZoneTopMdM, units)} · " +
                        "${stringResource(R.string.trigger_seconds)}: ${Units.fmt(scenario.kick.triggerAtSec, 0)} s",
                    color = Muted,
                    fontSize = 9.sp
                )
                if (snapshot.kickTriggerMode == com.mosquitobr.controlepoco.core.KickTriggerMode.MANUAL) {
                    Spacer(Modifier.height(5.dp))
                    ActionButton(stringResource(R.string.inject_kick), Modifier.fillMaxWidth(), Red) {
                        simulator.triggerManualKick()
                        refresh()
                    }
                }
                KickConfigFields(simulator, snapshot, scenario, units, refresh)
            }
        }
        item {
            SectionCard(stringResource(R.string.kick_alarm_controls)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.alarm_sound), color = TextMain, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = snapshot.alarmSoundEnabled,
                        onCheckedChange = {
                            simulator.setAlarmSound(it)
                            refresh()
                        }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.gain_log), color = TextMain, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = snapshot.gainLogEnabled,
                        onCheckedChange = {
                            simulator.setGainLog(it)
                            refresh()
                        }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ActionButton(stringResource(R.string.mute_alarm), Modifier.weight(1f)) {
                        simulator.muteAlarm(com.mosquitobr.controlepoco.core.AlarmCode.PIT_GAIN)
                        refresh()
                    }
                    ActionButton(
                        if (snapshot.gainLogEnabled) stringResource(R.string.gain_log_active)
                        else stringResource(R.string.gain_log_paused),
                        Modifier.weight(1f),
                        if (snapshot.gainLogEnabled) Panel3 else Amber
                    ) {
                        simulator.setGainLog(!snapshot.gainLogEnabled)
                        refresh()
                    }
                }
            }
        }
    }
}

@Composable
private fun KickConfigFields(
    simulator: Simulator,
    snapshot: SimSnapshot,
    scenario: Scenario,
    units: UnitSystem,
    refresh: () -> Unit
) {
    val source = scenario.kick
    var maxVolume by remember(snapshot.kickMaxVolumeM3) {
        mutableStateOf(volumeInput(snapshot.kickMaxVolumeM3.coerceAtLeast(0.1), units))
    }
    var rate by remember(snapshot.kickCharacteristicRateLpm) {
        mutableStateOf(rateInput(snapshot.kickCharacteristicRateLpm.coerceAtLeast(1.0), units))
    }
    var gradient by remember(snapshot.kickZoneGradientKPaPerM) {
        mutableStateOf(gradientInput(snapshot.kickZoneGradientKPaPerM, units))
    }
    var permeability by remember(snapshot.kickZonePermeabilityMd) {
        mutableStateOf(Units.fmt(snapshot.kickZonePermeabilityMd, 1))
    }
    Spacer(Modifier.height(6.dp))
    Text(stringResource(R.string.kick_dynamic_note), color = Muted, fontSize = 8.sp)
    if (snapshot.kickTriggered) {
        Spacer(Modifier.height(3.dp))
        Text(
            "${stringResource(R.string.kick_max_volume)}: " +
                volumeText(snapshot.kickMaxVolumeM3, units) + " · " +
                "${stringResource(R.string.kick_characteristic_rate)}: " +
                rateText(snapshot.kickCharacteristicRateLpm, units),
            color = TextMain,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            "${stringResource(R.string.kick_closing_time)}: " +
                Units.fmt(snapshot.kickClosingTimeSec, 1) + " s · " +
                "${stringResource(R.string.kick_stabilization)}: " +
                Units.fmt(snapshot.kickStabilizationFactor, 2),
            color = Muted,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        KickNumberField(
            label = "${stringResource(R.string.kick_max_volume)} (${volumeUnit(units)})",
            value = maxVolume,
            onChange = { maxVolume = it },
            modifier = Modifier.weight(1f)
        )
        KickNumberField(
            label = "${stringResource(R.string.kick_characteristic_rate)} (${rateUnit(units)})",
            value = rate,
            onChange = { rate = it },
            modifier = Modifier.weight(1f)
        )
    }
    Text(stringResource(R.string.kick_rate_hint), color = Muted, fontSize = 7.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        KickNumberField(
            label = "${stringResource(R.string.kick_zone_pore_gradient)} (${gradientUnit(units)})",
            value = gradient,
            onChange = { gradient = it },
            modifier = Modifier.weight(1f)
        )
        KickNumberField(
            label = stringResource(R.string.kick_zone_permeability),
            value = permeability,
            onChange = { permeability = it },
            modifier = Modifier.weight(1f)
        )
    }
    Text(stringResource(R.string.kick_zone_note), color = Muted, fontSize = 7.sp)
    ActionButton(stringResource(R.string.apply_kick_config), Modifier.fillMaxWidth(), TealDark) {
        val newMax = parseVolumeInput(maxVolume, units) ?: snapshot.kickMaxVolumeM3
        val newRate = parseRateInput(rate, units) ?: snapshot.kickCharacteristicRateLpm
        val newGradient = parseGradientInput(gradient, units)
            ?: snapshot.kickZoneGradientKPaPerM
        val newPermeability = permeability.replace(',', '.').toDoubleOrNull()
            ?: snapshot.kickZonePermeabilityMd
        simulator.updateKickConfig(
            source.copy(
                maxVolumeM3 = newMax,
                characteristicRateLpm = newRate,
                zonePoreGradientKPaPerM = newGradient,
                zonePermeabilityMd = newPermeability
            )
        )
        refresh()
    }
}

@Composable
private fun ShutInGaugesCard(
    simulator: Simulator,
    snapshot: SimSnapshot,
    units: UnitSystem,
    refresh: () -> Unit
) {
    var sidppInput by remember(snapshot.recordedSidppKPa, units) {
        mutableStateOf(pressureInput(snapshot.recordedSidppKPa, units))
    }
    var sicpInput by remember(snapshot.recordedSicpKPa, units) {
        mutableStateOf(pressureInput(snapshot.recordedSicpKPa, units))
    }
    SectionCard(stringResource(R.string.shut_in_gauges_title)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MetricCard(
                stringResource(R.string.sidpp),
                if (snapshot.sidppValid) pressureText(snapshot.sidppDisplayKPa(), units) else "—",
                Modifier.weight(1f),
                Blue
            )
            MetricCard(
                stringResource(R.string.sicp),
                if (snapshot.sicpValid) pressureText(snapshot.sicpDisplayKPa(), units) else "—",
                Modifier.weight(1f),
                Amber
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (snapshot.shutInStabilized) {
                "${stringResource(R.string.shut_in_stabilized_label)}: " +
                    "SIDPP ${pressureText(snapshot.stabilizedSidppKPa, units)} · " +
                    "SICP ${pressureText(snapshot.stabilizedSicpKPa, units)}"
            } else {
                stringResource(R.string.guide_b4_waiting)
            },
            color = if (snapshot.shutInStabilized) Green else Muted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        if (snapshot.shutInStabilized && snapshot.migrationOverpressureKPa > 5.0) {
            Spacer(Modifier.height(3.dp))
            Text(
                stringResource(R.string.migration_sicp_hint) +
                    " (+${pressureText(snapshot.migrationOverpressureKPa, units)})",
                color = Amber,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (snapshot.shutInStabilized) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.shut_in_safety_factor), color = TextMain, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.shut_in_safety_hint), color = Muted, fontSize = 8.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val p50 = Units.psiToKpa(50.0)
                val p100 = Units.psiToKpa(100.0)
                ActionButton("+0", Modifier.weight(1f), if (snapshot.shutInSafetyOffsetKPa < 1.0) TealDark else Panel3) {
                    simulator.setShutInSafetyOffset(0.0); refresh()
                }
                ActionButton("+50 psi", Modifier.weight(1f), if (kotlin.math.abs(snapshot.shutInSafetyOffsetKPa - p50) < 10.0) TealDark else Panel3) {
                    simulator.setShutInSafetyOffset(p50); refresh()
                }
                ActionButton("+100 psi", Modifier.weight(1f), if (kotlin.math.abs(snapshot.shutInSafetyOffsetKPa - p100) < 10.0) TealDark else Panel3) {
                    simulator.setShutInSafetyOffset(p100); refresh()
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                KickNumberField(
                    label = "${stringResource(R.string.sidpp)} (${pressureUnit(units)})",
                    value = sidppInput,
                    onChange = { sidppInput = it },
                    modifier = Modifier.weight(1f)
                )
                KickNumberField(
                    label = "${stringResource(R.string.sicp)} (${pressureUnit(units)})",
                    value = sicpInput,
                    onChange = { sicpInput = it },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(4.dp))
            ActionButton(stringResource(R.string.apply_safety_pair), Modifier.fillMaxWidth(), TealDark) {
                val targetSidpp = parsePressureInput(sidppInput, units) ?: snapshot.recordedSidppKPa
                val targetSicp = parsePressureInput(sicpInput, units) ?: snapshot.recordedSicpKPa
                simulator.applyShutInSafetyPair(targetSidpp, targetSicp)
                refresh()
            }
        }
    }
}

@Composable
private fun KickNumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 8.sp) },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun TripPanel(
    simulator: Simulator,
    snapshot: SimSnapshot,
    scenario: Scenario,
    units: UnitSystem,
    refresh: () -> Unit
) {
    val lineupOptions = listOf(
        com.mosquitobr.controlepoco.core.ManifoldLineup.DRILLING,
        com.mosquitobr.controlepoco.core.ManifoldLineup.KILL,
        com.mosquitobr.controlepoco.core.ManifoldLineup.CHOKE,
        com.mosquitobr.controlepoco.core.ManifoldLineup.TRIP,
        com.mosquitobr.controlepoco.core.ManifoldLineup.LUBRICATE
    )
    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MetricCard(stringResource(R.string.trip_tank), volumeText(snapshot.tripTankM3, units), Modifier.weight(1f), Blue)
                MetricCard(stringResource(R.string.hole_fill), volumeText(snapshot.holeFillM3, units), Modifier.weight(1f), Amber)
                MetricCard(stringResource(R.string.bit_depth), depthText(snapshot.bitDepthM, units), Modifier.weight(1f), Teal)
                MetricCard(stringResource(R.string.flow_out), rateText(snapshot.flowOutLpm, units), Modifier.weight(1f), Muted)
            }
        }
        item {
            SectionCard(stringResource(R.string.trip)) {
                val tripManifoldBlocked = snapshot.standpipeLineup == com.mosquitobr.controlepoco.core.ManifoldLineup.KILL ||
                    snapshot.standpipeLineup == com.mosquitobr.controlepoco.core.ManifoldLineup.CHOKE ||
                    snapshot.chokeLineup == com.mosquitobr.controlepoco.core.ManifoldLineup.KILL ||
                    snapshot.chokeLineup == com.mosquitobr.controlepoco.core.ManifoldLineup.CHOKE
                val canTrip = !snapshot.closedBop && !snapshot.pumpOn && snapshot.pumpSpm <= 0.5 &&
                    !snapshot.rotaryOn && snapshot.rpm <= 1.0 && snapshot.lineupValid && !tripManifoldBlocked
                val canFill = !snapshot.closedBop && !snapshot.pumpOn && snapshot.pumpSpm <= 0.5 &&
                    snapshot.lineupValid && snapshot.holeFillM3 > 0.001
                Text(
                    "${stringResource(R.string.md)} ${depthText(snapshot.bitDepthM, units)} · " +
                        "${stringResource(R.string.tvd)} ${depthText(snapshot.bitTvdM, units)}",
                    color = TextMain,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
                if (!canTrip) {
                    Text(
                        "${stringResource(R.string.trip_blocked_hint)}: " + when {
                            snapshot.closedBop -> stringResource(R.string.bop_closed)
                            snapshot.pumpOn || snapshot.pumpSpm > 0.5 -> stringResource(R.string.pump_running)
                            snapshot.rotaryOn || snapshot.rpm > 1.0 -> stringResource(R.string.rotary_on)
                            else -> stringResource(R.string.lineup_invalid)
                        },
                        color = Red,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ActionButton(stringResource(R.string.pull_stand), Modifier.weight(1f), if (canTrip) Amber else Panel3) {
                        simulator.tripPipe(-27.0); refresh()
                    }
                    ActionButton(stringResource(R.string.run_stand), Modifier.weight(1f), if (canTrip) TealDark else Panel3) {
                        simulator.tripPipe(27.0); refresh()
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ActionButton("-9 m", Modifier.weight(1f)) {
                        simulator.tripPipe(-9.0); refresh()
                    }
                    ActionButton("+9 m", Modifier.weight(1f)) {
                        simulator.tripPipe(9.0); refresh()
                    }
                    ActionButton(stringResource(R.string.fill_hole), Modifier.weight(1f), if (canFill) TealDark else Panel3) {
                        simulator.fillHole(0.2); refresh()
                    }
                    ActionButton(stringResource(R.string.flow_check), Modifier.weight(1f)) {
                        simulator.flowCheck(); refresh()
                    }
                }
                Spacer(Modifier.height(4.dp))
                ActionButton(stringResource(R.string.refill_trip_tank), Modifier.fillMaxWidth()) {
                    simulator.transferPitToTripTank(1.0); refresh()
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.swab_surge) + ": " +
                        if (snapshot.holeFillM3 > 0.05) stringResource(R.string.warning) else stringResource(R.string.info),
                    color = if (snapshot.holeFillM3 > 0.05) Amber else Muted,
                    fontSize = 9.sp
                )
            }
        }
        item {
            SectionCard(stringResource(R.string.standpipe_lineup)) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    lineupOptions.forEach { option ->
                        FilterChip(
                            selected = snapshot.standpipeLineup == option,
                            onClick = {
                                simulator.setLineups(option, snapshot.chokeLineup)
                                refresh()
                            },
                            label = { Text(lineupLabel(option), fontSize = 9.sp) }
                        )
                    }
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.choke_lineup)) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    lineupOptions.forEach { option ->
                        FilterChip(
                            selected = snapshot.chokeLineup == option,
                            onClick = {
                                simulator.setLineups(snapshot.standpipeLineup, option)
                                refresh()
                            },
                            label = { Text(lineupLabel(option), fontSize = 9.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    if (snapshot.lineupValid) stringResource(R.string.lineup_valid) else stringResource(R.string.lineup_invalid),
                    color = if (snapshot.lineupValid) Green else Red,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    scenario.rig.standpipeManifold.joinToString(" · "),
                    color = Muted,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    scenario.rig.chokeManifold.joinToString(" · "),
                    color = Muted,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun pathLabel(path: com.mosquitobr.controlepoco.core.InfluxPath): String = when (path) {
    com.mosquitobr.controlepoco.core.InfluxPath.FORMATION -> stringResource(R.string.path_formation)
    com.mosquitobr.controlepoco.core.InfluxPath.ANNULUS -> stringResource(R.string.path_annulus)
    com.mosquitobr.controlepoco.core.InfluxPath.BOP -> stringResource(R.string.path_bop)
    com.mosquitobr.controlepoco.core.InfluxPath.RISER -> stringResource(R.string.path_riser)
    com.mosquitobr.controlepoco.core.InfluxPath.CHOKE_LINE -> stringResource(R.string.path_choke_line)
    com.mosquitobr.controlepoco.core.InfluxPath.KILL_LINE -> stringResource(R.string.path_kill_line)
    com.mosquitobr.controlepoco.core.InfluxPath.SURFACE -> stringResource(R.string.path_surface)
}

@Composable
private fun triggerLabel(mode: com.mosquitobr.controlepoco.core.KickTriggerMode): String = when (mode) {
    com.mosquitobr.controlepoco.core.KickTriggerMode.IMMEDIATE -> stringResource(R.string.trigger_immediate)
    com.mosquitobr.controlepoco.core.KickTriggerMode.TIME -> stringResource(R.string.trigger_time)
    com.mosquitobr.controlepoco.core.KickTriggerMode.DEPTH -> stringResource(R.string.trigger_depth)
    com.mosquitobr.controlepoco.core.KickTriggerMode.HYBRID -> stringResource(R.string.trigger_hybrid)
    com.mosquitobr.controlepoco.core.KickTriggerMode.MANUAL -> stringResource(R.string.trigger_manual)
}

@Composable
private fun influxTypeLabel(type: com.mosquitobr.controlepoco.core.InfluxType): String = when (type) {
    com.mosquitobr.controlepoco.core.InfluxType.GAS -> stringResource(R.string.influx_gas)
    com.mosquitobr.controlepoco.core.InfluxType.OIL -> stringResource(R.string.influx_oil)
    com.mosquitobr.controlepoco.core.InfluxType.WATER -> stringResource(R.string.influx_water)
    com.mosquitobr.controlepoco.core.InfluxType.SALTWATER -> stringResource(R.string.influx_saltwater)
}

@Composable
private fun lineupLabel(lineup: com.mosquitobr.controlepoco.core.ManifoldLineup): String = when (lineup) {
    com.mosquitobr.controlepoco.core.ManifoldLineup.DRILLING -> stringResource(R.string.lineup_drilling)
    com.mosquitobr.controlepoco.core.ManifoldLineup.KILL -> stringResource(R.string.lineup_kill)
    com.mosquitobr.controlepoco.core.ManifoldLineup.CHOKE -> stringResource(R.string.lineup_choke)
    com.mosquitobr.controlepoco.core.ManifoldLineup.TRIP -> stringResource(R.string.lineup_trip)
    com.mosquitobr.controlepoco.core.ManifoldLineup.LUBRICATE -> stringResource(R.string.lineup_lubricate)
    com.mosquitobr.controlepoco.core.ManifoldLineup.EDS -> stringResource(R.string.lineup_eds)
    com.mosquitobr.controlepoco.core.ManifoldLineup.CUSTOM -> stringResource(R.string.lineup_custom)
}

@Composable
private fun ChokePanel(
    simulator: Simulator,
    snapshot: SimSnapshot,
    scenario: Scenario,
    units: UnitSystem,
    refresh: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Gauge(
                    stringResource(R.string.casing),
                    pressureValue(snapshot.casingPressureKPa, units),
                    (snapshot.casingPressureKPa / 25000.0).toFloat(),
                    Amber,
                    Modifier.weight(1f).height(105.dp)
                )
                Gauge(
                    stringResource(R.string.spp),
                    pressureValue(snapshot.sppKPa, units),
                    (snapshot.sppKPa / 20000.0).toFloat(),
                    Teal,
                    Modifier.weight(1f).height(105.dp)
                )
                Gauge(
                    stringResource(R.string.choke),
                    "${Units.fmt(snapshot.chokePct)}%",
                    (snapshot.chokePct / 100.0).toFloat(),
                    Purple,
                    Modifier.weight(1f).height(105.dp)
                )
            }
        }
        item {
            // Choke A/B selector
            SectionCard(stringResource(R.string.active_choke)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.active_choke),
                        color = Muted,
                        fontSize = 9.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        snapshot.activeChokeName,
                        color = Teal,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (scenario.rig.chokes.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        scenario.rig.chokes.forEachIndexed { index, choke ->
                            FilterChip(
                                selected = snapshot.activeChokeIndex == index,
                                onClick = {
                                    simulator.setActiveChoke(index)
                                    refresh()
                                },
                                label = { Text(choke.name, fontSize = 8.sp) }
                            )
                        }
                    }
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.choke)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${Units.fmt(snapshot.chokePct)}%",
                        color = Purple,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (snapshot.chokeAuto) stringResource(R.string.auto_on) else stringResource(R.string.choke_manual),
                        color = if (snapshot.chokeAuto) Green else Muted,
                        fontSize = 10.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = snapshot.chokeAuto,
                        onCheckedChange = {
                            simulator.setChokeAuto(it, snapshot.casingPressureKPa)
                            refresh()
                        }
                    )
                }
                Slider(
                    value = snapshot.chokePct.toFloat(),
                    onValueChange = {
                        simulator.setChoke(it.toDouble())
                        refresh()
                    },
                    valueRange = 0f..100f
                )
                if (snapshot.chokeAuto) {
                    Text(
                        "${stringResource(R.string.setpoint)}: ${pressureText(snapshot.chokeSetpointKPa, units)}",
                        color = Muted,
                        fontSize = 9.sp
                    )
                    Slider(
                        value = snapshot.chokeSetpointKPa.toFloat(),
                        onValueChange = {
                            simulator.setChokeSetpoint(it.toDouble())
                            refresh()
                        },
                        valueRange = 0f..60000f
                    )
                }
                if (snapshot.chokeOpenNoPump) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.choke_open_depressurizing),
                        color = Red,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MetricCard(stringResource(R.string.flow_out), rateText(snapshot.flowOutLpm, units), Modifier.weight(1f), Amber)
                MetricCard(stringResource(R.string.accumulator), pressureText(snapshot.accumulatorKPa, units), Modifier.weight(1f), Blue)
                MetricCard(stringResource(R.string.choke_line), pressureText(snapshot.casingPressureKPa + snapshot.maspKPa - snapshot.sicpKPa, units), Modifier.weight(1f), Muted)
            }
        }
        item {
            SectionCard(stringResource(R.string.hcr)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (snapshot.hcrOpen) stringResource(R.string.hcr_open) else stringResource(R.string.hcr_closed),
                        color = if (snapshot.hcrOpen) Green else Muted,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = snapshot.hcrOpen,
                        onCheckedChange = {
                            simulator.setHcr(it); refresh()
                        }
                    )
                }
                if (snapshot.hcrOpen && snapshot.standpipeLineup == ManifoldLineup.DRILLING) {
                    Text(
                        stringResource(R.string.hcr_drilling_warning),
                        color = Amber,
                        fontSize = 8.sp
                    )
                }
                Text(
                    stringResource(R.string.hcr_help),
                    color = Muted,
                    fontSize = 8.sp
                )
            }
        }
        item {
            SectionCard(stringResource(R.string.manifolds)) {
                Text(
                    stringResource(R.string.standpipe_lineup),
                    color = Muted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    lineupLabel(snapshot.standpipeLineup),
                    color = Teal,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    scenario.rig.standpipeManifold.joinToString("  ·  "),
                    color = Muted,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    stringResource(R.string.choke_lineup),
                    color = Muted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    lineupLabel(snapshot.chokeLineup),
                    color = Amber,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    scenario.rig.chokeManifold.joinToString("  ·  "),
                    color = Muted,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    if (snapshot.lineupValid) stringResource(R.string.lineup_valid) else stringResource(R.string.lineup_invalid),
                    color = if (snapshot.lineupValid) Green else Red,
                    fontSize = 9.sp
                )
            }
        }
        item {
            SectionCard(stringResource(R.string.manifold_help)) {
                Text(stringResource(R.string.manifold_drilling_help), color = Muted, fontSize = 9.sp)
                Text(stringResource(R.string.manifold_kill_help), color = Muted, fontSize = 9.sp)
                Text(stringResource(R.string.manifold_choke_help), color = Muted, fontSize = 9.sp)
                Text(stringResource(R.string.manifold_trip_help), color = Muted, fontSize = 9.sp)
                Text(stringResource(R.string.manifold_lubricate_help), color = Muted, fontSize = 9.sp)
                Spacer(Modifier.height(5.dp))
                Text(stringResource(R.string.hcr_help), color = Amber, fontSize = 9.sp)
                Text(stringResource(R.string.scr_help), color = Teal, fontSize = 9.sp)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ActionButton(stringResource(R.string.bleed), Modifier.weight(1f)) {
                    simulator.bleed(0.2); refresh()
                }
                ActionButton(stringResource(R.string.lubricate), Modifier.weight(1f)) {
                    simulator.lubricate(0.2); refresh()
                }
                ActionButton(stringResource(R.string.weight_up_kill), Modifier.weight(1f)) {
                    simulator.weightUpToKillMud(); refresh()
                }
            }
        }
    }
}

@Composable
private fun BopPanel(
    simulator: Simulator,
    snapshot: SimSnapshot,
    scenario: Scenario,
    units: UnitSystem,
    refresh: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item {
            SectionCard(stringResource(R.string.bop_stack)) {
                scenarioElements(scenario).forEach { element ->
                    val isHcr = element == BopElement.HCR
                    val active = if (isHcr) snapshot.hcrOpen else snapshot.bopClosed.contains(element)
                    val activeColor = if (isHcr) {
                        if (active) Teal else Muted
                    } else {
                        if (active) Red else Green
                    }
                    val stateLabel = if (isHcr) {
                        if (active) "ABERTO" else "FECHADO"
                    } else {
                        if (active) "FECHADO" else "ABERTO"
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(if (active) activeColor.copy(alpha = 0.12f) else Panel2, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            elementName(element),
                            color = if (active) activeColor else TextMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            stateLabel,
                            color = activeColor,
                            fontSize = 9.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (isHcr) {
                                    simulator.setHcr(!active)
                                } else if (active) {
                                    simulator.openBopElement(element)
                                } else {
                                    simulator.closeBop(element)
                                }
                                refresh()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Panel3)
                        ) {
                            Text(
                                if (isHcr) {
                                    if (active) "Fechar" else "Abrir"
                                } else {
                                    if (active) "Abrir" else "Fechar"
                                },
                                fontSize = 9.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ActionButton(stringResource(R.string.close_bop), Modifier.weight(1f)) {
                        simulator.closeBop(BopElement.ANNULAR); refresh()
                    }
                    ActionButton(
                        stringResource(R.string.open_bop),
                        Modifier.weight(1f),
                        if (snapshot.readyToOpen) Green else Red
                    ) {
                        simulator.openBop(); refresh()
                    }
                }
            }
        }
        item {
            LineDisplacementCard(simulator, snapshot, units, refresh)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MetricCard(stringResource(R.string.accumulator), pressureText(snapshot.accumulatorKPa, units), Modifier.weight(1f), Blue)
                MetricCard(stringResource(R.string.bop_stack), if (snapshot.closedBop) stringResource(R.string.bop_closed) else stringResource(R.string.bop_open), Modifier.weight(1f), if (snapshot.closedBop) Red else Green)
                MetricCard("HCR", if (snapshot.hcrOpen) "OPEN" else "CLOSED", Modifier.weight(1f), if (snapshot.hcrOpen) Teal else Muted)
            }
        }
        item {
            Text(
                "Feche o anular para kick. Pipe rams para coluna. Blind/shear para emergência. Abra o HCR para alinhar a linha de choke.",
                color = Muted,
                fontSize = 9.sp
            )
        }
    }
}

// Índices das abas do simulador. A aba Guia é a primeira para ficar sempre acessível.
private const val TAB_GUIDE = 0
private const val TAB_DRILLER = 1
private const val TAB_KICK = 2
private const val TAB_TRIP = 3
private const val TAB_CHOKE = 4
private const val TAB_BOP = 5
private const val TAB_MUD = 6
private const val TAB_TRENDS = 7
private const val TAB_LOG = 8

private enum class GuideActionKind {
    NONE,
    KILL_SHEET,
    FIRST_CIRC,
    WEIGHT_UP,
    SECOND_CIRC,
    WW_CIRC,
    BLEED_OR_OPEN,
    BLEED_VOL,
    LUBRICATE,
    TAKE_SCR
}

/** Um passo do guia operacional, com local, ação e valor esperado. */
private data class GuideSpec(
    val phaseA: Boolean,
    val number: Int,
    val titleRes: Int,
    val whereRes: Int,
    val howRes: Int,
    val value: String,
    val done: Boolean,
    val targetTab: Int,
    val action: GuideActionKind = GuideActionKind.NONE
)

@Composable
private fun GuidePanel(
    simulator: Simulator,
    snapshot: SimSnapshot,
    scenario: Scenario,
    units: UnitSystem,
    onTab: (Int) -> Unit,
    onKillSheet: () -> Unit,
    refresh: () -> Unit,
    onSaveResult: () -> Unit,
    onResetGuide: () -> Unit,
    onResetSimulation: () -> Unit
) {
    val prep = snapshot.prepChecks.associateBy { it.id }
    fun done(id: PrepCheckId) = prep[id]?.done == true

    @Composable
    fun reading(id: PrepCheckId, gpm: Int, fallback: Int): String {
        val kPa = prep[id]?.value ?: 0.0
        return if (kPa > 0.0) "${pressureText(kPa, units)} @ $gpm gpm"
        else stringResource(fallback)
    }

    val scr50 = reading(PrepCheckId.SCR_50, 50, R.string.guide_a5_value)
    val scr100 = reading(PrepCheckId.SCR_100, 100, R.string.guide_a6_value)
    val scr150 = reading(PrepCheckId.SCR_150, 150, R.string.guide_a7_value)
    val scr200 = reading(PrepCheckId.SCR_200, 200, R.string.guide_a8_value)
    val deltaP = prep[PrepCheckId.DELTA_P_CHOKE_LINE]?.value ?: 0.0
    val deltaPText = if (deltaP > 0.0) {
        "${Units.fmt(deltaP, 0)} psi @ ${Units.fmt(snapshot.flowInLpm / 3.785411784, 0)} gpm"
    } else stringResource(R.string.guide_a9_value)

    val kmw = snapshot.killSheet?.killMudKgM3 ?: snapshot.targetMudDensityKgM3

    val phaseA = listOf(
        GuideSpec(
            true, 1, R.string.guide_a1_title, R.string.guide_a1_where, R.string.guide_a1_how,
            stringResource(R.string.guide_a1_value), done(PrepCheckId.FLOW_CHECK), TAB_DRILLER
        ),
        GuideSpec(
            true, 2, R.string.guide_a2_title, R.string.guide_a2_where, R.string.guide_a2_how,
            stringResource(R.string.guide_a2_value), done(PrepCheckId.GAIN_ALARM), TAB_GUIDE
        ),
        GuideSpec(
            true, 3, R.string.guide_a3_title, R.string.guide_a3_where, R.string.guide_a3_how,
            stringResource(R.string.guide_a3_value), done(PrepCheckId.LOSS_ALARM), TAB_GUIDE
        ),
        GuideSpec(
            true, 4, R.string.guide_a4_title, R.string.guide_a4_where, R.string.guide_a4_how,
            stringResource(R.string.guide_a4_value), done(PrepCheckId.RETURN_FLOW_ALARM), TAB_GUIDE
        ),
        GuideSpec(
            true, 5, R.string.guide_a5_title, R.string.guide_a5_where, R.string.guide_a5_how,
            scr50, done(PrepCheckId.SCR_50), TAB_GUIDE
        ),
        GuideSpec(
            true, 6, R.string.guide_a6_title, R.string.guide_a6_where, R.string.guide_a6_how,
            scr100, done(PrepCheckId.SCR_100), TAB_GUIDE
        ),
        GuideSpec(
            true, 7, R.string.guide_a7_title, R.string.guide_a7_where, R.string.guide_a7_how,
            scr150, done(PrepCheckId.SCR_150), TAB_GUIDE
        ),
        GuideSpec(
            true, 8, R.string.guide_a8_title, R.string.guide_a8_where, R.string.guide_a8_how,
            scr200, done(PrepCheckId.SCR_200), TAB_GUIDE
        ),
        GuideSpec(
            true, 9, R.string.guide_a9_title, R.string.guide_a9_where, R.string.guide_a9_how,
            deltaPText, done(PrepCheckId.DELTA_P_CHOKE_LINE), TAB_CHOKE
        ),
        GuideSpec(
            true, 10, R.string.guide_a10_title, R.string.guide_a10_where, R.string.guide_a10_how,
            stringResource(R.string.guide_a10_value), done(PrepCheckId.BOP_TEST), TAB_BOP
        )
    )

    val stabilizedValueText = if (snapshot.shutInStabilized) {
        stringResource(
            R.string.guide_b4_value,
            pressureText(snapshot.recordedSidppKPa, units),
            pressureText(snapshot.recordedSicpKPa, units)
        )
    } else {
        stringResource(R.string.guide_b4_waiting)
    }

    val isSandboxIdle = scenario.method == "Sandbox" && !snapshot.kickDetected && !snapshot.kickTriggered
    val phaseB: List<GuideSpec> = when {
        scenario.method == "Tutorial" -> listOf(
            GuideSpec(
                false, 1, R.string.guide_tut_1_title, R.string.guide_tut_1_where, R.string.guide_tut_1_how,
                stringResource(R.string.guide_tut_1_value),
                snapshot.pumpOn && snapshot.pumpSpm > 10.0 && snapshot.ecdKgM3 > snapshot.mudDensityKgM3,
                TAB_DRILLER
            ),
            GuideSpec(
                false, 2, R.string.guide_tut_2_title, R.string.guide_tut_2_where, R.string.guide_tut_2_how,
                stringResource(R.string.guide_tut_2_value),
                snapshot.scrTaken || snapshot.guideTutorialDone,
                TAB_DRILLER, GuideActionKind.TAKE_SCR
            ),
            GuideSpec(
                false, 3, R.string.guide_tut_3_title, R.string.guide_tut_3_where, R.string.guide_tut_3_how,
                stringResource(R.string.guide_tut_3_value),
                snapshot.prepComplete,
                TAB_GUIDE
            )
        )
        isSandboxIdle -> emptyList()
        scenario.method == "Wait & Weight" -> listOf(
            GuideSpec(
                false, 1, R.string.guide_b1_title, R.string.guide_b1_where, R.string.guide_b1_how,
                stringResource(R.string.guide_b1_value), snapshot.guideKickHandled, TAB_KICK
            ),
            GuideSpec(
                false, 2, R.string.guide_b2_title, R.string.guide_b2_where, R.string.guide_b2_how,
                stringResource(R.string.guide_b2_value),
                snapshot.guideKickHandled && snapshot.guideDrillingStopped, TAB_DRILLER
            ),
            GuideSpec(
                false, 3, R.string.guide_b3_title, R.string.guide_b3_where, R.string.guide_b3_how,
                stringResource(R.string.guide_b3_value), snapshot.guideBopClosed, TAB_BOP
            ),
            GuideSpec(
                false, 4, R.string.guide_b4_title, R.string.guide_b4_where, R.string.guide_b4_how,
                stabilizedValueText, snapshot.guideStabilized, TAB_KICK
            ),
            GuideSpec(
                false, 5, R.string.guide_b5_title, R.string.guide_b5_where, R.string.guide_b5_how,
                stringResource(R.string.guide_b5_value, densityText(kmw, units)),
                snapshot.guideKillSheetReady, TAB_MUD, GuideActionKind.KILL_SHEET
            ),
            GuideSpec(
                false, 6, R.string.guide_b7_title, R.string.guide_b7_where, R.string.guide_b7_how,
                stringResource(R.string.guide_b7_value, densityText(kmw, units)),
                snapshot.guideWeightedUp, TAB_MUD, GuideActionKind.WEIGHT_UP
            ),
            GuideSpec(
                false, 7, R.string.guide_ww_circulation_title, R.string.guide_ww_circulation_where, R.string.guide_ww_circulation_how,
                stringResource(R.string.guide_ww_circulation_value),
                snapshot.guideSecondCirculation, TAB_MUD, GuideActionKind.WW_CIRC
            ),
            GuideSpec(
                false, 8, R.string.guide_b9_title, R.string.guide_b9_where, R.string.guide_b9_how,
                stringResource(R.string.guide_b9_value), snapshot.guideLinesDisplaced, TAB_BOP
            ),
            GuideSpec(
                false, 9, R.string.guide_b10_title, R.string.guide_b10_where, R.string.guide_b10_how,
                stringResource(R.string.guide_b10_value),
                snapshot.guideBleedDone || snapshot.guideCycleComplete, TAB_BOP, GuideActionKind.BLEED_OR_OPEN
            )
        )
        scenario.method == "Volumétrico" -> listOf(
            GuideSpec(
                false, 1, R.string.guide_b1_title, R.string.guide_b1_where, R.string.guide_b1_how,
                stringResource(R.string.guide_b1_value), snapshot.guideKickHandled, TAB_KICK
            ),
            GuideSpec(
                false, 2, R.string.guide_b3_title, R.string.guide_b3_where, R.string.guide_vol_b2_how,
                stringResource(R.string.guide_b3_value),
                snapshot.guideBopClosed && snapshot.closedBop && !snapshot.pumpOn, TAB_BOP
            ),
            GuideSpec(
                false, 3, R.string.guide_b4_title, R.string.guide_b4_where, R.string.guide_b4_how,
                stabilizedValueText, snapshot.guideStabilized, TAB_KICK
            ),
            GuideSpec(
                false, 4, R.string.guide_vol_b4_title, R.string.guide_vol_b4_where, R.string.guide_vol_b4_how,
                stringResource(R.string.guide_vol_b4_value),
                snapshot.guideVolumetricBleedDone, TAB_CHOKE, GuideActionKind.BLEED_VOL
            ),
            GuideSpec(
                false, 5, R.string.guide_vol_b5_title, R.string.guide_vol_b5_where, R.string.guide_vol_b5_how,
                stringResource(R.string.guide_vol_b5_value),
                snapshot.gasAtSurface && !snapshot.gasReachedSurface, TAB_KICK
            )
        )
        scenario.method == "Lubrificar & sangrar" -> listOf(
            GuideSpec(
                false, 1, R.string.guide_b3_title, R.string.guide_b3_where, R.string.guide_b3_how,
                stringResource(R.string.guide_b3_value), snapshot.guideBopClosed && snapshot.closedBop, TAB_BOP
            ),
            GuideSpec(
                false, 2, R.string.guide_b4_title, R.string.guide_b4_where, R.string.guide_b4_how,
                stabilizedValueText, snapshot.guideStabilized, TAB_KICK
            ),
            GuideSpec(
                false, 3, R.string.guide_lub_b3_title, R.string.guide_lub_b3_where, R.string.guide_lub_b3_how,
                stringResource(R.string.guide_lub_b3_value),
                snapshot.trappedPressureKPa > 0.0 || snapshot.guideLubricateDone, TAB_CHOKE, GuideActionKind.LUBRICATE
            ),
            GuideSpec(
                false, 4, R.string.guide_lub_b4_title, R.string.guide_lub_b4_where, R.string.guide_lub_b4_how,
                stringResource(R.string.guide_lub_b4_value),
                snapshot.guideLubricateDone, TAB_CHOKE, GuideActionKind.BLEED_VOL
            )
        )
        scenario.method == "Emergência" -> listOf(
            GuideSpec(
                false, 1, R.string.guide_eds_b1_title, R.string.guide_eds_b1_where, R.string.guide_eds_b1_how,
                stringResource(R.string.guide_eds_b1_value),
                snapshot.guideKickHandled || snapshot.failures.isNotEmpty(), TAB_LOG
            ),
            GuideSpec(
                false, 2, R.string.guide_b3_title, R.string.guide_b3_where, R.string.guide_b3_how,
                stringResource(R.string.guide_b3_value), snapshot.guideBopClosed, TAB_BOP
            ),
            GuideSpec(
                false, 3, R.string.guide_eds_b3_title, R.string.guide_eds_b3_where, R.string.guide_eds_b3_how,
                stringResource(R.string.guide_eds_b3_value),
                snapshot.guideEdsProtected && !snapshot.gasReachedSurface, TAB_KICK
            ),
            GuideSpec(
                false, 4, R.string.guide_b4_title, R.string.guide_b4_where, R.string.guide_b4_how,
                stabilizedValueText,
                snapshot.guideStabilized || snapshot.influxPath == com.mosquitobr.controlepoco.core.InfluxPath.CHOKE_LINE,
                TAB_KICK
            )
        )
        else -> listOf(
            GuideSpec(
                false, 1, R.string.guide_b1_title, R.string.guide_b1_where, R.string.guide_b1_how,
                stringResource(R.string.guide_b1_value),
                snapshot.guideKickHandled, TAB_KICK
            ),
            GuideSpec(
                false, 2, R.string.guide_b2_title, R.string.guide_b2_where, R.string.guide_b2_how,
                stringResource(R.string.guide_b2_value),
                snapshot.guideKickHandled && snapshot.guideDrillingStopped, TAB_DRILLER
            ),
            GuideSpec(
                false, 3, R.string.guide_b3_title, R.string.guide_b3_where, R.string.guide_b3_how,
                stringResource(R.string.guide_b3_value), snapshot.guideBopClosed, TAB_BOP
            ),
            GuideSpec(
                false, 4, R.string.guide_b4_title, R.string.guide_b4_where, R.string.guide_b4_how,
                stabilizedValueText, snapshot.guideStabilized, TAB_KICK
            ),
            GuideSpec(
                false, 5, R.string.guide_b5_title, R.string.guide_b5_where, R.string.guide_b5_how,
                stringResource(R.string.guide_b5_value, densityText(kmw, units)),
                snapshot.guideKillSheetReady, TAB_MUD, GuideActionKind.KILL_SHEET
            ),
            GuideSpec(
                false, 6, R.string.guide_b6_title, R.string.guide_b6_where, R.string.guide_b6_how,
                stringResource(R.string.guide_b6_value),
                snapshot.guideFirstCirculation || (snapshot.firstCirculationReady && snapshot.firstCirculationKickOut),
                TAB_CHOKE, GuideActionKind.FIRST_CIRC
            ),
            GuideSpec(
                false, 7, R.string.guide_b7_title, R.string.guide_b7_where, R.string.guide_b7_how,
                stringResource(R.string.guide_b7_value, densityText(kmw, units)),
                snapshot.guideWeightedUp, TAB_MUD, GuideActionKind.WEIGHT_UP
            ),
            GuideSpec(
                false, 8, R.string.guide_b8_title, R.string.guide_b8_where, R.string.guide_b8_how,
                stringResource(R.string.guide_b8_value),
                snapshot.guideSecondCirculation, TAB_MUD, GuideActionKind.SECOND_CIRC
            ),
            GuideSpec(
                false, 9, R.string.guide_b9_title, R.string.guide_b9_where, R.string.guide_b9_how,
                stringResource(R.string.guide_b9_value), snapshot.guideLinesDisplaced, TAB_BOP
            ),
            GuideSpec(
                false, 10, R.string.guide_b10_title, R.string.guide_b10_where, R.string.guide_b10_how,
                stringResource(R.string.guide_b10_value),
                snapshot.guideBleedDone || snapshot.guideCycleComplete, TAB_BOP, GuideActionKind.BLEED_OR_OPEN
            )
        )
    }

    val all = phaseA + phaseB
    val doneCount = all.count { it.done }
    val phaseADone = phaseA.count { it.done }

    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            SectionCard(stringResource(R.string.guide_title)) {
                Text(
                    stringResource(R.string.guide_progress, doneCount, all.size),
                    color = TextMain,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = doneCount / all.size.toFloat(),
                    color = Teal,
                    trackColor = Panel3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusPill(
                        "${stringResource(R.string.guide_phase_a)} · $phaseADone/${phaseA.size}",
                        if (phaseADone == phaseA.size) Teal else Amber
                    )
                    StatusPill(
                        "${stringResource(R.string.guide_phase_b)} · ${doneCount - phaseADone}/${phaseB.size}",
                        if (snapshot.kickDetected) Red else Muted
                    )
                }
            }
        }

        item { GuidePhaseHeader(stringResource(R.string.guide_phase_a), Amber) }

        items(phaseA.size) { index ->
            val spec = phaseA[index]
            GuideStepCard(spec, onTab) {
                when (spec.number) {
                    1 -> Column {
                        if (!done(PrepCheckId.FLOW_CHECK) && !snapshot.flowCheckRunning) {
                            ActionButton(stringResource(R.string.start_flow_check), Modifier, TealDark) {
                                simulator.flowCheck(); refresh()
                            }
                        }
                        if (snapshot.flowCheckRunning) {
                            Text(
                                stringResource(R.string.flow_check_running),
                                color = Amber,
                                fontSize = 8.sp
                            )
                            LinearProgressIndicator(
                                progress = snapshot.flowCheckProgress.toFloat(),
                                color = Amber,
                                trackColor = Panel3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (snapshot.flowCheckProgress < 1.0) {
                                Text(
                                    stringResource(R.string.flow_check_wait),
                                    color = Muted,
                                    fontSize = 8.sp
                                )
                            }
                            Text(
                                stringResource(
                                    R.string.flow_check_flow_value,
                                    rateText(snapshot.flowCheckFlowLpm, units)
                                ),
                                color = Muted,
                                fontSize = 8.sp
                            )
                        } else if (done(PrepCheckId.FLOW_CHECK)) {
                            Text(
                                stringResource(R.string.flow_check_result) + ": " +
                                    rateText(snapshot.flowCheckFlowLpm, units),
                                color = Green,
                                fontSize = 8.sp
                            )
                        }
                    }
                    2 -> {
                        AlarmOption("5 bbl", done(PrepCheckId.GAIN_ALARM) && snapshot.pitGainAlarmM3 < Units.bblToM3(7.5)) {
                            simulator.setGainAlarmBbl(5.0); refresh()
                        }
                        AlarmOption("10 bbl", done(PrepCheckId.GAIN_ALARM) && snapshot.pitGainAlarmM3 >= Units.bblToM3(7.5)) {
                            simulator.setGainAlarmBbl(10.0); refresh()
                        }
                    }
                    3 -> {
                        AlarmOption("5 bbl", done(PrepCheckId.LOSS_ALARM) && snapshot.pitLossAlarmM3 < Units.bblToM3(7.5)) {
                            simulator.setLossAlarmBbl(5.0); refresh()
                        }
                        AlarmOption("10 bbl", done(PrepCheckId.LOSS_ALARM) && snapshot.pitLossAlarmM3 >= Units.bblToM3(7.5)) {
                            simulator.setLossAlarmBbl(10.0); refresh()
                        }
                    }
                    4 -> {
                        AlarmOption("±10%", done(PrepCheckId.RETURN_FLOW_ALARM) && snapshot.returnFlowAlarmPct <= 10.5) {
                            simulator.setReturnFlowAlarmPct(10.0); refresh()
                        }
                        AlarmOption("±5%", done(PrepCheckId.RETURN_FLOW_ALARM) && snapshot.returnFlowAlarmPct < 7.5) {
                            simulator.setReturnFlowAlarmPct(5.0); refresh()
                        }
                    }
                    in 5..8 -> {
                        val rate = 50 * (spec.number - 4)
                        ActionButton("$rate gpm", Modifier, Panel3) {
                            simulator.setRateGpm(rate.toDouble()); refresh()
                        }
                        ActionButton(stringResource(R.string.guide_record_reading), Modifier, TealDark) {
                            simulator.takeScrAt(rate); refresh()
                        }
                    }
                    9 -> ActionButton(stringResource(R.string.guide_measure_delta_p), Modifier, TealDark) {
                        simulator.takeChokeLineDeltaP(); refresh()
                    }
                }
            }
        }

        item {
            val phaseBHeader = when (scenario.method) {
                "Tutorial" -> stringResource(R.string.guide_tutorial_header)
                "Volumétrico" -> stringResource(R.string.guide_vol_header)
                "Lubrificar & sangrar" -> stringResource(R.string.guide_lub_header)
                "Emergência" -> stringResource(R.string.guide_eds_header)
                else -> "${stringResource(R.string.guide_phase_b)} · ${scenario.method}"
            }
            GuidePhaseHeader(phaseBHeader, if (scenario.method == "Tutorial" || isSandboxIdle) Teal else Red)
        }

        if (isSandboxIdle) {
            item {
                SectionCard(stringResource(R.string.guide_sandbox_idle_title)) {
                    Text(
                        stringResource(R.string.guide_sandbox_idle_body),
                        color = Muted,
                        fontSize = 10.sp
                    )
                }
            }
        } else if (scenario.method != "Tutorial") {
            item { GuideConditionCard(snapshot, units) }
        }

        items(phaseB.size) { index ->
            val spec = phaseB[index]
            GuideStepCard(spec, onTab) {
                when (spec.action) {
                    GuideActionKind.KILL_SHEET -> ActionButton(stringResource(R.string.kill_sheet), Modifier, Panel3) { onKillSheet() }
                    GuideActionKind.TAKE_SCR -> ActionButton(stringResource(R.string.take_scr), Modifier, TealDark) {
                        simulator.takeScr(); refresh()
                    }
                    GuideActionKind.FIRST_CIRC -> Column {
                        if (snapshot.firstCirculationFraction > 0.0 ||
                            snapshot.phase == Phase.CIRCULATING
                        ) {
                            LinearProgressIndicator(
                                progress = snapshot.firstCirculationFraction.toFloat(),
                                color = Blue,
                                trackColor = Panel3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                stringResource(
                                    R.string.b6_progress,
                                    volumeText(snapshot.firstCirculationPumpedM3, units),
                                    volumeText(snapshot.firstCirculationTotalM3, units)
                                ),
                                color = TextMain,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                stringResource(
                                    R.string.b6_strokes_pumped
                                ) + ": ${Units.fmt(snapshot.firstCirculationStrokes, 0)} / " +
                                    "${Units.fmt(snapshot.firstCirculationTotalStrokes, 0)} · " +
                                    stringResource(R.string.b6_strokes_remaining) +
                                    ": ${Units.fmt(snapshot.firstCirculationRemainingStrokes, 0)}",
                                color = Muted,
                                fontSize = 8.sp
                            )
                        }
                        if (snapshot.firstCirculationReady) {
                            ActionButton(stringResource(R.string.finish_first_circulation), Modifier, TealDark) {
                                simulator.finishFirstCirculation(); refresh()
                            }
                        } else {
                            Text(stringResource(R.string.b6_auto_hint), color = Muted, fontSize = 8.sp)
                        }
                    }
                    GuideActionKind.WEIGHT_UP -> if (snapshot.phase == Phase.KICK_CONTROLLED ||
                        (scenario.method == "Wait & Weight" && (snapshot.shutInStabilized || snapshot.phase == Phase.STABILIZED))
                    ) {
                        ActionButton(stringResource(R.string.weight_up_kill), Modifier, TealDark) {
                            simulator.weightUpToKillMud(); refresh()
                        }
                    }
                    GuideActionKind.SECOND_CIRC -> if (snapshot.phase == Phase.KICK_CONTROLLED) {
                        ActionButton(stringResource(R.string.start_second_circulation), Modifier, TealDark) {
                            simulator.startSecondCirculation(); refresh()
                        }
                    }
                    GuideActionKind.WW_CIRC -> if (snapshot.phase == Phase.STABILIZED ||
                        (snapshot.phase == Phase.SHUT_IN && snapshot.shutInStabilized)
                    ) {
                        ActionButton(stringResource(R.string.start_ww_circulation), Modifier, TealDark) {
                            simulator.startSecondCirculation(); refresh()
                        }
                    }
                    GuideActionKind.BLEED_OR_OPEN -> if (snapshot.phase == Phase.KILLED && !snapshot.lineDisplacementComplete) {
                        Text(stringResource(R.string.guide_b10_hint), color = Muted, fontSize = 8.sp)
                    } else if (snapshot.lineDisplacementComplete && !snapshot.residualBleedDone) {
                        ActionButton(stringResource(R.string.bleed_residual), Modifier, TealDark) {
                            simulator.bleedResidualPressure(); refresh()
                        }
                    } else if (snapshot.residualBleedDone && !snapshot.killSequenceComplete) {
                        ActionButton(stringResource(R.string.open_bop), Modifier, TealDark) {
                            simulator.openBop(); refresh()
                        }
                    }
                    GuideActionKind.BLEED_VOL -> if (snapshot.closedBop) {
                        ActionButton(stringResource(R.string.bleed), Modifier, TealDark) {
                            simulator.bleed(0.2); refresh()
                        }
                    }
                    GuideActionKind.LUBRICATE -> if (snapshot.closedBop) {
                        ActionButton(stringResource(R.string.lubricate), Modifier, TealDark) {
                            simulator.lubricate(0.2); refresh()
                        }
                    }
                    GuideActionKind.NONE -> Unit
                }
            }
        }
        // Rodapé da Guia: salvar resultado e resetar
        item {
            SectionCard(stringResource(R.string.guide_actions)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionButton(stringResource(R.string.guide_save_result), Modifier.weight(1f), TealDark) {
                        onSaveResult()
                    }
                    ActionButton(stringResource(R.string.guide_reset_guide), Modifier.weight(1f), Amber) {
                        onResetGuide()
                    }
                    ActionButton(stringResource(R.string.guide_reset_simulation), Modifier.weight(1f), Red) {
                        onResetSimulation()
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.guide_save_hint),
                    color = Muted,
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
private fun GuidePhaseHeader(title: String, color: Color) {
    Text(
        title.uppercase(),
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 2.dp, top = 4.dp)
    )
}

@Composable
private fun AlarmOption(label: String, selected: Boolean, onClick: () -> Unit) {
    ActionButton(label, Modifier, if (selected) TealDark else Panel3, onClick)
}

@Composable
private fun GuideStepCard(
    spec: GuideSpec,
    onTab: (Int) -> Unit,
    actions: @Composable () -> Unit
) {
    Card(Modifier.fillMaxWidth().background(Panel2)) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(16.dp)
                        .background(if (spec.done) Teal else Panel3, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (spec.done) "✓" else "${spec.number}",
                        color = if (spec.done) Bg else Muted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    "${if (spec.phaseA) "A" else "B"}${spec.number} · ${stringResource(spec.titleRes)}",
                    color = TextMain,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (spec.done) stringResource(R.string.guide_done) else stringResource(R.string.guide_pending),
                    color = if (spec.done) Teal else Muted,
                    fontSize = 8.sp
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                stringResource(R.string.guide_where, stringResource(spec.whereRes)),
                color = Muted,
                fontSize = 9.sp
            )
            Text(
                stringResource(R.string.guide_how, stringResource(spec.howRes)),
                color = Muted,
                fontSize = 9.sp
            )
            Text(
                stringResource(R.string.guide_value, spec.value),
                color = Amber,
                fontSize = 9.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions()
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { onTab(spec.targetTab) },
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(stringResource(R.string.guide_go_tab), color = Teal, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun GuideConditionCard(snapshot: SimSnapshot, units: UnitSystem) {
    SectionCard(stringResource(R.string.guide_kick_condition)) {
        val (label, color) = when (snapshot.kickCondition) {
            KickCondition.NONE -> stringResource(R.string.kick_cond_none) to Muted
            KickCondition.PORE_GT_HYDROSTATIC -> stringResource(R.string.kick_cond_hydro) to Amber
            KickCondition.PORE_GT_ECD -> stringResource(R.string.kick_cond_ecd) to Red
            KickCondition.INTERNAL_HIGH_ZONE -> stringResource(R.string.kick_cond_internal) to Red
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$label · ${stringResource(R.string.guide_pore_eq)} ${densityText(snapshot.poreEquivalentKgM3, units)}",
                color = color,
                fontSize = 10.sp,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            stringResource(R.string.guide_secondary).uppercase(),
            color = Teal,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(3.dp))
        if (snapshot.secondaryIndicators.isEmpty()) {
            Text("—", color = Muted, fontSize = 10.sp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                snapshot.secondaryIndicators.take(3).forEach { indicator ->
                    StatusPill(secondaryLabel(indicator), Amber)
                }
                val extra = snapshot.secondaryIndicators.size - 3
                if (extra > 0) {
                    Text("+$extra", color = Amber, fontSize = 9.sp)
                }
            }
        }
    }
}

private fun secondaryLabel(indicator: SecondaryIndicator): String = when (indicator) {
    SecondaryIndicator.ROP_UP -> "ROP ↑"
    SecondaryIndicator.WOB_DOWN -> "WOB ↓"
    SecondaryIndicator.SPP_DOWN -> "SPP ↓"
    SecondaryIndicator.ECD_UP -> "ECD ↑"
    SecondaryIndicator.PUMP_ACCEL -> "BOMBA ↑"
    SecondaryIndicator.RETURN_FLOW_CHANGE -> "RETORNO ±10%"
}

@Composable
private fun lineKindText(kind: LineKind): String = when (kind) {
    LineKind.KILL_LINE -> stringResource(R.string.line_kill_line)
    LineKind.CHOKE_LINE -> stringResource(R.string.line_choke_line)
    LineKind.BOOSTER_LINE -> stringResource(R.string.line_booster)
    LineKind.RISER_ANNULUS -> stringResource(R.string.line_riser_annulus)
}

@Composable
private fun LineDisplacementCard(
    simulator: Simulator,
    snapshot: SimSnapshot,
    units: UnitSystem,
    refresh: () -> Unit
) {
    val order = listOf(LineKind.KILL_LINE, LineKind.CHOKE_LINE, LineKind.BOOSTER_LINE, LineKind.RISER_ANNULUS)
    SectionCard(stringResource(R.string.line_displacement)) {
        Text(stringResource(R.string.line_displacement_hint), color = Muted, fontSize = 9.sp)
        Spacer(Modifier.height(5.dp))
        order.forEach { kind ->
            val required = kind in snapshot.requiredLines
            val fraction = snapshot.lineProgress[kind] ?: 0.0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    lineKindText(kind),
                    color = if (required) TextMain else Muted,
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (!required) stringResource(R.string.not_required)
                    else when {
                        fraction >= 0.999 -> stringResource(R.string.lines_complete)
                        snapshot.phase != com.mosquitobr.controlepoco.core.Phase.KILLED -> "Aguardando poço morto"
                        else -> "${Units.fmt(fraction * 100.0)}%"
                    },
                    color = if (fraction >= 0.999) Green else if (snapshot.phase != com.mosquitobr.controlepoco.core.Phase.KILLED) Muted else if (required) Amber else Muted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (required) {
                ProgressBar(fraction.toFloat(), if (fraction >= 0.999) Green else Amber, Modifier.fillMaxWidth())
                if (snapshot.phase == com.mosquitobr.controlepoco.core.Phase.KILLED && fraction < 0.999) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ActionButton(stringResource(R.string.displace_line), Modifier.weight(1f)) {
                            simulator.displaceLine(kind, 0.25); refresh()
                        }
                        ActionButton(stringResource(R.string.displace_line_full), Modifier.weight(1f), TealDark) {
                            simulator.displaceLine(kind, 1.0); refresh()
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${stringResource(R.string.residual_pressure)}: ${pressureText(snapshot.residualPressureKPa, units)}",
                color = if (snapshot.residualPressureKPa <= 200.0) Green else Amber,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            ActionButton(stringResource(R.string.bleed_residual), Modifier.weight(1f), TealDark) {
                simulator.bleedResidualPressure(); refresh()
            }
        }
        snapshot.openBopBlockedReason?.let { reason ->
            Text(
                "${stringResource(R.string.open_bop_blocked)}: $reason",
                color = Amber,
                fontSize = 9.sp
            )
        }
        if (snapshot.readyToOpen) {
            Text(stringResource(R.string.ready_to_open), color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun scenarioElements(scenario: Scenario): List<BopElement> {
    val elements = scenario.rig.bop.elements
    return if (elements.isEmpty()) {
        listOf(BopElement.ANNULAR, BopElement.PIPE_RAM, BopElement.BLIND_SHEAR, BopElement.HCR)
    } else {
        elements
    }
}

private fun elementName(element: BopElement): String = when (element) {
    BopElement.ANNULAR -> "Annular"
    BopElement.PIPE_RAM -> "Pipe ram"
    BopElement.BLIND_SHEAR -> "Blind / shear ram"
    BopElement.TEST_RAM -> "Test ram"
    BopElement.HCR -> "HCR"
}

@Composable
private fun MudPanel(
    simulator: Simulator,
    snapshot: SimSnapshot,
    scenario: Scenario,
    units: UnitSystem,
    refresh: () -> Unit,
    onKillSheet: () -> Unit
) {
    fun targetText(kgM3: Double): String =
        mudWeightInput(Units.kgM3ToPpg(kgM3), units)

    var weightText by remember { mutableStateOf(targetText(snapshot.targetMudDensityKgM3)) }
    var editingWeight by remember { mutableStateOf(false) }
    LaunchedEffect(snapshot.targetMudDensityKgM3, units) {
        if (!editingWeight) weightText = targetText(snapshot.targetMudDensityKgM3)
    }
    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Gauge(
                    stringResource(R.string.mud_weight),
                    densityText(snapshot.mudDensityKgM3, units),
                    ((snapshot.mudDensityKgM3 - 900.0) / 600.0).toFloat(),
                    Teal,
                    Modifier.weight(1f).height(105.dp)
                )
                Gauge(
                    stringResource(R.string.ecd),
                    densityText(snapshot.ecdKgM3, units),
                    ((snapshot.ecdKgM3 - 900.0) / 700.0).toFloat(),
                    Purple,
                    Modifier.weight(1f).height(105.dp)
                )
                Gauge(
                    stringResource(R.string.pit),
                    volumeText(snapshot.pitM3, units),
                    (snapshot.pitM3 / 250.0).toFloat(),
                    Blue,
                    Modifier.weight(1f).height(105.dp)
                )
            }
        }
        item {
            SectionCard(stringResource(R.string.mud_weight)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetricCard(
                        stringResource(R.string.mud_in_well),
                        densityText(snapshot.mudDensityKgM3, units, 1),
                        Modifier.weight(1f),
                        Blue
                    )
                    MetricCard(
                        stringResource(R.string.target),
                        densityText(snapshot.targetMudDensityKgM3, units, 1),
                        Modifier.weight(1f),
                        Teal
                    )
                }
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { text ->
                        weightText = text
                        parseMudWeightToPpg(text, units)?.let { ppg ->
                            simulator.weightUp(Units.ppgToKgM3(ppg))
                            refresh()
                        }
                    },
                    label = {
                        Text(
                            "${stringResource(R.string.target)} (${mudWeightUnit(units)})",
                            fontSize = 9.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { editingWeight = it.isFocused }
                )
                Slider(
                    value = if (units == UnitSystem.SI) {
                        snapshot.targetMudDensityKgM3.toFloat()
                    } else {
                        Units.kgM3ToPpg(snapshot.targetMudDensityKgM3).toFloat()
                    },
                    onValueChange = { value ->
                        val kgM3 = if (units == UnitSystem.SI) {
                            value.toDouble()
                        } else {
                            Units.ppgToKgM3(value.toDouble())
                        }
                        simulator.weightUp(kgM3)
                        refresh()
                    },
                    valueRange = if (units == UnitSystem.SI) 900f..2000f else 7.5f..16.5f
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ActionButton(stringResource(R.string.weight_up_kill), Modifier.weight(1f)) {
                        simulator.weightUpToKillMud(); refresh()
                    }
                    ActionButton(stringResource(R.string.kill_sheet), Modifier.weight(1f), TealDark) {
                        onKillSheet()
                    }
                }
                if (snapshot.mudDensityKgM3 < snapshot.targetMudDensityKgM3 - 1.0) {
                    Text(
                        stringResource(
                            R.string.mud_weight_ramping,
                            densityText(snapshot.mudDensityKgM3, units, 1),
                            densityText(snapshot.targetMudDensityKgM3, units, 1)
                        ),
                        color = Amber,
                        fontSize = 8.sp
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MetricCard(stringResource(R.string.trip_tank), volumeText(snapshot.tripTankM3, units), Modifier.weight(1f), Muted)
                MetricCard(stringResource(R.string.flow_in), rateText(snapshot.flowInLpm, units), Modifier.weight(1f), Teal)
                MetricCard(stringResource(R.string.flow_out), rateText(snapshot.flowOutLpm, units), Modifier.weight(1f), Amber)
            }
        }
        item {
            SectionCard(stringResource(R.string.kill_mud)) {
                val ks = snapshot.killSheet
                if (ks == null) {
                    Text(stringResource(R.string.no_kick_sheet), color = Muted, fontSize = 10.sp)
                } else {
                    Text(
                        "${stringResource(R.string.kill_mud_suggestion)}: ${densityText(ks.killMudKgM3, units, 2)}",
                        color = Teal,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (ks.marginKgM3 > 0.5) {
                        Text(
                            "${stringResource(R.string.kmw_no_margin)}: " +
                                "${densityText(ks.killMudNoMarginKgM3, units, 2)} · " +
                                "${stringResource(R.string.kill_margin_applied)}: " +
                                densityText(ks.marginKgM3, units, 2),
                            color = Muted,
                            fontSize = 9.sp
                        )
                    }
                    Text(
                        "ICP ${pressureText(ks.icpKPa, units)} · FCP ${pressureText(ks.fcpKPa, units)} · MAASP ${pressureText(ks.maaspKPa, units)}",
                        color = Muted,
                        fontSize = 9.sp
                    )
                }
            }
        }
        item {
            FirstCirculationCard(snapshot, units)
        }
        item {
            SecondCirculationCard(simulator, snapshot, scenario, units, refresh)
        }
    }
}

@Composable
private fun ProgressRow(label: String, fraction: Double, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextMain, fontSize = 9.sp, modifier = Modifier.weight(1f))
            Text("${Units.fmt(fraction * 100.0)}%", color = color, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
        ProgressBar(fraction.toFloat(), color, Modifier.fillMaxWidth())
    }
}

@Composable
private fun FirstCirculationCard(snapshot: SimSnapshot, units: UnitSystem) {
    SectionCard(stringResource(R.string.first_circulation_b6)) {
        val fraction = snapshot.firstCirculationFraction
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.b6_percent, (fraction * 100.0).toInt()),
                color = Blue,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (snapshot.firstCirculationKickOut) stringResource(R.string.b6_kick_out)
                else stringResource(R.string.b6_kick_present),
                color = if (snapshot.firstCirculationKickOut) Green else Red,
                fontSize = 8.sp
            )
        }
        LinearProgressIndicator(
            progress = fraction.toFloat(),
            color = Blue,
            trackColor = Panel3,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${stringResource(R.string.b6_pumped)}: " +
                volumeText(snapshot.firstCirculationPumpedM3, units) + " · " +
                "${stringResource(R.string.b6_total)}: " +
                volumeText(snapshot.firstCirculationTotalM3, units),
            color = TextMain,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            "${stringResource(R.string.b6_strokes_pumped)}: " +
                Units.fmt(snapshot.firstCirculationStrokes, 0) + " · " +
                "${stringResource(R.string.b6_strokes_total)}: " +
                Units.fmt(snapshot.firstCirculationTotalStrokes, 0) + " · " +
                "${stringResource(R.string.b6_strokes_remaining)}: " +
                Units.fmt(snapshot.firstCirculationRemainingStrokes, 0),
            color = Muted,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
        if (snapshot.phase == Phase.CIRCULATING) {
            Spacer(Modifier.height(3.dp))
            Text(stringResource(R.string.b6_auto_hint), color = Muted, fontSize = 8.sp)
        }
    }
}

@Composable
private fun MudInterfaceBar(label: String, fraction: Double) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = Muted, fontSize = 8.sp)
        Row(Modifier.fillMaxWidth().height(10.dp)) {
            Box(
                Modifier
                    .weight((1.0 - fraction).coerceAtLeast(0.0001).toFloat())
                    .fillMaxSize()
                    .background(Color(0xFF6D7B85))
            )
            Box(
                Modifier
                    .weight(fraction.coerceAtLeast(0.0001).toFloat())
                    .fillMaxSize()
                    .background(Color(0xFFE06C6C))
            )
        }
        Row {
            Text(stringResource(R.string.mud_old), color = Muted, fontSize = 7.sp, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.mud_new) + " ${Units.fmt(fraction * 100.0)}%",
                color = Color(0xFFE06C6C),
                fontSize = 7.sp
            )
        }
    }
}

@Composable
private fun SecondCirculationCard(
    simulator: Simulator,
    snapshot: SimSnapshot,
    scenario: Scenario,
    units: UnitSystem,
    refresh: () -> Unit
) {
    SectionCard(stringResource(R.string.second_circulation)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${stringResource(R.string.kill_stage)}: ${killStageText(snapshot.killStage)}",
                color = phaseColor(snapshot.phase),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (snapshot.killMudKgM3 > 0.0) {
                Text(densityText(snapshot.killMudKgM3, units, 2), color = Teal, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
        when {
            scenario.method == "Wait & Weight" &&
                (snapshot.phase == Phase.STABILIZED || (snapshot.phase == Phase.SHUT_IN && snapshot.shutInStabilized)) -> {
                Text(stringResource(R.string.guide_ww_circulation_how), color = Muted, fontSize = 9.sp)
                Spacer(Modifier.height(5.dp))
                ActionButton(stringResource(R.string.start_ww_circulation), Modifier.fillMaxWidth(), TealDark) {
                    simulator.startSecondCirculation()
                    refresh()
                }
            }
            snapshot.phase == Phase.CIRCULATING -> {
                Text(stringResource(R.string.second_circulation_hint), color = Muted, fontSize = 9.sp)
                Spacer(Modifier.height(4.dp))
                if (snapshot.firstCirculationReady) {
                    Text(
                        "${stringResource(R.string.trapped_pressure)}: ${pressureText(snapshot.trappedPressureKPa, units)}",
                        color = Amber,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(5.dp))
                    ActionButton(stringResource(R.string.finish_first_circulation), Modifier.fillMaxWidth(), TealDark) {
                        simulator.finishFirstCirculation()
                        refresh()
                    }
                } else {
                    Text(
                        stringResource(R.string.finish_first_circulation_hint),
                        color = Muted,
                        fontSize = 9.sp
                    )
                }
            }
            snapshot.phase == Phase.KICK_CONTROLLED -> {
                Text(stringResource(R.string.second_circulation_hint), color = Muted, fontSize = 9.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${stringResource(R.string.trapped_pressure)}: ${pressureText(snapshot.trappedPressureKPa, units)}",
                    color = Amber,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(5.dp))
                ActionButton(stringResource(R.string.start_second_circulation), Modifier.fillMaxWidth(), TealDark) {
                    simulator.startSecondCirculation()
                    refresh()
                }
            }
            snapshot.phase == Phase.DISPLACING_KILL_MUD || snapshot.phase == Phase.KILLED -> {
                ProgressRow(stringResource(R.string.kill_string_progress), snapshot.stringKillFraction, Teal)
                ProgressRow(stringResource(R.string.kill_annulus_progress), snapshot.annulusKillFraction, Purple)
                ProgressRow(stringResource(R.string.kill_choke_line_progress), snapshot.chokeLineKillFraction, Amber)
                Spacer(Modifier.height(3.dp))
                MudInterfaceBar(stringResource(R.string.mud_interface), snapshot.annulusKillFraction)
                Spacer(Modifier.height(5.dp))
                Text(
                    "${stringResource(R.string.effective_annulus_mud)}: " +
                        densityText(snapshot.effectiveMudKgM3, units, 2),
                    color = Teal,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${stringResource(R.string.kill_mud_pumped)}: ${volumeText(snapshot.killMudPumpedM3, units)}",
                        color = Muted,
                        fontSize = 9.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${stringResource(R.string.kill_casing_target)}: ${pressureText(snapshot.killCasingTargetKPa, units)}",
                        color = Amber,
                        fontSize = 9.sp
                    )
                }
                if (snapshot.phase == Phase.KILLED) {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.well_dead), color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            else -> {
                Text(stringResource(R.string.second_circulation_hint), color = Muted, fontSize = 9.sp)
                if (snapshot.firstCirculationFraction > 0.0) {
                    Spacer(Modifier.height(4.dp))
                    ProgressRow(stringResource(R.string.kill_stage_first), snapshot.firstCirculationFraction, Blue)
                }
            }
        }
    }
}

@Composable
private fun TrendsPanel(snapshot: SimSnapshot, units: UnitSystem) {
    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item {
            TrendChart(snapshot.trends, Modifier.fillMaxWidth().height(240.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MetricCard(stringResource(R.string.spp), pressureText(snapshot.sppKPa, units), Modifier.weight(1f), Teal)
                MetricCard(stringResource(R.string.sicp), pressureText(snapshot.sicpDisplayKPa(), units), Modifier.weight(1f), Amber)
                MetricCard(stringResource(R.string.bhp), pressureText(snapshot.bhpKPa, units), Modifier.weight(1f), Purple)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MetricCard(stringResource(R.string.pit), volumeText(snapshot.pitM3, units), Modifier.weight(1f), Blue)
                MetricCard(stringResource(R.string.gas), volumeText(snapshot.influxM3, units), Modifier.weight(1f), Amber)
                MetricCard(stringResource(R.string.choke), "${Units.fmt(snapshot.chokePct)}%", Modifier.weight(1f), Purple)
            }
        }
    }
}

@Composable
private fun LogPanel(
    simulator: Simulator,
    snapshot: SimSnapshot,
    units: UnitSystem,
    refresh: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Text(
                "${stringResource(R.string.alarm)}: ${snapshot.alarms.size}",
                color = Muted,
                fontSize = 10.sp
            )
        }
        if (snapshot.alarms.isEmpty()) {
            item { Text(stringResource(R.string.no_alarms), color = Muted, fontSize = 11.sp) }
        } else {
            items(snapshot.alarms, key = { it.id }) { alarm ->
                AlarmRow(alarm) {
                    simulator.acknowledgeAlarm(alarm.id)
                    refresh()
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.kick_alarm_controls)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.alarm_sound), color = TextMain, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = snapshot.alarmSoundEnabled,
                        onCheckedChange = {
                            simulator.setAlarmSound(it); refresh()
                        }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.gain_log), color = TextMain, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = snapshot.gainLogEnabled,
                        onCheckedChange = {
                            simulator.setGainLog(it); refresh()
                        }
                    )
                }
                Text(
                    if (snapshot.gainLogEnabled) stringResource(R.string.gain_log_active)
                    else stringResource(R.string.gain_log_paused),
                    color = if (snapshot.gainLogEnabled) Green else Amber,
                    fontSize = 9.sp
                )
            }
        }
        if (snapshot.milestones.isNotEmpty()) {
            item {
                SectionCard(stringResource(R.string.milestones)) {
                    snapshot.milestones.takeLast(8).forEach {
                        Text("• $it", color = TextMain, fontSize = 9.sp, modifier = Modifier.padding(vertical = 1.dp))
                    }
                }
            }
        }
        if (snapshot.recommendations.isNotEmpty()) {
            item {
                SectionCard(stringResource(R.string.recommendations)) {
                    snapshot.recommendations.forEach {
                        Text("• $it", color = TextMain, fontSize = 10.sp, modifier = Modifier.padding(vertical = 1.dp))
                    }
                }
            }
        }
        if (snapshot.failures.isNotEmpty()) {
            item {
                SectionCard(stringResource(R.string.warnings)) {
                    snapshot.failures.forEach {
                        Text("• $it", color = Amber, fontSize = 10.sp)
                    }
                }
            }
        }
        item {
            Text(
                "Strokes: ${Units.fmt(snapshot.totalStrokes, 0)} · ${rateText(simulator.pumpRateLpm(), units)}",
                color = Muted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = Panel3,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = TextMain)
    ) {
        Text(label, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BriefingDialog(
    scenario: Scenario,
    lang: String,
    onBegin: () -> Unit,
    onBack: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = Panel,
        titleContentColor = TextMain,
        textContentColor = TextMain,
        title = { Text(scenario.title(lang), fontSize = 18.sp) },
        text = {
            Column(
                Modifier
                    .height(360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                StatusPill(
                    if (scenario.stackType == StackType.SURFACE) {
                        stringResource(R.string.stack_surface)
                    } else stringResource(R.string.stack_subsea),
                    if (scenario.stackType == StackType.SURFACE) Teal else Blue
                )
                Spacer(Modifier.height(8.dp))
                Text(scenario.description(lang), color = TextMain, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.briefing).uppercase(), color = Teal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(scenario.briefing(lang), color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.objectives).uppercase(), color = Teal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                scenario.objectives(lang).forEach {
                    Text("• $it", color = TextMain, fontSize = 11.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row {
                    MetricCard(stringResource(R.string.tvd), "${scenario.well.tvdM.toInt()} m", Modifier.weight(1f), Purple)
                    Spacer(Modifier.width(5.dp))
                    MetricCard(stringResource(R.string.md), "${scenario.well.mdM.toInt()} m", Modifier.weight(1f), Teal)
                    Spacer(Modifier.width(5.dp))
                    MetricCard(stringResource(R.string.mud_weight), Units.fmt(Units.kgM3ToPpg(scenario.mud.densityKgM3), 1) + " ppg", Modifier.weight(1f), Teal)
                    Spacer(Modifier.width(5.dp))
                    MetricCard(stringResource(R.string.method_label), scenario.method, Modifier.weight(1f), Amber)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (scenario.well.isSubsea) "BOP no fundo · lâmina ${scenario.well.riser?.waterDepthM?.toInt()} m"
                    else "BOP de superfície · sapata ${scenario.well.shoeTvdM.toInt()} m",
                    color = Muted,
                    fontSize = 9.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onBegin,
                colors = ButtonDefaults.buttonColors(containerColor = TealDark, contentColor = TextMain)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.begin), fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back), color = Muted) }
        }
    )
}
