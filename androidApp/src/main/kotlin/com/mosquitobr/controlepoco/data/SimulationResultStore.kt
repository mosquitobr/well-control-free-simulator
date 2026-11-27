package com.mosquitobr.controlepoco.data

import android.content.Context
import com.mosquitobr.controlepoco.core.Scenario
import com.mosquitobr.controlepoco.core.SimSnapshot
import com.mosquitobr.controlepoco.core.Units
import org.json.JSONArray
import org.json.JSONObject

data class SimulationResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val scenarioId: String,
    val scenarioName: String,
    val score: Int,
    val phase: String,
    val timeSec: Double,
    val bitDepthM: Double,
    val initialMudPpg: Double,
    val finalMudPpg: Double,
    val maxCasingPsi: Double,
    val maxShoePsi: Double,
    val maxPitGainBbl: Double,
    val totalStrokes: Double,
    val milestones: List<String>,
    val kickDetected: Boolean,
    val gasReachedSurface: Boolean,
    val wellDead: Boolean,
    /** SIDPP e SICP congelados no momento da estabilização do shut-in. */
    val stabilizedSidppKPa: Double = 0.0,
    val stabilizedSicpKPa: Double = 0.0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("timestamp", timestamp)
        put("scenarioId", scenarioId)
        put("scenarioName", scenarioName)
        put("score", score)
        put("phase", phase)
        put("timeSec", timeSec)
        put("bitDepthM", bitDepthM)
        put("initialMudPpg", initialMudPpg)
        put("finalMudPpg", finalMudPpg)
        put("maxCasingPsi", maxCasingPsi)
        put("maxShoePsi", maxShoePsi)
        put("maxPitGainBbl", maxPitGainBbl)
        put("totalStrokes", totalStrokes)
        put("milestones", JSONArray(milestones))
        put("kickDetected", kickDetected)
        put("gasReachedSurface", gasReachedSurface)
        put("wellDead", wellDead)
        put("stabilizedSidppKPa", stabilizedSidppKPa)
        put("stabilizedSicpKPa", stabilizedSicpKPa)
    }

    companion object {
        fun fromJson(json: JSONObject): SimulationResult {
            val milestones = mutableListOf<String>()
            val array = json.getJSONArray("milestones")
            for (i in 0 until array.length()) {
                milestones.add(array.getString(i))
            }
            return SimulationResult(
                id = json.getString("id"),
                timestamp = json.getLong("timestamp"),
                scenarioId = json.getString("scenarioId"),
                scenarioName = json.getString("scenarioName"),
                score = json.getInt("score"),
                phase = json.getString("phase"),
                timeSec = json.getDouble("timeSec"),
                bitDepthM = json.getDouble("bitDepthM"),
                initialMudPpg = json.getDouble("initialMudPpg"),
                finalMudPpg = json.getDouble("finalMudPpg"),
                maxCasingPsi = json.getDouble("maxCasingPsi"),
                maxShoePsi = json.getDouble("maxShoePsi"),
                maxPitGainBbl = json.getDouble("maxPitGainBbl"),
                totalStrokes = json.getDouble("totalStrokes"),
                milestones = milestones,
                kickDetected = json.getBoolean("kickDetected"),
                gasReachedSurface = json.getBoolean("gasReachedSurface"),
                wellDead = json.getBoolean("wellDead"),
                stabilizedSidppKPa = json.getDouble("stabilizedSidppKPa"),
                stabilizedSicpKPa = json.getDouble("stabilizedSicpKPa")
            )
        }
    }
}

object SimulationResultStore {
    private const val FILE_NAME = "simulation_results.json"
    private const val MAX_RESULTS = 50

    fun load(context: Context): MutableList<SimulationResult> {
        val file = context.getFileStreamPath(FILE_NAME)
        if (!file.exists()) return mutableListOf<SimulationResult>()
        return try {
            val content = file.readText()
            val array = JSONArray(content)
            val results = mutableListOf<SimulationResult>()
            (0 until array.length()).forEach { index ->
                results.add(SimulationResult.fromJson(array.getJSONObject(index)))
            }
            results.sortedByDescending { it.timestamp }.toMutableList()
        } catch (e: Exception) {
            mutableListOf<SimulationResult>()
        }
    }

    fun save(context: Context, result: SimulationResult) {
        var results = load(context)
        results.add(0, result)
        if (results.size > MAX_RESULTS) {
            results = results.take(MAX_RESULTS).toMutableList()
        }
        val array = JSONArray(results.map { it.toJson() })
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use { it.write(array.toString(2).toByteArray()) }
    }

    fun delete(context: Context, id: String) {
        var results = load(context)
        results = results.filter { it.id != id }.toMutableList()
        val array = JSONArray(results.map { it.toJson() })
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use { it.write(array.toString(2).toByteArray()) }
    }

    fun clear(context: Context) {
        context.deleteFile(FILE_NAME)
    }

    fun createFromSnapshot(
        scenario: Scenario,
        snapshot: SimSnapshot,
        initialMudPpg: Double
    ): SimulationResult {
        return SimulationResult(
            scenarioId = scenario.id,
            scenarioName = scenario.title("pt"),
            score = snapshot.score,
            phase = snapshot.phase.name,
            timeSec = snapshot.timeSec,
            bitDepthM = snapshot.bitDepthM,
            initialMudPpg = initialMudPpg,
            finalMudPpg = Units.kgM3ToPpg(snapshot.mudDensityKgM3),
            maxCasingPsi = Units.kpaToPsi(snapshot.maxCasingKPa),
            maxShoePsi = Units.kpaToPsi(snapshot.maxShoeKPa),
            maxPitGainBbl = Units.m3ToBbl(snapshot.maxPitGainM3),
            totalStrokes = snapshot.totalStrokes,
            milestones = snapshot.milestones,
            kickDetected = snapshot.kickDetected,
            gasReachedSurface = snapshot.gasReachedSurface,
            wellDead = snapshot.wellDead,
            stabilizedSidppKPa = snapshot.stabilizedSidppKPa,
            stabilizedSicpKPa = snapshot.stabilizedSicpKPa
        )
    }

    /**
     * Empacota um ou mais resultados em um JSON autoexplicativo para exportar
     * pelo Storage Access Framework (o usuário escolhe pasta e nome).
     */
    fun exportJson(results: List<SimulationResult>): String {
        val root = JSONObject().apply {
            put("app", "Controle de Poço")
            put("formatVersion", 1)
            put("exportedAt", System.currentTimeMillis())
            put("results", JSONArray(results.map { it.toJson() }))
        }
        return root.toString(2)
    }

    /** Nome sugerido para o arquivo exportado, com data e hora locais. */
    fun exportFileName(): String {
        val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US)
            .format(java.util.Date())
        return "controle-poco-resultados-$stamp.json"
    }
}