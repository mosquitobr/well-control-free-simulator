package com.mosquitobr.controlepoco.data

import com.mosquitobr.controlepoco.core.Scenario
import com.mosquitobr.controlepoco.core.SimSnapshot
import com.mosquitobr.controlepoco.core.Units
import com.mosquitobr.controlepoco.platform.AppPaths
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class SimulationResult(
    val id: String = UUID.randomUUID().toString(),
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
    fun toJson(): JsonObject = buildJsonObject {
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
        put("milestones", buildJsonArray { milestones.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
        put("kickDetected", kickDetected)
        put("gasReachedSurface", gasReachedSurface)
        put("wellDead", wellDead)
        put("stabilizedSidppKPa", stabilizedSidppKPa)
        put("stabilizedSicpKPa", stabilizedSicpKPa)
    }

    companion object {
        fun fromJson(json: JsonObject): SimulationResult {
            val milestones = (json["milestones"] as? JsonArray)
                ?.map { it.jsonPrimitive.content }
                .orEmpty()
            return SimulationResult(
                id = json.getValue("id").jsonPrimitive.content,
                timestamp = json.getValue("timestamp").jsonPrimitive.long,
                scenarioId = json.getValue("scenarioId").jsonPrimitive.content,
                scenarioName = json.getValue("scenarioName").jsonPrimitive.content,
                score = json.getValue("score").jsonPrimitive.int,
                phase = json.getValue("phase").jsonPrimitive.content,
                timeSec = json.getValue("timeSec").jsonPrimitive.double,
                bitDepthM = json.getValue("bitDepthM").jsonPrimitive.double,
                initialMudPpg = json.getValue("initialMudPpg").jsonPrimitive.double,
                finalMudPpg = json.getValue("finalMudPpg").jsonPrimitive.double,
                maxCasingPsi = json.getValue("maxCasingPsi").jsonPrimitive.double,
                maxShoePsi = json.getValue("maxShoePsi").jsonPrimitive.double,
                maxPitGainBbl = json.getValue("maxPitGainBbl").jsonPrimitive.double,
                totalStrokes = json.getValue("totalStrokes").jsonPrimitive.double,
                milestones = milestones,
                kickDetected = json.getValue("kickDetected").jsonPrimitive.boolean,
                gasReachedSurface = json.getValue("gasReachedSurface").jsonPrimitive.boolean,
                wellDead = json.getValue("wellDead").jsonPrimitive.boolean,
                stabilizedSidppKPa = json["stabilizedSidppKPa"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.doubleOrNull ?: 0.0,
                stabilizedSicpKPa = json["stabilizedSicpKPa"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.doubleOrNull ?: 0.0
            )
        }
    }
}

object SimulationResultStore {
    private const val FILE_NAME = "simulation_results.json"
    private const val MAX_RESULTS = 50
    private val prettyJson = Json { prettyPrint = true }

    private fun storeFile(): File = File(AppPaths.dataDir(), FILE_NAME)

    fun load(): MutableList<SimulationResult> {
        val file = storeFile()
        if (!file.exists()) return mutableListOf()
        return try {
            val content = file.readText(Charsets.UTF_8)
            val array = prettyJson.parseToJsonElement(content).jsonArray
            array.map { SimulationResult.fromJson(it.jsonObject) }
                .sortedByDescending { it.timestamp }
                .toMutableList()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun save(result: SimulationResult) {
        var results = load()
        results.add(0, result)
        if (results.size > MAX_RESULTS) {
            results = results.take(MAX_RESULTS).toMutableList()
        }
        writeAll(results)
    }

    fun delete(id: String) {
        val results = load().filter { it.id != id }
        writeAll(results)
    }

    fun clear() {
        storeFile().delete()
    }

    private fun writeAll(results: List<SimulationResult>) {
        val array = buildJsonArray { results.forEach { add(it.toJson()) } }
        val file = storeFile()
        file.parentFile?.mkdirs()
        file.writeText(prettyJson.encodeToString(JsonArray.serializer(), array), Charsets.UTF_8)
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

    /** Empacota um ou mais resultados em um JSON autoexplicativo para exportação local. */
    fun exportJson(results: List<SimulationResult>): String {
        val root = buildJsonObject {
            put("app", "Controle de Poço 2 Desktop")
            put("formatVersion", 1)
            put("exportedAt", System.currentTimeMillis())
            put("results", buildJsonArray { results.forEach { add(it.toJson()) } })
        }
        return prettyJson.encodeToString(JsonObject.serializer(), root)
    }

    /** Nome sugerido para o arquivo exportado, com data e hora locais. */
    fun exportFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        return "controle-poco-resultados-$stamp.json"
    }
}
