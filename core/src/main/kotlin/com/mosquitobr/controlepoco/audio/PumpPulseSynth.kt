package com.mosquitobr.controlepoco.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Síntese percussiva da bomba tripla.
 *
 * A fundamental de uma bomba a 45 SPM é 0,75 Hz — praticamente infrassom.
 * Em vez de tentar ouvir essa frequência, cada stroke dispara um transiente
 * curto com ressonâncias em 82, 147 e 219 Hz, que o alto-falante do celular
 * reproduz com clareza. O resultado lembra o "toc-toc-toc" de uma bomba
 * de lama real, sem arquivos de áudio.
 */
class PumpPulseSynth(sampleRate: Int) {
    private val sampleRateD = sampleRate.toDouble().coerceAtLeast(8_000.0)

    private var timeSec = 0.0
    private var strokePhase = 0.0
    private var timeSinceStroke = 10.0
    private var phaseLow = 0.0
    private var phaseMid = 0.0
    private var phaseHigh = 0.0
    private var noise = 0x51A7C3

    fun reset() {
        timeSec = 0.0
        strokePhase = 0.0
        timeSinceStroke = 10.0
        phaseLow = 0.0
        phaseMid = 0.0
        phaseHigh = 0.0
        noise = 0x51A7C3
    }

    /**
     * Retorna uma amostra normalizada (-1..1) para a rotação informada.
     * A intensidade geral é aplicada pelo mixer do [RigAudioEngine].
     */
    fun next(spm: Double): Double {
        val safeSpm = if (spm.isFinite()) spm.coerceIn(0.0, 240.0) else 0.0
        if (safeSpm < 0.5) {
            strokePhase = 0.0
            timeSinceStroke = 10.0
            return 0.0
        }
        val strokesPerSecond = safeSpm / 60.0
        val dt = 1.0 / sampleRateD

        strokePhase += strokesPerSecond * dt
        if (strokePhase >= 1.0) {
            strokePhase -= 1.0
            timeSinceStroke = 0.0
            phaseLow = 0.0
            phaseMid = 0.0
            phaseHigh = 0.0
        } else {
            timeSinceStroke += dt
        }
        timeSec += dt

        // Ataque curto + decaimento rápido: cada stroke é um golpe seco.
        val attack = min(1.0, timeSinceStroke / 0.0035)
        val decay = exp(-timeSinceStroke / 0.085)
        val envelope = attack * decay

        phaseLow += 2.0 * PI * 82.0 * dt
        phaseMid += 2.0 * PI * 147.0 * dt
        phaseHigh += 2.0 * PI * 219.0 * dt
        val ring = 0.58 * sin(phaseLow) +
            0.30 * sin(phaseMid + 0.35) +
            0.12 * sin(phaseHigh + 0.9)

        noise = noise * 1103515245 + 12345
        val white = ((noise shr 16) and 0x7fff) / 16384.0 - 1.0
        val click = if (timeSinceStroke < 0.006) {
            white * (1.0 - timeSinceStroke / 0.006) * 0.32
        } else 0.0

        // Ruído mecânico contínuo bem baixo, para a bomba não "sumir" entre strokes.
        val hum = 0.055 * sin(2.0 * PI * 55.0 * timeSec) +
            0.025 * sin(2.0 * PI * 110.0 * timeSec + 0.7)

        return (envelope * ring + click + hum * (0.35 + 0.65 * envelope))
            .coerceIn(-1.0, 1.0)
    }
}
