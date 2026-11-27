package com.mosquitobr.controlepoco.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosquitobr.controlepoco.R
import com.mosquitobr.controlepoco.audio.RigAudioEngine
import com.mosquitobr.controlepoco.core.Scenario
import com.mosquitobr.controlepoco.core.Scenarios
import com.mosquitobr.controlepoco.core.StackType
import com.mosquitobr.controlepoco.BuildConfig
import com.mosquitobr.controlepoco.core.UnitSystem
import com.mosquitobr.controlepoco.core.Units
import com.mosquitobr.controlepoco.core.CustomWellSpec
import com.mosquitobr.controlepoco.data.Academy
import com.mosquitobr.controlepoco.data.AppSettings
import com.mosquitobr.controlepoco.data.CustomWellStore
import com.mosquitobr.controlepoco.data.SettingsStore
import com.mosquitobr.controlepoco.platform.AppPaths
import com.mosquitobr.controlepoco.platform.LocalAppStringsLanguage
import com.mosquitobr.controlepoco.platform.stringResource

val LocalAppLanguage = staticCompositionLocalOf { "pt" }

private sealed interface AppScreen {
    data object Home : AppScreen
    data object Settings : AppScreen
    data object About : AppScreen
    data object Academy : AppScreen
    data object SavedResults : AppScreen
    data class Sim(val scenarioId: String) : AppScreen
    data class WellEditor(val customId: String? = null) : AppScreen
}

