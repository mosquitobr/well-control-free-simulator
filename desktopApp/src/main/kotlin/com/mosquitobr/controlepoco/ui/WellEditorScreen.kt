package com.mosquitobr.controlepoco.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mosquitobr.controlepoco.platform.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosquitobr.controlepoco.R
import com.mosquitobr.controlepoco.core.CasingSpec
import com.mosquitobr.controlepoco.core.CustomWellDefaults
import com.mosquitobr.controlepoco.core.CustomWellSpec
import com.mosquitobr.controlepoco.core.DrillStringSpec
import com.mosquitobr.controlepoco.core.InfluxType
import com.mosquitobr.controlepoco.core.KickTriggerMode
import com.mosquitobr.controlepoco.core.SurfaceType
import com.mosquitobr.controlepoco.core.UnitSystem
import com.mosquitobr.controlepoco.core.defaultInfluxRateLpm
import java.util.UUID

@Composable
fun WellEditorScreen(
    initial: CustomWellSpec?,
    units: UnitSystem,
    onSave: (CustomWellSpec, Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "Meu poço") }
    var surface by remember { mutableStateOf(initial?.surfaceType ?: SurfaceType.LAND) }
    var tvd by remember { mutableStateOf(depthInput(initial?.tvdM ?: 3000.0, units)) }
    var md by remember { mutableStateOf(depthInput(initial?.mdM ?: 3000.0, units)) }
    var shoe by remember { mutableStateOf(depthInput(initial?.shoeTvdM ?: 2000.0, units)) }
    var water by remember { mutableStateOf(depthInput(initial?.waterDepthM ?: 0.0, units)) }
    var inclination by remember { mutableStateOf((initial?.inclinationDeg ?: 0.0).toString()) }
    var combined by remember { mutableStateOf(initial?.combinedCasing ?: false) }
    var pore by remember { mutableStateOf(gradientInput(initial?.poreGradientKPaPerM ?: 11.5, units)) }
    var frac by remember { mutableStateOf(gradientInput(initial?.fracGradientKPaPerM ?: 18.0, units)) }
    var pressureMode by remember { mutableStateOf(GradientEntryMode.GRADIENT) }
    var mud by remember { mutableStateOf(mudWeightInput(initial?.mudPpg ?: 9.6, units)) }
    var killMargin by remember {
        mutableStateOf(mudWeightInput(initial?.killMarginPpg ?: 0.1, units))
    }
    var kickEnabled by remember { mutableStateOf(initial?.kickEnabled ?: true) }
    var kickType by remember { mutableStateOf(initial?.kickType ?: InfluxType.GAS) }
    var kickVolume by remember {
        mutableStateOf(volumeInput(initial?.maxKickVolumeM3 ?: initial?.kickVolumeM3 ?: 2.5, units))
    }
    var characteristicRate by remember {
        mutableStateOf(
            rateInput(
                initial?.characteristicRateLpm
                    ?: defaultInfluxRateLpm(initial?.kickType ?: InfluxType.GAS),
                units
            )
        )
    }
    var zoneGradient by remember {
        mutableStateOf(gradientInput(initial?.zonePoreGradientKPaPerM ?: 0.0, units))
    }
    var zoneGradientMode by remember { mutableStateOf(GradientEntryMode.GRADIENT) }
    var zonePermeability by remember {
        mutableStateOf((initial?.zonePermeabilityMd ?: 0.0).toString())
    }
    var kickMode by remember { mutableStateOf(initial?.kickTriggerMode ?: KickTriggerMode.TIME) }
    var triggerAt by remember { mutableStateOf((initial?.triggerAtSec ?: 20.0).toString()) }
    var zoneTop by remember {
        mutableStateOf(initial?.zoneTopMdM?.takeIf { it > 0.0 }?.let { depthInput(it, units) } ?: "")
    }
    var zoneBase by remember {
        mutableStateOf(initial?.zoneBaseMdM?.takeIf { it > 0.0 }?.let { depthInput(it, units) } ?: "")
    }
    var triggerDepth by remember {
        mutableStateOf(initial?.triggerDepthMdM?.takeIf { it > 0.0 }?.let { depthInput(it, units) } ?: "")
    }
    var initialBitDepth by remember {
        mutableStateOf(initial?.initialBitDepthMdM?.let { depthInput(it, units) } ?: "")
    }
    var kickDepth by remember {
        mutableStateOf(initial?.kickDepthMdM?.let { depthInput(it, units) } ?: "")
    }
    var kickIntensity by remember {
        mutableStateOf(initial?.kickIntensityKPa?.let { pressureInput(it, units) } ?: "")
    }
    var initialSpm by remember { mutableStateOf((initial?.initialSpm ?: 55.0).toString()) }
    var openHole by remember { mutableStateOf((initial?.openHoleDiameterIn ?: 8.5).toString()) }
    var killId by remember { mutableStateOf((initial?.killLineIdIn ?: 3.0).toString()) }
    var chokeId by remember { mutableStateOf((initial?.chokeLineIdIn ?: 3.0).toString()) }
    var boosterId by remember { mutableStateOf((initial?.boosterLineIdIn ?: 3.0).toString()) }
    var killLen by remember { mutableStateOf(depthInput(initial?.killLineLengthM ?: 0.0, units)) }
    var chokeLen by remember { mutableStateOf(depthInput(initial?.chokeLineLengthM ?: 0.0, units)) }
    var boosterLen by remember { mutableStateOf(depthInput(initial?.boosterLineLengthM ?: 0.0, units)) }
    var riserId by remember { mutableStateOf((initial?.riserIdIn ?: 19.25).toString()) }
    var riserOd by remember { mutableStateOf((initial?.riserOdIn ?: 21.0).toString()) }
    var tempSurface by remember { mutableStateOf((initial?.tempSurfaceC ?: 24.0).toString()) }
    var tempBottom by remember { mutableStateOf((initial?.tempBottomC ?: 90.0).toString()) }
    var seaSurfaceTemp by remember { mutableStateOf((initial?.seaSurfaceTempC ?: 24.0).toString()) }
    var seaMudlineTemp by remember { mutableStateOf((initial?.seaMudlineTempC ?: 4.0).toString()) }
    var customString by remember { mutableStateOf(initial?.drillStringSpec != null) }
    val ds = initial?.drillStringSpec
    var dpOd by remember { mutableStateOf((ds?.drillPipeOdIn ?: 5.0).toString()) }
    var dpId by remember { mutableStateOf((ds?.drillPipeIdIn ?: 4.276).toString()) }
    var hwdpOd by remember { mutableStateOf((ds?.hwdpOdIn ?: 5.0).toString()) }
    var hwdpId by remember { mutableStateOf((ds?.hwdpIdIn ?: 3.0).toString()) }
    var dcOd by remember { mutableStateOf((ds?.drillCollarOdIn ?: 6.5).toString()) }
    var dcId by remember { mutableStateOf((ds?.drillCollarIdIn ?: 2.25).toString()) }
    var bhaLength by remember { mutableStateOf(depthInput(ds?.bhaLengthM ?: 180.0, units)) }
    var bitTfa by remember { mutableStateOf((ds?.bitTfaIn2 ?: 0.48).toString()) }
    val casingDrafts = remember {
        mutableStateListOf<CasingDraft>().apply {
            addAll(initial?.customCasings?.map { CasingDraft.from(it, units) } ?: emptyList())
        }
    }
    var error by remember { mutableStateOf<String?>(null) }
    val errorText = stringResource(R.string.save_error)
    val startTriggerError = stringResource(R.string.start_before_trigger_error)

    fun number(value: String, fallback: Double) = value.replace(',', '.').toDoubleOrNull() ?: fallback

    fun switchPressureMode(target: GradientEntryMode) {
        if (target == pressureMode) return
        val poreGradient = parseGradientForMode(pore, units, pressureMode) ?: 11.5
        val fracGradient = parseGradientForMode(frac, units, pressureMode) ?: 18.0
        pressureMode = target
        pore = gradientInputForMode(poreGradient, units, target)
        frac = gradientInputForMode(fracGradient, units, target)
    }

    fun switchZoneGradientMode(target: GradientEntryMode) {
        if (target == zoneGradientMode) return
        val current = parseGradientForMode(zoneGradient, units, zoneGradientMode) ?: 0.0
        zoneGradientMode = target
        zoneGradient = gradientInputForMode(current, units, target)
    }

    // Sugestões ativas: usadas pelo core quando o campo está vazio e exibidas
    // como placeholder para o operador saber o que será aplicado.
    val mdParsed = parseDepthInput(md, units) ?: 0.0
    val triggerParsed = parseDepthInput(triggerDepth, units) ?: 0.0
    val zoneTopParsed = parseDepthInput(zoneTop, units) ?: 0.0
    val zoneBaseParsed = parseDepthInput(zoneBase, units) ?: 0.0
    val suggestedTrigger = CustomWellDefaults.triggerDepthMd(triggerParsed, mdParsed)
    val suggestedZoneTop = CustomWellDefaults.zoneTopMd(zoneTopParsed, suggestedTrigger, mdParsed)
    val suggestedZoneBase = CustomWellDefaults.zoneBaseMd(zoneBaseParsed, suggestedZoneTop, mdParsed)
    val suggestedStart = CustomWellDefaults.startDepthMd(
        null, mdParsed, suggestedZoneTop, suggestedTrigger, kickEnabled
    )
    val suggestedKickDepth = CustomWellDefaults.kickDepthMd(
        null, mdParsed, suggestedZoneTop, suggestedZoneBase, suggestedTrigger
    )
    val suggestedIntensity = CustomWellDefaults.intensityKPa(null)
    @Composable
    fun suggestion(value: Double, unit: String, decimals: Int = 1): String =
        stringResource(R.string.suggested_value, "${depthInput(value, units, decimals)} $unit")

    fun applyPreset(preset: String) {
        fun d(m: Double) = depthInput(m, units)
        fun g(kPaPerM: Double) = gradientInput(kPaPerM, units)
        fun w(ppg: Double) = mudWeightInput(ppg, units)
        fun v(m3: Double) = volumeInput(m3, units)
        initialBitDepth = ""
        kickDepth = ""
        kickIntensity = ""
        pressureMode = GradientEntryMode.GRADIENT
        zoneGradientMode = GradientEntryMode.GRADIENT
        zoneGradient = ""
        killMargin = w(0.1)
        when (preset) {
            "vertical" -> {
                name = "Poço vertical"; surface = SurfaceType.LAND
                tvd = d(3000.0); md = d(3000.0); shoe = d(2000.0); water = d(0.0); inclination = "0"
                combined = false; pore = g(11.5); frac = g(18.0); mud = w(9.6)
                kickEnabled = true; kickType = InfluxType.GAS; kickVolume = v(2.5)
                kickMode = KickTriggerMode.TIME; triggerAt = "20"
                zoneTop = d(2820.0); zoneBase = d(3000.0); triggerDepth = d(2820.0); initialSpm = "55"
                openHole = "8.5"; riserId = "19.25"; riserOd = "21.0"
                tempSurface = "24"; tempBottom = "85"; seaSurfaceTemp = "24"; seaMudlineTemp = "4"
            }
            "directional" -> {
                name = "Poço direcional HP"; surface = SurfaceType.LAND
                tvd = d(3200.0); md = d(3500.0); shoe = d(2200.0); water = d(0.0); inclination = "34"
                combined = false; pore = g(11.8); frac = g(18.2); mud = w(10.0)
                kickEnabled = true; kickType = InfluxType.GAS; kickVolume = v(3.2)
                kickMode = KickTriggerMode.DEPTH; triggerAt = "24"
                zoneTop = d(3350.0); zoneBase = d(3460.0); triggerDepth = d(3350.0); initialSpm = "60"
                openHole = "8.5"; tempSurface = "26"; tempBottom = "105"
            }
            "deepwater" -> {
                name = "Poço submarino"; surface = SurfaceType.SUBSEA
                tvd = d(4500.0); md = d(4500.0); shoe = d(3200.0); water = d(1200.0); inclination = "0"
                combined = false; pore = g(12.3); frac = g(18.6); mud = w(10.3)
                kickEnabled = true; kickType = InfluxType.GAS; kickVolume = v(2.5)
                kickMode = KickTriggerMode.TIME; triggerAt = "18"
                zoneTop = d(4300.0); zoneBase = d(4500.0); triggerDepth = d(4300.0); initialSpm = "65"
                openHole = "8.5"; killLen = d(1240.0); chokeLen = d(1240.0); boosterLen = d(1240.0)
                riserId = "19.25"; riserOd = "21.0"
                tempSurface = "24"; tempBottom = "95"; seaSurfaceTemp = "24"; seaMudlineTemp = "4"
            }
            "narrow" -> {
                name = "Janela estreita"; surface = SurfaceType.SUBSEA
                tvd = d(5200.0); md = d(5200.0); shoe = d(4200.0); water = d(1800.0); inclination = "0"
                combined = true; pore = g(12.75); frac = g(14.6); mud = w(10.9)
                kickEnabled = true; kickType = InfluxType.GAS; kickVolume = v(2.2)
                kickMode = KickTriggerMode.TIME; triggerAt = "20"
                zoneTop = d(5000.0); zoneBase = d(5200.0); triggerDepth = d(5000.0); initialSpm = "65"
                openHole = "8.5"; killLen = d(1840.0); chokeLen = d(1840.0); boosterLen = d(1840.0)
                riserId = "19.25"; riserOd = "21.0"
                tempSurface = "22"; tempBottom = "110"; seaSurfaceTemp = "22"; seaMudlineTemp = "3"
            }
        }
    }

    fun buildSpec(): CustomWellSpec? {
        val tvdValue = parseDepthInput(tvd, units) ?: 0.0
        val mdValue = parseDepthInput(md, units) ?: 0.0
        val shoeValue = parseDepthInput(shoe, units) ?: 0.0
        if (name.isBlank() || tvdValue < 300.0 || mdValue < tvdValue || shoeValue >= tvdValue) {
            error = errorText
            return null
        }
        if (surface == SurfaceType.SUBSEA && (parseDepthInput(water, units) ?: 0.0) >= tvdValue) {
            error = errorText
            return null
        }
        val triggerValue = parseDepthInput(triggerDepth, units) ?: 0.0
        val zoneTopValue = parseDepthInput(zoneTop, units) ?: 0.0
        val zoneBaseValue = parseDepthInput(zoneBase, units) ?: 0.0
        val resolvedTrigger = CustomWellDefaults.triggerDepthMd(triggerValue, mdValue)
        val resolvedZoneTop = CustomWellDefaults.zoneTopMd(zoneTopValue, resolvedTrigger, mdValue)
        val resolvedZoneBase = CustomWellDefaults.zoneBaseMd(zoneBaseValue, resolvedZoneTop, mdValue)
        val startValue = if (initialBitDepth.isBlank()) {
            null
        } else {
            parseDepthInput(initialBitDepth, units)
        }
        val resolvedStart = CustomWellDefaults.startDepthMd(
            startValue, mdValue, resolvedZoneTop, resolvedTrigger, kickEnabled
        )
        if (
            kickEnabled &&
            (kickMode == KickTriggerMode.DEPTH || kickMode == KickTriggerMode.HYBRID) &&
            resolvedStart >= resolvedTrigger
        ) {
            error = startTriggerError
            return null
        }
        error = null
        return CustomWellSpec(
            id = initial?.id ?: "custom:${UUID.randomUUID()}",
            name = name.trim(),
            surfaceType = surface,
            tvdM = tvdValue,
            mdM = mdValue,
            shoeTvdM = shoeValue,
            waterDepthM = parseDepthInput(water, units) ?: 0.0,
            inclinationDeg = number(inclination, 0.0),
            combinedCasing = combined,
            poreGradientKPaPerM = parseGradientForMode(pore, units, pressureMode) ?: 11.5,
            fracGradientKPaPerM = parseGradientForMode(frac, units, pressureMode) ?: 18.0,
            mudPpg = parseMudWeightToPpg(mud, units) ?: 9.6,
            killMarginPpg = (parseMudWeightToPpg(killMargin, units) ?: 0.1)
                .coerceIn(0.0, 1.0),
            kickEnabled = kickEnabled,
            kickType = kickType,
            kickVolumeM3 = parseVolumeInput(kickVolume, units) ?: 2.5,
            maxKickVolumeM3 = parseVolumeInput(kickVolume, units) ?: 2.5,
            characteristicRateLpm = parseRateInput(characteristicRate, units)
                ?: defaultInfluxRateLpm(kickType),
            zonePoreGradientKPaPerM =
                parseGradientForMode(zoneGradient, units, zoneGradientMode) ?: 0.0,
            zonePermeabilityMd = number(zonePermeability, 0.0),
            kickTriggerMode = kickMode,
            triggerAtSec = number(triggerAt, 20.0),
            zoneTopMdM = zoneTopValue,
            zoneBaseMdM = zoneBaseValue,
            triggerDepthMdM = triggerValue,
            initialBitDepthMdM = startValue,
            kickDepthMdM = if (kickDepth.isBlank()) null else parseDepthInput(kickDepth, units),
            kickIntensityKPa = if (kickIntensity.isBlank()) null else parsePressureInput(kickIntensity, units),
            initialSpm = number(initialSpm, 55.0),
            openHoleDiameterIn = number(openHole, 8.5),
            killLineIdIn = number(killId, 3.0),
            chokeLineIdIn = number(chokeId, 3.0),
            boosterLineIdIn = number(boosterId, 3.0),
            killLineLengthM = parseDepthInput(killLen, units) ?: 0.0,
            chokeLineLengthM = parseDepthInput(chokeLen, units) ?: 0.0,
            boosterLineLengthM = parseDepthInput(boosterLen, units) ?: 0.0,
            riserIdIn = number(riserId, 19.25),
            riserOdIn = number(riserOd, 21.0),
            tempSurfaceC = number(tempSurface, 24.0),
            tempBottomC = number(tempBottom, 90.0),
            seaSurfaceTempC = number(seaSurfaceTemp, 24.0),
            seaMudlineTempC = number(seaMudlineTemp, 4.0),
            customCasings = casingDrafts.mapNotNull { it.toSpec(units) },
            drillStringSpec = if (customString) {
                DrillStringSpec(
                    drillPipeOdIn = number(dpOd, 5.0),
                    drillPipeIdIn = number(dpId, 4.276),
                    hwdpOdIn = number(hwdpOd, 5.0),
                    hwdpIdIn = number(hwdpId, 3.0),
                    drillCollarOdIn = number(dcOd, 6.5),
                    drillCollarIdIn = number(dcId, 2.25),
                    bhaLengthM = parseDepthInput(bhaLength, units) ?: 180.0,
                    bitTfaIn2 = number(bitTfa, 0.48)
                )
            } else null
        )
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.well_editor), onCancel)
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SectionCard(stringResource(R.string.preset)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = { applyPreset("vertical") },
                            label = { Text(stringResource(R.string.preset_vertical), fontSize = 9.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { applyPreset("directional") },
                            label = { Text(stringResource(R.string.preset_directional), fontSize = 9.sp) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = { applyPreset("deepwater") },
                            label = { Text(stringResource(R.string.preset_deepwater), fontSize = 9.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { applyPreset("narrow") },
                            label = { Text(stringResource(R.string.preset_narrow), fontSize = 9.sp) }
                        )
                    }
                }
            }
            item {
                SectionCard(stringResource(R.string.well_geometry)) {
                    Field(stringResource(R.string.well_name), name, { name = it })
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.surface_type), color = Muted, fontSize = 9.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        FilterChip(
                            selected = surface == SurfaceType.LAND,
                            onClick = { surface = SurfaceType.LAND },
                            label = { Text(stringResource(R.string.land), fontSize = 9.sp) }
                        )
                        FilterChip(
                            selected = surface == SurfaceType.SUBSEA,
                            onClick = { surface = SurfaceType.SUBSEA },
                            label = { Text(stringResource(R.string.subsea), fontSize = 9.sp) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Field(stringResource(R.string.tvd) + " (${depthUnit(units)})", tvd, { tvd = it }, Modifier.weight(1f))
                        Field(stringResource(R.string.md) + " (${depthUnit(units)})", md, { md = it }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Field(stringResource(R.string.shoe_depth) + " (${depthUnit(units)})", shoe, { shoe = it }, Modifier.weight(1f))
                        Field(stringResource(R.string.inclination) + " (°)", inclination, { inclination = it }, Modifier.weight(1f))
                    }
                    if (surface == SurfaceType.SUBSEA) {
                        Field(stringResource(R.string.water_depth) + " (${depthUnit(units)})", water, { water = it })
                    }
                    Field(stringResource(R.string.open_hole_diameter) + " (pol)", openHole, { openHole = it })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (combined) stringResource(R.string.combined_casing) else stringResource(R.string.simple_casing),
                            color = TextMain,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = combined, onCheckedChange = { combined = it })
                    }
                }
            }
            item {
                SectionCard(stringResource(R.string.casings_section)) {
                    Text(
                        stringResource(R.string.casings_note),
                        color = Muted,
                        fontSize = 8.sp
                    )
                    casingDrafts.forEachIndexed { index, casing ->
                        Spacer(Modifier.height(5.dp))
                        Field(
                            stringResource(R.string.casing_name),
                            casing.name,
                            { casingDrafts[index] = casingDrafts[index].copy(name = it) }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Field(
                                stringResource(R.string.casing_top) + " (${depthUnit(units)})",
                                casing.top,
                                { casingDrafts[index] = casingDrafts[index].copy(top = it) },
                                Modifier.weight(1f)
                            )
                            Field(
                                stringResource(R.string.casing_bottom) + " (${depthUnit(units)})",
                                casing.bottom,
                                { casingDrafts[index] = casingDrafts[index].copy(bottom = it) },
                                Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Field(
                                stringResource(R.string.casing_id),
                                casing.id,
                                { casingDrafts[index] = casingDrafts[index].copy(id = it) },
                                Modifier.weight(1f)
                            )
                            Field(
                                stringResource(R.string.casing_od),
                                casing.od,
                                { casingDrafts[index] = casingDrafts[index].copy(od = it) },
                                Modifier.weight(1f)
                            )
                            Field(
                                stringResource(R.string.casing_hole),
                                casing.hole,
                                { casingDrafts[index] = casingDrafts[index].copy(hole = it) },
                                Modifier.weight(1f)
                            )
                        }
                        TextButton(
                            onClick = { casingDrafts.removeAt(index) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(stringResource(R.string.delete), color = Red, fontSize = 9.sp)
                        }
                    }
                    TextButton(
                        onClick = {
                            casingDrafts.add(
                                CasingDraft(
                                    name = "Revestimento ${casingDrafts.size + 1}",
                                    top = "0",
                                    bottom = shoe,
                                    id = "8.681",
                                    od = "9.625",
                                    hole = openHole
                                )
                            )
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(stringResource(R.string.add_casing), color = Teal, fontSize = 9.sp)
                    }
                }
            }
            item {
                SectionCard(stringResource(R.string.lines_riser)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Field(stringResource(R.string.kill_line_id), killId, { killId = it }, Modifier.weight(1f))
                        Field(stringResource(R.string.choke_line_id), chokeId, { chokeId = it }, Modifier.weight(1f))
                        Field(stringResource(R.string.booster_line_id), boosterId, { boosterId = it }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Field(stringResource(R.string.kill_line_length) + " (${depthUnit(units)})", killLen, { killLen = it }, Modifier.weight(1f))
                        Field(stringResource(R.string.choke_line_length) + " (${depthUnit(units)})", chokeLen, { chokeLen = it }, Modifier.weight(1f))
                        Field(stringResource(R.string.booster_line_length) + " (${depthUnit(units)})", boosterLen, { boosterLen = it }, Modifier.weight(1f))
                    }
                    if (surface == SurfaceType.SUBSEA) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Field(stringResource(R.string.riser_id), riserId, { riserId = it }, Modifier.weight(1f))
                            Field(stringResource(R.string.riser_od), riserOd, { riserOd = it }, Modifier.weight(1f))
                        }
                    }
                }
            }
            item {
                SectionCard(stringResource(R.string.temperature_section)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Field(stringResource(R.string.temp_surface), tempSurface, { tempSurface = it }, Modifier.weight(1f))
                        Field(stringResource(R.string.temp_bottom), tempBottom, { tempBottom = it }, Modifier.weight(1f))
                    }
                    if (surface == SurfaceType.SUBSEA) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Field(stringResource(R.string.sea_surface_temp), seaSurfaceTemp, { seaSurfaceTemp = it }, Modifier.weight(1f))
                            Field(stringResource(R.string.sea_mudline_temp), seaMudlineTemp, { seaMudlineTemp = it }, Modifier.weight(1f))
                        }
                    }
                }
            }
            item {
                SectionCard(stringResource(R.string.drill_string_section)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.custom_drill_string),
                            color = TextMain,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = customString, onCheckedChange = { customString = it })
                    }
                    if (customString) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Field(stringResource(R.string.dp_od), dpOd, { dpOd = it }, Modifier.weight(1f))
                            Field(stringResource(R.string.dp_id), dpId, { dpId = it }, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Field(stringResource(R.string.hwdp_od), hwdpOd, { hwdpOd = it }, Modifier.weight(1f))
                            Field(stringResource(R.string.hwdp_id), hwdpId, { hwdpId = it }, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Field(stringResource(R.string.dc_od), dcOd, { dcOd = it }, Modifier.weight(1f))
                            Field(stringResource(R.string.dc_id), dcId, { dcId = it }, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Field(stringResource(R.string.bha_length) + " (${depthUnit(units)})", bhaLength, { bhaLength = it }, Modifier.weight(1f))
                            Field(stringResource(R.string.bit_tfa), bitTfa, { bitTfa = it }, Modifier.weight(1f))
                        }
                    }
                }
            }
            item {
                SectionCard(stringResource(R.string.mud_system)) {
                    Text(stringResource(R.string.input_by), color = Muted, fontSize = 9.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = pressureMode == GradientEntryMode.GRADIENT,
                            onClick = { switchPressureMode(GradientEntryMode.GRADIENT) },
                            label = { Text(stringResource(R.string.gradient_mode), fontSize = 8.sp) }
                        )
                        FilterChip(
                            selected = pressureMode == GradientEntryMode.DENSITY,
                            onClick = { switchPressureMode(GradientEntryMode.DENSITY) },
                            label = { Text(stringResource(R.string.density_mode), fontSize = 8.sp) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Field(
                            stringResource(R.string.pore_gradient) +
                                " (${gradientEntryUnit(units, pressureMode)})",
                            pore,
                            { pore = it },
                            Modifier.weight(1f)
                        )
                        Field(
                            stringResource(R.string.frac_gradient) +
                                " (${gradientEntryUnit(units, pressureMode)})",
                            frac,
                            { frac = it },
                            Modifier.weight(1f)
                        )
                    }
                    if (pressureMode == GradientEntryMode.DENSITY) {
                        Text(
                            gradientDensityFormula(units),
                            color = Muted,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Field(stringResource(R.string.mud_weight) + " (${mudWeightUnit(units)})", mud, { mud = it }, Modifier.weight(1f))
                        Field(stringResource(R.string.spm), initialSpm, { initialSpm = it }, Modifier.weight(1f))
                    }
                    Text(stringResource(R.string.spm_hint), color = Muted, fontSize = 8.sp)
                    Field(
                        stringResource(R.string.kill_margin) + " (${mudWeightUnit(units)})",
                        killMargin,
                        { killMargin = it },
                        Modifier.fillMaxWidth()
                    )
                    Text(
                        stringResource(R.string.kill_margin_hint),
                        color = Muted,
                        fontSize = 8.sp
                    )
                }
            }
            item {
                SectionCard(stringResource(R.string.kick_config)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.kick_max_volume),
                            color = TextMain,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = kickEnabled, onCheckedChange = { kickEnabled = it })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Field(
                            stringResource(R.string.kick_max_volume) + " (${volumeUnit(units)})",
                            kickVolume,
                            { kickVolume = it },
                            Modifier.weight(1f)
                        )
                        Field(
                            stringResource(R.string.kick_characteristic_rate) + " (${rateUnit(units)})",
                            characteristicRate,
                            { characteristicRate = it },
                            Modifier.weight(1f)
                        )
                    }
                    Text(stringResource(R.string.kick_rate_hint), color = Muted, fontSize = 8.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Field(
                            stringResource(R.string.zone_top) + " (${depthUnit(units)})",
                            zoneTop,
                            { zoneTop = it },
                            Modifier.weight(1f),
                            placeholder = suggestion(suggestedZoneTop, depthUnit(units))
                        )
                        Field(
                            stringResource(R.string.zone_base) + " (${depthUnit(units)})",
                            zoneBase,
                            { zoneBase = it },
                            Modifier.weight(1f),
                            placeholder = suggestion(suggestedZoneBase, depthUnit(units))
                        )
                    }
                    Text(stringResource(R.string.input_by), color = Muted, fontSize = 9.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = zoneGradientMode == GradientEntryMode.GRADIENT,
                            onClick = { switchZoneGradientMode(GradientEntryMode.GRADIENT) },
                            label = { Text(stringResource(R.string.gradient_mode), fontSize = 8.sp) }
                        )
                        FilterChip(
                            selected = zoneGradientMode == GradientEntryMode.DENSITY,
                            onClick = { switchZoneGradientMode(GradientEntryMode.DENSITY) },
                            label = { Text(stringResource(R.string.density_mode), fontSize = 8.sp) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Field(
                            stringResource(R.string.kick_zone_pore_gradient) +
                                " (${gradientEntryUnit(units, zoneGradientMode)})",
                            zoneGradient,
                            { zoneGradient = it },
                            Modifier.weight(1f)
                        )
                        Field(
                            stringResource(R.string.kick_zone_permeability),
                            zonePermeability,
                            { zonePermeability = it },
                            Modifier.weight(1f)
                        )
                    }
                    if (zoneGradientMode == GradientEntryMode.DENSITY) {
                        Text(
                            gradientDensityFormula(units),
                            color = Muted,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(stringResource(R.string.permeability_hint), color = Muted, fontSize = 8.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = zonePermeability == "100",
                            onClick = { zonePermeability = "100" },
                            label = { Text(stringResource(R.string.permeability_sandstone), fontSize = 8.sp) }
                        )
                        FilterChip(
                            selected = zonePermeability == "10",
                            onClick = { zonePermeability = "10" },
                            label = { Text(stringResource(R.string.permeability_carbonate), fontSize = 8.sp) }
                        )
                    }
                    Text(stringResource(R.string.kick_zone_note), color = Muted, fontSize = 8.sp)
                    Field(
                        stringResource(R.string.trigger_depth_md) + " (${depthUnit(units)})",
                        triggerDepth,
                        { triggerDepth = it },
                        placeholder = suggestion(suggestedTrigger, depthUnit(units))
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Field(
                            stringResource(R.string.initial_bit_depth) + " (${depthUnit(units)})",
                            initialBitDepth,
                            { initialBitDepth = it },
                            Modifier.weight(1f),
                            placeholder = suggestion(suggestedStart, depthUnit(units))
                        )
                        Field(
                            stringResource(R.string.kick_depth) + " (${depthUnit(units)})",
                            kickDepth,
                            { kickDepth = it },
                            Modifier.weight(1f),
                            placeholder = suggestion(suggestedKickDepth, depthUnit(units))
                        )
                    }
                    Field(
                        stringResource(R.string.kick_intensity) + " (${pressureUnit(units)})",
                        kickIntensity,
                        { kickIntensity = it },
                        placeholder = stringResource(
                            R.string.suggested_value,
                            "${pressureInput(suggestedIntensity, units)} ${pressureUnit(units)}"
                        )
                    )
                    Text(stringResource(R.string.kick_intensity_hint), color = Muted, fontSize = 8.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Field(stringResource(R.string.trigger_seconds), triggerAt, { triggerAt = it }, Modifier.weight(1f))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.kick_type), color = Muted, fontSize = 9.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                InfluxType.entries.take(2).forEach { type ->
                                    FilterChip(
                                        selected = kickType == type,
                                        onClick = {
                                            kickType = type
                                            characteristicRate = rateInput(defaultInfluxRateLpm(type), units)
                                        },
                                        label = { Text(influxLabel(type), fontSize = 8.sp) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                InfluxType.entries.drop(2).forEach { type ->
                                    FilterChip(
                                        selected = kickType == type,
                                        onClick = {
                                            kickType = type
                                            characteristicRate = rateInput(defaultInfluxRateLpm(type), units)
                                        },
                                        label = { Text(influxLabel(type), fontSize = 8.sp) }
                                    )
                                }
                            }
                        }
                    }
                    Text(stringResource(R.string.kick_trigger_mode), color = Muted, fontSize = 9.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            KickTriggerMode.TIME,
                            KickTriggerMode.DEPTH,
                            KickTriggerMode.HYBRID,
                            KickTriggerMode.MANUAL
                        ).forEach { mode ->
                            FilterChip(
                                selected = kickMode == mode,
                                onClick = { kickMode = mode },
                                label = { Text(triggerText(mode), fontSize = 8.sp) }
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.carrier_zone_note),
                        color = Muted,
                        fontSize = 8.sp
                    )
                }
            }
            if (error != null) {
                item {
                    Text(error ?: "", color = Red, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            val spec = buildSpec()
                            if (spec != null) onSave(spec, false)
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Panel3, contentColor = TextMain)
                    ) {
                        Text(stringResource(R.string.save_well), fontSize = 10.sp, maxLines = 1)
                    }
                    Button(
                        onClick = {
                            val spec = buildSpec()
                            if (spec != null) onSave(spec, true)
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealDark, contentColor = TextMain)
                    ) {
                        Text(stringResource(R.string.save_and_start), fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
            item {
                Text(stringResource(R.string.settings_note), color = Muted, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 9.sp) },
        placeholder = placeholder?.let { hint -> { Text(hint, fontSize = 10.sp, color = Muted) } },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun influxLabel(type: InfluxType): String = when (type) {
    InfluxType.GAS -> stringResource(R.string.influx_gas)
    InfluxType.OIL -> stringResource(R.string.influx_oil)
    InfluxType.WATER -> stringResource(R.string.influx_water)
    InfluxType.SALTWATER -> stringResource(R.string.influx_saltwater)
}

@Composable
private fun triggerText(mode: KickTriggerMode): String = when (mode) {
    KickTriggerMode.IMMEDIATE -> stringResource(R.string.trigger_immediate)
    KickTriggerMode.TIME -> stringResource(R.string.trigger_time)
    KickTriggerMode.DEPTH -> stringResource(R.string.trigger_depth)
    KickTriggerMode.HYBRID -> stringResource(R.string.trigger_hybrid)
    KickTriggerMode.MANUAL -> stringResource(R.string.trigger_manual)
}

private data class CasingDraft(
    val name: String,
    val top: String,
    val bottom: String,
    val id: String,
    val od: String,
    val hole: String
) {
    fun toSpec(units: UnitSystem): CasingSpec? {
        val topValue = parseDepthInput(top, units) ?: return null
        val bottomValue = parseDepthInput(bottom, units) ?: return null
        val idValue = id.replace(',', '.').toDoubleOrNull() ?: return null
        val odValue = od.replace(',', '.').toDoubleOrNull() ?: idValue + 0.5
        val holeValue = hole.replace(',', '.').toDoubleOrNull() ?: odValue
        if (bottomValue <= topValue || idValue <= 0.0) return null
        return CasingSpec(
            name = name.ifBlank { "Revestimento" },
            topMdM = topValue,
            bottomMdM = bottomValue,
            idIn = idValue,
            odIn = odValue,
            holeDiameterIn = holeValue,
            // O core recalcula a sapata pela trajetória; este valor é apenas
            // um fallback para consumidores que não têm a trajetória à mão.
            shoeTvdM = bottomValue
        )
    }

    companion object {
        fun from(spec: CasingSpec, units: UnitSystem): CasingDraft = CasingDraft(
            name = spec.name,
            top = depthInput(spec.topMdM, units),
            bottom = depthInput(spec.bottomMdM, units),
            id = spec.idIn.toString(),
            od = spec.odIn.toString(),
            hole = spec.holeDiameterIn.toString()
        )
    }
}
