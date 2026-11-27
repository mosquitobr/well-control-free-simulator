package com.mosquitobr.controlepoco.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Motor de áudio operacional sintetizado em tempo real.
 *
 * Não usa arquivos de áudio nem permissões: gera bomba, rotação, peso da
 * coluna e gás na superfície em um único AudioTrack. O volume e os canais
 * são ajustáveis pelos Ajustes do app.
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
    @Volatile private var track: AudioTrack? = null

    @Synchronized
    fun start() {
        if (running) return
        running = true
        thread = Thread({ loop() }, "rig-audio").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY
            start()
        }
    }

    @Synchronized
    fun stop() {
        running = false
        val currentThread = thread
        thread = null
        runCatching { currentThread?.join(400) }
        val currentTrack = track
        track = null
        runCatching { currentTrack?.pause() }
        runCatching { currentTrack?.flush() }
        runCatching { currentTrack?.release() }
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

    private fun loop() {
        val sampleRate = 22_050
        val frames = 1_024
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(frames * 4)
        val audioTrack = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuffer)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return
        track = audioTrack
        runCatching { audioTrack.play() }

        val buffer = ShortArray(frames)
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

            for (i in buffer.indices) {
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
                buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }

            val written = runCatching {
                audioTrack.write(buffer, 0, buffer.size)
            }.getOrDefault(-1)
            if (written < 0) break
        }

        runCatching { audioTrack.pause() }
        runCatching { audioTrack.flush() }
        runCatching { audioTrack.release() }
        track = null
    }

    companion object {
        fun stream(): Int = AudioManager.STREAM_MUSIC
    }
}
