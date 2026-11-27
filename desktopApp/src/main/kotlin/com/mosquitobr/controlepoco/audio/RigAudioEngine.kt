package com.mosquitobr.controlepoco.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Motor de áudio operacional sintetizado em tempo real para Desktop (Windows 11 e Linux/WSLg).
 *
 * Usa Java Sound API (`javax.sound.sampled.SourceDataLine`) em PCM 16-bit signed little-endian,
 * 22.050 Hz mono — compatível com WASAPI/DirectSound no Windows 11 e PulseAudio/ALSA no WSLg.
 * Sem arquivos de áudio externos e com degradação silenciosa quando não há dispositivo de som.
 */
class RigAudioEngine {

    @Volatile var enabled: Boolean = true
    @Volatile var pumpOn: Boolean = false
    @Volatile var pumpSpm: Double = 0.0
    @Volatile var rotaryOn: Boolean = false
    @Volatile var rpm: Double = 0.0
    @Volatile var wobKlb: Double = 0.0
    @Volatile var gasAtSurface: Boolean = false
    @Volatile var volume: Float = 0.8f
    @Volatile var pumpChannel: Boolean = true
    @Volatile var rotaryChannel: Boolean = true
    @Volatile var weightChannel: Boolean = true
    @Volatile var gasChannel: Boolean = true

    @Volatile private var running = false
    @Volatile private var alarmEndMs = 0L
    @Volatile private var thread: Thread? = null
    @Volatile private var line: SourceDataLine? = null

