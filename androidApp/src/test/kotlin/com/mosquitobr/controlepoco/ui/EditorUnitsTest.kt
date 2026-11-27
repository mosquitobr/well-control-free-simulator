package com.mosquitobr.controlepoco.ui

import com.mosquitobr.controlepoco.core.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorUnitsTest {
    @Test
    fun `profundidade converte entre metros e pes`() {
        assertEquals("3280.8", depthInput(1000.0, UnitSystem.US_FIELD, 1))
        assertEquals(1000.0, parseDepthInput("3280.8", UnitSystem.US_FIELD) ?: 0.0, 0.05)
        assertEquals(1000.5, parseDepthInput("1000,5", UnitSystem.PETROBRAS) ?: 0.0, 0.001)
    }

    @Test
    fun `gradiente petrobras usa psi por metro e volta para kpa por metro`() {
        val shown = gradientInput(11.5, UnitSystem.PETROBRAS)
        assertEquals(1.668, shown.toDouble(), 0.01)
        assertEquals(11.5, parseGradientInput(shown, UnitSystem.PETROBRAS) ?: 0.0, 0.01)
    }

    @Test
    fun `gradiente de campo usa psi por pe e volta para kpa por metro`() {
        val shown = gradientInput(11.5, UnitSystem.US_FIELD)
        assertEquals(0.508, shown.toDouble(), 0.01)
        assertEquals(11.5, parseGradientInput(shown, UnitSystem.US_FIELD) ?: 0.0, 0.01)
    }

    @Test
    fun `volume e densidade fazem round trip`() {
        val bbl = volumeInput(2.5, UnitSystem.PETROBRAS)
        assertEquals(2.5, parseVolumeInput(bbl, UnitSystem.PETROBRAS) ?: 0.0, 0.01)
        val kgM3 = mudWeightInput(9.6, UnitSystem.SI)
        assertEquals(9.6, parseMudWeightToPpg(kgM3, UnitSystem.SI) ?: 0.0, 0.01)
    }


    @Test
    fun `densidade equivalente petrobras usa fator 0_1704`() {
        val gradient = parseGradientForMode("10", UnitSystem.PETROBRAS, GradientEntryMode.DENSITY) ?: 0.0
        assertEquals(11.75, gradient, 0.02)
        val psiPerM = gradient * 0.1450377
        assertEquals(10.0, psiPerM / 0.170434, 0.03)
        assertEquals("10", gradientInputForMode(gradient, UnitSystem.PETROBRAS, GradientEntryMode.DENSITY))
    }

    @Test
    fun `densidade equivalente si e campo fazem round trip`() {
        val siGradient = parseGradientForMode("1200", UnitSystem.SI, GradientEntryMode.DENSITY) ?: 0.0
        assertEquals(11.768, siGradient, 0.02)
        assertEquals("1200", gradientInputForMode(siGradient, UnitSystem.SI, GradientEntryMode.DENSITY))

        val usGradient = parseGradientForMode("10", UnitSystem.US_FIELD, GradientEntryMode.DENSITY) ?: 0.0
        assertEquals(11.75, usGradient, 0.02)
        assertEquals("10", gradientInputForMode(usGradient, UnitSystem.US_FIELD, GradientEntryMode.DENSITY))
    }

}
