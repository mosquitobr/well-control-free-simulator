package com.mosquitobr.controlepoco.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosquitobr.controlepoco.R
import com.mosquitobr.controlepoco.core.UnitSystem
import com.mosquitobr.controlepoco.core.Units
import com.mosquitobr.controlepoco.data.SimulationResult
import com.mosquitobr.controlepoco.data.SimulationResultStore
import com.mosquitobr.controlepoco.platform.FileExporter
import com.mosquitobr.controlepoco.platform.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedResultsScreen(
    units: UnitSystem,
    onClose: () -> Unit
) {
    var results by remember { mutableStateOf(SimulationResultStore.load()) }
    var showDeleteDialog by remember { mutableStateOf<SimulationResult?>(null) }
    var exportState by remember { mutableStateOf(0) } // 0 = idle, 1 = ok, 2 = erro
    val scope = rememberCoroutineScope()

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    fun exportAll() {
        val snapshot = results.toList()
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    FileExporter.saveJson(
                        SimulationResultStore.exportFileName(),
                        SimulationResultStore.exportJson(snapshot)
                    )
                }
            }
            exportState = when {
                outcome.isFailure -> 2
                outcome.getOrNull() != null -> 1
                else -> 0
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = stringResource(R.string.result_saved_list),
            onBack = onClose,
            action = {
                if (results.isNotEmpty()) {
                    TextButton(onClick = { exportAll() }) {
                        Text(
                            stringResource(R.string.result_export_json),
                            color = Teal,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )

        if (exportState != 0) {
            Text(
                stringResource(
                    if (exportState == 1) R.string.result_export_done
                    else R.string.result_export_failed
                ),
                color = if (exportState == 1) Green else Amber,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Muted, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.no_saved_results), color = Muted, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.id }) { result ->
                    ResultCard(result, dateFormat, units, onDelete = { showDeleteDialog = result })
                }
            }
        }

    }

    // Diálogo de confirmação como sobreposição real (AlertDialog), fora do
    // Column — caso contrário o LazyColumn consome a altura e o diálogo fica
    // invisível, impedindo a exclusão do resultado.
    val pendingDelete = showDeleteDialog
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_result_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.delete_result_confirm,
                        pendingDelete.scenarioName
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    SimulationResultStore.delete(pendingDelete.id)
                    results = SimulationResultStore.load()
                    showDeleteDialog = null
                }) {
                    Text(stringResource(R.string.delete), color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ResultCard(
    result: SimulationResult,
    dateFormat: SimpleDateFormat,
    units: UnitSystem,
    onDelete: () -> Unit
) {
    val date = Date(result.timestamp)
    val dateStr = dateFormat.format(date)
    val verdictColor = if (result.score >= 70 && !result.gasReachedSurface) Green else Amber

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel2)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    result.scenarioName,
                    color = TextMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    dateStr,
                    color = Muted,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    stringResource(R.string.score),
                    result.score.toString(),
                    Modifier.weight(1f),
                    verdictColor
                )
                MetricCard(
                    stringResource(R.string.phase),
                    result.phase,
                    Modifier.weight(1f),
                    TextMain
                )
                MetricCard(
                    stringResource(R.string.time_elapsed),
                    durationText(result.timeSec),
                    Modifier.weight(1f),
                    Purple
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    stringResource(R.string.mud_weight),
                    "${Units.fmt(result.initialMudPpg, 1)} → ${Units.fmt(result.finalMudPpg, 1)} ppg",
                    Modifier.weight(1f),
                    Teal
                )
                MetricCard(
                    "Max Casing",
                    pressureTextFromPsi(result.maxCasingPsi, units),
                    Modifier.weight(1f),
                    Amber
                )
                MetricCard(
                    "Max Sapata",
                    pressureTextFromPsi(result.maxShoePsi, units),
                    Modifier.weight(1f),
                    Red
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    stringResource(R.string.max_pit_gain),
                    volumeTextFromBbl(result.maxPitGainBbl, units),
                    Modifier.weight(1f),
                    Blue
                )
                MetricCard(
                    stringResource(R.string.strokes),
                    Units.fmt(result.totalStrokes, 0),
                    Modifier.weight(1f),
                    Purple
                )
                MetricCard(
                    stringResource(R.string.kick_detected),
                    if (result.kickDetected) "Yes" else "No",
                    Modifier.weight(1f),
                    if (result.kickDetected) Red else Green
                )
            }
            if (result.stabilizedSidppKPa > 0.0 || result.stabilizedSicpKPa > 0.0) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        "SIDPP Est.",
                        pressureText(result.stabilizedSidppKPa, units),
                        Modifier.weight(1f),
                        Teal
                    )
                    MetricCard(
                        "SICP Est.",
                        pressureText(result.stabilizedSicpKPa, units),
                        Modifier.weight(1f),
                        Amber
                    )
                }
            }
            if (result.milestones.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.milestones), color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                result.milestones.takeLast(4).forEach { m ->
                    Text("• $m", color = TextMain, fontSize = 9.sp, modifier = Modifier.fillMaxWidth().padding(top = 1.dp))
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Red)
                }
            }
        }
    }
}

