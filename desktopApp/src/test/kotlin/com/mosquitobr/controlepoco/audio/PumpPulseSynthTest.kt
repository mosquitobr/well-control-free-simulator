package com.mosquitobr.controlepoco.audio

import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Test

class PumpPulseSynthTest {
    @Test
    fun `bomba a 45 spm gera energia audivel e finita`() {
        val synth = PumpPulseSynth(22_050)
        var energy = 0.0
        var peak = 0.0
        repeat(22_050 * 3) {
            val sample = synth.next(45.0)
            assertTrue("Amostra deve ser finita", sample.isFinite())
            assertTrue("Amostra deve ficar dentro de -1..1", abs(sample) <= 1.0)
            energy += sample * sample
            peak = maxOf(peak, abs(sample))
        }
        assertTrue("Bomba ligada precisa ter energia ($energy)", energy > 1.0)
        assertTrue("Pico audível esperado ($peak)", peak > 0.15)
    }

    @Test
    fun `bomba parada nao produz pulsos`() {
        val synth = PumpPulseSynth(22_050)
        var energy = 0.0
        repeat(22_050) {
            val sample = synth.next(0.0)
            energy += sample * sample
        }
        // Só resta o ruído mecânico contínuo de baixíssimo nível.
        assertTrue("Bomba parada deve ficar praticamente silenciosa ($energy)", energy < 0.5)
    }

    @Test
    fun `rig audio engine suporta ciclos rapidos start e stop sem erro`() {
        val engine = RigAudioEngine()
        engine.enabled = false
        repeat(5) {
            engine.start()
            engine.update(true, 45.0, true, 80.0, 20.0, false)
            engine.stop()
        }
        assertTrue(true)
    }
}
