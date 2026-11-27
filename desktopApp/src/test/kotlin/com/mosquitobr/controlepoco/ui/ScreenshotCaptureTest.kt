package com.mosquitobr.controlepoco.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import com.mosquitobr.controlepoco.core.BopElement
import com.mosquitobr.controlepoco.core.Scenarios
import com.mosquitobr.controlepoco.core.Simulator
import com.mosquitobr.controlepoco.core.UnitSystem
import com.mosquitobr.controlepoco.data.AppSettings
import com.mosquitobr.controlepoco.platform.AppPaths
import com.mosquitobr.controlepoco.platform.LocalAppStringsLanguage
import org.jetbrains.skia.EncodedImageFormat
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Deterministic Compose Desktop screenshot generator (runs only with -DcaptureScreenshots=true).
 * Uses an isolated temp directory so no personal user configuration is ever accessed.
 */
class ScreenshotCaptureTest {

    private fun renderPng(
        outFile: File,
        lang: String = "en",
        width: Int = 1360,
        height: Int = 860,
        content: @Composable () -> Unit
    ) {
        val scene = ImageComposeScene(
            width = width,
            height = height,
            density = Density(1f)
        ) {
            ControlePocoTheme {
                CompositionLocalProvider(
                    LocalAppLanguage provides lang,
                    LocalAppStringsLanguage provides lang
                ) {
                    Box(Modifier.fillMaxSize().background(Bg)) {
                        content()
                    }
                }
            }
        }
        try {
            // Render multiple frames so layout, ScrollableTabRow, and animations settle cleanly
            scene.render(0L)
            val image = scene.render(100_000_000L)
            val encoded = image.encodeToData(EncodedImageFormat.PNG)
                ?: error("Failed to encode PNG for ${outFile.name}")
            outFile.parentFile?.mkdirs()
            outFile.writeBytes(encoded.bytes)
        } finally {
            scene.close()
        }
    }

    @Test
    fun captureDocumentationScreenshots() {
        assumeTrue(
            "Skipped unless -DcaptureScreenshots=true",
            System.getProperty("captureScreenshots") == "true"
        )

        val outDir = File(System.getProperty("screenshotOutDir", "../docs/screenshots"))
        val tempRoot = Files.createTempDirectory("well-control-screenshots").toFile()
        AppPaths.overrideRootDir = tempRoot
        try {
            val settingsEn = AppSettings(
                units = UnitSystem.PETROBRAS,
                language = "en",
                sound = false,
                haptics = false
            )

            // 1. Home & 19 Scenarios Catalog
            renderPng(File(outDir, "01-scenarios-home.png"), lang = "en") {
                HomeScreen(
                    settings = settingsEn,
                    customWells = emptyList(),
                    onStart = {},
                    onStartCustom = {},
                    onNewWell = {},
                    onEditWell = {},
                    onDeleteWell = {},
                    onSettings = {},
                    onAbout = {},
                    onAcademy = {},
                    onResults = {}
                )
            }

            // Prepare a realistic live kick + shut-in scenario in English
            val drillers = Scenarios.byId("drillers-surface")
            val sim = Simulator(drillers, initialLanguage = "en")
            // Drill briefly, trigger & detect gas kick, stop pump, close Annular BOP, stabilize
            repeat(14) { sim.advance(2.0) }
            sim.setPump(false, 0.0)
            sim.closeBop(BopElement.ANNULAR)
            repeat(10) { sim.advance(2.0) }

            // 2. Active Well Control Simulation (Schematic + Choke & Pressure Gauges)
            renderPng(File(outDir, "02-simulation-kick-shutin.png"), lang = "en") {
                SimulatorScreen(
                    scenario = drillers,
                    settings = settingsEn,
                    onBack = {},
                    onHome = {},
                    initialSimulator = sim,
                    initialStarted = true,
                    initialTab = 4 // Choke panel (SIDPP, SICP, SPP gauges + choke control)
                )
            }

            // 3. Real-Time Trends & Metrics Panel (pressure & pit gain history curves)
            renderPng(File(outDir, "03-trends-metrics.png"), lang = "en") {
                SimulatorScreen(
                    scenario = drillers,
                    settings = settingsEn,
                    onBack = {},
                    onHome = {},
                    initialSimulator = sim,
                    initialStarted = true,
                    initialTab = 7 // Trends panel
                )
            }

            // 4. Kill Sheet Calculator (KMW, ICP, FCP, MAASP & Circulation Schedule)
            renderPng(File(outDir, "04-kill-sheet.png"), lang = "en") {
                KillSheetScreen(
                    scenario = drillers,
                    snapshot = sim.snapshot(),
                    result = sim.killSheetResult(),
                    units = settingsEn.units,
                    onBack = {},
                    onApply = {}
                )
            }
        } finally {
            AppPaths.overrideRootDir = null
            tempRoot.deleteRecursively()
        }
    }
}
