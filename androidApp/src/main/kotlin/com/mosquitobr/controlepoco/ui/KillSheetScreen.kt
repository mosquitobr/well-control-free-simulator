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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosquitobr.controlepoco.R
import com.mosquitobr.controlepoco.core.KillSheetResult
import com.mosquitobr.controlepoco.core.Scenario
import com.mosquitobr.controlepoco.core.SimSnapshot
import com.mosquitobr.controlepoco.core.UnitSystem
import com.mosquitobr.controlepoco.core.Units

@Composable
fun KillSheetScreen(
    scenario: Scenario,
    snapshot: SimSnapshot,
    result: KillSheetResult?,
    units: UnitSystem,
    onBack: () -> Unit,
    onApply: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.kill_sheet_title), onBack) {
            TextButton(onClick = onApply, enabled = result != null) {
                Text(
                    stringResource(R.string.apply_kill_mud),
                    color = if (result != null) Teal else Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (result == null) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.no_kick_sheet),
                    color = Muted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "SIDPP ${pressureText(snapshot.sidppKPa, units)} · SICP ${pressureText(snapshot.sicpKPa, units)}",
                    color = TextMain,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SectionCard(stringResource(R.string.kill_sheet_subtitle)) {
                    Text(
                        "${scenario.title(LocalAppLanguage.current)} · ${scenario.method}",
                        color = TextMain,
                        fontSize = 11.sp
                    )
                    Text(
                        "${scenario.well.name} · TVD ${depthText(scenario.well.tvdM, units)} · " +
                            "${stringResource(R.string.shoe)} ${depthText(scenario.well.shoeTvdM, units)}",
                        color = Muted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            item {
                SectionCard(stringResource(R.string.results)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        MetricCard(
                            stringResource(R.string.kill_mud),
                            densityText(result.killMudKgM3, units, 2),
                            Modifier.weight(1f),
                            Teal
                        )
                        MetricCard("ICP", pressureText(result.icpKPa, units), Modifier.weight(1f), Amber)
                        MetricCard("FCP", pressureText(result.fcpKPa, units), Modifier.weight(1f), Purple)
                    }
                    if (result.marginKgM3 > 0.01) {
                        Text(
                            "${stringResource(R.string.kmw_no_margin)}: " +
                                "${densityText(result.killMudNoMarginKgM3, units, 2)} · " +
                                "${stringResource(R.string.kill_margin_applied)}: " +
                                densityText(result.marginKgM3, units, 2),
                            color = Muted,
                            fontSize = 9.sp
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        MetricCard("MAASP", pressureText(result.maaspKPa, units), Modifier.weight(1f), Red)
                        MetricCard("MASP", pressureText(result.maspKPa, units), Modifier.weight(1f), Blue)
                        MetricCard(
                            stringResource(R.string.kill_rate),
                            rateText(result.killRateLpm, units),
                            Modifier.weight(1f),
                            TextMain
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        MetricCard(
                            stringResource(R.string.strokes_to_bit),
                            Units.fmt(result.strokesToBit, 0),
                            Modifier.weight(1f),
                            TextMain
                        )
                        MetricCard(
                            stringResource(R.string.strokes_to_shoe),
                            Units.fmt(result.strokesToShoe, 0),
                            Modifier.weight(1f),
                            TextMain
                        )
                        MetricCard(
                            stringResource(R.string.total_strokes),
                            Units.fmt(result.totalStrokes, 0),
                            Modifier.weight(1f),
                            TextMain
                        )
                    }
                }
            }

            item {
                SectionCard(stringResource(R.string.scenario_data)) {
                    KillSheetField(stringResource(R.string.sidpp_input), pressureText(result.sidppKPa, units))
                    KillSheetField(stringResource(R.string.sicp_input), pressureText(result.sicpKPa, units))
                    if (snapshot.shutInSafetyOffsetKPa > 0.5) {
                        KillSheetField(
                            stringResource(R.string.shut_in_safety_factor),
                            "+${pressureText(snapshot.shutInSafetyOffsetKPa, units)}"
                        )
                    }
                    KillSheetField(
                        if (snapshot.scrTaken) stringResource(R.string.scr_measured)
                        else stringResource(R.string.scr_estimated),
                        pressureText(snapshot.scrKPa, units)
                    )
                    KillSheetField(stringResource(R.string.scr_rate), rateText(snapshot.scrRateLpm, units))
                    KillSheetField(stringResource(R.string.current_mud), densityText(snapshot.mudDensityKgM3, units, 2))
                    KillSheetField(stringResource(R.string.tvd), depthText(scenario.well.tvdM, units))
                    KillSheetField(stringResource(R.string.shoe_tvd), depthText(scenario.well.shoeTvdM, units))
                    KillSheetField(
                        stringResource(R.string.frac_gradient),
                        gradientText(scenario.well.fracGradientKPaPerM, units)
                    )
                    KillSheetField(
                        stringResource(R.string.pump_output),
                        Units.fmt(scenario.rig.pumps.first().displacementLPerStroke, 1)
                    )
                }
            }

            if (result.warnings.isNotEmpty()) {
                item {
                    SectionCard(stringResource(R.string.warnings)) {
                        result.warnings.forEach {
                            Text("• $it", color = Amber, fontSize = 10.sp, modifier = Modifier.padding(vertical = 1.dp))
                        }
                    }
                }
            }

            item {
                SectionCard(stringResource(R.string.schedule)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Panel2, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        ScheduleCell(stringResource(R.string.strokes), 0.9f, true)
                        ScheduleCell("SPP", 1f, true)
                        ScheduleCell("Casing", 1f, true)
                    }
                }
            }

            items(result.schedule) { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Panel2.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    ScheduleCell(Units.fmt(row.stroke, 0), 0.9f)
                    ScheduleCell(pressureText(row.sppKPa, units), 1f, false, Teal)
                    ScheduleCell(pressureText(row.casingKPa, units), 1f, false, Amber)
                }
                Text(
                    row.stage,
                    color = Muted,
                    fontSize = 8.sp,
                    modifier = Modifier.padding(start = 7.dp, bottom = 2.dp)
                )
            }

            item {
                Text(
                    "Cálculos didáticos: confirme sempre com o programa de controle de poço da operação.",
                    color = Muted,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun KillSheetField(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = TextMain,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ScheduleCell(
    text: String,
    weight: Float,
    header: Boolean = false,
    color: Color = TextMain
) {
    Text(
        text,
        color = if (header) Muted else color,
        fontSize = if (header) 8.sp else 9.sp,
        fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        modifier = Modifier.weight(weight)
    )
}
