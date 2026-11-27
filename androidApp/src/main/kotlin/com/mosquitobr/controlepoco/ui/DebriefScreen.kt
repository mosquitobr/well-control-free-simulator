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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosquitobr.controlepoco.R
import com.mosquitobr.controlepoco.core.Phase
import com.mosquitobr.controlepoco.core.Scenario
import com.mosquitobr.controlepoco.core.SimSnapshot
import com.mosquitobr.controlepoco.core.UnitSystem
import com.mosquitobr.controlepoco.core.Units

@Composable
fun DebriefScreen(
    scenario: Scenario,
    snapshot: SimSnapshot,
    units: UnitSystem,
    onRepeat: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val criticalFailure = snapshot.gasReachedSurface || snapshot.phase == Phase.FAILED
    val pass = snapshot.score >= 70 && !criticalFailure
    val verdictColor = if (pass) Green else Amber
    val frac = scenario.well.fracGradientKPaPerM * scenario.well.shoeTvdM

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.debrief_title), onClose)

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SectionCard(scenario.title(lang)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            snapshot.score.toString(),
                            color = verdictColor,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            " / 100",
                            color = Muted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 18.dp)
                        )
                        Spacer(Modifier.weight(1f))
                        StatusPill(
                            if (pass) stringResource(R.string.debrief_pass) else stringResource(R.string.debrief_fail),
                            verdictColor
                        )
                    }
                    Text(
                        if (pass) {
                            "Objetivos principais cumpridos. Compare o resultado com os indicadores abaixo."
                        } else if (criticalFailure) {
                            "Falha crítica: gás na superfície ou limite de pressão excedido. Revise o shut-in e o choke."
                        } else {
                            "Reveja o shut-in, a estabilização e o controle de pressão antes de repetir."
                        },
                        color = if (criticalFailure) Red else Muted,
                        fontSize = 10.sp
                    )
                }
            }

            item {
                SectionCard(stringResource(R.string.debrief_stats)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        MetricCard(
                            stringResource(R.string.time_elapsed),
                            durationText(snapshot.timeSec),
                            Modifier.weight(1f),
                            TextMain
                        )
                        MetricCard(
                            stringResource(R.string.phase),
                            phaseText(snapshot.phase),
                            Modifier.weight(1f),
                            phaseColor(snapshot.phase)
                        )
                        MetricCard(
                            stringResource(R.string.strokes),
                            Units.fmt(snapshot.totalStrokes, 0),
                            Modifier.weight(1f),
                            Purple
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        MetricCard(
                            "Max casing",
                            pressureText(snapshot.maxCasingKPa, units),
                            Modifier.weight(1f),
                            Amber
                        )
                        MetricCard(
                            "Max sapata",
                            pressureText(snapshot.maxShoeKPa, units),
                            Modifier.weight(1f),
                            Red
                        )
                        MetricCard(
                            stringResource(R.string.maasp),
                            pressureText(snapshot.maaspKPa, units),
                            Modifier.weight(1f),
                            Muted
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        MetricCard(
                            "Ganho máx.",
                            volumeText(snapshot.maxPitGainM3, units),
                            Modifier.weight(1f),
                            Amber
                        )
                        MetricCard(
                            "BHP mín.",
                            pressureText(snapshot.minBhpKPa, units),
                            Modifier.weight(1f),
                            Blue
                        )
                        MetricCard(
                            stringResource(R.string.kill_mud),
                            densityText(snapshot.mudDensityKgM3, units, 2),
                            Modifier.weight(1f),
                            Teal
                        )
                    }
                }
            }

            item {
                SectionCard("Limites do cenário") {
                    LimitBar(
                        label = "Sapata / fratura",
                        value = snapshot.maxShoeKPa,
                        limit = frac,
                        units = units
                    )
                    Spacer(Modifier.height(6.dp))
                    LimitBar(
                        label = "MAASP",
                        value = snapshot.maxCasingKPa,
                        limit = snapshot.maaspKPa,
                        units = units
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (snapshot.gasReachedSurface) {
                            "Gás alcançou a superfície durante a simulação: priorize segurança, sangria e comunicação."
                        } else {
                            "Nenhum gás reportado na superfície durante a simulação."
                        },
                        color = if (snapshot.gasReachedSurface) Red else Muted,
                        fontSize = 9.sp
                    )
                }
            }

            if (snapshot.failures.isNotEmpty()) {
                item {
                    SectionCard(stringResource(R.string.warnings)) {
                        snapshot.failures.distinct().forEach {
                            Text("• $it", color = Amber, fontSize = 10.sp)
                        }
                    }
                }
            }

            item {
                SectionCard(stringResource(R.string.objectives)) {
                    scenario.objectives(lang).forEach {
                        Text("• $it", color = TextMain, fontSize = 10.sp, modifier = Modifier.padding(vertical = 1.dp))
                    }
                }
            }

            item {
                SectionCard(stringResource(R.string.debrief_timeline)) {
                    if (snapshot.milestones.isEmpty()) {
                        Text(stringResource(R.string.log_empty), color = Muted, fontSize = 10.sp)
                    } else {
                        snapshot.milestones.takeLast(16).forEach {
                            Text("• $it", color = TextMain, fontSize = 10.sp, modifier = Modifier.padding(vertical = 1.dp))
                        }
                    }
                }
            }

            items(snapshot.alarms.take(24)) { alarm ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Panel2.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        durationText(alarm.timeSec),
                        color = Muted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(46.dp)
                    )
                    Text(
                        alarmText(alarm),
                        color = alarmColor(alarm.severity),
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                SectionCard(stringResource(R.string.debrief_recommendations)) {
                    val list = snapshot.recommendations.ifEmpty {
                        listOf(
                            "Revise o fluxo de detecção, shut-in e estabilização.",
                            "Compare SIDPP/SICP com os limites calculados no kill sheet.",
                            "Repita o cenário buscando score acima de 70."
                        )
                    }
                    list.forEach {
                        Text("• $it", color = TextMain, fontSize = 10.sp, modifier = Modifier.padding(vertical = 1.dp))
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onRepeat,
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealDark, contentColor = TextMain)
                    ) {
                        Text(stringResource(R.string.restart), fontSize = 10.sp, maxLines = 1)
                    }
                    Button(
                        onClick = onHome,
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Panel3, contentColor = TextMain)
                    ) {
                        Text(stringResource(R.string.home), fontSize = 10.sp, maxLines = 1)
                    }
                    TextButton(onClick = onClose, modifier = Modifier.height(36.dp)) {
                        Text(stringResource(R.string.continue_sim), color = Teal, fontSize = 10.sp)
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.disclaimer),
                    color = Muted,
                    fontSize = 8.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun LimitBar(label: String, value: Double, limit: Double, units: UnitSystem) {
    val fraction = if (limit > 0.0) (value / limit).toFloat().coerceIn(0f, 1f) else 0f
    val over = limit > 0.0 && value > limit
    Column {
        Row {
            Text(label, color = Muted, fontSize = 9.sp, modifier = Modifier.weight(1f))
            Text(
                "${pressureText(value, units)} / ${pressureText(limit, units)}",
                color = if (over) Red else TextMain,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(3.dp))
        androidx.compose.foundation.layout.Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Panel3, RoundedCornerShape(50))
        ) {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .background(if (over) Red else if (fraction > 0.85f) Amber else Teal, RoundedCornerShape(50))
            )
        }
    }
}
