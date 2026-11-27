package com.mosquitobr.controlepoco.data

import com.mosquitobr.controlepoco.core.CasingSpec
import com.mosquitobr.controlepoco.core.CustomWellSpec
import com.mosquitobr.controlepoco.core.DrillStringSpec
import com.mosquitobr.controlepoco.core.InfluxType
import com.mosquitobr.controlepoco.core.KickTriggerMode
import com.mosquitobr.controlepoco.core.SurfaceType
import com.mosquitobr.controlepoco.platform.AppPaths
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File

/**
 * Persistência local em JSON dos poços criados no editor. 100% offline.
 */
object CustomWellStore {
    private const val FILE_NAME = "custom_wells.json"
    private const val SCHEMA_VERSION = 2
    private val prettyJson = Json { prettyPrint = true }

    private fun storeFile(): File = File(AppPaths.dataDir(), FILE_NAME)

    fun load(): List<CustomWellSpec> {
        val file = storeFile()
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = prettyJson.parseToJsonElement(file.readText(Charsets.UTF_8)).jsonArray
            array.mapNotNull { elem ->
                runCatching { decode(elem.jsonObject) }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    fun save(wells: List<CustomWellSpec>) {
        val array = buildJsonArray {
            wells.forEach { add(encode(it)) }
        }
        val file = storeFile()
        com.mosquitobr.controlepoco.platform.AtomicFileWriter.writeTextAtomically(
            file,
            prettyJson.encodeToString(JsonArray.serializer(), array)
        )
    }

    fun upsert(well: CustomWellSpec): List<CustomWellSpec> {
        val current = load().filterNot { it.id == well.id } + well
        save(current)
        return current
    }

    fun delete(id: String): List<CustomWellSpec> {
        val current = load().filterNot { it.id == id }
        save(current)
        return current
    }

    private fun encode(spec: CustomWellSpec): JsonObject = buildJsonObject {
        put("schemaVersion", SCHEMA_VERSION)
        put("id", spec.id)
        put("name", spec.name)
        put("surfaceType", spec.surfaceType.name)
        put("tvd", spec.tvdM)
        put("md", spec.mdM)
        put("shoe", spec.shoeTvdM)
        put("water", spec.waterDepthM)
        put("inclination", spec.inclinationDeg)
        put("combined", spec.combinedCasing)
        put("pore", spec.poreGradientKPaPerM)
        put("frac", spec.fracGradientKPaPerM)
        put("mudPpg", spec.mudPpg)
        put("killMarginPpg", spec.killMarginPpg)
        put("kickEnabled", spec.kickEnabled)
        put("kickType", spec.kickType.name)
        put("kickVolume", spec.kickVolumeM3)
        put("kickMode", spec.kickTriggerMode.name)
        put("triggerAt", spec.triggerAtSec)
        put("zoneTop", spec.zoneTopMdM)
        put("zoneBase", spec.zoneBaseMdM)
        put("triggerDepth", spec.triggerDepthMdM)
        spec.initialBitDepthMdM?.let { put("initialBitDepth", it) } ?: put("initialBitDepth", JsonNull)
        spec.kickDepthMdM?.let { put("kickDepth", it) } ?: put("kickDepth", JsonNull)
        spec.kickIntensityKPa?.let { put("kickIntensity", it) } ?: put("kickIntensity", JsonNull)
        put("initialSpm", spec.initialSpm)
        put("openHoleDiameterIn", spec.openHoleDiameterIn)
        put("killLineIdIn", spec.killLineIdIn)
        put("chokeLineIdIn", spec.chokeLineIdIn)
        put("boosterLineIdIn", spec.boosterLineIdIn)
        put("killLineLengthM", spec.killLineLengthM)
        put("chokeLineLengthM", spec.chokeLineLengthM)
        put("boosterLineLengthM", spec.boosterLineLengthM)
        put("riserIdIn", spec.riserIdIn)
        put("riserOdIn", spec.riserOdIn)
        put("tempSurfaceC", spec.tempSurfaceC)
        put("tempBottomC", spec.tempBottomC)
        put("seaSurfaceTempC", spec.seaSurfaceTempC)
        put("seaMudlineTempC", spec.seaMudlineTempC)
        put("customCasings", buildJsonArray {
            spec.customCasings.forEach { casing ->
                add(buildJsonObject {
                    put("name", casing.name)
                    put("topMd", casing.topMdM)
                    put("bottomMd", casing.bottomMdM)
                    put("idIn", casing.idIn)
                    put("odIn", casing.odIn)
                    put("holeDiameterIn", casing.holeDiameterIn)
                    put("shoeTvd", casing.shoeTvdM)
                })
            }
        })
        val dsObj = spec.drillStringSpec?.let { ds ->
            buildJsonObject {
                put("drillPipeOdIn", ds.drillPipeOdIn)
                put("drillPipeIdIn", ds.drillPipeIdIn)
                put("hwdpOdIn", ds.hwdpOdIn)
                put("hwdpIdIn", ds.hwdpIdIn)
                put("drillCollarOdIn", ds.drillCollarOdIn)
                put("drillCollarIdIn", ds.drillCollarIdIn)
                put("bhaLengthM", ds.bhaLengthM)
                put("bitTfaIn2", ds.bitTfaIn2)
            }
        }
        if (dsObj != null) put("drillStringSpec", dsObj) else put("drillStringSpec", JsonNull)
    }

    private fun JsonObject.optString(key: String, default: String): String =
        this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull ?: default

    private fun JsonObject.optBoolean(key: String, default: Boolean): Boolean =
        this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.booleanOrNull ?: default

    private fun JsonObject.optDouble(key: String, default: Double): Double =
        this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.doubleOrNull ?: default

    private fun JsonObject.nullableDouble(key: String): Double? =
        this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.doubleOrNull

    private fun decode(json: JsonObject): CustomWellSpec = CustomWellSpec(
        id = json.getValue("id").jsonPrimitive.content,
        name = json.optString("name", "Poço personalizado"),
        surfaceType = runCatching {
            SurfaceType.valueOf(json.optString("surfaceType", SurfaceType.LAND.name))
        }.getOrDefault(SurfaceType.LAND),
        tvdM = json.optDouble("tvd", 3000.0),
        mdM = json.optDouble("md", 3000.0),
        shoeTvdM = json.optDouble("shoe", 2000.0),
        waterDepthM = json.optDouble("water", 0.0),
        inclinationDeg = json.optDouble("inclination", 0.0),
        combinedCasing = json.optBoolean("combined", false),
        poreGradientKPaPerM = json.optDouble("pore", 11.5),
        fracGradientKPaPerM = json.optDouble("frac", 18.0),
        mudPpg = json.optDouble("mudPpg", 9.6),
        killMarginPpg = json.optDouble("killMarginPpg", 0.1),
        kickEnabled = json.optBoolean("kickEnabled", true),
        kickType = runCatching {
            InfluxType.valueOf(json.optString("kickType", InfluxType.GAS.name))
        }.getOrDefault(InfluxType.GAS),
        kickVolumeM3 = json.optDouble("kickVolume", 2.5),
        kickTriggerMode = runCatching {
            KickTriggerMode.valueOf(json.optString("kickMode", KickTriggerMode.TIME.name))
        }.getOrDefault(KickTriggerMode.TIME),
        triggerAtSec = json.optDouble("triggerAt", 20.0),
        zoneTopMdM = json.optDouble("zoneTop", 0.0),
        zoneBaseMdM = json.optDouble("zoneBase", 0.0),
        triggerDepthMdM = json.optDouble("triggerDepth", 0.0),
        initialBitDepthMdM = json.nullableDouble("initialBitDepth"),
        kickDepthMdM = json.nullableDouble("kickDepth"),
        kickIntensityKPa = json.nullableDouble("kickIntensity"),
        initialSpm = json.optDouble("initialSpm", 55.0),
        openHoleDiameterIn = json.optDouble("openHoleDiameterIn", 8.5),
        killLineIdIn = json.optDouble("killLineIdIn", 3.0),
        chokeLineIdIn = json.optDouble("chokeLineIdIn", 3.0),
        boosterLineIdIn = json.optDouble("boosterLineIdIn", 3.0),
        killLineLengthM = json.optDouble("killLineLengthM", 0.0),
        chokeLineLengthM = json.optDouble("chokeLineLengthM", 0.0),
        boosterLineLengthM = json.optDouble("boosterLineLengthM", 0.0),
        riserIdIn = json.optDouble("riserIdIn", 19.25),
        riserOdIn = json.optDouble("riserOdIn", 21.0),
        tempSurfaceC = json.optDouble("tempSurfaceC", 24.0),
        tempBottomC = json.optDouble("tempBottomC", 90.0),
        seaSurfaceTempC = json.optDouble("seaSurfaceTempC", 24.0),
        seaMudlineTempC = json.optDouble("seaMudlineTempC", 4.0),
        customCasings = (json["customCasings"] as? JsonArray)?.mapNotNull { elem ->
            runCatching {
                val casing = elem.jsonObject
                CasingSpec(
                    name = casing.optString("name", "Revestimento"),
                    topMdM = casing.optDouble("topMd", 0.0),
                    bottomMdM = casing.optDouble("bottomMd", 0.0),
                    idIn = casing.optDouble("idIn", 8.681),
                    odIn = casing.optDouble("odIn", 9.625),
                    holeDiameterIn = casing.optDouble("holeDiameterIn", 12.25),
                    shoeTvdM = casing.optDouble("shoeTvd", 0.0)
                )
            }.getOrNull()
        }.orEmpty(),
        drillStringSpec = (json["drillStringSpec"] as? JsonObject)?.let { ds ->
            DrillStringSpec(
                drillPipeOdIn = ds.optDouble("drillPipeOdIn", 5.0),
                drillPipeIdIn = ds.optDouble("drillPipeIdIn", 4.276),
                hwdpOdIn = ds.optDouble("hwdpOdIn", 5.0),
                hwdpIdIn = ds.optDouble("hwdpIdIn", 3.0),
                drillCollarOdIn = ds.optDouble("drillCollarOdIn", 6.5),
                drillCollarIdIn = ds.optDouble("drillCollarIdIn", 2.25),
                bhaLengthM = ds.optDouble("bhaLengthM", 180.0),
                bitTfaIn2 = ds.optDouble("bitTfaIn2", 0.48)
            )
        }
    )
}