    @Synchronized
    fun start() {
        if (running) return
        running = true
        thread = Thread({ loop() }, "rig-audio-desktop").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY
            start()
        }
    }

    @Synchronized
    fun stop() {
        running = false
        val currentLine = line
        line = null
        runCatching { currentLine?.stop() }
        runCatching { currentLine?.flush() }
        runCatching { currentLine?.close() }
        val currentThread = thread
        thread = null
        runCatching { currentThread?.join(400) }
    }

    fun triggerAlarm() {
        alarmEndMs = System.currentTimeMillis() + 1_600L
    }

    fun update(
        pumpOn: Boolean,
        pumpSpm: Double,
        rotaryOn: Boolean,
        rpm: Double,
        wobKlb: Double,
        gasAtSurface: Boolean
    ) {
        this.pumpOn = pumpOn
        this.pumpSpm = pumpSpm
        this.rotaryOn = rotaryOn
        this.rpm = rpm
        this.wobKlb = wobKlb
        this.gasAtSurface = gasAtSurface
    }

    private fun openAudioLine(sampleRate: Int, bufferBytes: Int): SourceDataLine? {
        val format = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate.toFloat(),
            16,
            1,
            2,
            sampleRate.toFloat(),
            false
        )
        return runCatching {
            val info = DataLine.Info(SourceDataLine::class.java, format)
            if (!AudioSystem.isLineSupported(info)) return null
            (AudioSystem.getLine(info) as SourceDataLine).apply {
                open(format, bufferBytes)
                start()
            }
        }.getOrNull()
    }

    private fun loop() {
        val sampleRate = 22_050
        val frames = 1_024
        val audioLine = openAudioLine(sampleRate, frames * 8) ?: run {
            running = false
            return
        }
        line = audioLine

        val byteBuffer = ByteArray(frames * 2)
        val pumpSynth = PumpPulseSynth(sampleRate)
        var rotaryPhase = 0.0
        var creakPhase = 0.0
        var gasLow = 0.0
        var gasRumble = 0.0
        var noise = 0x1234567
        var alarmPhase = 0.0
        var currentGasLevel = 0.0

        while (running) {
            val now = System.currentTimeMillis()
            val pumpLevel = if (pumpChannel && pumpOn) (pumpSpm / 55.0).coerceIn(0.0, 1.6) else 0.0
            val rotaryLevel = if (rotaryChannel && rotaryOn && rpm > 2.0) {
                (rpm / 110.0).coerceIn(0.0, 1.6)
            } else 0.0
            val weightLevel = if (weightChannel) (wobKlb / 45.0).coerceIn(0.0, 1.0) else 0.0
            val targetGasLevel = if (gasChannel && gasAtSurface) 1.0 else 0.0
            currentGasLevel += (targetGasLevel - currentGasLevel) * 0.05
            if (currentGasLevel < 0.001) currentGasLevel = 0.0
            val alarmActive = now < alarmEndMs
            val rotaryFreq = 150.0 + rpm * 1.35
            val master = if (enabled) volume.coerceIn(0f, 1f) else 0f

            for (i in 0 until frames) {
                noise = noise * 1103515245 + 12345
                val white = ((noise shr 16) and 0x7fff) / 16384.0 - 1.0

                val pump = pumpSynth.next(pumpSpm)

                rotaryPhase += 2.0 * PI * rotaryFreq / sampleRate
                if (rotaryPhase > 2.0 * PI) rotaryPhase -= 2.0 * PI
                val rotary = sin(rotaryPhase) * 0.55 + sin(rotaryPhase * 2.01) * 0.18 +
                    sin(rotaryPhase * 0.5) * 0.12

                creakPhase += 2.0 * PI * (34.0 + 6.0 * sin(now / 1_700.0)) / sampleRate
                if (creakPhase > 2.0 * PI) creakPhase -= 2.0 * PI
                val creak = sin(creakPhase) * (0.5 + 0.5 * sin(now / 830.0)) +
                    white * 0.18

                gasLow += (white - gasLow) * 0.055
                gasRumble += (sin(2.0 * PI * 48.0 * (i / sampleRate.toDouble())) - gasRumble) * 0.02

                alarmPhase += 2.0 * PI * (if (((now / 180) % 2) == 0L) 880.0 else 660.0) / sampleRate
                if (alarmPhase > 2.0 * PI) alarmPhase -= 2.0 * PI
                val alarm = if (alarmActive) sin(alarmPhase) * 0.22 else 0.0

                val mix = pumpLevel * 0.52 * pump +
                    rotaryLevel * 0.075 * rotary +
                    weightLevel * 0.045 * creak +
                    currentGasLevel * (0.30 * gasLow + 0.18 * gasRumble) +
                    alarm
                val sample = (mix * master).coerceIn(-1.0, 1.0)
                val pcm = (sample * Short.MAX_VALUE).toInt()
                byteBuffer[i * 2] = (pcm and 0xFF).toByte()
                byteBuffer[i * 2 + 1] = ((pcm shr 8) and 0xFF).toByte()
            }

            val written = runCatching {
                audioLine.write(byteBuffer, 0, byteBuffer.size)
            }.getOrDefault(-1)
            if (written < 0) break
        }

        runCatching { audioLine.stop() }
        runCatching { audioLine.flush() }
        runCatching { audioLine.close() }
        line = null
    }

    companion object {
        /** Reproduz um tom curto de confirmação nos Ajustes (sem depender de RingtoneManager). */
        fun playTestTone(volume: Float = 0.8f) {
            Thread({
                val sampleRate = 22_050
                val durationMs = 320
                val frames = sampleRate * durationMs / 1_000
                val format = AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sampleRate.toFloat(),
                    16,
                    1,
                    2,
                    sampleRate.toFloat(),
                    false
                )
                runCatching {
                    val info = DataLine.Info(SourceDataLine::class.java, format)
                    if (!AudioSystem.isLineSupported(info)) return@runCatching
                    val line = AudioSystem.getLine(info) as SourceDataLine
                    line.open(format, frames * 2)
                    line.start()
                    val buf = ByteArray(frames * 2)
                    val gain = volume.coerceIn(0.05f, 1.0f) * 0.35
                    for (i in 0 until frames) {
                        val t = i.toDouble() / sampleRate
                        val freq = if (t < 0.14) 660.0 else 880.0
                        val env = exp(-t * 6.0)
                        val sample = (sin(2.0 * PI * freq * t) * env * gain).coerceIn(-1.0, 1.0)
                        val pcm = (sample * Short.MAX_VALUE).toInt()
                        buf[i * 2] = (pcm and 0xFF).toByte()
                        buf[i * 2 + 1] = ((pcm shr 8) and 0xFF).toByte()
                    }
                    line.write(buf, 0, buf.size)
                    line.drain()
                    line.stop()
                    line.close()
                }
            }, "rig-audio-test").apply {
                isDaemon = true
                start()
            }
        }
    }
}
