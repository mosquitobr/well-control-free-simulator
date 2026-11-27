package com.mosquitobr.controlepoco.platform

import com.mosquitobr.controlepoco.core.CustomWellSpec
import com.mosquitobr.controlepoco.core.InfluxType
import com.mosquitobr.controlepoco.core.KickTriggerMode
import com.mosquitobr.controlepoco.core.Scenarios
import com.mosquitobr.controlepoco.core.Simulator
import com.mosquitobr.controlepoco.core.SurfaceType
import com.mosquitobr.controlepoco.core.UnitSystem
import com.mosquitobr.controlepoco.data.AppSettings
import com.mosquitobr.controlepoco.data.CustomWellStore
import com.mosquitobr.controlepoco.data.SettingsStore
import com.mosquitobr.controlepoco.data.SimulationResultStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DesktopStoresTest {
    private lateinit var tempRoot: File

    @Before
    fun setUp() {
        tempRoot = Files.createTempDirectory("well-control-desktop-test").toFile()
        AppPaths.overrideRootDir = tempRoot
    }

    @After
    fun tearDown() {
        AppPaths.overrideRootDir = null
        tempRoot.deleteRecursively()
    }

    @Test
    fun `settings store persiste e recarrega configuracoes`() {
        val expected = AppSettings(
            units = UnitSystem.SI,
            language = "en",
            sound = false,
            haptics = false,
            volume = 0.45f,
            soundPump = true,
            soundRotary = false,
            soundWeight = true,
            soundGas = false
        )
        SettingsStore.save(expected)
        val loaded = SettingsStore.load()
        assertEquals(expected.units, loaded.units)
        assertEquals(expected.language, loaded.language)
        assertEquals(expected.sound, loaded.sound)
        assertEquals(expected.haptics, loaded.haptics)
        assertEquals(expected.volume, loaded.volume, 0.001f)
        assertEquals(expected.soundRotary, loaded.soundRotary)
        assertEquals(expected.soundGas, loaded.soundGas)
    }

    @Test
    fun `custom well store faz upsert e delete com fidelidade`() {
        val spec = CustomWellSpec(
            id = "custom:test-well",
            name = "Poço Pré-Sal Teste",
            surfaceType = SurfaceType.SUBSEA,
            tvdM = 4800.0,
            mdM = 5100.0,
            shoeTvdM = 3600.0,
            waterDepthM = 2100.0,
            mudPpg = 10.4,
            killMarginPpg = 0.2,
            kickType = InfluxType.GAS,
            kickTriggerMode = KickTriggerMode.DEPTH
        )
        val afterUpsert = CustomWellStore.upsert(spec)
        assertEquals(1, afterUpsert.size)
        val loaded = CustomWellStore.load().single()
        assertEquals("Poço Pré-Sal Teste", loaded.name)
        assertEquals(SurfaceType.SUBSEA, loaded.surfaceType)
        assertEquals(2100.0, loaded.waterDepthM, 0.01)
        assertEquals(0.2, loaded.killMarginPpg, 0.001)

        val afterDelete = CustomWellStore.delete(spec.id)
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun `simulation result store salva exporta json e exclui corretamente`() {
        val scenario = Scenarios.byId("drillers-surface")
        val sim = Simulator(scenario)
        sim.advance(2.0)
        val result = SimulationResultStore.createFromSnapshot(scenario, sim.snapshot(), 9.6)
        SimulationResultStore.save(result)

        val loaded = SimulationResultStore.load()
        assertEquals(1, loaded.size)
        assertEquals(result.id, loaded.first().id)

        val exported = SimulationResultStore.exportJson(loaded)
        assertTrue(exported.contains("Controle de Poço 2 Desktop"))
        assertTrue(exported.contains(scenario.id))

        SimulationResultStore.delete(result.id)
        assertTrue(SimulationResultStore.load().isEmpty())
    }
}
