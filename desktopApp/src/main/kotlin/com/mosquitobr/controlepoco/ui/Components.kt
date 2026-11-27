package com.mosquitobr.controlepoco.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.mosquitobr.controlepoco.platform.stringResource
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Paint as SkPaint
import org.jetbrains.skia.TextLine
import org.jetbrains.skia.Typeface as SkTypeface
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosquitobr.controlepoco.R
import com.mosquitobr.controlepoco.core.Alarm
import com.mosquitobr.controlepoco.core.AlarmCode
import com.mosquitobr.controlepoco.core.AlarmSeverity
import com.mosquitobr.controlepoco.core.InjectionPath
import com.mosquitobr.controlepoco.core.KillStage
import com.mosquitobr.controlepoco.core.LineKind
import com.mosquitobr.controlepoco.core.Phase
import com.mosquitobr.controlepoco.core.ReturnPath
import com.mosquitobr.controlepoco.core.Scenario
import com.mosquitobr.controlepoco.core.SimSnapshot
import com.mosquitobr.controlepoco.core.StackType
import com.mosquitobr.controlepoco.core.TrendPoint
import com.mosquitobr.controlepoco.core.UnitSystem
import com.mosquitobr.controlepoco.core.Units
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun Gauge(
    label: String,
    value: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Panel2,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * fraction.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            val angle = Math.toRadians((135.0 + 270.0 * fraction.coerceIn(0f, 1f).toDouble()))
            val radius = size.minDimension * 0.31f
            drawLine(
                color = color,
                start = center,
                end = Offset(
                    center.x + cos(angle).toFloat() * radius,
                    center.y + sin(angle).toFloat() * radius
                ),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(color = color, radius = 3.dp.toPx(), center = center)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                color = Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = value,
                color = TextMain,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = TextMain
) {
    Column(
        modifier = modifier
            .background(Panel2, RoundedCornerShape(10.dp))
            .border(1.dp, Panel3, RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 7.dp)
    ) {
        Text(label, color = Muted, fontSize = 9.sp, maxLines = 1)
        Text(
            value,
            color = accent,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .background(Panel, RoundedCornerShape(12.dp))
            .border(1.dp, Panel3, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        if (title != null) {
            Text(
                title.uppercase(),
                color = Teal,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(7.dp))
        }
        content()
    }
}

@Composable
fun StatusPill(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        maxLines = 1
    )
}

@Composable
fun phaseText(phase: Phase): String = when (phase) {
    Phase.DRILLING -> stringResource(R.string.phase_drilling)
    Phase.KICK_DETECTED -> stringResource(R.string.phase_kick)
    Phase.SHUT_IN -> stringResource(R.string.phase_shut_in)
    Phase.STABILIZED -> stringResource(R.string.phase_stabilized)
    Phase.KILL_PREP -> stringResource(R.string.phase_kill_prep)
    Phase.CIRCULATING -> stringResource(R.string.phase_circulating)
    Phase.KICK_CONTROLLED -> stringResource(R.string.phase_kick_controlled)
    Phase.DISPLACING_KILL_MUD -> stringResource(R.string.phase_displacing_kill_mud)
    Phase.KILLED -> stringResource(R.string.phase_killed)
    Phase.FAILED -> stringResource(R.string.phase_failed)
}

fun phaseColor(phase: Phase): Color = when (phase) {
    Phase.DRILLING -> Teal
    Phase.KICK_DETECTED -> Red
    Phase.SHUT_IN -> Amber
    Phase.STABILIZED -> Blue
    Phase.KILL_PREP -> Blue
    Phase.CIRCULATING -> Purple
    Phase.KICK_CONTROLLED -> Blue
    Phase.DISPLACING_KILL_MUD -> Purple
    Phase.KILLED -> Green
    Phase.FAILED -> Red
}

@Composable
fun killStageText(stage: KillStage): String = when (stage) {
    KillStage.NONE -> stringResource(R.string.kill_stage_none)
    KillStage.FIRST_CIRCULATION -> stringResource(R.string.kill_stage_first)
    KillStage.KICK_CONTROLLED -> stringResource(R.string.kill_stage_controlled)
    KillStage.STRING -> stringResource(R.string.kill_stage_string)
    KillStage.ANNULUS -> stringResource(R.string.kill_stage_annulus)
    KillStage.CHOKE_LINE -> stringResource(R.string.kill_stage_choke)
    KillStage.WELL_KILLED -> stringResource(R.string.kill_stage_killed)
    KillStage.LINE_DISPLACEMENT -> stringResource(R.string.kill_stage_lines)
    KillStage.READY_TO_OPEN -> stringResource(R.string.kill_stage_ready)
}

@Composable
fun ProgressBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .height(7.dp)
            .background(Panel3, RoundedCornerShape(4.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(7.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun alarmText(alarm: Alarm): String = when (alarm.code) {
    AlarmCode.KICK_DETECTED -> stringResource(R.string.alarm_kick)
    AlarmCode.FLOW_WITH_PUMPS_OFF -> stringResource(R.string.alarm_flow)
    AlarmCode.PIT_GAIN -> stringResource(R.string.alarm_gain)
    AlarmCode.PIT_LOSS -> stringResource(R.string.alarm_loss)
    AlarmCode.RETURN_FLOW_CHANGE -> stringResource(R.string.alarm_return_flow)
    AlarmCode.MAASP_EXCEEDED, AlarmCode.CASING_HIGH -> stringResource(R.string.alarm_maasp)
    AlarmCode.FRAC_EXCEEDED -> stringResource(R.string.alarm_frac)
    AlarmCode.RETURN_PATH_CLOSED -> stringResource(R.string.alarm_return_path)
    AlarmCode.ACCUMULATOR_LOW -> stringResource(R.string.alarm_accumulator)
    AlarmCode.GAS_AT_SURFACE -> stringResource(R.string.alarm_gas)
    AlarmCode.STABILIZED -> stringResource(R.string.alarm_stabilized)
    AlarmCode.KILL_COMPLETE -> stringResource(R.string.alarm_killed)
    AlarmCode.FAILED -> stringResource(R.string.alarm_failed)
    AlarmCode.PUMP_FAILURE -> stringResource(R.string.alarm_pump)
    AlarmCode.CHOKE_PLUGGED, AlarmCode.CHOKE_WASHED -> stringResource(R.string.alarm_choke)
    AlarmCode.SPP_LOW, AlarmCode.SPP_HIGH -> stringResource(R.string.alarm_info)
    AlarmCode.INFO -> stringResource(R.string.alarm_info)
}

fun alarmColor(severity: AlarmSeverity): Color = when (severity) {
    AlarmSeverity.CRITICAL -> Red
    AlarmSeverity.WARNING -> Amber
    AlarmSeverity.INFO -> Blue
}

@Composable
fun AlarmRow(alarm: Alarm, onAck: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(alarmColor(alarm.severity).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .clickable { onAck() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(7.dp)
                .background(alarmColor(alarm.severity), RoundedCornerShape(50))
        )
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(alarmText(alarm), color = TextMain, fontSize = 11.sp, maxLines = 2)
            Text(
                "${durationText(alarm.timeSec)}  ${alarm.detail}",
                color = Muted,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
        Text(
            stringResource(R.string.ack),
            color = alarmColor(alarm.severity),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(start = 6.dp)
                .background(alarmColor(alarm.severity).copy(alpha = 0.12f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private enum class LabelAlign { LEFT, CENTER, RIGHT }

private val desktopMonoTypeface: SkTypeface by lazy {
    val fm = FontMgr.default
    fm.matchFamilyStyle("JetBrains Mono", FontStyle.NORMAL)
        ?: fm.matchFamilyStyle("DejaVu Sans Mono", FontStyle.NORMAL)
        ?: fm.matchFamilyStyle("Consolas", FontStyle.NORMAL)
        ?: fm.matchFamilyStyle("Liberation Mono", FontStyle.NORMAL)
        ?: fm.matchFamilyStyle("monospace", FontStyle.NORMAL)
        ?: SkTypeface.makeEmpty()
}

private fun DrawScope.label(
    text: String,
    x: Float,
    y: Float,
    color: Color,
    sizePx: Float = 9.dp.toPx(),
    align: LabelAlign = LabelAlign.LEFT
) {
    val skPaint = SkPaint().apply {
        this.color = color.toArgb()
        isAntiAlias = true
    }
    val font = Font(desktopMonoTypeface, sizePx)
    val line = TextLine.make(text, font)
    val drawX = when (align) {
        LabelAlign.LEFT -> x
        LabelAlign.CENTER -> x - line.width / 2f
        LabelAlign.RIGHT -> x - line.width
    }
    drawContext.canvas.nativeCanvas.drawTextLine(line, drawX, y, skPaint)
    line.close()
    font.close()
    skPaint.close()
}

@Composable
fun WellSchematic(
    scenario: Scenario,
    snapshot: SimSnapshot,
    units: UnitSystem,
    modifier: Modifier = Modifier
) {
    val well = scenario.well
    val seaLevelText = stringResource(R.string.sea_level)
    val mudlineText = stringResource(R.string.mudline)
    val rigFloorText = stringResource(R.string.rig_floor)
    val riserText = stringResource(R.string.riser)
    val bopText = stringResource(R.string.bop_stack)
    val shoeText = stringResource(R.string.shoe)
    val bitText = stringResource(R.string.bit)
    val killLineText = stringResource(R.string.kill_line)
    val chokeLineText = stringResource(R.string.choke_line)
    val boosterText = stringResource(R.string.booster_line)
    val standpipeText = stringResource(R.string.standpipe)
    val killManifoldText = stringResource(R.string.kill_manifold)
    val chokeManifoldText = stringResource(R.string.choke_manifold)
    val pitText = stringResource(R.string.pit)
    val flowText = stringResource(R.string.flow_path)
    val noReturnText = stringResource(R.string.no_return)
    val returnRiserText = stringResource(R.string.return_riser)
    val returnFlowlineText = stringResource(R.string.return_flowline)
    val returnChokeText = stringResource(R.string.return_choke)
    val gasText = stringResource(R.string.gas)
    val bopClosedText = stringResource(R.string.bop_closed)
    val bopOpenText = stringResource(R.string.bop_open)

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Bg)

        val top = 26.dp.toPx()
        val bottom = h - 22.dp.toPx()
        val subsea = well.isSubsea
        val waterTvd = well.riser?.waterDepthM ?: 0.0

        fun yForTvd(depth: Double): Float {
            val d = depth.coerceIn(0.0, well.tvdM.coerceAtLeast(1.0))
            return (top + (d / well.tvdM) * (bottom - top)).toFloat()
        }
        fun yForMd(md: Double): Float = yForTvd(well.tvdAtMd(md))

        val surfaceY = top
        val mudlineY = if (subsea) yForTvd(waterTvd) else top
        val wellCenterX = w * 0.40f
        val killX = w * 0.13f
        val boosterX = w * 0.63f
        val chokeX = w * 0.84f
        val pitX = w * 0.94f

        val pumpOn = snapshot.pumpOn
        val chokePct = snapshot.chokePct
        val hcrOpen = snapshot.hcrOpen
        val closedBop = snapshot.closedBop
        val bopClosed = snapshot.bopClosed
        val bob = ((snapshot.timeSec * 0.9) % 1.0).toFloat()

        val oldMudColor = Color(0xFF6D7B85)
        val killMudColor = Color(0xFFE06C6C)
        val gasColor = Amber
        val flowColor = Teal
        val mutedLine = Color(0xFF3F4F5C)

        fun line(a: Offset, b: Offset, color: Color, width: Float = 1.6.dp.toPx()) {
            drawLine(color, a, b, width)
        }

        // Ponta de seta apontando na direção (dx, dy) normalizada.
        fun arrowHead(x: Float, y: Float, dx: Float, dy: Float, color: Color) {
            val s = 4.5.dp.toPx()
            val nx = -dy
            val ny = dx
            drawLine(
                color,
                Offset(x, y),
                Offset(x - dx * s + nx * s * 0.65f, y - dy * s + ny * s * 0.65f),
                1.3.dp.toPx()
            )
            drawLine(
                color,
                Offset(x, y),
                Offset(x - dx * s - nx * s * 0.65f, y - dy * s - ny * s * 0.65f),
                1.3.dp.toPx()
            )
        }

        // Setas animadas ao longo de um segmento, respeitando qualquer direção.
        fun segmentArrows(a: Offset, b: Offset, color: Color, count: Int = 2) {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val len = kotlin.math.sqrt(dx * dx + dy * dy)
            if (len < 1f) return
            val ux = dx / len
            val uy = dy / len
            repeat(count) { i ->
                val f = ((i + bob) / count).coerceIn(0.02f, 0.98f)
                arrowHead(a.x + dx * f, a.y + dy * f, ux, uy, color)
            }
        }

        fun manifoldBox(x: Float, text: String, active: Boolean, color: Color) {
            val bw = min(56.dp.toPx(), w * 0.20f)
            val bh = 14.dp.toPx()
            val y = surfaceY - bh - 3.dp.toPx()
            drawRect(
                color = if (active) color.copy(alpha = 0.85f) else Color(0xFF22303C),
                topLeft = Offset(x - bw / 2f, y),
                size = Size(bw, bh)
            )
            drawRect(
                color = color.copy(alpha = 0.65f),
                topLeft = Offset(x - bw / 2f, y),
                size = Size(bw, bh),
                style = Stroke(width = 1.dp.toPx())
            )
            label(
                text,
                x - bw / 2f + 3.dp.toPx(),
                y + bh * 0.72f,
                if (active) Color.White else Muted,
                sizePx = 6.dp.toPx()
            )
        }

        // ============================================================
        // 1. Superfície: nível do mar, lâmina d'água e leito marinho
        // ============================================================
        if (subsea) {
            drawRect(
                color = Color(0xFF0A2233),
                topLeft = Offset(0f, surfaceY),
                size = Size(w, (mudlineY - surfaceY).coerceAtLeast(1f))
            )
            drawLine(Color(0xFF2C6488), Offset(0f, surfaceY), Offset(w, surfaceY), 1.5.dp.toPx())
            label(seaLevelText, 6.dp.toPx(), surfaceY + 9.dp.toPx(), Color(0xFF6FA3C2), 7.dp.toPx())
            drawLine(Color(0xFF5E7A88), Offset(0f, mudlineY), Offset(w, mudlineY), 2.dp.toPx())
            label(mudlineText, 6.dp.toPx(), mudlineY - 3.dp.toPx(), Color(0xFF87A4B3), 7.dp.toPx())
            label(depthText(waterTvd, units, 0), 6.dp.toPx(), mudlineY + 10.dp.toPx(), Muted, 6.dp.toPx())
        } else {
            drawLine(Color(0xFF33422E), Offset(0f, surfaceY), Offset(w, surfaceY), 3.dp.toPx())
            label(rigFloorText, 6.dp.toPx(), surfaceY + 11.dp.toPx(), Muted, 7.dp.toPx())
        }

        // ============================================================
        // 2. Poço: revestimentos, coluna e interface de lama
        // ============================================================
        val bitY = yForTvd(snapshot.bitTvdM)
        val shoeY = yForTvd(well.shoeTvdM)
        val casingColors = listOf(Color(0xFF8A949C), Color(0xFF6D7B85), Color(0xFF596770))
        well.casings.forEachIndexed { index, casing ->
            val half = (7.dp.toPx() + index * 3.dp.toPx())
            val startY = yForMd(casing.topMdM)
            val endY = yForMd(casing.bottomMdM)
            val color = casingColors[index % casingColors.size]
            line(Offset(wellCenterX - half, startY), Offset(wellCenterX - half, endY), color, 1.4.dp.toPx())
            line(Offset(wellCenterX + half, startY), Offset(wellCenterX + half, endY), color, 1.4.dp.toPx())
            label(casing.name, wellCenterX + half + 3.dp.toPx(), endY - 2.dp.toPx(), color, 6.dp.toPx())
        }
        line(
            Offset(wellCenterX - 9.dp.toPx(), shoeY),
            Offset(wellCenterX - 9.dp.toPx(), bottom),
            Color(0xFF5B6B77), 1.4.dp.toPx()
        )
        line(
            Offset(wellCenterX + 9.dp.toPx(), shoeY),
            Offset(wellCenterX + 9.dp.toPx(), bottom),
            Color(0xFF5B6B77), 1.4.dp.toPx()
        )
        label(shoeText, wellCenterX + 14.dp.toPx(), shoeY, Muted, 6.dp.toPx())

        // Coluna de perfuração (COP): da superfície até a broca.
        val stringTopY = surfaceY
        val stringSpan = (bitY - stringTopY).coerceAtLeast(1f)
        val stringKillFrac = snapshot.stringKillFraction.toFloat().coerceIn(0f, 1f)
        val annulusKillFrac = snapshot.annulusKillFraction.toFloat().coerceIn(0f, 1f)

        // Coluna (COP): lama nova desce da superfície para a broca (cima → baixo).
        val stringLeft = wellCenterX - 2.2.dp.toPx()
        val stringWidth = 4.4.dp.toPx()
        drawRect(oldMudColor, Offset(stringLeft, stringTopY), Size(stringWidth, stringSpan))
        if (stringKillFrac > 0.001f) {
            val frontier = stringTopY + stringKillFrac * stringSpan
            drawRect(killMudColor, Offset(stringLeft, stringTopY), Size(stringWidth, frontier - stringTopY))
        }
        line(Offset(stringLeft, stringTopY), Offset(stringLeft, bitY), oldMudColor.copy(alpha = 0.6f), 1.dp.toPx())
        line(Offset(stringLeft + stringWidth, stringTopY), Offset(stringLeft + stringWidth, bitY), oldMudColor.copy(alpha = 0.6f), 1.dp.toPx())

        // Anular (retorno): da broca até o BOP/mesa, lama nova sobe (baixo → cima).
        val annulusWidth = 3.2.dp.toPx()
        val annulusLeftX = wellCenterX - 7.dp.toPx()
        val annulusRightX = wellCenterX + 7.dp.toPx() - annulusWidth
        val mudTopY = if (subsea) mudlineY else surfaceY
        val mudSpan = (bitY - mudTopY).coerceAtLeast(1f)
        listOf(annulusLeftX, annulusRightX).forEach { x ->
            drawRect(oldMudColor, Offset(x, mudTopY), Size(annulusWidth, mudSpan))
            if (annulusKillFrac > 0.001f) {
                val frontier = bitY - annulusKillFrac * mudSpan
                drawRect(
                    killMudColor,
                    Offset(x, frontier),
                    Size(annulusWidth, (bitY - frontier).coerceAtLeast(1f))
                )
            }
        }

        // Linha de retorno (choke line / kill line / riser): da superfície até BOP/mesa.
        // Desenha a frente de lama nova subindo pela choke/kill line até a superfície.
        val chokeLineKillFrac = snapshot.chokeLineKillFraction.toFloat().coerceIn(0f, 1f)
        val chokeLineTopY = if (subsea) surfaceY else surfaceY
        val chokeLineSpan = (bitY - chokeLineTopY).coerceAtLeast(1f)
        // Kill line (esquerda)
        val killLineX = w * 0.13f
        val killLineLeft = killLineX - 1.6.dp.toPx()
        val killLineWidth = 3.2.dp.toPx()
        drawRect(oldMudColor, Offset(killLineLeft, mudTopY), Size(killLineWidth, (mudTopY - chokeLineTopY).coerceAtLeast(1f)))
        if (chokeLineKillFrac > 0.001f) {
            val killLineSpan = (mudTopY - chokeLineTopY).coerceAtLeast(1f)
            val frontier = mudTopY - chokeLineKillFrac * killLineSpan
            drawRect(killMudColor, Offset(killLineLeft, frontier), Size(killLineWidth, (mudTopY - frontier).coerceAtLeast(1f)))
        }
        // Choke line (direita)
        val chokeLineX = w * 0.84f
        val chokeLineLeft = chokeLineX - 1.6.dp.toPx()
        drawRect(oldMudColor, Offset(chokeLineLeft, mudTopY), Size(killLineWidth, (mudTopY - chokeLineTopY).coerceAtLeast(1f)))
        if (chokeLineKillFrac > 0.001f) {
            val chokeLineSpan = (mudTopY - chokeLineTopY).coerceAtLeast(1f)
            val frontier = mudTopY - chokeLineKillFrac * chokeLineSpan
            drawRect(killMudColor, Offset(chokeLineLeft, frontier), Size(killLineWidth, (mudTopY - frontier).coerceAtLeast(1f)))
        }

        // Broca.
        drawCircle(Amber, 2.6.dp.toPx(), Offset(wellCenterX, bitY))
        label(bitText, wellCenterX + 14.dp.toPx(), bitY, Amber, 6.dp.toPx())
        label(depthText(snapshot.bitDepthM, units, 0), wellCenterX + 14.dp.toPx(), bitY + 9.dp.toPx(), Muted, 6.dp.toPx())

        // ============================================================
        // 3. BOP / LMRP no leito marinho ou na superfície
        // ============================================================
        val elements = scenario.rig.bop.elements.take(6)
        val bopH = 8.5.dp.toPx()
        val bopGap = 1.4.dp.toPx()
        val bopTopY = if (subsea) mudlineY + 2.dp.toPx() else surfaceY + 8.dp.toPx()
        elements.forEachIndexed { index, element ->
            val ey = bopTopY + index * (bopH + bopGap)
            val isHcr = element == com.mosquitobr.controlepoco.core.BopElement.HCR
            val closed = bopClosed.contains(element)
            val active = if (isHcr) hcrOpen else closed
            val color = if (isHcr) {
                if (active) Teal else Muted
            } else {
                if (active) Red else Color(0xFF3A4A58)
            }
            drawRect(
                color,
                Offset(wellCenterX - 15.dp.toPx(), ey),
                Size(30.dp.toPx(), bopH)
            )
            drawRect(
                color.copy(alpha = 0.5f),
                Offset(wellCenterX - 15.dp.toPx(), ey),
                Size(30.dp.toPx(), bopH),
                style = Stroke(width = 1.dp.toPx())
            )
            label(
                when (element) {
                    com.mosquitobr.controlepoco.core.BopElement.ANNULAR -> "ANN"
                    com.mosquitobr.controlepoco.core.BopElement.PIPE_RAM -> "RAM"
                    com.mosquitobr.controlepoco.core.BopElement.BLIND_SHEAR -> "B/S"
                    com.mosquitobr.controlepoco.core.BopElement.TEST_RAM -> "TEST"
                    com.mosquitobr.controlepoco.core.BopElement.HCR -> "HCR"
                    else -> "?"
                },
                wellCenterX + 17.dp.toPx(), ey + bopH * 0.82f, color, 6.dp.toPx()
            )
        }
        val bopBottomY = bopTopY + elements.size * (bopH + bopGap)
        if (subsea) {
            label(bopText, wellCenterX - 18.dp.toPx(), bopTopY + bopH, Muted, 6.dp.toPx(), LabelAlign.RIGHT)
            label("LMRP", wellCenterX + 17.dp.toPx(), bopTopY + bopH * 0.8f, Muted, 6.dp.toPx())
            // Riser entre o LMRP e a superfície.
            val riserColor = if (closedBop) Color(0xFF4A5A68) else Teal
            line(Offset(wellCenterX - 6.dp.toPx(), bopTopY), Offset(wellCenterX - 6.dp.toPx(), surfaceY), riserColor, 2.dp.toPx())
            line(Offset(wellCenterX + 6.dp.toPx(), bopTopY), Offset(wellCenterX + 6.dp.toPx(), surfaceY), riserColor, 2.dp.toPx())
            label(riserText, wellCenterX + 9.dp.toPx(), (surfaceY + mudlineY) / 2f, riserColor.copy(alpha = 0.85f), 6.dp.toPx())
        }

        // ============================================================
        // 4. Linhas de controle: kill (esq.), choke (dir.) e booster
        // ============================================================
        val annularIndex = elements.indexOfFirst { it == com.mosquitobr.controlepoco.core.BopElement.ANNULAR }
        val tapIndex = if (annularIndex >= 0) annularIndex + 1 else 1
        val tapY = bopTopY + tapIndex * (bopH + bopGap)
        val sideX = 17.dp.toPx()

        // Standpipe: alimenta a coluna pela superfície. Não se confunde com a
        // kill line, que só recebe fluxo no alinhamento de kill.
        val standpipeActive = snapshot.standpipeActive
        val standpipeColor = if (standpipeActive) flowColor else mutedLine
        line(Offset(wellCenterX, surfaceY), Offset(wellCenterX, mudTopY), standpipeColor, 1.6.dp.toPx())
        manifoldBox(wellCenterX, standpipeText, standpipeActive, Teal)

        // Kill line: highlight somente quando ela é o caminho de injeção.
        val killActive = snapshot.killLineActive
        val killColor = if (killActive) Color(0xFFE06C6C) else mutedLine
        val killTap = Offset(wellCenterX - sideX, tapY)
        val killRise = Offset(killX, tapY)
        val killTop = Offset(killX, surfaceY)
        line(killTap, killRise, killColor, 1.8.dp.toPx())
        line(killRise, killTop, killColor, 1.8.dp.toPx())
        manifoldBox(killX, killManifoldText, killActive, Teal)
        label(killLineText, killX + 4.dp.toPx(), tapY - 4.dp.toPx(), killColor, 6.dp.toPx())

        // Choke manifold: ativo apenas quando o retorno passa pela choke line.
        val chokeActive = snapshot.chokeManifoldActive
        val chokeColor = if (chokeActive) Teal else mutedLine
        val chokeTap = Offset(wellCenterX + sideX, tapY)
        val chokeRise = Offset(chokeX, tapY)
        val chokeTop = Offset(chokeX, surfaceY)
        line(chokeTap, chokeRise, chokeColor, 1.8.dp.toPx())
        line(chokeRise, chokeTop, chokeColor, 1.8.dp.toPx())
        manifoldBox(chokeX, chokeManifoldText, chokeActive, Teal)
        label(chokeLineText, chokeX + 3.dp.toPx(), tapY - 4.dp.toPx(), chokeColor, 6.dp.toPx(), LabelAlign.RIGHT)

        val boosterActive = subsea && !closedBop
        if (subsea) {
            val boosterColor = if (boosterActive) Teal else mutedLine
            val boosterTap = Offset(wellCenterX + sideX, bopTopY)
            val boosterRise = Offset(boosterX, bopTopY)
            val boosterTop = Offset(boosterX, surfaceY)
            line(boosterTap, boosterRise, boosterColor, 1.8.dp.toPx())
            line(boosterRise, boosterTop, boosterColor, 1.8.dp.toPx())
            manifoldBox(boosterX, boosterText, boosterActive, Teal)
            label(boosterText, boosterX + 4.dp.toPx(), bopTopY - 4.dp.toPx(), boosterColor, 6.dp.toPx())
        }

        // ============================================================
        // 5. Retorno e fluxo
        // ============================================================
        val returnPath = snapshot.returnPath
        val annulusFlowX = wellCenterX + 4.5.dp.toPx()
        val returnColor = when (returnPath) {
            ReturnPath.FLOWLINE, ReturnPath.RISER -> Teal
            ReturnPath.CHOKE_LINE -> Amber
            ReturnPath.KILL_LINE -> Teal
            ReturnPath.TRIP_TANK -> Teal
            ReturnPath.LUBRICATE_BLEED -> Teal
            ReturnPath.BLOCKED -> Red
        }

        // Pit sempre na superfície.
        val pitY = surfaceY + 10.dp.toPx()
        drawRect(
            Color(0xFF2A3A4A),
            Offset(pitX - 14.dp.toPx(), pitY),
            Size(28.dp.toPx(), 12.dp.toPx())
        )
        drawRect(
            Teal.copy(alpha = 0.4f),
            Offset(pitX - 14.dp.toPx(), pitY),
            Size(28.dp.toPx(), 12.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )
        label(pitText, pitX - 8.dp.toPx(), pitY + 9.dp.toPx(), Muted, 6.dp.toPx())

        // Retorno sobe sempre pelo anular, nunca pelo interior da coluna.
        if (returnPath != ReturnPath.BLOCKED) {
            val annulusTop = if (returnPath == ReturnPath.CHOKE_LINE) tapY else bopTopY
            segmentArrows(Offset(annulusFlowX, bitY), Offset(annulusFlowX, annulusTop), returnColor, 3)
        }

        // Setas do caminho de retorno no esquemático: riser acima do BOP,
        // flowline até o pit e choke line acompanhando a geometria.
        val flowlineY = (surfaceY + 9.dp.toPx())
        fun flowlineToPit() {
            val start = Offset(wellCenterX, flowlineY)
            val end = Offset(pitX - 14.dp.toPx(), flowlineY)
            line(start, end, returnColor, 1.4.dp.toPx())
            segmentArrows(start, end, returnColor, 3)
        }
        when (returnPath) {
            ReturnPath.RISER -> {
                segmentArrows(
                    Offset(wellCenterX + 6.dp.toPx(), bopTopY),
                    Offset(wellCenterX + 6.dp.toPx(), surfaceY),
                    returnColor,
                    4
                )
                flowlineToPit()
            }
            ReturnPath.FLOWLINE -> flowlineToPit()
            ReturnPath.CHOKE_LINE -> {
                segmentArrows(chokeTap, chokeRise, returnColor, 2)
                segmentArrows(chokeRise, chokeTop, returnColor, 3)
                val manifoldToPit = Offset(pitX - 14.dp.toPx(), surfaceY + 3.dp.toPx())
                line(chokeTop, manifoldToPit, returnColor, 1.4.dp.toPx())
                segmentArrows(chokeTop, manifoldToPit, returnColor, 2)
            }
            else -> Unit
        }

        when (returnPath) {
            ReturnPath.FLOWLINE, ReturnPath.RISER -> Teal
            ReturnPath.CHOKE_LINE -> Amber
            ReturnPath.KILL_LINE -> Teal
            ReturnPath.TRIP_TANK -> Teal
            ReturnPath.LUBRICATE_BLEED -> Teal
            ReturnPath.BLOCKED -> Red
        }

        // Ida: standpipe → coluna → broca, ou kill line → poço.
        val goColor = if (snapshot.killMudPumpedM3 > 0.0) killMudColor else flowColor
        when (snapshot.injectionPath) {
            InjectionPath.STANDPIPE -> {
                segmentArrows(Offset(wellCenterX, surfaceY), Offset(wellCenterX, bitY), goColor, 3)
                label(flowText, wellCenterX + 14.dp.toPx(), (mudTopY + bitY) / 2f, goColor, 6.dp.toPx())
            }
            InjectionPath.KILL_LINE -> {
                segmentArrows(killTop, killRise, goColor, 2)
                segmentArrows(killRise, killTap, goColor, 2)
                label(flowText, killX + 4.dp.toPx(), tapY - 12.dp.toPx(), goColor, 6.dp.toPx())
            }
            InjectionPath.CHOKE_LINE -> {
                segmentArrows(chokeTop, chokeRise, goColor, 2)
                label(flowText, chokeX + 4.dp.toPx(), surfaceY + 12.dp.toPx(), goColor, 6.dp.toPx())
            }
            InjectionPath.TRIP_LINE -> {
                segmentArrows(Offset(0f, bopTopY), Offset(0f, surfaceY), goColor, 2)
                label(flowText, wellCenterX + 14.dp.toPx(), surfaceY + 12.dp.toPx(), goColor, 6.dp.toPx())
            }
            InjectionPath.LUBRICATE_LINE -> {
                segmentArrows(Offset(wellCenterX, bitY), Offset(wellCenterX, bitY), goColor, 2)
                label(flowText, wellCenterX + 14.dp.toPx(), (mudTopY + bitY) / 2f, goColor, 6.dp.toPx())
            }
            InjectionPath.NONE -> Unit
        }

        // ============================================================
        // 6. Influxo / gás
        // ============================================================
        if (snapshot.influxM3 > 0.01) {
            val topTvdY = yForTvd(snapshot.influxTopTvdM)
            val baseTvdY = yForTvd(snapshot.influxBaseTvdM)
            when (snapshot.influxPath) {
                com.mosquitobr.controlepoco.core.InfluxPath.CHOKE_LINE -> {
                    for (i in 0 until 6) {
                        val f = (i + 0.5f) / 6f
                        drawCircle(
                            gasColor,
                            2.2.dp.toPx(),
                            Offset(chokeTap.x + (chokeRise.x - chokeTap.x) * f, chokeTap.y - 3.dp.toPx())
                        )
                    }
                    label(gasText, chokeRise.x + 4.dp.toPx(), tapY - 14.dp.toPx(), gasColor, 6.dp.toPx())
                }
                com.mosquitobr.controlepoco.core.InfluxPath.RISER -> {
                    for (i in 0 until 8) {
                        val f = (i + 0.5f) / 8f
                        drawCircle(
                            gasColor.copy(alpha = 0.85f),
                            2.2.dp.toPx(),
                            Offset(wellCenterX + 2.dp.toPx(), surfaceY + (mudlineY - surfaceY) * f)
                        )
                    }
                    label(gasText, wellCenterX + 9.dp.toPx(), mudlineY - 14.dp.toPx(), gasColor, 6.dp.toPx())
                }
                com.mosquitobr.controlepoco.core.InfluxPath.BOP -> {
                    drawCircle(gasColor, 3.5.dp.toPx(), Offset(wellCenterX, tapY + 4.dp.toPx()))
                    label(gasText, wellCenterX + 18.dp.toPx(), tapY + 4.dp.toPx(), gasColor, 6.dp.toPx())
                }
                else -> {
                    val yTop = min(topTvdY, baseTvdY)
                    val yBase = max(topTvdY, baseTvdY)
                    drawRect(
                        gasColor.copy(alpha = 0.28f),
                        Offset(wellCenterX - 5.dp.toPx(), yTop),
                        Size(10.dp.toPx(), (yBase - yTop).coerceAtLeast(2.dp.toPx()))
                    )
                    val count = (4 + snapshot.influxM3 * 2).toInt().coerceIn(4, 12)
                    for (i in 0 until count) {
                        val f = (i + 0.5f) / count
                        drawCircle(
                            gasColor,
                            1.9.dp.toPx(),
                            Offset(wellCenterX + (if (i % 2 == 0) -2.5.dp.toPx() else 2.5.dp.toPx()), yTop + (yBase - yTop) * f)
                        )
                    }
                    label(gasText, wellCenterX + 14.dp.toPx(), (yTop + yBase) / 2f, gasColor, 6.dp.toPx())
                }
            }
        }

        // ============================================================
        // 7. Rodapé do esquemático
        // ============================================================
        label(
            if (closedBop) bopClosedText else bopOpenText,
            6.dp.toPx(), h - 6.dp.toPx(),
            if (closedBop) Red else Green, 7.dp.toPx()
        )
        label(
            "CHOKE ${snapshot.activeChokeName} ${Units.fmt(chokePct)}%",
            w - 6.dp.toPx(), h - 6.dp.toPx(), Amber, 7.dp.toPx(), LabelAlign.RIGHT
        )
    }
}

@Composable
fun KickMonitor(
    scenario: Scenario,
    snapshot: SimSnapshot,
    units: UnitSystem,
    modifier: Modifier = Modifier
) {
    val well = scenario.well
    val emptyText = stringResource(R.string.no_kick)
    val gasText = stringResource(R.string.gas)
    val zoneText = stringResource(R.string.kick_zone)
    Box(modifier.background(Panel2, RoundedCornerShape(10.dp))) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val w = size.width
            val h = size.height
            val top = 12.dp.toPx()
            val bottom = h - 12.dp.toPx()
            val axisX = 34.dp.toPx()
            val wellX = w * 0.52f
            fun yForTvd(depth: Double): Float =
                (top + (depth / well.tvdM.coerceAtLeast(1.0)).coerceIn(0.0, 1.0) * (bottom - top)).toFloat()

            // Depth grid.
            for (i in 0..4) {
                val tvd = well.tvdM * i / 4.0
                val y = yForTvd(tvd)
                drawLine(Panel3, Offset(axisX, y), Offset(w - 4.dp.toPx(), y), 1f)
                label("${Units.fmt(tvd, 0)}", 4.dp.toPx(), y + 3.dp.toPx(), Muted, 7.dp.toPx())
            }

            // Formation layers.
            val colors = listOf(
                Color(0xFF3B4C3A), Color(0xFF3A4654), Color(0xFF4A4436),
                Color(0xFF3E3A4E), Color(0xFF4E3A3A), Color(0xFF354A4A)
            )
            well.formations.forEachIndexed { index, layer ->
                val topY = yForTvd(well.tvdAtMd(layer.topMdM))
                val baseY = yForTvd(well.tvdAtMd(layer.baseMdM))
                drawRect(
                    color = colors[index % colors.size].copy(alpha = 0.55f),
                    topLeft = Offset(axisX + 2.dp.toPx(), topY),
                    size = Size(w - axisX - 6.dp.toPx(), (baseY - topY).coerceAtLeast(1f))
                )
                if (layer.kickZone) {
                    drawRect(
                        color = Amber.copy(alpha = 0.18f),
                        topLeft = Offset(axisX + 2.dp.toPx(), topY),
                        size = Size(w - axisX - 6.dp.toPx(), (baseY - topY).coerceAtLeast(1f))
                    )
                    label(zoneText, axisX + 6.dp.toPx(), topY + 9.dp.toPx(), Amber, 7.dp.toPx())
                }
            }

            // Wellbore.
            drawLine(Color(0xFF7E8B96), Offset(wellX, top), Offset(wellX, bottom), 2.dp.toPx())
            drawLine(Color(0xFF7E8B96), Offset(wellX - 4.dp.toPx(), top), Offset(wellX - 4.dp.toPx(), bottom), 1.dp.toPx())
            drawLine(Color(0xFF7E8B96), Offset(wellX + 4.dp.toPx(), top), Offset(wellX + 4.dp.toPx(), bottom), 1.dp.toPx())

            // Sea / mudline and BOP.
            if (well.isSubsea) {
                val mudlineY = yForTvd(well.riser?.waterDepthM ?: 0.0)
                drawLine(Color(0xFF2D6A8A), Offset(axisX, mudlineY), Offset(w - 4.dp.toPx(), mudlineY), 1.5f)
                label("MUDLINE", wellX + 6.dp.toPx(), mudlineY - 3.dp.toPx(), Color(0xFF5D8CA8), 7.dp.toPx())
                val bopY = yForTvd(well.riser?.waterDepthM ?: 0.0)
                drawRect(
                    color = if (snapshot.closedBop) Red.copy(alpha = 0.6f) else Green.copy(alpha = 0.4f),
                    topLeft = Offset(wellX - 12.dp.toPx(), bopY - 2.dp.toPx()),
                    size = Size(24.dp.toPx(), 9.dp.toPx())
                )
                label("BOP", wellX - 14.dp.toPx(), bopY + 8.dp.toPx(), Muted, 7.dp.toPx(), LabelAlign.RIGHT)
            }

            // Kick zone configured for the trigger.
            val zoneTopY = yForTvd(well.tvdAtMd(snapshot.kickZoneTopMdM))
            val zoneBaseY = yForTvd(well.tvdAtMd(snapshot.kickZoneBaseMdM))
            if (snapshot.kickZoneBaseMdM > snapshot.kickZoneTopMdM) {
                drawRect(
                    color = Red.copy(alpha = 0.12f),
                    topLeft = Offset(wellX - 8.dp.toPx(), zoneTopY),
                    size = Size(16.dp.toPx(), (zoneBaseY - zoneTopY).coerceAtLeast(2f))
                )
            }

            // Influx band.
            if (snapshot.influxM3 > 0.001) {
                val topY = yForTvd(snapshot.influxTopTvdM)
                val baseY = yForTvd(snapshot.influxBaseTvdM)
                val yTop = min(topY, baseY)
                val height = (kotlin.math.abs(baseY - topY)).coerceAtLeast(4.dp.toPx())
                drawRect(
                    color = Amber.copy(alpha = 0.55f),
                    topLeft = Offset(wellX - 7.dp.toPx(), yTop),
                    size = Size(14.dp.toPx(), height)
                )
                drawLine(Amber, Offset(wellX - 12.dp.toPx(), topY), Offset(wellX + 12.dp.toPx(), topY), 1.5f)
                drawLine(Amber, Offset(wellX - 12.dp.toPx(), baseY), Offset(wellX + 12.dp.toPx(), baseY), 1.5f)
                label("$gasText ${depthText(snapshot.influxTopTvdM, units, 0)}", wellX + 14.dp.toPx(), topY, Amber, 7.dp.toPx())
                label(depthText(snapshot.influxBaseTvdM, units, 0), wellX + 14.dp.toPx(), baseY + 8.dp.toPx(), Amber, 7.dp.toPx())
            }

            // Bit.
            val bitY = yForTvd(snapshot.bitTvdM)
            drawCircle(Teal, 3.dp.toPx(), Offset(wellX, bitY))
            label("BIT", wellX + 8.dp.toPx(), bitY + 3.dp.toPx(), Teal, 7.dp.toPx())

            if (snapshot.influxM3 <= 0.001) {
                label(emptyText, axisX + 10.dp.toPx(), (top + bottom) / 2f, Muted, 9.dp.toPx())
            }
        }
    }
}

@Composable
fun TrendChart(
    trends: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    Box(modifier.background(Panel2, RoundedCornerShape(10.dp)).padding(8.dp)) {
        val chartTrends = trends.takeLast(240)
        if (chartTrends.size < 2) {
            Text(
                stringResource(R.string.trends_empty),
                color = Muted,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val left = 4.dp.toPx()
                val right = size.width - 4.dp.toPx()
                val top = 4.dp.toPx()
                val bottom = size.height - 4.dp.toPx()
                val t0 = chartTrends.first().timeSec
                val t1 = max(chartTrends.last().timeSec, t0 + 1.0)
                for (i in 0..4) {
                    val y = top + (bottom - top) * i / 4f
                    drawLine(Panel3, Offset(left, y), Offset(right, y), 1f)
                }
                fun x(t: Double): Float = (left + ((t - t0) / (t1 - t0)).toFloat() * (right - left))
                fun y(value: Double, maxValue: Double): Float =
                    bottom - (value / maxValue.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) * (bottom - top)

                val maxPressure = max(
                    chartTrends.maxOf { it.sppKPa },
                    chartTrends.maxOf { it.casingKPa }
                ).coerceAtLeast(1000.0)
                val maxPit = chartTrends.maxOf { it.pitM3 }.coerceAtLeast(1.0)
                val maxChoke = 100.0

                fun line(color: Color, values: List<Pair<Double, Double>>, maxV: Double) {
                    var prev: Offset? = null
                    values.forEach { (t, v) ->
                        val point = Offset(x(t), y(v, maxV))
                        prev?.let { drawLine(color, it, point, 2.dp.toPx()) }
                        prev = point
                    }
                }
                line(Teal, chartTrends.map { it.timeSec to it.sppKPa }, maxPressure)
                line(Amber, chartTrends.map { it.timeSec to it.casingKPa }, maxPressure)
                line(Blue, chartTrends.map { it.timeSec to it.pitM3 }, maxPit)
                line(Purple, chartTrends.map { it.timeSec to it.chokePct }, maxChoke)
            }
            Row(
                Modifier.align(Alignment.TopStart).padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LegendDot("SPP", Teal)
                LegendDot("CASING", Amber)
                LegendDot("PIT", Blue)
                LegendDot("CHOKE", Purple)
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(5.dp).background(color, RoundedCornerShape(50)))
        Spacer(Modifier.width(3.dp))
        Text(label, color = Muted, fontSize = 8.sp)
    }
}