@Composable
fun AppRoot() {
    var settings by remember { mutableStateOf(SettingsStore.load()) }
    var customWells by remember { mutableStateOf(CustomWellStore.load()) }
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    val lang = settings.language

    CompositionLocalProvider(
        LocalAppLanguage provides lang,
        LocalAppStringsLanguage provides lang
    ) {
        SurfaceBox {
            when (val current = screen) {
                AppScreen.Home -> HomeScreen(
                    settings = settings,
                    customWells = customWells,
                    onStart = { screen = AppScreen.Sim(it.id) },
                    onStartCustom = { screen = AppScreen.Sim(it.id) },
                    onNewWell = { screen = AppScreen.WellEditor() },
                    onEditWell = { screen = AppScreen.WellEditor(it.id) },
                    onDeleteWell = { well ->
                        customWells = CustomWellStore.delete(well.id)
                    },
                    onSettings = { screen = AppScreen.Settings },
                    onAbout = { screen = AppScreen.About },
                    onAcademy = { screen = AppScreen.Academy },
                    onResults = { screen = AppScreen.SavedResults }
                )

                AppScreen.Settings -> SettingsScreen(
                    settings = settings,
                    onChange = {
                        settings = it
                        SettingsStore.save(it)
                    },
                    onBack = { screen = AppScreen.Home },
                    onAbout = { screen = AppScreen.About }
                )

                AppScreen.About -> AboutScreen(onBack = { screen = AppScreen.Settings })

                AppScreen.Academy -> AcademyScreen(onBack = { screen = AppScreen.Home })

                AppScreen.SavedResults -> SavedResultsScreen(
                    units = settings.units,
                    onClose = { screen = AppScreen.Home }
                )

                is AppScreen.Sim -> {
                    val scenario = if (current.scenarioId.startsWith("custom:")) {
                        customWells.firstOrNull { it.id == current.scenarioId }
                            ?.let { Scenarios.custom(it) }
                            ?: Scenarios.byId("sandbox")
                    } else {
                        Scenarios.byId(current.scenarioId)
                    }
                    SimulatorScreen(
                        scenario = scenario,
                        settings = settings,
                        onBack = { screen = AppScreen.Home },
                        onHome = { screen = AppScreen.Home },
                        onOpenResults = { screen = AppScreen.SavedResults }
                    )
                }

                is AppScreen.WellEditor -> {
                    val initial = current.customId?.let { id ->
                        customWells.firstOrNull { it.id == id }
                    }
                    WellEditorScreen(
                        initial = initial,
                        units = settings.units,
                        onCancel = { screen = AppScreen.Home },
                        onSave = { spec, start ->
                            customWells = CustomWellStore.upsert(spec)
                            screen = if (start) AppScreen.Sim(spec.id) else AppScreen.Home
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SurfaceBox(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Bg)
            // Edge-to-edge: recolhe o conteúdo para fora da status bar e da
            // barra de navegação/gestos, mantendo os botões inferiores visíveis.
            .safeDrawingPadding()
    ) {
        content()
    }
}

@Composable
fun HomeScreen(
    settings: AppSettings,
    customWells: List<CustomWellSpec>,
    onStart: (Scenario) -> Unit,
    onStartCustom: (CustomWellSpec) -> Unit,
    onNewWell: () -> Unit,
    onEditWell: (CustomWellSpec) -> Unit,
    onDeleteWell: (CustomWellSpec) -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onAcademy: () -> Unit,
    onResults: () -> Unit
) {
    var filter by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<CustomWellSpec?>(null) }
    val lang = LocalAppLanguage.current
    val scenarios = Scenarios.all.filter {
        filter == null || (filter == "surface" && it.stackType == StackType.SURFACE) ||
            (filter == "subsea" && it.stackType == StackType.SUBSEA)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = 18.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextMain
                )
                Text(
                    stringResource(R.string.home_subtitle),
                    color = Teal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    stringResource(R.string.home_tagline),
                    color = Muted,
                    fontSize = 10.sp
                )
            }
            IconButton(onClick = onAcademy) {
                Text("A", color = Amber, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            IconButton(onClick = onAbout) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Muted)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Muted)
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter == null,
                onClick = { filter = null },
                label = { Text(stringResource(R.string.filter_all), fontSize = 11.sp) }
            )
            FilterChip(
                selected = filter == "surface",
                onClick = { filter = "surface" },
                label = { Text(stringResource(R.string.filter_surface), fontSize = 11.sp) }
            )
            FilterChip(
                selected = filter == "subsea",
                onClick = { filter = "subsea" },
                label = { Text(stringResource(R.string.filter_subsea), fontSize = 11.sp) }
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onResults) {
                Text(
                    stringResource(R.string.result_saved_list),
                    color = Teal,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = onNewWell) {
                Text(stringResource(R.string.new_well), color = Teal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (customWells.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.my_wells).uppercase(),
                        color = Teal,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(customWells, key = { it.id }) { well ->
                    CustomWellCard(
                        well = well,
                        onStart = { onStartCustom(well) },
                        onEdit = { onEditWell(well) },
                        onDelete = { pendingDelete = well }
                    )
                }
                item {
                    Text(
                        stringResource(R.string.custom_well).uppercase(),
                        color = Muted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            items(scenarios, key = { it.id }) { scenario ->
                ScenarioCard(scenario, lang, onStart)
            }
            item {
                Text(
                    stringResource(R.string.disclaimer),
                    color = Muted,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(vertical = 14.dp)
                )
            }
        }
    }

    pendingDelete?.let { well ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Panel,
            titleContentColor = TextMain,
            textContentColor = TextMain,
            title = { Text(stringResource(R.string.confirm_delete), fontSize = 15.sp) },
            text = { Text(well.name, color = Muted, fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteWell(well)
                    pendingDelete = null
                }) { Text(stringResource(R.string.delete), color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel), color = Muted)
                }
            }
        )
    }
}

@Composable
private fun CustomWellCard(
    well: CustomWellSpec,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStart() },
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Panel3)
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    well.name,
                    color = TextMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                StatusPill(
                    if (well.surfaceType == com.mosquitobr.controlepoco.core.SurfaceType.SUBSEA) {
                        stringResource(R.string.subsea)
                    } else stringResource(R.string.surface),
                    if (well.surfaceType == com.mosquitobr.controlepoco.core.SurfaceType.SUBSEA) Blue else Teal
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${stringResource(R.string.tvd)} ${well.tvdM.toInt()} m · " +
                    "${stringResource(R.string.md)} ${well.mdM.toInt()} m · " +
                    "${Units.fmt(well.mudPpg, 1)} ppg",
                color = Muted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "${stringResource(R.string.trigger_mode)}: ${well.kickTriggerMode.name} · " +
                    "${stringResource(R.string.kick_volume)} ${Units.fmt(well.kickVolumeM3, 1)} m³",
                color = Amber,
                fontSize = 9.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onStart, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text(stringResource(R.string.start), color = Teal, fontSize = 10.sp)
                }
                TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text(stringResource(R.string.edit), color = Blue, fontSize = 10.sp)
                }
                TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text(stringResource(R.string.delete), color = Red, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun ScenarioCard(scenario: Scenario, lang: String, onStart: (Scenario) -> Unit) {
    val stackColor = if (scenario.stackType == StackType.SURFACE) Teal else Blue
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStart(scenario) },
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Panel3)
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(
                    if (scenario.stackType == StackType.SURFACE) {
                        stringResource(R.string.stack_surface)
                    } else stringResource(R.string.stack_subsea),
                    stackColor
                )
                Spacer(Modifier.width(6.dp))
                StatusPill(scenario.method, Amber)
                Spacer(Modifier.weight(1f))
                Text(
                    "★".repeat(scenario.difficulty) + "☆".repeat((5 - scenario.difficulty).coerceAtLeast(0)),
                    color = Amber,
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                scenario.title(lang),
                color = TextMain,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                scenario.subtitle(lang),
                color = Muted,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${scenario.well.tvdM.toInt()} m TVD · ${scenario.well.name}",
                    color = Muted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onStart(scenario) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealDark, contentColor = TextMain)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.start), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onBack: () -> Unit,
    onAbout: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.settings), onBack)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard(stringResource(R.string.unit_system)) {
                    UnitRow(
                        stringResource(R.string.units_petrobras),
                        settings.units == UnitSystem.PETROBRAS
                    ) { onChange(settings.copy(units = UnitSystem.PETROBRAS)) }
                    UnitRow(
                        stringResource(R.string.units_us_field),
                        settings.units == UnitSystem.US_FIELD
                    ) { onChange(settings.copy(units = UnitSystem.US_FIELD)) }
                    UnitRow(
                        stringResource(R.string.units_si),
                        settings.units == UnitSystem.SI
                    ) { onChange(settings.copy(units = UnitSystem.SI)) }
                }
            }
            item {
                SectionCard(stringResource(R.string.language_label)) {
                    UnitRow(
                        stringResource(R.string.portuguese),
                        settings.language == "pt"
                    ) { onChange(settings.copy(language = "pt")) }
                    UnitRow(
                        stringResource(R.string.english),
                        settings.language == "en"
                    ) { onChange(settings.copy(language = "en")) }
                }
            }
            item {
                SectionCard(stringResource(R.string.settings)) {
                    ToggleRow(stringResource(R.string.sound), settings.sound) { onChange(settings.copy(sound = it)) }
                    Text(
                        "${stringResource(R.string.sound_volume)}: ${(settings.volume * 100).toInt()}%",
                        color = TextMain,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Slider(
                        value = settings.volume,
                        onValueChange = { onChange(settings.copy(volume = it)) },
                        valueRange = 0f..1f
                    )
                    ToggleRow(stringResource(R.string.sound_pump), settings.soundPump) {
                        onChange(settings.copy(soundPump = it))
                    }
                    ToggleRow(stringResource(R.string.sound_rotary), settings.soundRotary) {
                        onChange(settings.copy(soundRotary = it))
                    }
                    ToggleRow(stringResource(R.string.sound_weight), settings.soundWeight) {
                        onChange(settings.copy(soundWeight = it))
                    }
                    ToggleRow(stringResource(R.string.sound_gas), settings.soundGas) {
                        onChange(settings.copy(soundGas = it))
                    }
                    TextButton(
                        onClick = {
                            RigAudioEngine.playTestTone(settings.volume)
                        }
                    ) {
                        Text(stringResource(R.string.sound_test), color = Teal, fontSize = 11.sp)
                    }
                    ToggleRow(stringResource(R.string.haptics), settings.haptics) { onChange(settings.copy(haptics = it)) }
                    Text(
                        stringResource(R.string.settings_note),
                        color = Muted,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            item {
                TextButton(onClick = onAbout) {
                    Text(stringResource(R.string.about), color = Teal)
                }
            }
        }
    }
}

@Composable
private fun UnitRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, color = TextMain, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextMain, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)? = null, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Panel)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextMain)
            }
        }
        Text(
            title,
            color = TextMain,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.about), onBack)
        LazyColumn(
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Teal
                )
                Text(
                    stringResource(R.string.about_text),
                    color = TextMain,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item {
                SectionCard(stringResource(R.string.references)) {
                    Text(
                        stringResource(R.string.references_text),
                        color = TextMain,
                        fontSize = 11.sp
                    )
                    Text(
                        stringResource(R.string.references_note),
                        color = Muted,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            item {
                SectionCard(stringResource(R.string.version)) {
                    Text(
                        "${BuildConfig.VERSION_NAME} (versionCode ${BuildConfig.VERSION_CODE})",
                        color = TextMain,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(AppPaths.platformLabel, color = Muted, fontSize = 10.sp)
                    Text(
                        "Dados locais: ${AppPaths.dataDir().absolutePath}",
                        color = Muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        stringResource(R.string.license_note),
                        color = Muted,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.about_disclaimer),
                    color = Amber,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun AcademyScreen(onBack: () -> Unit) {
    val lang = LocalAppLanguage.current
    var expanded by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.academy), onBack)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(stringResource(R.string.academy_title), color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.academy_subtitle), color = Muted, fontSize = 11.sp)
            }
            items(Academy.lessons, key = { it.titlePt }) { lesson ->
                val title = if (lang == "en") lesson.titleEn else lesson.titlePt
                val body = if (lang == "en") lesson.bodyEn else lesson.bodyPt
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Panel, RoundedCornerShape(12.dp))
                        .clickable { expanded = if (expanded == lesson.titlePt) null else lesson.titlePt }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            color = TextMain,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(if (expanded == lesson.titlePt) "−" else "+", color = Teal, fontSize = 18.sp)
                    }
                    if (expanded == lesson.titlePt) {
                        Spacer(Modifier.height(6.dp))
                        Text(body, color = Muted, fontSize = 11.sp)
                    }
                }
            }
            item {
                SectionCard(stringResource(R.string.references)) {
                    Text(stringResource(R.string.references_text), color = Muted, fontSize = 10.sp)
                }
            }
        }
    }
}
