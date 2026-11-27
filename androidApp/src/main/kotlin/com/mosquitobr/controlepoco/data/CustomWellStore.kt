package com.mosquitobr.controlepoco.data

import android.content.Context
import com.mosquitobr.controlepoco.core.CasingSpec
import com.mosquitobr.controlepoco.core.CustomWellSpec
import com.mosquitobr.controlepoco.core.DrillStringSpec
import com.mosquitobr.controlepoco.core.InfluxType
import com.mosquitobr.controlepoco.core.KickTriggerMode
import com.mosquitobr.controlepoco.core.SurfaceType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistência local dos poços criados no editor. Nada sai do aparelho.
 */
object CustomWellStore {
    private const val FILE = "controle_poco_custom_wells"
    private const val KEY_WELLS = "wells"
    private const val SCHEMA_VERSION = 2

    fun load(context: Context): List<CustomWellSpec> {
        val raw = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_WELLS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                runCatching { decode(array.getJSONObject(index)) }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, wells: List<CustomWellSpec>) {
        val array = JSONArray()
        wells.forEach { array.put(encode(it)) }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WELLS, array.toString())
            .apply()
    }

    fun upsert(context: Context, well: CustomWellSpec): List<CustomWellSpec> {
        val current = load(context).filterNot { it.id == well.id } + well
        save(context, current)
        return current
    }

    fun delete(context: Context, id: String): List<CustomWellSpec> {
        val current = load(context).filterNot { it.id == id }
        save(context, current)
        return current
    }

    private fun JSONObject.putNullable(key: String, value: Double?) {
        put(key, value ?: JSONObject.NULL)
    }

    private fun encode(spec: CustomWellSpec): JSONObject = JSONObject().apply {
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
        putNullable("initialBitDepth", spec.initialBitDepthMdM)
        putNullable("kickDepth", spec.kickDepthMdM)
        putNullable("kickIntensity", spec.kickIntensityKPa)
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
        put("customCasings", JSONArray().apply {
            spec.customCasings.forEach { casing ->
                put(JSONObject().apply {
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
        put("drillStringSpec", spec.drillStringSpec?.let { ds ->
            JSONObject().apply {
                put("drillPipeOdIn", ds.drillPipeOdIn)
                put("drillPipeIdIn", ds.drillPipeIdIn)
                put("hwdpOdIn", ds.hwdpOdIn)
                put("hwdpIdIn", ds.hwdpIdIn)
                put("drillCollarOdIn", ds.drillCollarOdIn)
                put("drillCollarIdIn", ds.drillCollarIdIn)
                put("bhaLengthM", ds.bhaLengthM)
                put("bitTfaIn2", ds.bitTfaIn2)
            }
        } ?: JSONObject.NULL)
    }

    private fun JSONObject.nullableDouble(key: String): Double? =
        if (has(key) && !isNull(key)) optDouble(key) else null

    private fun decode(json: JSONObject): CustomWellSpec = CustomWellSpec(
        id = json.getString("id"),
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
        customCasings = json.optJSONArray("customCasings")?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                runCatching {
                    val casing = array.getJSONObject(index)
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
            }
        }.orEmpty(),
        drillStringSpec = json.optJSONObject("drillStringSpec")?.let { ds ->
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
